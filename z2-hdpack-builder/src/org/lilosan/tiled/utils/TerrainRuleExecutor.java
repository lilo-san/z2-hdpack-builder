package org.lilosan.tiled.utils;

import org.lilosan.tiled.TiledTMX;
import org.lilosan.tiled.TiledTSX;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static org.lilosan.tiled.TiledTSX.*;

public class TerrainRuleExecutor {
    public static void run(File finalTsxFile, List<File> finalAreaTMXs, Map<String, List<String>> inputsToOutputs) throws Exception {
        // Recover map file ids to obtain the ids to replace
        Map<String, Integer> md5ToId = new HashMap<>();
        List<Node> tilesMetadata = getTiles(finalTsxFile);
        for (int logicalId = 0; logicalId < tilesMetadata.size(); logicalId++) {
            int physicalId = getId(tilesMetadata.get(logicalId));
            String md5 = getOriginalDataMD5(tilesMetadata.get(logicalId));
            if (md5 != null) {
                md5ToId.put(md5, physicalId);
            }
        }

        // Ids to replace
        Set<Integer> initialTerrain = new HashSet<>();
        for (String input:inputsToOutputs.keySet()) {
            initialTerrain.add(md5ToId.get(input));
        }

        // Terrain to replace fo
        Map<Integer, Integer[][]> inputsToFinalTerrains = new HashMap<>();
        Map<Integer, Integer[][]> inputsToFinalTerrainsCorners = new HashMap<>();

        for (String input:inputsToOutputs.keySet()) {
            Integer inputAsInt = md5ToId.get(input);
            List<String> outputs = inputsToOutputs.get(input);
            Integer[][] finalTerrain = new Integer[3][3];
            finalTerrain[0][0] = Integer.parseInt(outputs.get(0));
            finalTerrain[0][1] = Integer.parseInt(outputs.get(1));
            finalTerrain[0][2] = Integer.parseInt(outputs.get(2));
            finalTerrain[1][0] = Integer.parseInt(outputs.get(3));
            finalTerrain[1][1] = Integer.parseInt(outputs.get(4));
            finalTerrain[1][2] = Integer.parseInt(outputs.get(5));
            finalTerrain[2][0] = Integer.parseInt(outputs.get(6));
            finalTerrain[2][1] = Integer.parseInt(outputs.get(7));
            finalTerrain[2][2] = Integer.parseInt(outputs.get(8));
            inputsToFinalTerrains.put(inputAsInt, finalTerrain);

            Integer[][] finalTerrainCorners = new Integer[2][2];
            finalTerrainCorners[0][0] = Integer.parseInt(outputs.get(9));
            finalTerrainCorners[0][1] = Integer.parseInt(outputs.get(10));
            finalTerrainCorners[1][0] = Integer.parseInt(outputs.get(11));
            finalTerrainCorners[1][1] = Integer.parseInt(outputs.get(12));
            inputsToFinalTerrainsCorners.put(inputAsInt, finalTerrainCorners);
        }

        // For every map
        String tsxName = finalTsxFile.getName().substring(0, finalTsxFile.getName().indexOf(".tsx"));
        int tileWidth = TiledTSX.getTileWidth(finalTsxFile);
        int tileHeight = TiledTSX.getTileHeight(finalTsxFile);

        for (File areaTMX:finalAreaTMXs) {
            System.out.println("processFile: " + areaTMX.getName());

            // Fix maps
            Integer[][] map = TiledTMX.getCSV(areaTMX);
            TiledTMX areaMap = new TiledTMX(1, tsxName, map.length, map[0].length, tileWidth, tileHeight);

            for (int y = 0; y < map[0].length; y++) {
                for (int x = 0; x < map.length; x++) {
                    Integer initialValue = map[x][y];
                    if (initialTerrain.contains(initialValue)) {
                        areaMap.add(x, y, getTerrain(x, y, map, initialTerrain, inputsToFinalTerrains.get(initialValue), inputsToFinalTerrainsCorners.get(initialValue)));

                    } else {
                        areaMap.add(x, y, initialValue);
                    }
                }
            }
            // Create Fixed map file
            Files.write(areaTMX.toPath(), areaMap.getTMX().getBytes(StandardCharsets.UTF_8));
        }

    }

