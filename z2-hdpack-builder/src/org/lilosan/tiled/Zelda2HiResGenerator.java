package org.lilosan.tiled;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

        hires.append(Files.readString(Path.of(CUSTOM_ASSETS + "/hires-loading-screen.txt"), StandardCharsets.UTF_8));
        copyDirectory(CUSTOM_ASSETS + "/-loading-screen", HDPACK_ASSETS + "/-loading-screen");

        hires.append("\n").append("\n");

        hires.append("# Map Backgrounds").append("\n");
        addBackgrounds(hires, getMapBackgrounds(ORIGINAL_ASSETS), ORIGINAL_ASSETS);
        addBackgrounds(hires, getMapBackgrounds(CUSTOM_ASSETS), CUSTOM_ASSETS);

        Files.write(Path.of(HDPACK_ASSETS + "/" + NAME), hires.toString().getBytes(StandardCharsets.UTF_8));
    }

    public static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation)
            throws IOException {

        Files.walk(Paths.get(sourceDirectoryLocation))
                .forEach(source -> {
                    Path destination = Paths.get(destinationDirectoryLocation, source.toString()
                            .substring(sourceDirectoryLocation.length()));
                    try {
                        Files.copy(source, destination);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void addBackgrounds(StringBuilder hires, List<String> originalBackgrounds, String backgroundsRoot) throws Exception {

        for (String background: originalBackgrounds) {
            String backgroundPNG = getPNGBackground(background, backgroundsRoot);
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
    }

    private static String getPNGBackground(String background, String backgroundsRoot) throws Exception {
        String area = background.substring(0, background.indexOf('/'));
        String tsx = backgroundsRoot + "/" + area + "/" + area + ".tsx";
        TiledTSX tsxFile = TiledTSX.getInstance(new File(tsx));
        String tmx = backgroundsRoot + "/" + background;
        TiledTMX tmxFile = TiledTMX.getInstance(new File(tmx));

        BufferedImage image = tmxFile.getBufferedImage(tsxFile, 0);
        int[] imageRGB = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        int[] imageSectionRGB = image.getRGB(1024, 0, 799, image.getHeight(), null, 0, image.getWidth());

        int SPACING = 32;

        BufferedImage hdPackImage = new BufferedImage(image.getWidth(), image.getHeight() * 2 + (SPACING * 4), BufferedImage.TYPE_INT_ARGB);
        hdPackImage.setRGB(0, SPACING * 2, image.getWidth(), image.getHeight(), imageRGB, 0, image.getWidth());
        hdPackImage.setRGB(0, image.getHeight() + (SPACING * 4), image.getWidth(), image.getHeight(), imageRGB, 0, image.getWidth());
        hdPackImage.setRGB(0, image.getHeight() + (SPACING * 4), 799, image.getHeight(), imageSectionRGB, 0, image.getWidth());
        
        File directory = new File(HDPACK_ASSETS + "/" + area);
        directory.mkdir();
        ImageIO.write(hdPackImage, "png", new FileOutputStream(HDPACK_ASSETS + "/" + background.replace(".tmx", ".png")));
        return background.replace(".tmx", ".png");
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
