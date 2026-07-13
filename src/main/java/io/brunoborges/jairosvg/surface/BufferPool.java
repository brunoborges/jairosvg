package io.brunoborges.jairosvg.surface;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
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
 */
final class BufferPool {

    /** Cap the number of distinct sizes retained per thread to bound memory. */
    private static final int MAX_SIZES = 8;

    /** Cap the number of buffers retained per distinct size. */
    private static final int MAX_PER_SIZE = 2;

    private static final ThreadLocal<BufferPool> LOCAL = ThreadLocal.withInitial(BufferPool::new);

    private final Map<Long, ArrayDeque<BufferedImage>> bySize = new HashMap<>();

    private BufferPool() {
    }

    private static long key(int w, int h) {
        return (((long) w) << 32) | (h & 0xFFFFFFFFL);
    }

    /**
     * Borrow a cleared {@code TYPE_INT_ARGB} image of the requested size, reusing a
     * pooled backing array when available.
     */
    static BufferedImage acquire(int w, int h) {
        BufferPool pool = LOCAL.get();
        ArrayDeque<BufferedImage> free = pool.bySize.get(key(w, h));
        if (free != null) {
            BufferedImage img = free.pollFirst();
            if (img != null) {
                clear(img);
                return img;
            }
        }
        return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    /**
     * Return an image previously obtained from {@link #acquire(int, int)} to the
     * pool for reuse. Images with an incompatible layout are dropped.
     */
    static void release(BufferedImage img) {
        if (img == null || img.getType() != BufferedImage.TYPE_INT_ARGB) {
            return;
        }
        BufferPool pool = LOCAL.get();
        if (pool.bySize.size() >= MAX_SIZES && !pool.bySize.containsKey(key(img.getWidth(), img.getHeight()))) {
            return;
        }
        ArrayDeque<BufferedImage> free = pool.bySize.computeIfAbsent(key(img.getWidth(), img.getHeight()),
                k -> new ArrayDeque<>());
        if (free.size() < MAX_PER_SIZE) {
            free.addLast(img);
        }
    }

    private static void clear(BufferedImage img) {
        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        Arrays.fill(px, 0);
    }
}