    private static Integer getTerrain(int x, int y, Integer[][] map, Set<Integer> initialTerrain, Integer[][] finalTerrain, Integer[][] finalTerrainCorners) {
        Integer center = map[x][y];
        if (initialTerrain.contains(center)) { // Check only if this is true
            // Surroundings, not necessarily valid positions;
            Integer topLeft = null;
            if ( ((x - 1) >= 0) && ((y - 1) >= 0)) {
                topLeft = map[x - 1][y - 1];
            }
            Integer top = null;
            if (((y - 1) >= 0)) {
                top = map[x][y - 1];
            }
            Integer topRight = null;
            if (((x + 1) < map.length) && ((y - 1) >= 0)) {
                topRight = map[x + 1][y - 1];
            }
            Integer left = null;
            if ( ((x - 1) >= 0) ) {
                left = map[x - 1][y];
            }
            // center is defined outside, the current position
            Integer right = null;
            if (((x + 1) < map.length)) {
                right = map[x + 1][y];
            }
            Integer bottomLeft = null;
            if (((x - 1) >= 0) && ((y + 1) < map[x - 1].length)) {
                bottomLeft = map[x - 1][y + 1];
            }
            Integer bottom = null;
            if (((y + 1) < map[x].length)) {
                bottom = map[x][y + 1];
            }
            Integer bottomRight = null;
            if (((x + 1) < map.length) && ((y + 1) < map[x + 1].length)) {
                bottomRight = map[x + 1][y + 1];
            }

            // the terrain starts by being the center terrain
            Integer newTerrain = finalTerrain[1][1];

            // Top-Left
            if (    (left != null && !initialTerrain.contains(left)) &&
                    (top != null && !initialTerrain.contains(top)) &&
                    (right == null || initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[0][0];
            }
            // Top
            if (    (left == null || initialTerrain.contains(left)) &&
                    (top != null && !initialTerrain.contains(top)) &&
                    (right == null || initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[0][1];
            }
            // Top-Right
            if (    (left == null || initialTerrain.contains(left)) &&
                    (top != null && !initialTerrain.contains(top)) &&
                    (right != null && !initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[0][2];
            }
            // Left
            if (    (top == null || initialTerrain.contains(top)) &&
                    (left != null && !initialTerrain.contains(left)) &&
                    (bottom == null || initialTerrain.contains(bottom))
            ) {
                newTerrain = finalTerrain[1][0];
            }
            // Center (default)
            // Right
            if (    (top == null || initialTerrain.contains(top)) &&
                    (right != null && !initialTerrain.contains(right)) &&
                    (bottom == null || initialTerrain.contains(bottom))
            ) {
                newTerrain = finalTerrain[1][2];
            }
            // Bottom-Left
            if (    (left != null && !initialTerrain.contains(left)) &&
                    (bottom != null && !initialTerrain.contains(bottom)) &&
                    (right == null || initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[2][0];
            }
            // Bottom
            if (    (left == null || initialTerrain.contains(left)) &&
                    (bottom != null && !initialTerrain.contains(bottom)) &&
                    (right == null || initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[2][1];
            }
            // Bottom-Right
            if (    (left == null || initialTerrain.contains(left)) &&
                    (bottom != null && !initialTerrain.contains(bottom)) &&
                    (right != null && !initialTerrain.contains(right))
            ) {
                newTerrain = finalTerrain[2][2];
            }

            // Corners //

            // Top-Left Corner
            if (    (bottom != null && initialTerrain.contains(bottom)) &&
                    (right != null && initialTerrain.contains(right)) &&
                    (bottomRight == null || !initialTerrain.contains(bottomRight))
            ) {
                newTerrain = finalTerrainCorners[0][0];
            }

            // Top-Right Corner
            if (    (bottom != null && initialTerrain.contains(bottom)) &&
                    (left != null && initialTerrain.contains(left)) &&
                    (bottomLeft == null || !initialTerrain.contains(bottomLeft))
            ) {
                newTerrain = finalTerrainCorners[0][1];
            }

            // Bottom-Left Corner
            if (    (top != null && initialTerrain.contains(top)) &&
                    (right != null && initialTerrain.contains(right)) &&
                    (topRight == null || !initialTerrain.contains(topRight))
            ) {
                newTerrain = finalTerrainCorners[1][0];
            }

            // Bottom-Right Corner
            if (    (top != null && initialTerrain.contains(top)) &&
                    (left != null && initialTerrain.contains(left)) &&
                    (topLeft == null || !initialTerrain.contains(topLeft))
            ) {
                newTerrain = finalTerrainCorners[1][1];
            }
            return newTerrain;
        } else {
            return center;
        }
    }
}