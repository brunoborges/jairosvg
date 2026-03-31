package io.brunoborges.jairosvg.draw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Gaussian blur via 3-pass box blur approximation (W3C recommended).
 *
 * @see <a href="http://blog.ivank.net/fastest-gaussian-blur.html">Fastest
 *      Gaussian Blur</a>
 */
final class GaussianBlur {

    private GaussianBlur() {
    }

    static BufferedImage apply(BufferedImage input, double stdDeviation, BufferedImage temp, BufferedImage output) {
        if (stdDeviation <= 0) {
            return input;
        }
        int w = input.getWidth();
        int h = input.getHeight();
        int[] radii = boxRadii(stdDeviation);
        int[] src = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] tmp = ((DataBufferInt) temp.getRaster().getDataBuffer()).getData();
        int[] dst = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        boxBlurH(src, tmp, w, h, radii[0]);
        boxBlurV(tmp, dst, w, h, radii[0]);
        boxBlurH(dst, tmp, w, h, radii[1]);
        boxBlurV(tmp, dst, w, h, radii[1]);
        boxBlurH(dst, tmp, w, h, radii[2]);
        boxBlurV(tmp, dst, w, h, radii[2]);
        return output;
    }

    private static int[] boxRadii(double sigma) {
        double ideal = Math.sqrt((12 * sigma * sigma / 3) + 1);
        int wl = (int) Math.floor(ideal);
        if (wl % 2 == 0) {
            wl--;
        }
        int wu = wl + 2;
        int m = (int) Math.round((12 * sigma * sigma - 3.0 * wl * wl - 12.0 * wl - 9) / (-4.0 * wl - 4));
        return new int[]{(m > 0 ? wl : wu) / 2, (m > 1 ? wl : wu) / 2, (m > 2 ? wl : wu) / 2};
    }

    private static void boxBlurH(int[] src, int[] dst, int w, int h, int r) {
        if (r <= 0) {
            System.arraycopy(src, 0, dst, 0, src.length);
            return;
        }
        int boxSize = r + r + 1;
        int scale = (1 << 24) / boxSize;
        for (int y = 0; y < h; y++) {
            int off = y * w;
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int px = src[off + Math.min(Math.max(i, 0), w - 1)];
                sa += (px >>> 24);
                sr += (px >> 16) & 0xFF;
                sg += (px >> 8) & 0xFF;
                sb += px & 0xFF;
            }
            int prev = dst[off] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                    | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
            for (int x = 1; x < w; x++) {
                int addPx = src[off + Math.min(x + r, w - 1)];
                int remPx = src[off + Math.max(x - r - 1, 0)];
                if (addPx == remPx) {
                    dst[off + x] = prev;
                } else {
                    sa += (addPx >>> 24) - (remPx >>> 24);
                    sr += ((addPx >> 16) & 0xFF) - ((remPx >> 16) & 0xFF);
                    sg += ((addPx >> 8) & 0xFF) - ((remPx >> 8) & 0xFF);
                    sb += (addPx & 0xFF) - (remPx & 0xFF);
                    prev = dst[off + x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                            | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
                }
            }
        }
    }

    private static void boxBlurV(int[] src, int[] dst, int w, int h, int r) {
        if (r <= 0) {
            System.arraycopy(src, 0, dst, 0, src.length);
            return;
        }
        int boxSize = r + r + 1;
        int scale = (1 << 24) / boxSize;
        for (int x = 0; x < w; x++) {
            int sa = 0, sr = 0, sg = 0, sb = 0;
            for (int i = -r; i <= r; i++) {
                int px = src[Math.min(Math.max(i, 0), h - 1) * w + x];
                sa += (px >>> 24);
                sr += (px >> 16) & 0xFF;
                sg += (px >> 8) & 0xFF;
                sb += px & 0xFF;
            }
            int prev = dst[x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                    | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
            for (int y = 1; y < h; y++) {
                int addPx = src[Math.min(y + r, h - 1) * w + x];
                int remPx = src[Math.max(y - r - 1, 0) * w + x];
                if (addPx == remPx) {
                    dst[y * w + x] = prev;
                } else {
                    sa += (addPx >>> 24) - (remPx >>> 24);
                    sr += ((addPx >> 16) & 0xFF) - ((remPx >> 16) & 0xFF);
                    sg += ((addPx >> 8) & 0xFF) - ((remPx >> 8) & 0xFF);
                    sb += (addPx & 0xFF) - (remPx & 0xFF);
                    prev = dst[y * w + x] = ((sa * scale) & 0xFF000000) | (((sr * scale) & 0xFF000000) >>> 8)
                            | (((sg * scale) & 0xFF000000) >>> 16) | (((sb * scale) & 0xFF000000) >>> 24);
                }
            }
        }
    }
}
