package org.twdata.maven.yamlpom;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Collections;

import org.apache.commons.io.FileUtils;
import org.dom4j.*;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;


/**
 *
 */
public class YamlToXmlConverterTest extends AbstractConverterTestbase
{
    public void testConvert() throws Exception
    {
        convertTest("/pom.good.yml", "x:groupId='org.twdata.maven'",
                                     "x:artifactId='maven-yamlpom-plugin'",
                                     "x:version='1.00'",
                                     "x:packaging='maven-plugin'");
        
    }

    public void testConvertWithEmbeddedXml() throws Exception
    {
        convertTest("/pom.config.yml", "//x:plugin/x:executions/x:execution/x:configuration/x:tasks/x:mkdir");

    }

    private void convertTest(String path, String... xpathList) throws Exception
    {
        File pom = File.createTempFile("pom", ".xml");
        new YamlToXmlConverter()
                .indentSpaces(2)
                .fromFile(pathToFile(path))
                .targetFile(pom)
                .convert();

        Document doc = DocumentHelper.parseText(FileUtils.readFileToString(pom));
        for (final String xp : xpathList)
        {
            final XPath xpath = DocumentHelper.createXPath(xp);
            xpath.setNamespaceURIs(Collections.singletonMap("x", "http://maven.apache.org/POM/4.0.0"));
            final Object obj = xpath.evaluate(doc.getRootElement());
            if (obj instanceof Node)
            {
                // test passed
            }
            else if (obj instanceof Boolean)
            {
                if (!((Boolean) obj).booleanValue())
                {
                    printDocument(doc);
                    Assert.fail("Unable to match xpath: " + xp);
                }
            }
            else if (obj == null)
            {
                printDocument(doc);
                Assert.fail("Unable to match xpath: " + xp);
            }
            else if (obj instanceof List)
            {
                if (((List)obj).isEmpty())
                {
                    printDocument(doc);
                    Assert.fail("Unable to match xpath: " + xp);
                }
            }
            else
            {
                printDocument(doc);
                Assert.fail("Unexpected result:" + obj);
            }
        }
    }

    static String elementToString(Element pluginRoot)
            throws IOException
    {
        final StringWriter swriter = new StringWriter();
        final OutputFormat outformat = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(swriter, outformat);
        writer.write(pluginRoot.getDocument());
        writer.flush();
        return swriter.toString();
    }

    private static void printDocument(final Document springDoc) throws IOException
    {
        final OutputFormat outformat = OutputFormat.createPrettyPrint();
        final XMLWriter writer = new XMLWriter(System.out, outformat);
        writer.write(springDoc);
        writer.flush();
    }

}
