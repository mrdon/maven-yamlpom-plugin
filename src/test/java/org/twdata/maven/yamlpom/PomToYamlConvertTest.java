package org.twdata.maven.yamlpom;

import java.io.File;
import java.util.Map;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.yaml.snakeyaml.Yaml;

/**
 *
 */
public class PomToYamlConvertTest extends AbstractConverterTestbase
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

    public void testConvertScalarList() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.scalarlist.xml");

        assertTrue(data.get("properties") instanceof List);
        assertEquals("foo", ((List)data.get("properties")).get(0));
    }

    public void testConvertAttributes() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.attrs.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(0);
        Map tasks = (Map) ((Map)plugin.get("configuration")).get("tasks");
        
        assertTrue(data.get("properties") instanceof List);
        assertEquals("foo", ((List)data.get("properties")).get(0));


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
