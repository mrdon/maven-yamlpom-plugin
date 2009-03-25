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
    private final int MAX_LINE_LENGTH = 120;
    private final char[] INVALID_SCALAR_CHARACTERS = new char[] {':', '#', '[', ']', '{', '}', ',', '*', '\t'};

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
                yamlWriter.write(tabs + prefix + name + ": " + sanitizeScalar(element.getTextTrim()) + "\n");
            }
            // is list
            else if (isList(element))
            {
                yamlWriter.write(tabs + prefix + name + ":");
                if (shouldInline(element, (tabs + tab + "  ").length()))
                {
                    printInlineList(element, yamlWriter);
                }
                else
                {
                    yamlWriter.write("\n");
                    for (Iterator i = element.elementIterator(); i.hasNext(); )
                    {
                        Element list = (Element) i.next();
                        if (shouldInline(list, (tabs + tab + "  ").length()))
                        {
                            printInlineMap(list, tabs + tab + "- ", yamlWriter);
                        }
                        // is scalar entry
                        else if (list.elements().isEmpty())
                        {
                            yamlWriter.write(tabs + tab + "- " + list.getTextTrim() + "\n");
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

    private String sanitizeScalar(String val)
    {
        boolean needsQuoted = false;
        for (char forbiddenChar : INVALID_SCALAR_CHARACTERS)
        {
            if (val.indexOf(forbiddenChar) > -1)
            {
                needsQuoted = true;
            }
        }

        if (needsQuoted)
        {
            val = val.replaceAll("\\\\", "\\\\");
            val = val.replaceAll("\"", "\\\"");
            val = "\"" + val + "\"";
        }
        return val;
    }

    private void printInlineMap(Element listItem, String tab, Writer yamlWriter) throws IOException
    {
        yamlWriter.write(tab + "{ ");
        for (Iterator i = listItem.elementIterator(); i.hasNext(); )
        {
            Element e = (Element) i.next();
            yamlWriter.write(e.getName() + ": " + sanitizeScalar(e.getTextTrim()));
            if (i.hasNext())
            {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" }\n");
    }

    private void printInlineList(Element listItem, Writer yamlWriter) throws IOException
    {
        yamlWriter.write(" [ ");
        for (Iterator i = listItem.elementIterator(); i.hasNext(); )
        {
            Element e = (Element) i.next();
            yamlWriter.write(sanitizeScalar(e.getTextTrim()));
            if (i.hasNext())
            {
                yamlWriter.write(", ");
            }
        }
        yamlWriter.write(" ]\n");
    }

    private boolean shouldInline(Element element, int startLength)
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