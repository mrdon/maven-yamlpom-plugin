package org.twdata.maven.yamlpom;

import org.yaml.snakeyaml.Yaml;
import org.w3c.dom.*;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.ls.DOMImplementationLS;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;
import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

/**
 *
 */
public class XmlToYamlConverter implements Converter {
    private static final int MAX_LINE_LENGTH = 120;
    private static final char[] INVALID_SCALAR_CHARACTERS = new char[]{':', '#', '[', ']', '{', '}', ',', '*', '\t'};

    private static final DocumentBuilderFactory factory;

    static {
        factory = DocumentBuilderFactory.newInstance();
        factory.setIgnoringComments(false);
        factory.setIgnoringElementContentWhitespace(true);
        factory.setCoalescing(false);
        factory.setValidating(false);
    }


    private void validateTargetContents(String yamlText) throws InvalidFormatException {
        Yaml yaml = YamlUtils.buildYaml();
        Object obj = null;
        try {
            obj = yaml.load(yamlText);
        }
        catch (RuntimeException ex) {
            throw new InvalidFormatException("Invalid YAML", yamlText, ex);
        }

        if (!(obj instanceof Map)) {
            throw new InvalidFormatException("YAML file not a map", yamlText);
        }
    }

    public String convert(Reader xmlReader, ConverterOptions options) throws InvalidFormatException, IOException {
        StringWriter yamlWriter = new StringWriter();

        try {

            Document doc = factory.newDocumentBuilder().parse(new InputSource(xmlReader));
            for (Iterator it = elementIterator(doc.getDocumentElement()); it.hasNext();) {
                Element element = (Element) it.next();
                if (!"modelVersion".equals(element.getTagName())) {
                    convert(element, "", false, yamlWriter, options.getIndent());
                }
            }
        } catch (SAXException e) {
            throw new InvalidFormatException("POM XML is not valid", null, e);
        } catch (ParserConfigurationException e) {
            throw new InvalidFormatException("POM XML is not valid", null, e);
        }
        String text = yamlWriter.toString();
        validateTargetContents(text);
        return text;
    }

    private void convert(Element element, String tabs, boolean isInList, Writer yamlWriter, String tab) throws IOException {
        if (element != null) {
            String name = element.getTagName();
            String prefix = isInList ? "- " : "";

            if ("configuration".equals(name)) {
                if (isConfigurationNotYamlSafe(element, tabs, tab)) {
                    yamlWriter.write(tabs + prefix + "configuration : |\n");
                    StringWriter blockWriter = new StringWriter();
                    for (Iterator i = elementIterator(element); i.hasNext();) {
                        blockWriter.append(elementToBlockString((Element) i.next()));
                    }
                    yamlWriter.write(indent(blockWriter.toString(), (isInList ? "  " : "") + tabs + tab, tab));
                    return;
                }
            }

            // is scalar
            if (elementList(element).isEmpty()) {
                yamlWriter.write(tabs + prefix + name + ": " + sanitizeScalar(trimmedContent(element)) + "\n");
            }
            // is list
            else if (isList(element)) {
                yamlWriter.write(tabs + prefix + name + ":");
                if (shouldInline(element, (tabs + tab + "  ").length())) {
                    printInlineList(element, yamlWriter);
                } else {
                    yamlWriter.write("\n");
                    for (Iterator i = elementIterator(element); i.hasNext();) {
                        Element list = (Element) i.next();
                        if (shouldInline(list, (tabs + tab + "  ").length())) {
                            printInlineMap(list, tabs + tab + "- ", yamlWriter);
                        }
                        // is scalar entry
                        else if (elementList(list).isEmpty()) {
                            yamlWriter.write(tabs + tab + "- " + trimmedContent(list) + "\n");
                        } else {
                            boolean isFirst = true;
                            for (Iterator it = elementIterator(list); it.hasNext();) {
                                Element listItem = (Element) it.next();
                                convert(listItem, (!isFirst ? "  " : "") + tabs + tab, isFirst, yamlWriter, tab);
                                isFirst = false;
                            }
                        }
                    }
                }
                // is map
            } else {
                yamlWriter.write(tabs + prefix + name + ":\n");
                for (Iterator i = elementIterator(element); i.hasNext();) {
                    convert((Element) i.next(), (isInList ? "  " : "") + tabs + tab, false, yamlWriter, tab);
                }
            }
        }
    }

