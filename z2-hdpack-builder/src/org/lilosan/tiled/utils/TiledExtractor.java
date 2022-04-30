package org.lilosan.tiled.utils;

import org.lilosan.tiled.Tile;
import org.lilosan.tiled.TiledTMX;
import org.lilosan.tiled.TiledTSX;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import javax.imageio.ImageIO;

public class TiledExtractor {
    public static final String INPUT_FOLDER = "../../game-maps-original";
    public static final String GENERATED_FOLDER = "../../game-maps-generated";

    public static final int inputTileSizeWidth = 8;
    public static final int inputTileSizeHeight = 8;
    public static final int outputScaleFactor = 2;

    public static final int NUMBER_OF_TILES_IN_ONE_TILESET_ROW = 32;

    public static void main(String[] args) throws Exception {
        File inputFolder = new File(INPUT_FOLDER);
        for (File areaDirectory:inputFolder.listFiles()) {
            if(areaDirectory.isDirectory()) {
                processArea(areaDirectory);
            }
        }
    }

    private static void processArea(File originalAreaDirectory) throws Exception {
        System.out.println("processArea: " + originalAreaDirectory.getName());
        File generatedAreaDirectory = new File(GENERATED_FOLDER + "/" + originalAreaDirectory.getName());
        generatedAreaDirectory.mkdirs();

        TiledTSX areaTiles = new TiledTSX(originalAreaDirectory.getName(), outputScaleFactor, inputTileSizeWidth, inputTileSizeHeight, NUMBER_OF_TILES_IN_ONE_TILESET_ROW);
        areaTiles.add(Tile.getEmptyTile(inputTileSizeWidth, inputTileSizeHeight));

        for (File areaMapPNG:originalAreaDirectory.listFiles()) {
            if (areaMapPNG.getName().endsWith(".png")) {
                String areaMapName = areaMapPNG.getName().substring(0, areaMapPNG.getName().indexOf(".png"));
                TiledTMX areaMap = processAreaMap(areaTiles, areaMapPNG);
                System.out.println("Creating Map TMX with size: " + areaMap.getSize());
                File areaMapTMXMeta = new File(generatedAreaDirectory.getAbsolutePath() + "/" + areaMapName + ".tmx");
                Files.write(areaMapTMXMeta.toPath(), areaMap.getTMX().getBytes(StandardCharsets.UTF_8));
                // File areaMapTMXPng = new File(generatedAreaDirectory.getAbsolutePath() + "/" + areaMapName + ".png");
                // ImageIO.write(areaMap.getBufferedImage(areaTiles), "png", areaMapTMXPng);
            }
        }

        System.out.println("Creating Area TSX with size: " + areaTiles.size());
        File tileSetTSXImage = new File(generatedAreaDirectory.getAbsolutePath() + "/" + areaTiles.getName() + ".png");
        ImageIO.write(areaTiles.getBufferedImage(), "png", tileSetTSXImage);
        File tileSetTSXMeta = new File(generatedAreaDirectory.getAbsolutePath() + "/" + areaTiles.getName() + ".tsx");
        Files.write(tileSetTSXMeta.toPath(), areaTiles.getTSX().getBytes(StandardCharsets.UTF_8));
    }

    private static TiledTMX processAreaMap(TiledTSX areaTiles, File areaMapPNG) throws Exception {
        System.out.println("processAreaMap: " + areaMapPNG.getName());
        BufferedImage inputImage = ImageIO.read(areaMapPNG);
        Raster raster = inputImage.getData();
        int width = raster.getWidth();
        System.out.println("Width: " + width);
        int height = raster.getHeight();
        System.out.println("Height: " + height);
        TiledTMX areaMap = new TiledTMX(outputScaleFactor, areaTiles.getName(), width / inputTileSizeWidth, height / inputTileSizeHeight, areaTiles.getTileWidth(), areaTiles.getTileHeight());
        for(int y = 0; y < height; y += inputTileSizeHeight) {
            for (int x = 0; x < width; x += inputTileSizeWidth) {
                BufferedImage tile = inputImage.getSubimage(x, y, inputTileSizeWidth, inputTileSizeHeight);
                Tile xy = new Tile(tile);
                Integer id = areaTiles.add(xy);
                areaMap.add(x / inputTileSizeWidth, y / inputTileSizeHeight, id);
            }
        }
        return areaMap;

    }
}
