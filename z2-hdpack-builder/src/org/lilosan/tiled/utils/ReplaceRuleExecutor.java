package org.lilosan.tiled.utils;

import org.lilosan.tiled.TiledTMX;
import org.lilosan.tiled.TiledTSX;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.lilosan.tiled.TiledTSX.*;

public class ReplaceRuleExecutor {
    public static void run(File finalTsxFile, List<File> finalAreaTMXs, TiledRuleProcessor.InputOutputType inputType, List<String> inputItems, TiledRuleProcessor.InputOutputType outputType, List<String> outputItems) throws Exception {
        // Recover map file ids
        Map<String, Integer> md5ToId = new HashMap<>();
        List<Node> tilesMetadata = getTiles(finalTsxFile);
        for (int logicalId = 0; logicalId < tilesMetadata.size(); logicalId++) {
            int physicalId = getId(tilesMetadata.get(logicalId));
            String md5 = getOriginalDataMD5(tilesMetadata.get(logicalId));
            if (md5 != null) {
                md5ToId.put(md5, physicalId);
            }
        }

        // Obtain input to output mapping ids
        Random random = new Random();
        Map<Integer, Integer> replaceFromToId = new HashMap<>();
        for (String input:inputItems) {
            Integer inputTile = null;
            switch (inputType) {
                case id:
                    inputTile = Integer.parseInt(input);
                    break;
                case md5:
                    inputTile = md5ToId.get(input);
                    break;
            }
            Integer outputTile = null;
            switch (outputType) {
                case id:
                    outputTile = Integer.parseInt(getRandomElement(outputItems, random));
                    break;
                case md5:
                    outputTile = md5ToId.get(getRandomElement(outputItems, random));
                    break;
            }
            replaceFromToId.put(inputTile, outputTile);
        }

        // For every map
        String tsxName = finalTsxFile.getName().substring(0, finalTsxFile.getName().indexOf(".tsx"));
        int tileWidth = TiledTSX.getTileWidth(finalTsxFile);
        int tileHeight = TiledTSX.getTileHeight(finalTsxFile);

        for (File areaTMX:finalAreaTMXs) {
            System.out.println("processFile: " + areaTMX.getName());
            String tmxName = areaTMX.getName().substring(0, areaTMX.getName().indexOf(".tmx"));

            // Fix maps
            Integer[][] csv = TiledTMX.getCSV(areaTMX);
            TiledTMX areaMap = new TiledTMX(1, tsxName, csv.length, csv[0].length, tileWidth, tileHeight);

            for (int y = 0; y < csv[0].length; y++) {
                for (int x = 0; x < csv.length; x++) {
                    Integer toReplace = replaceFromToId.get(csv[x][y]);
                    if (toReplace != null) {
                        areaMap.add(x, y, toReplace);
                    } else {
                        areaMap.add(x, y, csv[x][y]);
                    }
                }
            }
            // Create Fixed map file
            Files.write(areaTMX.toPath(), areaMap.getTMX().getBytes(StandardCharsets.UTF_8));
        }

    }

    public static String getRandomElement(List<String> collection, Random random) {
        int size = collection.size();
        int item = random.nextInt(size);
        return collection.get(item);
    }
}