package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;
import java.util.Collection;

/**
 *
 */
public class YamlToPomConverter
{
    private final String tab;

    public YamlToPomConverter(String tab)
    {
        this.tab = tab;
    }

    public void convert(File yamlFile, File pomFile) throws MojoExecutionException
    {
        Reader yamlReader = null;
        Writer xmlWriter = null;
        try
        {
            xmlWriter = new FileWriter(pomFile);
            yamlReader = new FileReader(yamlFile);
            Yaml yaml = new Yaml();
            Object yamlPom = yaml.load(yamlReader);

            xmlWriter.write(
                    "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
                    tab + tab + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    tab + tab + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">\n" +
                    tab + "<modelVersion>4.0.0</modelVersion>\n");

            convert((Map<String,Object>) yamlPom, tab, xmlWriter);
            xmlWriter.write("</project>\n");
        }
        catch (FileNotFoundException e)
        {
            throw new MojoExecutionException("no file", e);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("something", e);
        }
        finally
        {
            IOUtils.closeQuietly(yamlReader);
            IOUtils.closeQuietly(xmlWriter);
        }
        pomFile.setLastModified(yamlFile.lastModified());
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
                writer.write(tabs + "<" + key + ">" + value + "</" + key + ">\n");
            }
        }
    }
}
