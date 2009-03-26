package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.resolver.Resolver;

import java.io.*;
import java.util.Map;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 *
 */
public class YamlToPomConverter
{
    private final String tab;

    public YamlToPomConverter(String tab)
    {
        this.tab = "    ";
    }

    public void convert(File yamlFile, File pomFile) throws MojoExecutionException
    {
        Reader yamlReader = null;
        Writer xmlWriter = null;
        File pomTmpFile = null;
        try
        {
            pomTmpFile = File.createTempFile("pom", "xml");
            xmlWriter = new FileWriter(pomTmpFile);
            yamlReader = new FileReader(yamlFile);
            Yaml yaml = buildYaml();
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
        if (pomTmpFile != null)
        {
            try
            {
                FileUtils.copyFile(pomTmpFile, pomFile);
                pomFile.setLastModified(yamlFile.lastModified());
            }
            catch (IOException e)
            {
                throw new MojoExecutionException("Cannot copy pom tmp", e);
            }
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

    Yaml buildYaml()
    {
        Loader myLoader = new Loader();
        Yaml yaml = new Yaml(myLoader);

        // Don't let the YAML parser try to guess things.  Will screw up things like version numbers that look like
        // 1.00 by converting them to an int "1.0"
        myLoader.setResolver(new Resolver()
        {
            @Override
            public String resolve(NodeId kind, String value, boolean implicit)
            {
                String tag = super.resolve(kind, value, implicit);
                if (implicit)
                {
                    if (tag.equals("tag:yaml.org,2002:bool") ||
                        tag.equals("tag:yaml.org,2002:float") ||
                        tag.equals("tag:yaml.org,2002:int") ||
                        tag.equals("tag:yaml.org,2002:timestamp") ||
                        tag.equals("tag:yaml.org,2002:value"))
                    {
                        return "tag:yaml.org,2002:str";
                    }
                }
                return tag;
            }
        });
        return yaml;
    }
}
