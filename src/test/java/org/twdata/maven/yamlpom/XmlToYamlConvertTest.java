package org.twdata.maven.yamlpom;

import org.yaml.snakeyaml.Yaml;

import java.util.List;
import java.util.Map;

/**
 *
 */
public class XmlToYamlConvertTest extends AbstractConverterTestbase
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

    public void testConvertConfigAsXml() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.config.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(0);
        String config = (String) plugin.get("configuration");
        assertNotNull(config);
        //System.out.println(config);
        assertTrue(config.contains("<tasks>"));
    }

    public void testConvertConfigAsYaml() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.config.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(1);
        Object config = plugin.get("configuration");
        assertNotNull(config);
        assertTrue(config instanceof Map);
        assertEquals("1.5", ((Map<String,String>)config).get("source"));
        assertEquals("1.5", ((Map<String,String>)config).get("target"));
    }

    public void testConvertConfigAsYamlWithSingleAttribute() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.config.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(2);
        Object config = plugin.get("configuration");
        assertNotNull(config);
        assertTrue(config instanceof String);
        //System.out.println(config);
        assertTrue(config.toString().contains("implementation=\"org.apache"));
    }

    public void testConvertConfigAsYamlWithConfigAsFirstElement() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.config.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(3);
        Object exec = ((List<Object>)plugin.get("executions")).get(0);
        assertNotNull(exec);
        assertTrue(exec instanceof Map);
        //System.out.println(config);
        Object config = ((Map<String,Object>)exec).get("configuration");
        assertNotNull(config);
        assertTrue(config instanceof Map);
        assertEquals("true", ((Map<String,String>)config).get("createDependencyReducedPom"));

        Object goals = ((Map<String,Object>)exec).get("goals");
        assertNotNull(goals);
        assertEquals(1, ((List<String>)goals).size());
        assertEquals("go", ((List<String>)goals).get(0));
    }

    public void testConvertConfigAsStringWithConfigAsFirstElement() throws Exception
    {
        Map<String, Object> data = buildYaml("/pom.config.xml");

        Map plugin = (Map) ((List)((Map)data.get("build")).get("plugins")).get(4);
        Object exec = ((List<Object>)plugin.get("executions")).get(0);
        assertNotNull(exec);
        assertTrue(exec instanceof Map);
        //System.out.println(config);
        Object config = ((Map<String,Object>)exec).get("configuration");
        assertNotNull(config);
        assertTrue(config instanceof String);
        assertTrue(config.toString().contains("implementation=\"org.apache"));

        Object goals = ((Map<String,Object>)exec).get("goals");
        assertNotNull(goals);
        assertEquals(1, ((List<String>)goals).size());
        assertEquals("go", ((List<String>)goals).get(0));
    }

    private Map<String, Object> buildYaml(String path) throws Exception
    {
        ConverterOptions opt = new ConverterOptions().indent("  ");
        String yamlText = new XmlToYamlConverter().convert(pathToReader(path), opt);

        Yaml yaml = YamlUtils.buildYaml();
        //System.out.println(yamlText);
        Map<String,Object> data = (Map<String,Object>) yaml.load(yamlText);
        return data;
    }
}
