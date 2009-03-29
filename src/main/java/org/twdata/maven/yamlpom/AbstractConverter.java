package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.logging.Log;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.FileReader;

/**
 *
 */
public abstract class AbstractConverter<T extends AbstractConverter>
{
    private File toFile;
    private File fromFile;
    protected String tab;
    protected Log log;

    public T targetFile(File toFile)
    {
        this.toFile = toFile;
        return (T) this;
    }

    public T fromFile(File fromFile)
    {
        this.fromFile = fromFile;
        return (T) this;
    }

    public T indentSpaces(int spaces)
    {
        String tab = "";
        for (int x=0; x<spaces; x++)
        {
            tab += " ";
        }
        this.tab = tab;
        return (T) this;
    }

    public T logWith(Log log)
    {
        this.log = log;
        return (T) this;
    }

    public void convert() throws IOException
    {
        Reader reader = null;
        try
        {
            reader = new FileReader(fromFile);
            String text = buildTarget(reader);
            if (isValidTargetContents(text))
            {
                FileUtils.writeStringToFile(toFile, text);
            }
            else
            {
                log.error("Cannot generate a valid document for " + toFile);
            }
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }
    }

    protected abstract String buildTarget(Reader fromFile) throws IOException;

    protected abstract boolean isValidTargetContents(String text);


}
