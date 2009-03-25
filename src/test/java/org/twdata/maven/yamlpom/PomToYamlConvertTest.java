package org.twdata.maven.yamlpom;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.util.Map;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
 *
 */
public class PomToYamlConvertTest extends AbstractConverterTestBase
{
    public void testConvertSimple() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.good.xml");

        assertEquals("org.twdata.maven", data.get("groupId"));
        assertEquals("maven-yamlpom-plugin", data.get("artifactId"));
        assertEquals("1.0-SNAPSHOT", data.get("version"));
        assertEquals("maven-plugin", data.get("packaging"));
    }

    public void testConvertSelf() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.self.xml");

        assertEquals("org.twdata.maven", data.get("groupId"));
        assertEquals("maven-yamlpom-plugin", data.get("artifactId"));
        assertEquals("1.0-SNAPSHOT", data.get("version"));
        assertEquals("maven-plugin", data.get("packaging"));

        assertTrue(data.get("dependencies") instanceof List);
    }

    private Map<String, Object> buildYaml(String path) throws Exception
    {
        PomToYamlConverter converter = new PomToYamlConverter("  ");
        File yamlFile = File.createTempFile("pom", ".yaml");
        converter.convert(pathToFile(path), yamlFile);

        Yaml yaml = new Yaml();
        String yamlText = FileUtils.readFileToString(yamlFile);
        System.out.println(yamlText);
        Map<String,Object> data = (Map<String,Object>) yaml.load(yamlText);
        return data;
    }
}
