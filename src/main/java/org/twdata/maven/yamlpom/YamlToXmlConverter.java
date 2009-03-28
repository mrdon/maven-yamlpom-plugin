package org.twdata.maven.yamlpom;

import org.apache.commons.io.IOUtils;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Collection;
import java.util.Map;

/**
 *
 */
public class YamlToXmlConverter extends AbstractConverter<YamlToXmlConverter>
{
    protected String buildTarget(File fromFile) throws IOException
    {
        Reader yamlReader = null;
        StringWriter xmlWriter = null;
        try
        {
            xmlWriter = new StringWriter();
            yamlReader = new FileReader(fromFile);
            Yaml yaml = YamlUtils.buildYaml();
            Object yamlPom = yaml.load(yamlReader);

            xmlWriter.write(
                    "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                    tab + tab + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    tab + tab + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                    tab + "<modelVersion>4.0.0</modelVersion>\n");

            convert((Map<String,Object>) yamlPom, tab, xmlWriter);
            xmlWriter.write("</project>\n");
        }
        finally
        {
            IOUtils.closeQuietly(yamlReader);
            IOUtils.closeQuietly(xmlWriter);
        }
        return xmlWriter.toString();
    }

    protected boolean isValidTargetContents(String text)
    {
        try
        {
            new SAXReader().read(new StringReader(text));
            return true;
        }
        catch (DocumentException e)
        {
            log.error("Generated XML is not valid", e);
            return false;
        }
    }

    private void convert(Map<String,Object> map, String tabs, Writer writer) throws IOException
    {
        for (Map.Entry<String,Object> entry : map.entrySet())
        {
            String key = entry.getKey().trim();
            Object value = entry.getValue();
            if (value instanceof Map)
            {
                writer.write(tabs);
                writer.write("<" + key + ">\n");
                convert((Map<String,Object>)value, tabs + tab, writer);
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
                        convert((Map<String,Object>) item, tabs + tab + tab, writer);
                        writer.write(tabs + tab + "</" + singleName + ">\n");
                    }
                    else
                    {
                        writer.write(tabs + tab + "<" + singleName + ">" + item + "</" + singleName + ">\n");
                    }
                }
                writer.write(tabs + "</" + key + ">\n");
            }
            else
            {
                String text = indent(value.toString(), tabs + tab);
                if (text.endsWith("\n"))
                {
                    text += tabs;
                }
                writer.write(tabs + "<" + key + ">" + text + "</" + key + ">\n");
            }
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
