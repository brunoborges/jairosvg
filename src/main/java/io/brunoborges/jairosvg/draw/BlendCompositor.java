package io.brunoborges.jairosvg.draw;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * SVG feBlend pixel compositing engine. Supports normal, multiply, screen,
 * darken, and lighten blend modes with optimized bulk pixel loops.
 */
final class BlendCompositor {

    private static final int BLEND_NORMAL = 0;
    private static final int BLEND_MULTIPLY = 1;
    private static final int BLEND_SCREEN = 2;
    private static final int BLEND_DARKEN = 3;
    private static final int BLEND_LIGHTEN = 4;

    // Precomputed reciprocals for alpha un-premultiply: ALPHA_RECIP[a] ≈
    // (1<<32)/(a*255).
    // Replaces per-channel integer division with multiply+shift in the blend loop.
    private static final long[] ALPHA_RECIP = new long[256];
    static {
        for (int a = 1; a <= 255; a++) {
            long divisor = a * 255L;
            ALPHA_RECIP[a] = ((1L << 32) + divisor - 1) / divisor;
        }
    }

    private BlendCompositor() {
    }

    static BufferedImage blend(BufferedImage input, BufferedImage destination, String mode, BufferedImage output) {
        int width = Math.max(input.getWidth(), destination.getWidth());
        int height = Math.max(input.getHeight(), destination.getHeight());
        boolean sameDimensions = input.getWidth() == width && input.getHeight() == height
                && destination.getWidth() == width && destination.getHeight() == height;

        if (sameDimensions) {
            return blendDirect(input, destination, blendModeId(mode), width, height, output);
        }

        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            java.util.Arrays.fill(((DataBufferInt) output.getRaster().getDataBuffer()).getData(), 0);
        }
        int modeId = blendModeId(mode);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int src = x < input.getWidth() && y < input.getHeight() ? input.getRGB(x, y) : 0;
                int dst = x < destination.getWidth() && y < destination.getHeight() ? destination.getRGB(x, y) : 0;
                output.setRGB(x, y, blendPixel(src, dst, modeId));
            }
        }
        return output;
    }

    private static int blendModeId(String mode) {
        return switch (mode) {
            case "multiply" -> BLEND_MULTIPLY;
            case "screen" -> BLEND_SCREEN;
            case "darken" -> BLEND_DARKEN;
            case "lighten" -> BLEND_LIGHTEN;
            default -> BLEND_NORMAL;
        };
    }

    private static BufferedImage blendDirect(BufferedImage input, BufferedImage destination, int modeId, int width,
            int height, BufferedImage output) {
        int[] srcPixels = ((DataBufferInt) input.getRaster().getDataBuffer()).getData();
        int[] dstPixels = ((DataBufferInt) destination.getRaster().getDataBuffer()).getData();
        if (output == null || output.getWidth() != width || output.getHeight() != height) {
            output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        int[] outPixels = ((DataBufferInt) output.getRaster().getDataBuffer()).getData();
        int len = srcPixels.length;

        switch (modeId) {
            case BLEND_NORMAL -> blendLoopNormal(srcPixels, dstPixels, outPixels, len);
            case BLEND_MULTIPLY -> blendLoopMultiply(srcPixels, dstPixels, outPixels, len);
            case BLEND_SCREEN -> blendLoopScreen(srcPixels, dstPixels, outPixels, len);
            case BLEND_DARKEN -> blendLoopDarken(srcPixels, dstPixels, outPixels, len);
            case BLEND_LIGHTEN -> blendLoopLighten(srcPixels, dstPixels, outPixels, len);
            default -> blendLoopNormal(srcPixels, dstPixels, outPixels, len);
        }
        return output;
    }

    private static void blendLoopNormal(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int srcA255 = srcA * 255;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + srcA255 * srcR) * recip >>> 32);
            int outG = (int) ((f1 * dstG + srcA255 * srcG) * recip >>> 32);
            int outB = (int) ((f1 * dstB + srcA255 * srcB) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopMultiply(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * ((dstR * srcR + 127) / 255)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * ((dstG * srcG + 127) / 255)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * ((dstB * srcB + 127) / 255)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopScreen(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * (dstR + srcR - (dstR * srcR + 127) / 255)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * (dstG + srcG - (dstG * srcG + 127) / 255)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * (dstB + srcB - (dstB * srcB + 127) / 255)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopDarken(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * Math.min(dstR, srcR)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * Math.min(dstG, srcG)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * Math.min(dstB, srcB)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static void blendLoopLighten(int[] srcPixels, int[] dstPixels, int[] outPixels, int len) {
        for (int i = 0; i < len; i++) {
            int src = srcPixels[i];
            int dst = dstPixels[i];
            int srcA = src >>> 24;
            int dstA = dst >>> 24;
            if ((srcA | dstA) == 0)
                continue;

            int outA = srcA + dstA - (srcA * dstA + 127) / 255;
            if (outA <= 0)
                continue;

            int srcR = (src >> 16) & 0xFF, srcG = (src >> 8) & 0xFF, srcB = src & 0xFF;
            int dstR = (dst >> 16) & 0xFF, dstG = (dst >> 8) & 0xFF, dstB = dst & 0xFF;

            int f1 = (255 - srcA) * dstA;
            int f2 = (255 - dstA) * srcA;
            int f3 = srcA * dstA;
            long recip = ALPHA_RECIP[outA];
            int outR = (int) ((f1 * dstR + f2 * srcR + f3 * Math.max(dstR, srcR)) * recip >>> 32);
            int outG = (int) ((f1 * dstG + f2 * srcG + f3 * Math.max(dstG, srcG)) * recip >>> 32);
            int outB = (int) ((f1 * dstB + f2 * srcB + f3 * Math.max(dstB, srcB)) * recip >>> 32);

            outPixels[i] = (outA << 24) | (outR << 16) | (outG << 8) | outB;
        }
    }

    private static int blendPixel(int src, int dst, int modeId) {
        int srcA = (src >>> 24) & 0xFF;
        int dstA = (dst >>> 24) & 0xFF;
        if ((srcA | dstA) == 0) {
            return 0;
        }

        int srcR = (src >>> 16) & 0xFF;
        int srcG = (src >>> 8) & 0xFF;
        int srcB = src & 0xFF;
        int dstR = (dst >>> 16) & 0xFF;
        int dstG = (dst >>> 8) & 0xFF;
        int dstB = dst & 0xFF;

        int outA = srcA + dstA - (srcA * dstA + 127) / 255;
        if (outA <= 0) {
            return 0;
        }

        int outR = blendComposite(srcR, dstR, srcA, dstA, outA, modeId);
        int outG = blendComposite(srcG, dstG, srcA, dstA, outA, modeId);
        int outB = blendComposite(srcB, dstB, srcA, dstA, outA, modeId);

        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }

    private static int blendComposite(int src, int dst, int srcA, int dstA, int outA, int modeId) {
        int blended = switch (modeId) {
            case BLEND_MULTIPLY -> (dst * src + 127) / 255;
            case BLEND_SCREEN -> dst + src - (dst * src + 127) / 255;
            case BLEND_DARKEN -> Math.min(dst, src);
            case BLEND_LIGHTEN -> Math.max(dst, src);
            default -> src;
        };
        int premul = (255 - srcA) * dstA * dst + (255 - dstA) * srcA * src + srcA * dstA * blended;
        int result = (premul + outA * 255 / 2) / (outA * 255);
        return Math.clamp(result, 0, 255);
    }
}
