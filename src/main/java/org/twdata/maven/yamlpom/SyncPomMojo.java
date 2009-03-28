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

import java.io.*;

/**
 * Goal which touches a timestamp file.
 *
 * @goal sync
 * @phase initialize
 */
public class SyncPomMojo extends AbstractMojo
{
    /**
     * Yaml pom file
     *
     * @parameter expression="${pom.yaml}"
     */
    private String yamlPomName = "pom.yml";

    /**
     * Sync file name
     *
     * @parameter expression="${yamlpom.syncfile}"
     */
    private String syncFileName = ".pom.yml";

    /**
     * Number of spaces to indent YAML with
     *
     * @parameter expression="${yamlpom.yaml.indent}"
     */
    private int yamlIndent = 2;

    /**
     * Number of spaces to indent XML with
     *
     * @parameter expression="${yamlpom.xml.indent}"
     */
    private int xmlIndent = 4;

    public void execute() throws MojoExecutionException
    {
        File xmlFile = new File("pom.xml");

        File yamlFile = new File(yamlPomName);
        File syncFile = new File(syncFileName);

        SyncManager syncManager = new SyncManager(xmlFile, yamlFile, syncFile);

        try
        {
            switch (syncManager.determineFormatToTarget())
            {
                case YAML:
                    getLog().info("Converting "+xmlFile.getName() + " into " + yamlFile.getName());
                    new XmlToYamlConverter()
                        .indentSpaces(yamlIndent)
                        .fromFile(xmlFile)
                        .targetFile(yamlFile)
                        .syncFile(syncFile)
                        .logWith(getLog())
                        .convert();
                    syncManager.save();
                    break;
                case XML:
                    getLog().info("Converting "+yamlFile.getName() + " into " + xmlFile.getName());
                    new YamlToXmlConverter()
                        .indentSpaces(xmlIndent)
                        .fromFile(yamlFile)
                        .targetFile(xmlFile)
                        .syncFile(syncFile)
                        .logWith(getLog())
                        .convert();
                    syncManager.save();
                    break;
                case SYNC_FILE_ONLY:
                    getLog().info("Files in sync, creating a sync file");
                    syncManager.save();
                    break;
                case NONE:
                    getLog().info("No sync required");
                    break;
                case UNKNOWN:
                    getLog().error("Unable to automatically sync");
                    throw new MojoExecutionException("Unable to automatically sync");
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error syncing YAML pom", e);
        }
    }
}
