package org.twdata.maven.yamlpom;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.Loader;
import org.yaml.snakeyaml.constructor.Constructor;
import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 *
 */
public class SyncManager
{
    public enum FormatToTarget
    {
        XML,
        YAML,
        SYNC_FILE_ONLY,
        UNKNOWN,
        NONE
    }

    private final File xmlFile;
    private final File yamlFile;
    private final File syncFile;

    private final SyncFile sync;

    public SyncManager(File xmlFile, File yamlFile, File syncFile)
    {
        this.xmlFile = xmlFile;
        this.yamlFile = yamlFile;
        this.syncFile = syncFile;
        Loader loader = new Loader(new Constructor(SyncFile.class));
        Yaml yaml = new Yaml(loader);
        Reader reader = null;
        SyncFile tmpSync;
        try
        {
            reader = new FileReader(syncFile);
            tmpSync = (SyncFile) yaml.load(reader);
        }
        catch (FileNotFoundException e)
        {
            tmpSync = null;
        }
        finally
        {
            IOUtils.closeQuietly(reader);
        }
        sync = tmpSync;
    }

    public FormatToTarget determineFormatToTarget()
    {
        if (!yamlFile.exists() && xmlFile.exists())
        {
            return FormatToTarget.YAML;
        }
        else if (yamlFile.exists() && !xmlFile.exists())
        {
            return FormatToTarget.XML;
        }
        else if (sync != null)
        {
            if (xmlHasSameTimestamp() && yamlHasSameTimestamp())
            {
                return FormatToTarget.NONE;
            }
            else
            {
                MD5 md5 = new MD5();
                String xmlMd5 = md5.hashFile(xmlFile);
                String yamlMd5 = md5.hashFile(yamlFile);
                if (xmlHasSameHash(xmlMd5) && yamlHasSameHash(yamlMd5))
                {
                    return FormatToTarget.SYNC_FILE_ONLY;
                }
                else if (!xmlHasSameHash(xmlMd5) && yamlHasSameHash(yamlMd5))
                {
                    return FormatToTarget.YAML;
                }
                else if (xmlHasSameHash(xmlMd5) && !yamlHasSameHash(yamlMd5))
                {
                    return FormatToTarget.XML;
                }
            }
        }
        return FormatToTarget.UNKNOWN;
    }

    public void save()
    {
        MD5 md5 = new MD5();
        SyncFile sync = new SyncFile();

        SyncFile.FileInfo xmlInfo = new SyncFile.FileInfo();
        xmlInfo.setSyncBy(System.getProperty("user.name"));
        xmlInfo.setTimestamp(xmlFile.lastModified());
        xmlInfo.setMd5(md5.hashFile(xmlFile));
        sync.setXml(xmlInfo);

        SyncFile.FileInfo yamlInfo = new SyncFile.FileInfo();
        yamlInfo.setSyncBy(System.getProperty("user.name"));
        yamlInfo.setTimestamp(yamlFile.lastModified());
        yamlInfo.setMd5(md5.hashFile(yamlFile));
        sync.setYaml(yamlInfo);

        Yaml yaml = new Yaml();
        Writer writer = null;
        try
        {
            writer = new FileWriter(syncFile);
            yaml.dump(sync, writer);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private boolean xmlHasSameTimestamp()
    {
        return sync.getXml().getTimestamp() == xmlFile.lastModified();
    }

    private boolean yamlHasSameTimestamp()
    {
        return sync.getYaml().getTimestamp() == yamlFile.lastModified();
    }

    private boolean xmlHasSameHash(String xmlHash)
    {
        return sync.getXml().getMd5().equals(xmlHash);
    }

    private boolean yamlHasSameHash(String yamlHash)
    {
        return sync.getYaml().getMd5().equals(yamlHash);
    }
}
