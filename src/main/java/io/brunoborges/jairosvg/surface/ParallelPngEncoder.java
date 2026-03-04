package io.brunoborges.jairosvg.surface;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.Adler32;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * High-performance PNG encoder that uses virtual threads to compress image
 * strips in parallel. For images above a size threshold, the image is split
 * into horizontal strips that are deflate-compressed concurrently, then
 * assembled into a valid PNG stream.
 *
 * <p>
 * The parallel approach exploits the fact that a zlib stream is just a 2-byte
 * header, one or more raw deflate blocks, and a 4-byte Adler32 checksum.
 * Independent Deflater instances produce non-final blocks (via
 * {@code SYNC_FLUSH}) for all strips except the last, which uses
 * {@code FINISH}. Concatenating these yields a valid deflate stream.
 */
final class ParallelPngEncoder {

    /** Images smaller than this (in total pixels) use single-threaded encoding. */
    private static final int PARALLEL_THRESHOLD = 256 * 256;

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private ParallelPngEncoder() {
    }

    /**
     * Encode a BufferedImage as PNG and write to the given output stream.
     *
     * @param image
     *            the image to encode (must be TYPE_INT_ARGB or compatible)
     * @param out
     *            destination stream
     * @param compressionLevel
     *            0-9 (0=fastest, 9=smallest), or -1 for default (maps to 4)
     */
    static void encode(BufferedImage image, OutputStream out, int compressionLevel) throws IOException {
        int width = image.getWidth();
        int height = image.getHeight();
        int level = compressionLevel < 0 ? 4 : compressionLevel;

        // Step 1: Extract pixels and apply row filtering
        int bytesPerPixel = 4; // RGBA
        int rawRowLen = width * bytesPerPixel;
        int filteredRowLen = 1 + rawRowLen; // filter byte + pixel data
        byte[] filteredData = new byte[height * filteredRowLen];

        filterRows(image, filteredData, width, height, bytesPerPixel, rawRowLen, filteredRowLen);

        // Step 2: Compress (parallel for large images, single-threaded for small)
        byte[] compressedData;
        long totalPixels = (long) width * height;
        if (totalPixels >= PARALLEL_THRESHOLD) {
            compressedData = compressParallel(filteredData, filteredRowLen, height, level);
        } else {
            compressedData = compressSingleThreaded(filteredData, level);
        }

        // Step 3: Write PNG file
        writePngFile(out, width, height, compressedData);
    }

    /**
     * Apply Sub filter (type 1) to all rows. Sub filter encodes each byte as the
     * difference from the corresponding byte in the previous pixel (same row). It's
     * fast to compute and provides good compression for typical rendered SVG
     * output.
     */
    private static void filterRows(BufferedImage image, byte[] filtered, int width, int height, int bpp, int rawRowLen,
            int filteredRowLen) {
        int[] rowPixels = new int[width];
        byte[] rawRow = new byte[rawRowLen];

        for (int y = 0; y < height; y++) {
            image.getRGB(0, y, width, 1, rowPixels, 0, width);

            // Convert ARGB int[] to RGBA byte[]
            for (int x = 0; x < width; x++) {
                int argb = rowPixels[x];
                int base = x * bpp;
                rawRow[base] = (byte) ((argb >> 16) & 0xFF); // R
                rawRow[base + 1] = (byte) ((argb >> 8) & 0xFF); // G
                rawRow[base + 2] = (byte) (argb & 0xFF); // B
                rawRow[base + 3] = (byte) ((argb >> 24) & 0xFF); // A
            }

            int offset = y * filteredRowLen;
            filtered[offset] = 1; // Sub filter type

            // First pixel: Sub(x) = Raw(x) - 0 = Raw(x)
            System.arraycopy(rawRow, 0, filtered, offset + 1, bpp);

            // Remaining pixels: Sub(x) = Raw(x) - Raw(x - bpp)
            for (int i = bpp; i < rawRowLen; i++) {
                filtered[offset + 1 + i] = (byte) ((rawRow[i] & 0xFF) - (rawRow[i - bpp] & 0xFF));
            }
        }
    }

