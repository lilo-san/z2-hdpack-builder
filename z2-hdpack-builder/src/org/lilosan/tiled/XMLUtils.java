package org.lilosan.tiled;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class XMLUtils {

    //
    // XML Parsing
    //

    public static String getAttribute(Node node, String name) throws Exception {
        return node.getAttributes().getNamedItem(name).getNodeValue();
    }

    public static Node getRootNode(File file, String tagName) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document tsx = db.parse(file);
        NodeList tags = tsx.getElementsByTagName(tagName);
        return tags.item(0);
    }

    public static Node getChildTag(Node node, String tagName) throws Exception {
        return getChildTags(node, tagName).get(0);
    }

    public static List<Node> getChildTags(Node node, String tagName) throws Exception {
        List<Node> tags = new ArrayList<>();
        if (node.hasChildNodes()) {
            NodeList nodeList = node.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node tag = nodeList.item(i);
                if (tag.getNodeName().equals(tagName)) {
                    tags.add(tag);
                }
            }
        }
        return tags;
    }

}
