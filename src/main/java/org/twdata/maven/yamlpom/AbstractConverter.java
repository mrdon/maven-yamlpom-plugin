package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.logging.Log;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public abstract class AbstractConverter<T extends AbstractConverter>
{
    private File toFile;
    private File fromFile;
    private File syncFile;
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

    public T syncFile(File syncFile)
    {
        this.syncFile = syncFile;
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
        String text = buildTarget(fromFile);
        if (isValidTargetContents(text))
        {
            FileUtils.writeStringToFile(toFile, text);
        }
        else
        {
            log.error("Cannot generate a valid document for " + toFile);
        }
    }

    protected abstract String buildTarget(File fromFile) throws IOException;

    protected abstract boolean isValidTargetContents(String text);


}
