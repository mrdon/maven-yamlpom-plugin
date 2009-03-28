package org.twdata.maven.yamlpom;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;
import java.net.URISyntaxException;

/**
 *
 */
public abstract class AbstractConverterTestbase extends TestCase
{
    protected static File pathToFile(String path) throws Exception
    {
        URL url = YamlToXmlConverterTest.class.getResource(path);
        if (url == null)
        {
            throw new RuntimeException("Unable to locate "+path);
        }
        try
        {
            return new File(url.toURI());
        }
        catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }


    }
}