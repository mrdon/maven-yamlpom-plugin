package org.twdata.maven.yamlpom;

/**
 *
 */
public class SyncFile
{
    private FileInfo xml;
    private FileInfo yaml;

    public FileInfo getXml()
    {
        return xml;
    }

    public void setXml(FileInfo xml)
    {
        this.xml = xml;
    }

    public FileInfo getYaml()
    {
        return yaml;
    }

    public void setYaml(FileInfo yaml)
    {
        this.yaml = yaml;
    }

    public static class FileInfo
    {
        private long timestamp;
        private String md5;
        private String syncBy;

        public long getTimestamp()
        {
            return timestamp;
        }

        public void setTimestamp(long timestamp)
        {
            this.timestamp = timestamp;
        }

        public String getMd5()
        {
            return md5;
        }

        public void setMd5(String md5)
        {
            this.md5 = md5;
        }

        public String getSyncBy()
        {
            return syncBy;
        }

        public void setSyncBy(String syncBy)
        {
            this.syncBy = syncBy;
        }
    }
}
