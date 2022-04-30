package org.lilosan.tiled.utils;

import org.lilosan.tiled.Tile;
import org.lilosan.tiled.TiledTMX;
import org.lilosan.tiled.TiledTSX;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lilosan.tiled.TiledTMX.getCSV;
import static org.lilosan.tiled.TiledTSX.*;

public class OrderRuleExecutor {

    public static void run(File afterRulesAreaDirectory, File rulesTsxFile, File rulesPngFile, List<File> areaTMXs) throws Exception {
        System.out.println("Order Rule");
        System.out.println("processFile: " + rulesTsxFile.getName());
        //
        // The TSX File contains the tiles in logical order
        //
        Map<Integer, Integer> physicalToLogicalId = new HashMap<>();
        Map<Integer, String> physicalIdToMD5 = new HashMap<>();
        List<Node> tilesMetadata = getTiles(rulesTsxFile);
        for (int logicalId = 0; logicalId < tilesMetadata.size(); logicalId++) {
            int physicalId = getId(tilesMetadata.get(logicalId));
            String md5 = getOriginalDataMD5(tilesMetadata.get(logicalId));
            physicalToLogicalId.put(physicalId, logicalId);
            if (md5 != null) {
                physicalIdToMD5.put(physicalId, md5);
            }
        }

        //
        // The PNG File contains the tiles in physical order
        //
        int tileWidth = getTileWidth(rulesTsxFile);
        int tileHeight = getTileHeight(rulesTsxFile);
        Tile[] tiles = new Tile[tilesMetadata.size()];

        BufferedImage tilesImages = ImageIO.read(rulesPngFile);
        Raster raster = tilesImages.getData();
        int width = raster.getWidth();
        int height = raster.getHeight();
        int physicalId = 0;
        for(int y = 0; y < height; y += tileHeight) {
            for (int x = 0; x < width; x += tileWidth) {
                BufferedImage tileImage = tilesImages.getSubimage(x, y, tileWidth, tileHeight);
                Integer logicalId = physicalToLogicalId.get(physicalId);
                if (logicalId == null) {
                    continue;
                }
                String md5 = physicalIdToMD5.get(physicalId);
                tiles[logicalId] = new Tile(tileImage, md5);
                physicalId++;
            }
        }

        //
        // Putting it all together and Writing the output
        //
        String tsxName = rulesTsxFile.getName().substring(0, rulesTsxFile.getName().indexOf(".tsx"));
        int columns = getColumns(rulesTsxFile);
        TiledTSX areaTileset = new TiledTSX(tsxName, 1, tileWidth, tileHeight, columns);
        for (Tile tile:tiles) {
            areaTileset.add(tile, true);
        }

        File tileSetTSXImage = new File(afterRulesAreaDirectory.getAbsolutePath() + "/" + afterRulesAreaDirectory.getName() + ".png");
        ImageIO.write(areaTileset.getBufferedImage(), "png", tileSetTSXImage);
        File tileSetTSXMeta = new File(afterRulesAreaDirectory.getAbsolutePath() + "/" + afterRulesAreaDirectory.getName() + ".tsx");
        Files.write(tileSetTSXMeta.toPath(), areaTileset.getTSX().getBytes(StandardCharsets.UTF_8));

        //
        // Now let's go though the maps
        //
        for (File areaTMX:areaTMXs) {
            System.out.println("processFile: " + areaTMX.getName());
            Integer[][] generatedCSV = getCSV(areaTMX);
            TiledTMX areaMap = new TiledTMX(1, areaTileset.getName(), generatedCSV.length, generatedCSV[0].length, areaTileset.getTileWidth(), areaTileset.getTileHeight());
            for (int y = 0; y < generatedCSV[0].length; y++) {
                for (int x = 0; x < generatedCSV.length; x++) {
                    int xyO = generatedCSV[x][y];
                    int xyF = physicalToLogicalId.get(generatedCSV[x][y]);
                    areaMap.add(x, y, physicalToLogicalId.get(generatedCSV[x][y]));
                }
            }
            File areaMapTMXMeta = new File(afterRulesAreaDirectory.getAbsolutePath() + "/" + areaTMX.getName());
            Files.write(areaMapTMXMeta.toPath(), areaMap.getTMX().getBytes(StandardCharsets.UTF_8));
        }
    }

}
