package org.twdata.maven.yamlpom;

import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class XmlToYamlConverter implements Converter
{
    private final int MAX_LINE_LENGTH = 120;
    private final char[] INVALID_SCALAR_CHARACTERS = new char[] {':', '#', '[', ']', '{', '}', ',', '*', '\t'};


    private void validateTargetContents(String yamlText) throws InvalidFormatException
    {
        Yaml yaml = YamlUtils.buildYaml();
        Object obj = null;
        try
        {
            obj = yaml.load(yamlText);
        }
        catch (RuntimeException ex)
        {
            throw new InvalidFormatException("Invalid YAML", yamlText, ex);
        }

        if (!(obj instanceof Map))
        {
            throw new InvalidFormatException("YAML file not a map", yamlText);
        }
    }

    public String convert(Reader xmlReader, ConverterOptions options) throws InvalidFormatException, IOException
    {
        StringWriter yamlWriter = new StringWriter();

        try
        {
            SAXReader sax = new SAXReader();
            sax.setStripWhitespaceText(false);
            sax.setMergeAdjacentText(false);
            sax.setIgnoreComments(false);
            Document doc = new SAXReader().read(xmlReader);

            for (Iterator it = doc.getRootElement().elementIterator(); it.hasNext(); ) {
                Element element = (Element) it.next();
                if (!"modelVersion".equals(element.getName()))
                {
                    convert(element, "", false, yamlWriter, options.getIndent());
                }
            }
        }
        catch (DocumentException e)
        {
            throw new InvalidFormatException("POM XML is not valid", null, e);
        }
        String text = yamlWriter.toString();
        validateTargetContents(text);
        return text;
    }

    private void convert(Element element, String tabs, boolean isInList, Writer yamlWriter, String tab) throws IOException
    {
        if (element != null)
        {
            String name = element.getName();
            String prefix = isInList ? "- " : "";

            if ("configuration".equals(name))
            {
                if (isConfigurationNotYamlSafe(element, tabs, tab))
                {
                    yamlWriter.write(tabs + prefix + "configuration : |\n");
                    StringWriter blockWriter = new StringWriter();
                    for (Iterator i = element.elementIterator(); i.hasNext(); )
                    {
                        blockWriter.append(elementToBlockString((Element) i.next()));
                    }
                    yamlWriter.write(indent(blockWriter.toString(), (isInList ? "  " : "") + tabs + tab, tab));
                    return;
                }
            }

            // is scalar
            if (element.elements().isEmpty())
            {
                yamlWriter.write(tabs + prefix + name + ": " + sanitizeScalar(element.getTextTrim()) + "\n");
            }
            // is list
            else if (isList(element))
            {
                yamlWriter.write(tabs + prefix + name + ":");
                if (shouldInline(element, (tabs + tab + "  ").length()))
                {
                    printInlineList(element, yamlWriter);
                }
                else
                {
                    yamlWriter.write("\n");
                    for (Iterator i = element.elementIterator(); i.hasNext(); )
                    {
                        Element list = (Element) i.next();
                        if (shouldInline(list, (tabs + tab + "  ").length()))
                        {
                            printInlineMap(list, tabs + tab + "- ", yamlWriter);
                        }
                        // is scalar entry
                        else if (list.elements().isEmpty())
                        {
                            yamlWriter.write(tabs + tab + "- " + list.getTextTrim() + "\n");
                        }
                        else
                        {
                            boolean isFirst = true;
                            for (Iterator it = list.elementIterator(); it.hasNext(); )
                            {
                                Element listItem = (Element)it.next();
                                convert(listItem, (!isFirst ? "  " : "")  + tabs + tab, isFirst, yamlWriter, tab);
                                isFirst = false;
                            }
                        }
                    }
                }
            // is map
            } else
            {
                yamlWriter.write(tabs + prefix + name + ":\n");
                for (Iterator i = element.elementIterator(); i.hasNext(); )
                {
                    convert((Element) i.next(), (isInList ? "  " : "") + tabs + tab, false, yamlWriter, tab);
                }
            }
        }
    }

    private boolean isConfigurationNotYamlSafe(Element element, String tabs, String tab) throws IOException
    {
        StringWriter configWriter = new StringWriter();

        element.setName("testing");
        convert(element, tabs + tab, false, configWriter, tab);

        try
        {
            String configAsXml = new YamlToXmlConverter().convert(new StringReader(configWriter.toString()), new ConverterOptions());
            Document doc = DocumentHelper.parseText(configAsXml);
            Element config = doc.getRootElement().element("testing");
            element.setName("configuration");
            return !areElementsEqual(element, config);
        }
        catch (InvalidFormatException e)
        {
            return true;
        }
        catch (DocumentException e)
        {
            return true;
        }
        catch (RuntimeException ex)
        {
            return true;
        }
    }

    static String elementToBlockString(Element root)
            throws IOException
    {
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
    }

    static String indent(String text, String indent, String tab)
    {
        int oldIndent = 0;
        StringWriter block = new StringWriter();
        String[] lines = text.split("\n");
        for (String line : lines)
        {
            block.write(indent);
            if (oldIndent == 0)
            {
                oldIndent = firstNonSpace(line);
            }
            if (oldIndent > 0)
            {
                int firstNonSpace = firstNonSpace(line);
                if (firstNonSpace >= oldIndent)
                {
                    line = tab + line.substring(oldIndent);
                }
                else
                {
                    line = line.substring(firstNonSpace);
                }
            }
            block.write(line);
            block.write("\n");
        }
        return block.toString();
    }

    static int firstNonSpace(String text)
    {
        int pos = 0;
        for (int x=0; x<text.length(); x++)
        {
            if (text.charAt(x) == ' ')
            {
                pos++;
            }
            else
            {
                break;
            }
        }
        return pos;
    }


    static void removeNamespaceFromElement(Element node)
    {
        node.setQName(new QName(node.getName()));
        for (Iterator i = node.elementIterator(); i.hasNext(); )
        {
            removeNamespaceFromElement((Element)i.next());
        }
    }

    static boolean areElementsEqual(Element e1, Element e2)
    {
        if (e1.attributeCount() == e2.attributeCount())
        {
            for (int x=0; x<e1.attributeCount(); x++)
            {
                Attribute a1 = e1.attribute(x);
                Attribute a2 = e2.attribute(x);
                if (!a1.getName().equals(a2.getName()) || !a1.getValue().equals(a2.getValue()))
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }

        String text1 = e1.getText() == null ? "" : e1.getText().trim();
        String text2 = e2.getText() == null ? "" : e2.getText().trim();

        if (!text1.equals(text2))
        {
            return false;
        }

        List<Element> kids1 = e1.elements();
        List<Element> kids2 = e2.elements();
        if (kids1.size() == kids2.size())
        {
            for (int x=0; x<kids1.size(); x++)
            {
                if (!areElementsEqual(kids1.get(x), kids2.get(x)))
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }
        return true;
    }


    private String sanitizeScalar(String val)
    {
        boolean needsQuoted = false;
        for (char forbiddenChar : INVALID_SCALAR_CHARACTERS)
        {
            if (val.indexOf(forbiddenChar) > -1)
            {
                needsQuoted = true;
            }
        }

        if (needsQuoted)
        {
            val = convertToQuoted(val);
        }
        return val;
    }

    private static String convertToQuoted(String val)
    {
        val = val.replaceAll("\\\\", "\\\\");
        val = val.replaceAll("\"", "\\\"");
        val = "\"" + val + "\"";
        return val;
    }

    private void printInlineMap(Element listItem, String tab, Writer yamlWriter) throws IOException
    {
        yamlWriter.write(tab + "{ ");
        for (Iterator i = listItem.elementIterator(); i.hasNext(); )
        {
            Element e = (Element) i.next();
            yamlWriter.write(e.getName() + ": " + sanitizeScalar(e.getTextTrim()));
            if (i.hasNext())
            {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" }\n");
    }

    private void printInlineList(Element listItem, Writer yamlWriter) throws IOException
    {
        yamlWriter.write(" [ ");
        for (Iterator i = listItem.elementIterator(); i.hasNext(); )
        {
            Element e = (Element) i.next();
            yamlWriter.write(sanitizeScalar(e.getTextTrim()));
            if (i.hasNext())
            {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" ]\n");
    }

    private boolean shouldInline(Element element, int startLength)
    {
        int length = startLength;
        if (element.elements().isEmpty())
        {
            return false;
        }
        for (Iterator i = element.elementIterator(); i.hasNext() && length < MAX_LINE_LENGTH; )
        {
            Element e = (Element) i.next();
            if (!e.elements().isEmpty())
            {
                length = Integer.MAX_VALUE;
                break;
            }
            else
            {
                length += (e.getName() + ": " + e.getTextTrim()).length();
            }

        }
        return length < MAX_LINE_LENGTH;
    }

    private boolean isList(Element element)
    {
        String name = element.getName();
        return (
                (name.endsWith("s") && !element.elements(name.substring(0, name.length() - 1)).isEmpty()) ||
                (name.endsWith("ies") && !element.elements(name.substring(0, name.length() - 3) + "y").isEmpty())
        );
    }

}