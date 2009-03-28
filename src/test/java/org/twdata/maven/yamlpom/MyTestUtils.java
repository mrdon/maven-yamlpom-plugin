package org.twdata.maven.yamlpom;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class MyTestUtils
{
    public static File createTempDirectory(String name) throws IOException
    {
        File tmpDir = new File("target" + File.separator + "tmp" + File.separator + name).getAbsoluteFile();
        if (tmpDir.exists())
        {
            FileUtils.cleanDirectory(tmpDir);
        }
        else
        {
            tmpDir.mkdirs();
        }
        return tmpDir;
    }

}
