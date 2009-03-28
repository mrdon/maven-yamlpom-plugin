package org.twdata.maven.yamlpom;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 *
 */
public class SyncManagerTest extends TestCase
{
    File syncDir;
    File syncFile;
    File xmlFile;
    File yamlFile;

    @Override
    protected void setUp() throws Exception
    {
        syncDir = MyTestUtils.createTempDirectory("sync");
        xmlFile = new File(syncDir, "pom.xml");
        yamlFile = new File(syncDir, "pom.yml");
        syncFile = new File(syncDir, "sync.yml");
    }

    @Override
    protected void tearDown() throws Exception
    {
        FileUtils.deleteDirectory(syncDir);
    }

    public void testNoSyncFile() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        yamlFile.setLastModified(yamlFile.lastModified() + 1000);
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.UNKNOWN, mgr.determineFormatToTarget());
    }

    public void testNoYaml() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.YAML, mgr.determineFormatToTarget());
    }

    public void testNoXml() throws IOException
    {
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.XML, mgr.determineFormatToTarget());
    }

    public void testSyncSameTimestampFile() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        yamlFile.setLastModified(yamlFile.lastModified() + 1000);
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        mgr.save();
        mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.NONE, mgr.determineFormatToTarget());
    }

    public void testSyncDifferentTimestampSameContent() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        yamlFile.setLastModified(yamlFile.lastModified() + 1000);
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        mgr.save();
        xmlFile.setLastModified(xmlFile.lastModified() + 3000);
        yamlFile.setLastModified(yamlFile.lastModified() + 4000);
        mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.SYNC_FILE_ONLY, mgr.determineFormatToTarget());
    }

    public void testSyncDifferentTimestampDifferentXmlContent() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        yamlFile.setLastModified(yamlFile.lastModified() + 1000);
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        mgr.save();
        FileUtils.writeStringToFile(xmlFile, "<project><bob/></project>");
        yamlFile.setLastModified(yamlFile.lastModified() + 4000);
        mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.YAML, mgr.determineFormatToTarget());
    }

    public void testSyncDifferentTimestampDifferentYamlContent() throws IOException
    {
        FileUtils.writeStringToFile(xmlFile, "<project />");
        FileUtils.writeStringToFile(yamlFile, "artifactId: boo");
        yamlFile.setLastModified(yamlFile.lastModified() + 1000);
        SyncManager mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        mgr.save();
        FileUtils.writeStringToFile(yamlFile, "artifactId: jim");
        xmlFile.setLastModified(xmlFile.lastModified() + 3000);
        mgr = new SyncManager(xmlFile, yamlFile, syncFile);
        assertEquals(SyncManager.FormatToTarget.XML, mgr.determineFormatToTarget());
    }

}
