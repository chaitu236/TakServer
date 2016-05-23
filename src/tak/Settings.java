/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tak;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author chaitu
 */
public class Settings {
    public static Document doc = null;
    public static void parse() {
        File xmlFile = new File("properties.xml");
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(xmlFile);
            doc.getDocumentElement().normalize();
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        parseEmail();
        parseIRC();
    }
    
    private static void parseEmail() {
        NodeList nList = doc.getElementsByTagName("email");
        Node node = nList.item(0);
        Element element = (Element)node;
        EMail.host = element.getElementsByTagName("host").item(0).getTextContent();
        EMail.user = element.getElementsByTagName("user").item(0).getTextContent();
        EMail.password = element.getElementsByTagName("password").item(0).getTextContent();
        
        System.out.println("user "+EMail.user+" host "+EMail.host);
    }
    
    private static void parseIRC() {
        NodeList nList = doc.getElementsByTagName("irc");
        Node node = nList.item(0);
        Element element = (Element)node;
        
        IRCBridge.enabled = "true".equals(element.getElementsByTagName("enabled").item(0).getTextContent());
        IRCBridge.server = element.getElementsByTagName("server").item(0).getTextContent();
        IRCBridge.nick = element.getElementsByTagName("nick").item(0).getTextContent();
        IRCBridge.login = element.getElementsByTagName("login").item(0).getTextContent();
        IRCBridge.channel = element.getElementsByTagName("channel").item(0).getTextContent();
        IRCBridge.password = element.getElementsByTagName("password").item(0).getTextContent();
    }
}
