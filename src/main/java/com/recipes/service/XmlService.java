package com.recipes.service;

import com.recipes.model.Recipe;
import com.recipes.model.User;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class XmlService {

    private static final String[] CUISINES = {
            "Italian", "Asian", "Mexican", "French", "British",
            "Mediterranean", "American", "Indian", "Middle Eastern", "Japanese"
    };
    private static final String[] DIFFICULTIES = { "Beginner", "Intermediate", "Advanced" };

    private final Path xmlPath;
    private Document doc;
    private final Random rng = new Random(42);

    public XmlService() throws Exception {
        xmlPath = Paths.get(System.getProperty("user.dir"), "data", "recipes.xml");
        Files.createDirectories(xmlPath.getParent());

        if (!Files.exists(xmlPath)) {
            try (InputStream in = getClass().getResourceAsStream("/xml/recipes.xml")) {
                if (in == null)
                    throw new IllegalStateException("recipes.xml not found on classpath");
                Files.copy(in, xmlPath);
            }
        }
        reloadFromDisk();
    }

    // internal helpers
    private void reloadFromDisk() throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        doc = db.parse(xmlPath.toFile());
        doc.normalizeDocument();
    }

    private void saveToDisk() throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance(
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                getClass().getClassLoader());
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        try (OutputStream os = Files.newOutputStream(xmlPath)) {
            t.transform(new DOMSource(doc), new StreamResult(os));
        }
    }

    private Object evalXPath(String expression, Object context,
            javax.xml.namespace.QName returnType) throws Exception {
        XPath xp = XPathFactory.newInstance().newXPath();
        return xp.evaluate(expression, context, returnType);
    }

    // recipe CRUD
    public List<Recipe> getAllRecipes() throws Exception {
        NodeList nl = (NodeList) evalXPath("//recipe", doc, XPathConstants.NODESET);
        return nodeListToRecipes(nl);
    }

    public Optional<Recipe> getRecipeById(String id) throws Exception {
        Node n = (Node) evalXPath("//recipe[@id='" + safe(id) + "']", doc, XPathConstants.NODE);
        return n == null ? Optional.empty() : Optional.of(nodeToRecipe((Element) n));
    }

    public void addRecipe(Recipe recipe) throws Exception {
        Element recipesEl = (Element) doc.getElementsByTagName("recipes").item(0);
        Element r = doc.createElement("recipe");
        r.setAttribute("id", recipe.getId());
        appendText(r, "title", recipe.getTitle());
        appendText(r, "cuisine1", recipe.getCuisine1());
        appendText(r, "cuisine2", recipe.getCuisine2());
        appendText(r, "difficulty", recipe.getDifficulty());
        recipesEl.appendChild(r);
        saveToDisk();
    }

    // user CRUD
    public List<User> getAllUsers() throws Exception {
        NodeList nl = (NodeList) evalXPath("//user", doc, XPathConstants.NODESET);
        List<User> list = new ArrayList<>();
        for (int i = 0; i < nl.getLength(); i++)
            list.add(nodeToUser((Element) nl.item(i)));
        return list;
    }

    public Optional<User> getFirstUser() throws Exception {
        Node n = (Node) evalXPath("//user[1]", doc, XPathConstants.NODE);
        return n == null ? Optional.empty() : Optional.of(nodeToUser((Element) n));
    }

    public void addUser(User user) throws Exception {
        Element usersEl = (Element) doc.getElementsByTagName("users").item(0);
        Element u = doc.createElement("user");
        u.setAttribute("id", user.getId());
        appendText(u, "name", user.getName());
        appendText(u, "surname", user.getSurname());
        appendText(u, "skillLevel", user.getSkillLevel());
        appendText(u, "preferredCuisine", user.getPreferredCuisine());
        usersEl.appendChild(u);
        saveToDisk();
    }

    // XPath queries
    public List<Recipe> getRecipesBySkillLevel(String skillLevel) throws Exception {
        NodeList nl = (NodeList) evalXPath(
                "//recipe[difficulty='" + safe(skillLevel) + "']", doc, XPathConstants.NODESET);
        return nodeListToRecipes(nl);
    }

    public List<Recipe> getRecipesBySkillAndCuisine(String skillLevel, String cuisine) throws Exception {
        String s = safe(skillLevel), c = safe(cuisine);
        NodeList nl = (NodeList) evalXPath(
                "//recipe[difficulty='" + s + "' and (cuisine1='" + c + "' or cuisine2='" + c + "')]",
                doc, XPathConstants.NODESET);
        return nodeListToRecipes(nl);
    }

    public List<Recipe> getRecipesByCuisine(String cuisine) throws Exception {
        String c = safe(cuisine);
        NodeList nl = (NodeList) evalXPath(
                "//recipe[cuisine1='" + c + "' or cuisine2='" + c + "']",
                doc, XPathConstants.NODESET);
        return nodeListToRecipes(nl);
    }

    // XSL transformation
    public String transformWithXsl(String userSkillLevel) throws Exception {
        // 1. clone DOM, stamp userSkillLevel attribute on root
        byte[] xmlBytes = serializeDoc(cloneWithAttribute(userSkillLevel));

        // 2. load XSL from classpath
        byte[] xslBytes;
        try (InputStream xslIn = getClass().getResourceAsStream("/xsl/recipes.xsl")) {
            if (xslIn == null)
                throw new IllegalStateException("recipes.xsl not found");
            xslBytes = xslIn.readAllBytes();
        }

        net.sf.saxon.s9api.Processor processor = new net.sf.saxon.s9api.Processor(false);

        net.sf.saxon.s9api.XsltExecutable exec = processor.newXsltCompiler()
                .compile(new StreamSource(new ByteArrayInputStream(xslBytes)));

        net.sf.saxon.s9api.XsltTransformer transformer = exec.load();

        net.sf.saxon.s9api.XdmNode inputDoc = processor.newDocumentBuilder()
                .build(new StreamSource(new ByteArrayInputStream(xmlBytes)));

        transformer.setInitialContextNode(inputDoc);
        StringWriter sw = new StringWriter();
        transformer.setDestination(processor.newSerializer(sw));
        transformer.transform();
        return sw.toString();
    }

    // id generation
    public String nextRecipeId() throws Exception {
        NodeList nl = (NodeList) evalXPath("//recipe/@id", doc, XPathConstants.NODESET);
        return String.format("r%03d", maxSuffix(nl) + 1);
    }

    public String nextUserId() throws Exception {
        NodeList nl = (NodeList) evalXPath("//user/@id", doc, XPathConstants.NODESET);
        return String.format("u%03d", maxSuffix(nl) + 1);
    }

    // scraper helpers
    public String randomCuisine() {
        return CUISINES[rng.nextInt(CUISINES.length)];
    }

    public String randomDifficulty() {
        return DIFFICULTIES[rng.nextInt(DIFFICULTIES.length)];
    }

    public String[] getCuisines() {
        return CUISINES;
    }

    public String[] getDifficulties() {
        return DIFFICULTIES;
    }

    // helpers
    private Document cloneWithAttribute(String userSkillLevel) throws Exception {
        Document snapshot = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder().newDocument();
        Node root = snapshot.importNode(doc.getDocumentElement(), true);
        ((Element) root).setAttribute("userSkillLevel", userSkillLevel);
        snapshot.appendChild(root);
        return snapshot;
    }

    private byte[] serializeDoc(Document d) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance(
                "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl",
                getClass().getClassLoader());
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        t.transform(new DOMSource(d), new StreamResult(baos));
        return baos.toByteArray();
    }

    private int maxSuffix(NodeList nl) {
        int max = 0;
        for (int i = 0; i < nl.getLength(); i++) {
            try {
                max = Math.max(max, Integer.parseInt(
                        nl.item(i).getNodeValue().replaceAll("[^0-9]", "")));
            } catch (NumberFormatException ignored) {
            }
        }
        return max;
    }

    private List<Recipe> nodeListToRecipes(NodeList nl) {
        List<Recipe> list = new ArrayList<>();
        if (nl == null)
            return list;
        for (int i = 0; i < nl.getLength(); i++)
            list.add(nodeToRecipe((Element) nl.item(i)));
        return list;
    }

    private Recipe nodeToRecipe(Element e) {
        return new Recipe(e.getAttribute("id"),
                childText(e, "title"), childText(e, "cuisine1"),
                childText(e, "cuisine2"), childText(e, "difficulty"));
    }

    private User nodeToUser(Element e) {
        return new User(e.getAttribute("id"),
                childText(e, "name"), childText(e, "surname"),
                childText(e, "skillLevel"), childText(e, "preferredCuisine"));
    }

    private String childText(Element parent, String tag) {
        NodeList nl = parent.getElementsByTagName(tag);
        return nl.getLength() == 0 ? "" : nl.item(0).getTextContent().trim();
    }

    private void appendText(Element parent, String tag, String text) {
        Element child = doc.createElement(tag);
        child.setTextContent(text);
        parent.appendChild(child);
    }

    private String safe(String val) {
        return val == null ? "" : val.replaceAll("['\"]", "");
    }
}
