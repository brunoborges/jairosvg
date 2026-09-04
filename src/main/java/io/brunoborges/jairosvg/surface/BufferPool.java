package io.brunoborges.jairosvg.surface;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local pool of {@link BufferedImage} instances (TYPE_INT_ARGB) keyed by
 * dimensions, used to recycle the backing {@code int[]} pixel arrays of render
 * targets that are fully consumed internally (e.g. the output image of the
 * {@code byte[]}-returning conversion paths, which is rendered then encoded and
 * discarded).
 *
 * <p>
 * Pooling is only safe when the image never escapes to the caller. Paths that
 * hand the rendered {@link BufferedImage} back to the caller (e.g.
 * {@code ConversionBuilder.toImage()}) must not use this pool.
 *
 * <p>
 * Rationale: rendering the same-sized SVG repeatedly (batch/server workloads)
 * otherwise allocates a fresh multi-megabyte {@code int[]} per call, dominating
 * the allocation profile. Recycling those buffers removes that churn without
 * changing rendering semantics — buffers are cleared to fully transparent on
 * acquire.
 *
 * <p>
 * Retention is bounded by a per-thread byte budget rather than a fixed count of
 * size classes: batch workloads that interleave several distinct output sizes
 * (e.g. converting a directory of differently-sized documents) keep their whole
 * working set pooled as long as it fits the budget, instead of permanently
 * dropping every size beyond an arbitrary limit. When the budget is exceeded
 * the least-recently-used size class is evicted first.
 */
final class BufferPool {

    /**
     * Per-thread retention budget in bytes for pooled backing arrays. Sized to
     * comfortably hold the working set of a typical multi-size batch while keeping
     * memory bounded across many render threads. A single buffer larger than this
     * is never pooled.
     */
    private static final long MAX_BYTES = 32L * 1024 * 1024;

    /** Cap the number of buffers retained per distinct size. */
    private static final int MAX_PER_SIZE = 2;

    private static final ThreadLocal<BufferPool> LOCAL = ThreadLocal.withInitial(BufferPool::new);

    /** Access-ordered so the eldest entry is the least-recently-used size class. */
    private final Map<Long, ArrayDeque<BufferedImage>> bySize = new LinkedHashMap<>(16, 0.75f, true);

    private long retainedBytes;

    private BufferPool() {
    }

    private static long key(int w, int h) {
        return (((long) w) << 32) | (h & 0xFFFFFFFFL);
    }

    private static long bytesOf(int w, int h) {
        return (long) w * h * Integer.BYTES;
    }

    /**
     * Borrow a cleared {@code TYPE_INT_ARGB} image of the requested size, reusing a
     * pooled backing array when available.
     */
    static BufferedImage acquire(int w, int h) {
        BufferPool pool = LOCAL.get();
        // get() marks this size class as most-recently-used under access ordering.
        ArrayDeque<BufferedImage> free = pool.bySize.get(key(w, h));
        if (free != null) {
            BufferedImage img = free.pollFirst();
            if (img != null) {
                pool.retainedBytes -= bytesOf(w, h);
                if (free.isEmpty()) {
                    pool.bySize.remove(key(w, h));
                }
                clear(img);
                return img;
            }
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Return an image previously obtained from {@link #acquire(int, int)} to the
     * pool for reuse. Images with an incompatible layout are dropped, as are
     * buffers larger than the whole budget.
     */
    static void release(BufferedImage img) {
        if (img == null || img.getType() != BufferedImage.TYPE_INT_ARGB) {
            return;
        }
        int w = img.getWidth();
        int h = img.getHeight();
        long bytes = bytesOf(w, h);
        if (bytes > MAX_BYTES) {
            return;
        }
        BufferPool pool = LOCAL.get();
        ArrayDeque<BufferedImage> free = pool.bySize.get(key(w, h));
        if (free == null) {
            free = new ArrayDeque<>();
            pool.bySize.put(key(w, h), free);
        }
        if (free.size() >= MAX_PER_SIZE) {
            return;
        }
        free.addLast(img);
        pool.retainedBytes += bytes;
        pool.evictToBudget(key(w, h));
    }

    /** Evict least-recently-used size classes until within the byte budget. */
    private void evictToBudget(long keepKey) {
        while (retainedBytes > MAX_BYTES) {
            Iterator<Map.Entry<Long, ArrayDeque<BufferedImage>>> it = bySize.entrySet().iterator();
            if (!it.hasNext()) {
                break;
            }
            Map.Entry<Long, ArrayDeque<BufferedImage>> eldest = it.next();
            // Never evict the size we just released (it is the hot one); a buffer
            // that alone exceeds the budget was already rejected in release().
            if (eldest.getKey() == keepKey && bySize.size() == 1) {
                break;
            }
            ArrayDeque<BufferedImage> dq = eldest.getValue();
            BufferedImage victim = dq.pollFirst();
            if (victim != null) {
                retainedBytes -= bytesOf(victim.getWidth(), victim.getHeight());
            }
            if (dq.isEmpty()) {
                it.remove();
            }
        }
    }

    private static void clear(BufferedImage img) {
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        Arrays.fill(px, 0);
    }
}
