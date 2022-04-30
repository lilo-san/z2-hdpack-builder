package org.lilosan.tiled;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class Tile implements Comparable<Tile> {

    public static final String ORIGINAL_DATA_MD5 = "original-data-md5";

    private final BufferedImage bufferedImage;
    private final String originalDataMD5;


    public Tile(BufferedImage bufferedImage) throws Exception {
        this(bufferedImage, getMD5(getBytes(bufferedImage)));
    }

    public Tile(BufferedImage bufferedImage, String originalDataMD5) {
        this.bufferedImage = bufferedImage;
        this.originalDataMD5 = originalDataMD5;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public String getOriginalDataMd5() {
        return originalDataMD5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tile tile = (Tile) o;
        return originalDataMD5.equals(tile.originalDataMD5);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalDataMD5);
    }

    @Override
    public int compareTo(Tile o) {
        return originalDataMD5.compareTo(o.originalDataMD5);
    }

    //
    // Utility methods to obtain a unique identifier for the tile
    //

    private static byte[] getBytes(BufferedImage bufferedImage) {
        int[] pixels = bufferedImage.getRaster().getPixels(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), (int[]) null);
        ByteBuffer byteBuffer = ByteBuffer.allocate(4 * pixels.length);
        byteBuffer.order(ByteOrder.nativeOrder());
        for (int i = 0; i < pixels.length; ++i) {
            byteBuffer.putInt(pixels[i]);
        }
        return byteBuffer.array();
    }

    private static String getMD5(byte[] bytes) throws NoSuchAlgorithmException {
        byte[] messageDigest = MessageDigest.getInstance("MD5").digest(bytes);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < messageDigest.length; i++) {
            if ((0xff & messageDigest[i]) < 0x10) {
                sb.append('0');
            }
            sb.append(Integer.toHexString(0xff & messageDigest[i]));
        }
        String md5 = sb.toString();
        return md5;
    }

    //
    // Utility methods for other classes
    //

    public static Tile getEmptyTile(int width, int height) throws Exception {
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        return new Tile(bufferedImage);
    }
}
