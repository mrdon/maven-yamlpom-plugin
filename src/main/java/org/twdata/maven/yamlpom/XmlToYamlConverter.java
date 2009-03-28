package org.twdata.maven.yamlpom;

import org.apache.commons.io.IOUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Iterator;
import java.util.Map;

/**
 *
 */
public class XmlToYamlConverter extends AbstractConverter<XmlToYamlConverter>
{
    private final int MAX_LINE_LENGTH = 120;
    private final char[] INVALID_SCALAR_CHARACTERS = new char[] {':', '#', '[', ']', '{', '}', ',', '*', '\t'};


    protected boolean isValidTargetContents(String yamlText)
    {
        Yaml yaml = YamlUtils.buildYaml();
        try
        {
            Object obj = yaml.load(yamlText);
            if (obj instanceof Map)
            {
                return true;
            }
        }
        catch (RuntimeException ex)
        {
            log.error("Generated YAML is not valid: \n" + yamlText, ex);
        }
        return false;
    }

    protected String buildTarget(File pomFile) throws IOException
    {
        StringWriter yamlWriter = null;
        Reader xmlReader = null;
        try
        {
            xmlReader = new FileReader(pomFile);
            yamlWriter = new StringWriter();

            Document doc = new SAXReader().read(xmlReader);

            for (Iterator it = doc.getRootElement().elementIterator(); it.hasNext(); ) {
                Element element = (Element) it.next();
                if (!"modelVersion".equals(element.getName()))
                {
                    convert(element, "", false, yamlWriter);
                }
            }
        }
        catch (DocumentException e)
        {
            log.error(pomFile.getName() + " is not valid", e);
            throw new RuntimeException(e);
        }
        finally
        {
            IOUtils.closeQuietly(yamlWriter);
            IOUtils.closeQuietly(xmlReader);
        }
        return yamlWriter.toString();
    }

    private void convert(Element element, String tabs, boolean isInList, Writer yamlWriter) throws IOException
    {
        if (element != null)
        {
            String name = element.getName();
            String prefix = isInList ? "- " : "";

            if ("configuration".equals(name))
            {

                StringWriter configWriter = new StringWriter();
                for (Iterator i = element.elementIterator(); i.hasNext(); )
                {
                    configWriter.append(elementToBlockString((Element) i.next()));
                }
                yamlWriter.write(tabs + "configuration : |\n");
                yamlWriter.write(indent(configWriter.toString(), tabs + tab));

                return;
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
                                convert(listItem, (!isFirst ? "  " : "")  + tabs + tab, isFirst, yamlWriter);
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
                    convert((Element) i.next(), tabs + tab, false, yamlWriter);
                }
            }
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
        outformat.setNewLineAfterNTags(1);
        outformat.setNewlines(true);
        final XMLWriter writer = new XMLWriter(swriter, outformat);
        writer.write(root);
        writer.flush();
        return swriter.toString();
    }

    static String indent(String text, String indent)
    {
        StringWriter block = new StringWriter();
        String[] lines = text.split("\n");
        for (String line : lines)
        {
            block.write(indent);
            block.write(line);
            block.write("\n");
        }
        return block.toString();
    }


    static void removeNamespaceFromElement(Element node)
    {
        node.setQName(new QName(node.getName()));
        for (Iterator i = node.elementIterator(); i.hasNext(); )
        {
            removeNamespaceFromElement((Element)i.next());
        }
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