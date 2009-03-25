package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.commons.io.IOUtils;
import org.dom4j.*;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.util.Iterator;

/**
 *
 */
public class PomToYamlConverter
{
    private final String tab;
    private final int MAX_LINE_LENGTH = 80;

    public PomToYamlConverter(String tab)
    {
        this.tab = tab;
    }

    public void convert(File pomFile, File yamlFile) throws MojoExecutionException
    {
        Writer yamlWriter = null;
        Reader xmlReader = null;
        try
        {
            xmlReader = new FileReader(pomFile);
            yamlWriter = new FileWriter(yamlFile);

            Document doc = new SAXReader().read(xmlReader);

            for (Iterator it = doc.getRootElement().elementIterator(); it.hasNext(); ) {
                Element element = (Element) it.next();
                if (!"modelVersion".equals(element.getName()))
                {
                    convert(element, "", false, yamlWriter);
                }
            }
        }
        catch (FileNotFoundException e)
        {
            throw new MojoExecutionException("no file", e);
        }
        catch (IOException e)
        {
            throw new MojoExecutionException("something", e);
        }
        catch (DocumentException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        finally
        {
            IOUtils.closeQuietly(yamlWriter);
            IOUtils.closeQuietly(xmlReader);
        }
        yamlFile.setLastModified(pomFile.lastModified());
    }

    private void convert(Element element, String tabs, boolean isInList, Writer yamlWriter) throws IOException
    {
        if (element != null)
        {
            String name = element.getName();
            String prefix = isInList ? "- " : "";

            // is scalar
            if (element.elements().isEmpty())
            {
                yamlWriter.write(tabs + prefix + name + ": " +element.getTextTrim() + "\n");
            }
            // is list
            else if (isList(element))
            {
                yamlWriter.write(tabs + prefix + name + ":\n");
                for (Iterator i = element.elementIterator(); i.hasNext(); )
                {
                    Element list = (Element) i.next();
                    if (shouldInlineMap(list, (tabs + tab + "  ").length()))
                    {
                        printInlineMap(list, tabs + tab + "- ", yamlWriter);
                    }
                    else
                    {
                        boolean isFirst = true;
                        for (Iterator it = list.elementIterator(); it.hasNext(); )
                        {
                            Element listItem = (Element)it.next();
                            convert(listItem, (!isFirst ? "  " : "")  + tabs + tab, isFirst, yamlWriter);
                            isFirst = false;
                        }
                    }
                }
            // is map
            } else
            {
                yamlWriter.write(tabs + prefix + name + ":\n");
                for (Iterator i = element.elementIterator(); i.hasNext(); )
                {
                    convert((Element) i.next(), tabs + tab, false, yamlWriter);
                }
            }
        }
    }

    private void printInlineMap(Element listItem, String tab, Writer yamlWriter) throws IOException
    {
        yamlWriter.write(tab + "{ ");
        for (Iterator i = listItem.elementIterator(); i.hasNext(); )
        {
            Element e = (Element) i.next();
            yamlWriter.write(e.getName() + ": " + e.getTextTrim());
            if (i.hasNext())
            {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write("}\n");
    }

    private boolean shouldInlineMap(Element element, int startLength)
    {
        int length = startLength;
        if (element.elements().isEmpty())
        {
            return false;
        }
        for (Iterator i = element.elementIterator(); i.hasNext() && length < MAX_LINE_LENGTH; )
        {
            Element e = (Element) i.next();
            if (!e.elements().isEmpty())
            {
                length = Integer.MAX_VALUE;
                break;
            }
            else
            {
                length += (e.getName() + ": " + e.getTextTrim()).length();
            }

        }
        return length < MAX_LINE_LENGTH;
    }

    private boolean isList(Element element)
    {
        String name = element.getName();
        return (
                (name.endsWith("s") && !element.elements(name.substring(0, name.length() - 1)).isEmpty()) ||
                (name.endsWith("ies") && !element.elements(name.substring(0, name.length() - 3) + "y").isEmpty())
        );
    }
}