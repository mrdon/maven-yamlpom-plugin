package org.twdata.maven.yamlpom;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.IOUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Map;
import java.util.Collection;

/**
 * Goal which touches a timestamp file.
 *
 * @goal sync
 * @phase process-sources
 */
public class SyncPomMojo extends AbstractMojo
{
    /**
     * Location of the file.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Yaml pom file
     *
     * @parameter expression="${pom.yaml}"
     */
    private String yamlPomName = "pom.yaml";

    /**
     * Yaml tab
     *
     * @parameter expression="${yamlpom.tab}"
     */
    private String tab = "    ";

    public void execute() throws MojoExecutionException
    {
        File yamlFile = new File(yamlPomName);
        if (!yamlFile.exists())
        {
            throw new MojoExecutionException("No YAML POM file: " + yamlPomName);
        }
        File pomFile = new File("pom.xml");
        if (!pomFile.exists())
        {
            throw new MojoExecutionException("Cannot find pom.xml");
        }

        if (pomFile.lastModified() > yamlFile.lastModified())
        {
            throw new MojoExecutionException("pom.xml is newer than "+yamlPomName+", will not overwrite pom.xml");
        }
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
            xmlWriter.write("</project>");
            System.out.println("POM:\n" + xmlWriter.toString());
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
