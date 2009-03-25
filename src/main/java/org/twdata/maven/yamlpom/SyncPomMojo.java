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
        File pomFile = new File("pom.xml");

        File yamlFile = new File(yamlPomName);
        if (!yamlFile.exists())
        {
            PomToYamlConverter converter = new PomToYamlConverter(tab);
            converter.convert(pomFile, yamlFile);
            return;
        }
        if (pomFile.exists() && pomFile.lastModified() > yamlFile.lastModified())
        {
            throw new MojoExecutionException("pom.xml is newer than "+yamlPomName+", will not overwrite pom.xml");
        }
        
        YamlToPomConverter converter = new YamlToPomConverter(tab);
        converter.convert(yamlFile, pomFile);
    }
}
