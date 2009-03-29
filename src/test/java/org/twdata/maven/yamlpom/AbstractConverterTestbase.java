package org.twdata.maven.yamlpom;

import junit.framework.TestCase;

import java.io.File;
import java.io.Reader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URISyntaxException;

/**
 *
 */
public abstract class AbstractConverterTestbase extends TestCase
{
    protected static Reader pathToReader(String path) throws Exception
    {
        URL url = YamlToXmlConverterTest.class.getResource(path);
        if (url == null)
        {
            throw new RuntimeException("Unable to locate "+path);
        }
        return new InputStreamReader(url.openStream());
    }
}