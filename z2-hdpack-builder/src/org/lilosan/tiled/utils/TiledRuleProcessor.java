package org.lilosan.tiled.utils;

import org.lilosan.tiled.TiledTMX;
import org.lilosan.tiled.TiledTSX;
import org.lilosan.tiled.XMLUtils;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lilosan.tiled.utils.TiledExtractor.GENERATED_FOLDER;
import static org.lilosan.tiled.utils.TiledExtractor.outputScaleFactor;

public class TiledRuleProcessor {

    public enum OrderType { order, replace, terrain };
    public enum InputOutputType { id, md5 };

    public static final String RULES_FOLDER = "../../game-maps-rules";
    public static final String AFTER_RULES_FOLDER = "../../game-maps-after-rules";

    public static void main(String[] args) throws Exception {
        File inputFolder = new File(RULES_FOLDER);
        for (File areaDirectory : inputFolder.listFiles()) {
            if (areaDirectory.isDirectory()) {
                processArea(areaDirectory);
            }
        }
    }

    private static void processArea(File rulesAreaDirectory) throws Exception {
        System.out.println("processArea: " + rulesAreaDirectory.getName());
        File rulesFile = new File(RULES_FOLDER + "/" + rulesAreaDirectory.getName() + "/rules.xml");
        if (!rulesFile.exists()) {
            System.out.println("No rules.xml found");
            return; // Do nothing
        }
        File afterRulesAreaDirectory = new File(AFTER_RULES_FOLDER + "/" + rulesAreaDirectory.getName());
        afterRulesAreaDirectory.mkdirs();

        Node rulesRoot = XMLUtils.getRootNode(rulesFile, "rules");
        List<Node> rules = XMLUtils.getChildTags(rulesRoot, "rule");
        for (Node rule:rules) {
            OrderType type = OrderType.valueOf(XMLUtils.getAttribute(rule, "type"));
            switch (type) {
                case order:
                    runOrder(rule, rulesAreaDirectory.getName());
                    break;
                case replace:
                    runReplace(rule, rulesAreaDirectory.getName());
                    break;
                case terrain:
                    runTerrain(rule, rulesAreaDirectory.getName());
                    break;
            }
        }

        // Write finished maps
        List<File> tmxFiles = getAreaTMXs(AFTER_RULES_FOLDER, rulesAreaDirectory.getName());
        File tsxFile = getAreaTSX(AFTER_RULES_FOLDER, rulesAreaDirectory.getName());
        TiledTSX tsx = TiledTSX.getInstance(tsxFile);
        for (File map:tmxFiles) {
            TiledTMX tmx = TiledTMX.getInstance(map);
            String areaMapName = map.getName().substring(0, map.getName().indexOf(".tmx"));
            ImageIO.write(tmx.getBufferedImage(tsx, outputScaleFactor * 32), "png", new File(AFTER_RULES_FOLDER + "/" + rulesAreaDirectory.getName() + "/" + areaMapName + ".png"));
        }
    }

    private static void runTerrain(Node rule, String areaName) throws Exception {
        File areaTSX = getAreaTSX(AFTER_RULES_FOLDER, areaName);
        List<File> areaTMXs = getAreaTMXs(AFTER_RULES_FOLDER, areaName);
        Node input = XMLUtils.getChildTag(rule, "input");
        String[] md5sInputGroups = input.getFirstChild().getNodeValue().split(", ");
        Node output = XMLUtils.getChildTag(rule, "output");
        String[] idsOutputGroups = output.getFirstChild().getNodeValue().split(", ");
        if (md5sInputGroups.length != idsOutputGroups.length) {
            throw new RuntimeException("runTerrain: input and output groups number differ");
        }
        Map<String, List<String>> inputsToOutputs = new HashMap<>();
        for (int i = 0; i < md5sInputGroups.length; i++) {
            List<String> idsOutputGroup = List.of(idsOutputGroups[i].split(" "));
            inputsToOutputs.put(md5sInputGroups[i], idsOutputGroup);
        }
        TerrainRuleExecutor.run(areaTSX, areaTMXs, inputsToOutputs);
    }

    private static void runReplace(Node rule, String areaName) throws Exception {
        File areaTSX = getAreaTSX(AFTER_RULES_FOLDER, areaName);
        List<File> areaTMXs = getAreaTMXs(AFTER_RULES_FOLDER, areaName);
        Node input = XMLUtils.getChildTag(rule, "input");
        InputOutputType inputType = InputOutputType.valueOf(XMLUtils.getAttribute(input, "type"));
        List<String> inputItems = List.of(input.getFirstChild().getNodeValue().split(" "));
        Node output = XMLUtils.getChildTag(rule, "output");
        InputOutputType outputType = InputOutputType.valueOf(XMLUtils.getAttribute(output, "type"));
        List<String> outputItems = List.of(output.getFirstChild().getNodeValue().split(" "));
        ReplaceRuleExecutor.run(areaTSX, areaTMXs, inputType, inputItems, outputType, outputItems);
    }

    private static void runOrder(Node rule, String areaName) throws Exception {
        File areaTSX = getAreaTSX(RULES_FOLDER, areaName);
        File areaPNG = getAreaPNG(RULES_FOLDER, areaName);
        List<File> areaTMXs = getAreaTMXs(GENERATED_FOLDER, areaName);
        OrderRuleExecutor.run(new File(AFTER_RULES_FOLDER + "/" + areaName), areaTSX, areaPNG, areaTMXs);
    }

    private static File getAreaPNG(String stepFolder, String areaName) {
        return new File(stepFolder + "/" + areaName + "/" + areaName + ".png");
    }

    private static File getAreaTSX(String stepFolder, String areaName) {
        return new File(stepFolder + "/" + areaName + "/" + areaName + ".tsx");
    }

    private static List<File> getAreaTMXs(String stepFolder, String areaName) {
        List<File> areaTMXs = new ArrayList<>();
        File generatedAreaDirectory = new File(stepFolder + "/" + areaName);
        for (File file : generatedAreaDirectory.listFiles()) {
            if (file.getName().endsWith(".tmx")) {
                areaTMXs.add(file);
            }
        }
        return areaTMXs;
    }

}