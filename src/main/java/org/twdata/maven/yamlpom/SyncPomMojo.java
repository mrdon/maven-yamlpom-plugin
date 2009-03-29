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
import static org.twdata.maven.yamlpom.ConverterBuilder.convertYamlToXml;
import static org.twdata.maven.yamlpom.ConverterBuilder.convertXmlToYaml;

import java.io.*;

/**
 * Goal which touches a timestamp file.
 *
 * @goal sync
 * @phase initialize
 * @aggregator
 * @requiresProject false
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

    /**
     * Instructs the plugin to stop execution if the pom XML is generated
     *
     * @parameter expression="${yamlpom.failIfXmlSync}"
     */
    private boolean failIfXmlSync = true;

    /**
     * Instructs the plugin to stop execution it detects changes that it cannot sync
     *
     * @parameter expression="${yamlpom.failIfCannotSync}"
     */
    private boolean failIfCannotSync = true;



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
                    sync(xmlFile, yamlFile, syncFile, false);
                    syncManager.save();
                    break;
                case XML:
                    getLog().info("Converting "+yamlFile.getName() + " into " + xmlFile.getName());
                    sync(xmlFile, yamlFile, syncFile, true);
                    syncManager.save();
                    if (failIfXmlSync)
                    {
                        throw new MojoExecutionException("pom.xml modified.  You must retry your Maven command.");
                    }
                    break;
                case SYNC_FILE_ONLY:
                    getLog().info("Files in sync, creating a sync file");
                    syncManager.save();
                    break;
                case NONE:
                    getLog().info("No sync required");
                    break;
                case UNKNOWN:
                    if (failIfCannotSync)
                        throw new MojoExecutionException("Unable to automatically sync");
                    else
                        getLog().error("Unable to automatically sync");
            }
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("Error syncing YAML pom", e);
        }
        catch (InvalidFormatException e)
        {
            throw new MojoExecutionException("Unable to create or parse a valid format: \n" + e.getText(), e);
        }
    }

    private void sync(File xmlFile, File yamlFile, File syncFile, boolean xmlFirst) throws IOException, InvalidFormatException
    {
        if (xmlFirst)
        {
            convertYamlToXml()
                .indentSpaces(xmlIndent)
                .fromFile(yamlFile)
                .toFile(xmlFile)
                .logWith(getLog())
                .convert();
            convertXmlToYaml()
                .indentSpaces(yamlIndent)
                .fromFile(xmlFile)
                .toFile(yamlFile)
                .logWith(getLog())
                .convert();
        }
        else
        {
            convertXmlToYaml()
                .indentSpaces(yamlIndent)
                .fromFile(xmlFile)
                .toFile(yamlFile)
                .logWith(getLog())
                .convert();
            convertYamlToXml()
                .indentSpaces(xmlIndent)
                .fromFile(yamlFile)
                .toFile(xmlFile)
                .logWith(getLog())
                .convert();
        }
    }
}