    private boolean isConfigurationNotYamlSafe(Element element, String tabs, String tab) throws IOException {
        StringWriter configWriter = new StringWriter();
        Element testing = element.getOwnerDocument().createElement("testing");
        for (Element kid : elementList(element)) {
            testing.appendChild(kid.cloneNode(true));
        }
        convert(testing, tabs + tab, false, configWriter, tab);

        try {
            String configAsXml = new YamlToXmlConverter().convert(new StringReader(configWriter.toString()), new ConverterOptions());
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(configAsXml)));
            Element config = (Element) doc.getDocumentElement().getElementsByTagName("testing").item(0);
            return !areElementsEqual(testing, config);
        }
        catch (InvalidFormatException e) {
            return true;
        }
        catch (RuntimeException ex) {
            return true;
        } catch (SAXException e) {
            return true;
        } catch (ParserConfigurationException e) {
            return true;
        }
    }

    static String elementToBlockString(Element root)
            throws IOException {
        /*
        //root.remove(root.getNamespace());
        //root.setQName(new QName(root.getName()));
        removeNamespaceFromElement(root);
        final StringWriter swriter = new StringWriter();
        final OutputFormat outformat = OutputFormat.createPrettyPrint();
        outformat.setSuppressDeclaration(true);
        outformat.setNewLineAfterNTags(0);
        outformat.setNewlines(false);
        outformat.setPadText(true);
        outformat.setTrimText(false);
        final XMLWriter writer = new XMLWriter(swriter, outformat);
        writer.write(root);
        writer.flush();
        return swriter.toString();
        */
        /*
        // Pretty-prints a DOM document to XML using DOM Load and Save's LSSerializer.
             // Note that the "format-pretty-print" DOM configuration parameter can only be set in JDK 1.6+.
             DOMImplementation domImplementation = root.getOwnerDocument().getImplementation();
             if (domImplementation.hasFeature("LS", "3.0") && domImplementation.hasFeature("Core", "2.0")) {
                 DOMImplementationLS domImplementationLS = (DOMImplementationLS) domImplementation.getFeature("LS", "3.0");
                 LSSerializer lsSerializer = domImplementationLS.createLSSerializer();
                 DOMConfiguration domConfiguration = lsSerializer.getDomConfig();
                 if (domConfiguration.canSetParameter("format-pretty-print", Boolean.TRUE)) {
                    lsSerializer.getDomConfig().setParameter("format-pretty-print", true);
                    lsSerializer.getDomConfig().setParameter("xml-declaration", false);
                    LSOutput lsOutput = domImplementationLS.createLSOutput();
                    lsOutput.setEncoding("UTF-8");
                    StringWriter stringWriter = new StringWriter();
                    lsOutput.setCharacterStream(stringWriter);
                    lsSerializer.write(root, lsOutput);
                    return stringWriter.toString();
                } else {
                    throw new RuntimeException("DOMConfiguration 'format-pretty-print' parameter isn't settable.");
                }
            } else {
                throw new RuntimeException("DOM 3.0 LS and/or DOM 2.0 Core not supported.");
            }
            */
        StringWriter out = new StringWriter();
        TransformerFactory tfactory = TransformerFactory.newInstance();
        Transformer serializer;
        try {
            serializer = tfactory.newTransformer();
            //Setup indenting to "pretty print"
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            DOMSource xmlSource = new DOMSource(root);
            StreamResult outputTarget = new StreamResult(out);
            serializer.transform(xmlSource, outputTarget);
        } catch (TransformerException e) {
            // this is fatal, just dump the stack and throw a runtime exception
            e.printStackTrace();

            throw new RuntimeException(e);
        }
        return out.toString();

    }

    static String indent(String text, String indent, String tab) {
        int oldIndent = 0;
        StringWriter block = new StringWriter();
        String[] lines = text.split("\n");
        for (String line : lines) {
            block.write(indent);
            if (oldIndent == 0) {
                oldIndent = firstNonSpace(line);
            }
            if (oldIndent > 0) {
                int firstNonSpace = firstNonSpace(line);
                if (firstNonSpace >= oldIndent) {
                    line = tab + line.substring(oldIndent);
                } else {
                    line = line.substring(firstNonSpace);
                }
            }
            block.write(line);
            block.write("\n");
        }
        return block.toString();
    }

    static int firstNonSpace(String text) {
        int pos = 0;
        for (int x = 0; x < text.length(); x++) {
            if (text.charAt(x) == ' ') {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    static boolean areElementsEqual(Element e1, Element e2) {
        if (e1.getAttributes().getLength() == e2.getAttributes().getLength()) {
            NamedNodeMap attrs1 = e1.getAttributes();
            NamedNodeMap attrs2 = e2.getAttributes();
            for (int x = 0; x < attrs1.getLength(); x++) {
                Attr a1 = (Attr) attrs1.item(x);
                Attr a2 = (Attr) attrs2.item(x);
                if (!a1.getName().equals(a2.getName()) || !a1.getValue().equals(a2.getValue())) {
                    return false;
                }
            }
        } else {
            return false;
        }


        List<Element> kids1 = elementList(e1);
        List<Element> kids2 = elementList(e2);

        if (kids1.isEmpty() && kids2.isEmpty()) {
            String text1 = trimmedContent(e1);
            String text2 = trimmedContent(e2);

            if (!text1.equals(text2)) {
                return false;
            }
        }
        if (kids1.size() == kids2.size()) {
            for (int x = 0; x < kids1.size(); x++) {
                if (!areElementsEqual(kids1.get(x), kids2.get(x))) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }


    private static String sanitizeScalar(String val) {
        boolean needsQuoted = false;
        for (char forbiddenChar : INVALID_SCALAR_CHARACTERS) {
            if (val.indexOf(forbiddenChar) > -1) {
                needsQuoted = true;
            }
        }

        if (needsQuoted) {
            val = convertToQuoted(val);
        }
        return val;
    }

    private static String convertToQuoted(String val) {
        val = val.replaceAll("\\\\", "\\\\");
        val = val.replaceAll("\"", "\\\"");
        val = "\"" + val + "\"";
        return val;
    }

    private static void printInlineMap(Element listItem, String tab, Writer yamlWriter) throws IOException {
        yamlWriter.write(tab + "{ ");
        for (Iterator i = elementIterator(listItem); i.hasNext();) {
            Element e = (Element) i.next();
            yamlWriter.write(e.getTagName() + ": " + sanitizeScalar(trimmedContent(e)));
            if (i.hasNext()) {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" }\n");
    }

    private static void printInlineList(Element listItem, Writer yamlWriter) throws IOException {
        yamlWriter.write(" [ ");
        for (Iterator i = elementIterator(listItem); i.hasNext();) {
            Element e = (Element) i.next();
            yamlWriter.write(sanitizeScalar(trimmedContent(e)));
            if (i.hasNext()) {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" ]\n");
    }

    private static boolean shouldInline(Element element, int startLength) {
        int length = startLength;
        if (elementList(element).isEmpty()) {
            return false;
        }
        for (Iterator i = elementIterator(element); i.hasNext() && length < MAX_LINE_LENGTH;) {
            Element e = (Element) i.next();
            if (!elementList(element).isEmpty()) {
                length = Integer.MAX_VALUE;
                break;
            } else {
                length += (e.getTagName() + ": " + trimmedContent(e)).length();
            }

        }
        return length < MAX_LINE_LENGTH;
    }

    private static boolean isList(Element element) {
        String name = element.getTagName();
        return (
                (name.endsWith("s") && hasChildElement(element, name.substring(0, name.length() - 1))) ||
                        (name.endsWith("ies") && hasChildElement(element, name.substring(0, name.length() - 3) + "y"))
        );
    }

    private static String trimmedContent(Element e) {
        String content = e.getTextContent();
        return content != null ? content.trim() : "";
    }

    private static boolean hasChildElement(Element e, final String tagName) {
        return !elementList(e, new ElementFilter() {
            public boolean shouldInclude(Element e) {
                return e.getTagName().equals(tagName);
            }
        }).isEmpty();
    }

    private static Iterator<Element> elementIterator(Element root) {
        return elementList(root).iterator();
    }

    private static List<Element> elementList(Element root) {
        return elementList(root, ALL_ELEMENTS_FILTER);
    }

    private static List<Element> elementList(Element root, ElementFilter filter) {
        final List<Element> result = new ArrayList<Element>();
        NodeList nl = root.getChildNodes();
        for (int x = 0; x < nl.getLength(); x++) {
            if (nl.item(x).getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) nl.item(x);
                if (filter.shouldInclude(e)) {
                    result.add(e);
                }
            }
        }
        return result;
    }

private static interface ElementFilter {
    boolean shouldInclude(Element e);

}

    private static final ElementFilter ALL_ELEMENTS_FILTER = new ElementFilter() {
        public boolean shouldInclude(Element e) {
            return true;
        }
    };

}