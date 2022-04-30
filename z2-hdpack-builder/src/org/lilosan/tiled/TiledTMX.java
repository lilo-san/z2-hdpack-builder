package org.lilosan.tiled;

import org.w3c.dom.Node;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

public class TiledTMX {

    private final int scaleFactor;
    private final String tileSetName;
    private final Integer[][] map;
    private final int tileWidth;
    private final int tileHeight;

    public static TiledTMX getInstance(File file) throws Exception {
        String source = getSource(file);
        int tilewidth = getTileWidth(file);
        int tileHeight = getTileHeight(file);
        Integer[][] csv = getCSV(file);
        TiledTMX tmx = new TiledTMX(1, source, csv.length, csv[0].length,  tilewidth, tileHeight);
        for (int y = 0; y < csv[0].length; y++) {
            for (int x = 0; x < csv.length; x++) {
                tmx.add(x, y, csv[x][y]);
            }
        }
        return tmx;
    }

    public TiledTMX(int scaleFactor, String tileSetName, int mapTileWidth, int mapTileHeight, int tileWidth, int tileHeight) {
        this.scaleFactor = scaleFactor;
        this.tileSetName = tileSetName;
        this.map = new Integer[mapTileWidth][mapTileHeight];
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public int getSize() {
        return map.length * map[0].length;
    }

    public void add(int x, int y, int id) {
        this.map[x][y] = id;
    }

    public String getCSV() {
        StringBuilder csv = new StringBuilder();
        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {
                if (x > 0) {
                    csv.append(",");
                }
                csv.append(map[x][y]);
            }
            if (y < map[0].length -1) {
                csv.append(",");
            }
            csv.append("\n");
        }
        return csv.toString();
    }

    public BufferedImage getBufferedImage(TiledTSX tsx, int yOffset) {
        BufferedImage before = new BufferedImage(map.length * tsx.getTileWidth(), yOffset + map[0].length * tsx.getTileHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < map[0].length; y++) {
            for (int x = 0; x < map.length; x++) {
                Integer tileId = map[x][y];
                BufferedImage tileImage = tsx.getTile(tileId).getBufferedImage();
                int[] argb = tileImage.getRGB(0, 0, tsx.getTileWidth(), tsx.getTileHeight(), null, 0, tsx.getTileWidth());
                before.setRGB(x * tsx.getTileWidth(), yOffset + y * tsx.getTileHeight(), tsx.getTileWidth(), tsx.getTileHeight(), argb, 0, tsx.getTileWidth());
            }
        }

        BufferedImage after = null;
        if (scaleFactor == 1) {
            after = before;
        } else {
            after = new BufferedImage(before.getWidth() * scaleFactor, before.getHeight() * scaleFactor, BufferedImage.TYPE_INT_ARGB);
            AffineTransform at = new AffineTransform();
            at.scale(scaleFactor, scaleFactor);
            AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            after = scaleOp.filter(before, after);
        }
        return after;
    }

    public String getTMX() {
        StringBuilder tmx = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        tmx.append("<map version=\"1.8\" tiledversion=\"1.8.2\" orientation=\"orthogonal\" renderorder=\"right-down\" width=\"").append(map.length).append("\" height=\"").append(map[0].length).append("\" tilewidth=\"").append(tileWidth * scaleFactor).append("\" tileheight=\"").append(tileHeight * scaleFactor).append("\" infinite=\"0\" nextlayerid=\"6\" nextobjectid=\"1\">\n");
        tmx.append(" <tileset firstgid=\"0\" source=\"").append(tileSetName).append(".tsx\"/>\n");
        tmx.append(" <layer id=\"5\" name=\"base\" width=\"").append(map.length).append("\" height=\"").append(map[0].length).append("\">\n");
        tmx.append("  <data encoding=\"csv\">\n");
        tmx.append(getCSV());
        tmx.append("</data>\n");
        tmx.append(" </layer>\n");
        tmx.append("</map>\n");
        return tmx.toString();
    }

    //
    // Utility methods to read TMX
    //

    public static Integer[][] getCSV(File file) throws Exception {
        String csv = getCsvAsString(file);
        String[] lines = csv.trim().split("\n");
        Integer[][] data = null;
        for (int y = 0; y < lines.length; y++) {
            String[] line = lines[y].split(",");
            if (data == null) {
                data = new Integer[line.length][lines.length];
            }
            for (int x = 0; x < line.length; x++) {
                data[x][y] = Integer.parseInt(line[x]);
            }
        }
        return data;
    }

    private static String getCsvAsString(File file) throws Exception {
        Node map = XMLUtils.getRootNode(file, "map");
        Node layer = XMLUtils.getChildTag(map, "layer");
        Node data = XMLUtils.getChildTag(layer, "data");
        String encoding = XMLUtils.getAttribute(data, "encoding");
        if (encoding.equals("csv")) {
            return data.getFirstChild().getNodeValue();
        }
        throw new Exception("CSV not found");
    }

    private static String getSource(File file) throws Exception {
        Node map = XMLUtils.getRootNode(file, "map");
        Node tileset = XMLUtils.getChildTag(map, "tileset");
        return XMLUtils.getAttribute(tileset, "source");
    }

    public static int getTileWidth(File file) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(XMLUtils.getRootNode(file, "map"), "tilewidth"));
    }

    public static int getTileHeight(File file) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(XMLUtils.getRootNode(file, "map"), "tileheight"));
    }
}
