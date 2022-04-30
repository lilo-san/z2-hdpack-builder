package org.lilosan.tiled;

import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.util.*;

public class TiledTSX {

    private final Map<String, Tile> tilesByOriginalDataMd5;
    private final Map<String, Integer> idByOriginalDataMd5;
    private final Map<Integer, Tile> tilesById;

    private final String name;
    private final int scaleFactor;
    private final int tileWidth;
    private final int tileHeight;
    private final int columns;

    public static TiledTSX getInstance(File file) throws Exception {
        String name = getName(file);
        String source = getSource(file);
        int columns = getColumns(file);
        int tileWidth = getTileWidth(file);
        int tileHeight = getTileHeight(file);

        // Load metadata
        Map<Integer, String> physicalIdToMD5 = new HashMap<>();
        List<Node> tilesMetadata = getTiles(file);
        for (int logicalId = 0; logicalId < tilesMetadata.size(); logicalId++) {
            int physicalId = getId(tilesMetadata.get(logicalId));
            String md5 = getOriginalDataMD5(tilesMetadata.get(logicalId));
            if (md5 != null) {
                physicalIdToMD5.put(physicalId, md5);
            }
        }

        // Load tiles
        TiledTSX tsx = new TiledTSX(name, 1, tileWidth, tileHeight, columns);
        BufferedImage tilesImages = ImageIO.read(new File(file.getParent() + "/" + source));
        Raster raster = tilesImages.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int physicalId = 0;
        for(int y = 0; y < height; y += tileHeight) {
            for (int x = 0; x < width; x += tileWidth) {
                BufferedImage tileImage = tilesImages.getSubimage(x, y, tileWidth, tileHeight);
                tsx.add( new Tile(tileImage, physicalIdToMD5.get(physicalId)), true);
                physicalId++;
            }
        }

        return tsx;
    }

    public TiledTSX(String name, int scaleFactor, int tileWidth, int tileHeight, int columns) {
        this.name = name;
        this.scaleFactor = scaleFactor;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.tilesByOriginalDataMd5 = new HashMap<>();
        this.tilesById = new HashMap<>();
        this.idByOriginalDataMd5 = new HashMap<>();
        this.columns = columns;
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public int getTileHeight() {
        return tileHeight;
    }

    public int getWidth() {
        return columns * getTileWidth();
    }

    public int getHeight() {
        return (tilesById.size() / columns + ((tilesById.size()%columns > 0)?1:0)) * getTileHeight();
    }

    public String getName() {
        return name;
    }

    public int size() {
        return tilesById.size();
    }

    public Integer add(Tile tile) {
        return add(tile, false);
    }

    public Integer add(Tile tile, boolean allowDuplicated) {
        Integer id;
        if (!allowDuplicated && tilesByOriginalDataMd5.containsKey(tile.getOriginalDataMd5())) {
            id = idByOriginalDataMd5.get(tile.getOriginalDataMd5());
        } else {
            id = tilesById.size();
            tilesById.put(id, tile);
            if (!allowDuplicated) {
                tilesByOriginalDataMd5.put(tile.getOriginalDataMd5(), tile);
                idByOriginalDataMd5.put(tile.getOriginalDataMd5(), id);
            }
        }
        return id;
    }

    public Tile getTile(Integer id) {
        return tilesById.get(id);
    }

    public BufferedImage getBufferedImage() {
        BufferedImage before = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        int x = 0;
        int y = 0;
        for (Integer id:tilesById.keySet()) {
            BufferedImage tileImage = tilesById.get(id).getBufferedImage();
            int[] argb = tileImage.getRGB(0, 0, tileWidth, tileHeight, null, 0, tileWidth);
            before.setRGB(x, y, tileWidth, tileHeight, argb, 0, tileWidth);

            if (x == getWidth() - tileWidth) { // If last tile in the row has been written
                x = 0; // start a new row
                y += tileHeight;
            } else { // If not
                x += tileWidth; // Move to next available position on the row
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

    public String getTSX() {
        StringBuilder tsx = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        tsx.append("<tileset version=\"1.8\" tiledversion=\"1.8.2\" name=\"").append(name).append("\" tilewidth=\"").append(tileWidth * scaleFactor).append("\" tileheight=\"").append(tileHeight * scaleFactor).append("\" tilecount=\"").append(size()).append("\" columns=\"").append(columns).append("\">\n");
        tsx.append(" <image source=\"").append(name).append(".png\" trans=\"ff00ff\" width=\"").append(getWidth() * scaleFactor).append("\" height=\"").append(getHeight() * scaleFactor).append("\"/>\n");
        for (Integer id:tilesById.keySet()) {
            tsx.append(" <tile id=\"").append(id).append("\">\n");
            String originalDataMd5 = tilesById.get(id).getOriginalDataMd5();
            if (originalDataMd5 != null) {
                tsx.append("  <properties>\n");
                tsx.append("   <property name=\"" + Tile.ORIGINAL_DATA_MD5 + "\" value=\"").append(originalDataMd5).append("\"/>\n");
                tsx.append("  </properties>\n");
            }
            tsx.append(" </tile>\n");
        }
        tsx.append("</tileset>");
        return tsx.toString();
    }

    //
    // Utility methods to read TSX
    //

    public static int getId(Node node) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(node, "id"));
    }

    public static String getOriginalDataMD5(Node node) throws Exception {
        String md5 = null;
        if (node.hasChildNodes()) {
            List<Node> propertiesList = XMLUtils.getChildTags(node, "properties");
            if (!propertiesList.isEmpty()) {
                List<Node> propertyList = XMLUtils.getChildTags(propertiesList.get(0), "property");
                for (Node property:propertyList) {
                    if (XMLUtils.getAttribute(property, "name").equals("original-data-md5")) {
                        md5 = XMLUtils.getAttribute(property, "value");
                    }
                }
            }
        }
        return md5;
    }

    public static int getColumns(File file) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(XMLUtils.getRootNode(file, "tileset"), "columns"));
    }

    public static int getTileWidth(File file) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(XMLUtils.getRootNode(file, "tileset"), "tilewidth"));
    }

    public static int getTileHeight(File file) throws Exception {
        return Integer.parseInt(XMLUtils.getAttribute(XMLUtils.getRootNode(file, "tileset"), "tileheight"));
    }

    public static List<Node> getTiles(File file) throws Exception {
        Node tileset = XMLUtils.getRootNode(file, "tileset");
        return XMLUtils.getChildTags(tileset, "tile");
    }

    private static String getName(File file) throws Exception {
        return XMLUtils.getAttribute(XMLUtils.getRootNode(file, "tileset"), "name");
    }

    private static String getSource(File file) throws Exception {
        Node tileset = XMLUtils.getRootNode(file, "tileset");
        Node image = XMLUtils.getChildTag(tileset, "image");
        return XMLUtils.getAttribute(image, "source");
    }
}