    /** Single-threaded zlib compression. */
    private static byte[] compressSingleThreaded(byte[] data, int level) {
         // raw deflate, no zlib wrapper
        try (var deflater = new Deflater(level, true);) {
            deflater.setInput(data);
            deflater.finish();

            ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length / 2);

            // Zlib header
            writeZlibHeader(baos, level);

            // Deflate
            byte[] buf = new byte[32768];
            while (!deflater.finished()) {
                int n = deflater.deflate(buf);
                if (n > 0)
                    baos.write(buf, 0, n);
            }

            // Adler32
            writeAdler32(baos, data);

            return baos.toByteArray();
        }
    }

    /** Parallel zlib compression using virtual threads. */
    private static byte[] compressParallel(byte[] filteredData, int filteredRowLen, int height, int level) {
        int numStrips = Math.min(height, Runtime.getRuntime().availableProcessors());
        if (numStrips <= 1) {
            return compressSingleThreaded(filteredData, level);
        }

        int rowsPerStrip = (height + numStrips - 1) / numStrips;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<byte[]>> futures = new ArrayList<>(numStrips);

            for (int s = 0; s < numStrips; s++) {
                int startRow = s * rowsPerStrip;
                int endRow = Math.min(startRow + rowsPerStrip, height);
                if (startRow >= height)
                    break;

                boolean isLast = (endRow >= height);
                int start = startRow * filteredRowLen;
                int len = (endRow - startRow) * filteredRowLen;

                futures.add(executor.submit(() -> compressStrip(filteredData, start, len, level, isLast)));
            }

            // Assemble zlib stream
            ByteArrayOutputStream baos = new ByteArrayOutputStream(filteredData.length / 2);
            writeZlibHeader(baos, level);

            for (Future<byte[]> future : futures) {
                baos.write(future.get());
            }

            writeAdler32(baos, filteredData);

            return baos.toByteArray();
        } catch (Exception e) {
            // Fall back to single-threaded on any error
            return compressSingleThreaded(filteredData, level);
        }
    }

    /**
     * Compress a single strip using raw deflate (no zlib wrapper). Non-final strips
     * use SYNC_FLUSH to produce non-final deflate blocks. The last strip uses
     * FINISH to produce the final block with BFINAL=1.
     */
    private static byte[] compressStrip(byte[] data, int offset, int length, int level, boolean isLast) {
        Deflater deflater = new Deflater(level, true); // raw deflate, no zlib wrapper
        try {
            deflater.setInput(data, offset, length);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(length / 2);
            byte[] buf = new byte[32768];

            if (isLast) {
                deflater.finish();
                while (!deflater.finished()) {
                    int n = deflater.deflate(buf);
                    if (n > 0)
                        baos.write(buf, 0, n);
                }
            } else {
                // SYNC_FLUSH produces non-final blocks
                int n = deflater.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
                baos.write(buf, 0, n);
                // Handle large strips that don't fit in one buffer
                while (!deflater.needsInput()) {
                    n = deflater.deflate(buf, 0, buf.length, Deflater.SYNC_FLUSH);
                    if (n > 0)
                        baos.write(buf, 0, n);
                    else
                        break;
                }
            }

            return baos.toByteArray();
        } finally {
            deflater.end();
        }
    }

    /** Write 2-byte zlib header (CMF + FLG). */
    private static void writeZlibHeader(ByteArrayOutputStream baos, int level) {
        int cmf = 0x78; // deflate, window size 32KB
        // FLEVEL: 0=fastest, 1=fast, 2=default, 3=max
        int flevel = level <= 1 ? 0 : level <= 5 ? 1 : level <= 7 ? 2 : 3;
        int flg = (flevel << 6);
        // FCHECK: make (CMF*256 + FLG) divisible by 31
        int remainder = (cmf * 256 + flg) % 31;
        if (remainder != 0)
            flg += (31 - remainder);
        baos.write(cmf);
        baos.write(flg);
    }

    /** Compute and write 4-byte Adler32 checksum over uncompressed data. */
    private static void writeAdler32(ByteArrayOutputStream baos, byte[] data) {
        Adler32 adler = new Adler32();
        adler.update(data);
        long checksum = adler.getValue();
        baos.write((int) (checksum >> 24) & 0xFF);
        baos.write((int) (checksum >> 16) & 0xFF);
        baos.write((int) (checksum >> 8) & 0xFF);
        baos.write((int) checksum & 0xFF);
    }

    /** Write complete PNG file structure. */
    private static void writePngFile(OutputStream out, int width, int height, byte[] compressedData)
            throws IOException {
        out.write(PNG_SIGNATURE);

        // IHDR chunk
        byte[] ihdr = new byte[13];
        writeInt(ihdr, 0, width);
        writeInt(ihdr, 4, height);
        ihdr[8] = 8; // bit depth
        ihdr[9] = 6; // color type: RGBA
        ihdr[10] = 0; // compression method
        ihdr[11] = 0; // filter method
        ihdr[12] = 0; // interlace method
        writeChunk(out, "IHDR", ihdr);

        // IDAT chunk(s) — split into 64KB chunks for compatibility
        int offset = 0;
        int chunkSize = 65536;
        while (offset < compressedData.length) {
            int len = Math.min(chunkSize, compressedData.length - offset);
            writeChunk(out, "IDAT", compressedData, offset, len);
            offset += len;
        }

        // IEND chunk
        writeChunk(out, "IEND", new byte[0]);
    }

    /** Write a PNG chunk: length + type + data + CRC32. */
    private static void writeChunk(OutputStream out, String type, byte[] data) throws IOException {
        writeChunk(out, type, data, 0, data.length);
    }

    private static void writeChunk(OutputStream out, String type, byte[] data, int offset, int length)
            throws IOException {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        // Length (4 bytes, big-endian)
        byte[] lenBuf = new byte[4];
        writeInt(lenBuf, 0, length);
        out.write(lenBuf);

        // Type (4 bytes)
        out.write(typeBytes);

        // Data
        out.write(data, offset, length);

        // CRC32 over type + data
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data, offset, length);
        byte[] crcBuf = new byte[4];
        writeInt(crcBuf, 0, (int) crc.getValue());
        out.write(crcBuf);
    }

    /** Write a 32-bit big-endian integer. */
    private static void writeInt(byte[] buf, int offset, int value) {
        buf[offset] = (byte) (value >> 24);
        buf[offset + 1] = (byte) (value >> 16);
        buf[offset + 2] = (byte) (value >> 8);
        buf[offset + 3] = (byte) value;
    }
}
