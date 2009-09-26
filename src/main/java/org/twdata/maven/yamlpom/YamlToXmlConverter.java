package org.twdata.maven.yamlpom;

import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;
import org.dom4j.DocumentException;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class YamlToXmlConverter implements Converter
{
    public String convert(Reader from, ConverterOptions options) throws InvalidFormatException, IOException
    {
        StringWriter xmlWriter = null;
        String tab = options.getIndent();
        try
        {
            xmlWriter = new StringWriter();
            Yaml yaml = YamlUtils.buildYaml();
            Object yamlPom = yaml.load(from);

            xmlWriter.write(
                    "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
                    "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                    tab + "<modelVersion>4.0.0</modelVersion>\n");

            convert((Map<String,Object>) yamlPom, tab, xmlWriter, options.getIndent());
            xmlWriter.write("</project>\n");
        }
        finally
        {
            IOUtils.closeQuietly(xmlWriter);
        }

        String text = xmlWriter.toString();
        validateTargetContents(text);
        return text;
    }

    private void validateTargetContents(String text) throws InvalidFormatException
    {
        try
        {
            SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(new StringReader(text)), new DefaultHandler());
        } catch (SAXException e) {
            throw new InvalidFormatException("Target XML is not well-formed", text, e);
        } catch (ParserConfigurationException e) {
            throw new InvalidFormatException("Target XML is not well-formed", text, e);
        } catch (IOException e) {
            throw new InvalidFormatException("Target XML is not well-formed", text, e);
        }
    }

    private void convert(Map<String,Object> map, String tabs, Writer writer, String tab) throws IOException
    {
        for (Map.Entry<String,Object> entry : map.entrySet())
        {
            String key = entry.getKey().trim();
            Object value = entry.getValue();
            if (value instanceof Map)
            {
                writer.write(tabs);
                writer.write("<" + key + ">\n");
                convert((Map<String,Object>)value, tabs + tab, writer, tab);
                writer.write(tabs);
                writer.write("</" + key + ">\n");
            }
            else if (value instanceof Collection)
            {
                writer.write(tabs + "<" + key + ">\n");
                String singleName = (key.endsWith("ies") ? key.substring(0, key.length() - 3) + "y" : key.substring(0, key.length() - 1));
                for (Object item : (Collection)value)
                {
                    if (item instanceof Map)
                    {
                        writer.write(tabs + tab + "<" + singleName + ">\n");
                        convert((Map<String,Object>) item, tabs + tab + tab, writer, tab);
                        writer.write(tabs + tab + "</" + singleName + ">\n");
                    }
                    else
                    {
                        writer.write(tabs + tab + "<" + singleName + ">" + text(item) + "</" + singleName + ">\n");
                    }
                }
                writer.write(tabs + "</" + key + ">\n");
            }
            else
            {
                String text = indent((value != null ? value.toString() : ""), tabs + tab);
                if (text.endsWith("\n"))
                {
                    text += tabs;
                }
                writer.write(tabs + "<" + key + ">" + text(text) + "</" + key + ">\n");
            }
        }
    }

    static String text(Object obj) {
        String text = obj.toString();
        if (text.indexOf("<") > -1 && text.indexOf("<![CDATA[") == -1) {
            return "<![CDATA[" + text + "]]>";
        } else {
            return text;
        }
    }

    static String indent(String text, String tab)
    {
        StringWriter block = new StringWriter();
        String[] lines = text.split("\n");
        if (lines.length > 1)
        {
            for (String line : lines)
            {
                block.write(tab);
                block.write(line);
                block.write("\n");
            }
        }
        else
        {
            block.write(text);
        }
        return block.toString();
    }
}
