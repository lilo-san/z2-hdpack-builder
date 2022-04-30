package org.lilosan.tiled;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Zelda2HiResGenerator {
    public static final String ORIGINAL_ASSETS = "../original-assets";
    public static final String CUSTOM_ASSETS = "../custom-assets";
    public static final String HDPACK_ASSETS = "../hdpack-assets";

    public static final String NAME = "hires.txt";
    public static final int SCALE = 2;
    public static final int STATIC_BACKGROUND_LAYER = 20;

    public static class Condition {
        private final String name;
        private final String memoryCheckConstant;
        private final String operation;
        private final String value;

        public Condition(String name, String memoryCheckConstant, String operation, String value) {
            this.name = name;
            this.memoryCheckConstant = memoryCheckConstant;
            this.operation = operation;
            this.value = value;
        }

        @Override
        public String toString() {
            return "<condition>" + name + "_" + value + ",memoryCheckConstant," +  memoryCheckConstant + ","  + operation + "," + value;
        }
    }

    public static final String LOCATION_CODE_CONSTANT = "76E";
    public static final String MAP_CODE_CONSTANT = "561";
    public static final String MAP_SLICE_CONSTANT = "3B";

    public static void main(String args[]) throws Exception {
        StringBuilder hires = new StringBuilder();
        hires.append(getHeader()).append("\n");

        hires.append("\n").append("\n");

        hires.append("# Location Codes").append("\n");
        for (String locationCode:getLocationCodes()) {
            Condition locationCondition = new Condition("LOCATION", LOCATION_CODE_CONSTANT, "==", locationCode);
            hires.append(locationCondition).append("\n");
        }

        hires.append("\n").append("\n");

        hires.append("# Map Codes").append("\n");
        for (String mapCode:getMapCodes()) {
            Condition mapCondition = new Condition("MAP", MAP_CODE_CONSTANT, "==", mapCode);
            hires.append(mapCondition).append("\n");
        }

        hires.append("\n").append("\n");

        hires.append("# Map Slice Codes").append("\n");
        for (String mapSliceCode:getMapSliceCodes()) {
            Condition mapSlice = new Condition("MAP_SLICE", MAP_SLICE_CONSTANT, "==", mapSliceCode);
            hires.append(mapSlice).append("\n");
        }

        hires.append("\n").append("\n");

        hires.append("# Map Backgrounds").append("\n");
        for (String background: getMapBackgrounds(ORIGINAL_ASSETS)) {
            String backgroundPNG = background.replace(".tmx", ".png");
            String name = background.substring(background.indexOf("/") + 1, background.indexOf(".tmx"));
            String[] locationCodes = name.split("-")[0].split("\\|");
            String[] mapCodes = name.split("-")[1].split("\\|");

            for (String locationCode:locationCodes) {
                for(String mapCode:mapCodes) {
                    for (String mapSliceCode:getMapSliceCodes()) {
                        int leftOffset = 0;
                        int topOffset = 0;
                        String sliceRule = "";

                        switch (mapSliceCode) {
                            case "0":
                                leftOffset = 0;
                                topOffset = 0;
                                sliceRule = "MAP_SLICE_0&!MAP_SLICE_1&!MAP_SLICE_2&!MAP_SLICE_3";
                                break;
                            case "1":
                                leftOffset = 0;
                                topOffset = 0;
                                sliceRule = "!MAP_SLICE_0&MAP_SLICE_1&!MAP_SLICE_2&!MAP_SLICE_3";
                                break;
                            case "2":
                                leftOffset = 0;
                                topOffset = 240;
                                sliceRule = "!MAP_SLICE_0&!MAP_SLICE_1&MAP_SLICE_2&!MAP_SLICE_3";
                                break;
                            case "3":
                                leftOffset = 512;
                                topOffset = 0;
                                sliceRule = "!MAP_SLICE_0&!MAP_SLICE_1&!MAP_SLICE_2&MAP_SLICE_3";
                                break;
                        }

                        String backgroundCondition = "[" + "LOCATION_" + locationCode + "&"
                                                        + "MAP_" + mapCode + "&"
                                                        + sliceRule + "]"
                                                        + "<background>" + backgroundPNG + ",1,1,1," + STATIC_BACKGROUND_LAYER + "," + leftOffset + "," + topOffset;

                        hires.append(backgroundCondition).append("\n");

                    }


                }
            }
            hires.append("\n");
        }

        Files.write(Path.of(HDPACK_ASSETS + "/" + NAME), hires.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static String getHeader() {
        StringBuilder header = new StringBuilder();
        header.append("<ver>106\n");
        header.append("<scale>2\n");
        header.append("<supportedRom>2B293E713ED2ECC6D55A6816CAD3EC0F67D0E43E");
        return header.toString();
    }

    public static List<String> getMapBackgrounds(String areasRoot) {
        List<String> result = new ArrayList<>();
        File areas = new File(areasRoot);
        for (File area:areas.listFiles()) {
            if (area.isDirectory()) {
                for (File map : area.listFiles()) {
                    if (map.getName().endsWith(".tmx") && !map.getName().equals(area.getName() + ".tmx") && !area.getName().equals("overworld")) {
                        result.add(area.getName()  + "/" + map.getName());
                    }
                }
            }
        }
        return result;
    }

    public static List<String> getMapSliceCodes() {
        List<String> values = new ArrayList<>();
        values.add("0");
        values.add("1");
        values.add("2");
        values.add("3");
//        values.add("FF");
        return values;
    }

    public static Set<String> getLocationCodes() {
        Set<String> result = new HashSet<>();
        File areas = new File(ORIGINAL_ASSETS);
        for (File area:areas.listFiles()) {
            if (area.isDirectory()) {
                for (File map : area.listFiles()) {
                    if (map.getName().endsWith(".tmx") && !map.getName().equals(area.getName() + ".tmx") && !area.getName().equals("overworld")) {
                        String name = map.getName().substring(0, map.getName().indexOf(".tmx"));
                        String[] locationCodes = name.split("-")[0].split("\\|");
                        for (String locationCode:locationCodes) {
                            result.add(locationCode);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static Set<String> getMapCodes() {
        Set<String> result = new HashSet<>();
        File areas = new File(ORIGINAL_ASSETS);
        for (File area:areas.listFiles()) {
            if (area.isDirectory()) {
                for (File map : area.listFiles()) {
                    if (map.getName().endsWith(".tmx") && !map.getName().equals(area.getName() + ".tmx") && !area.getName().equals("overworld")) {
                        String name = map.getName().substring(0, map.getName().indexOf(".tmx"));
                        String[] mapCodes = name.split("-")[1].split("\\|");
                        for (String mapCode:mapCodes) {
                            result.add(mapCode);
                        }
                    }
                }
            }
        }
        return result;
    }
}
