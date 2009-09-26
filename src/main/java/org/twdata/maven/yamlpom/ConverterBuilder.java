package org.twdata.maven.yamlpom;

import org.apache.commons.io.IOUtils;

import java.io.*;

/**
 *
 */
public class ConverterBuilder
{
    private File toFile;
    private File fromFile;
    private Reader fromReader;
    private Writer toWriter;
    private final Converter converter;
    private final ConverterOptions options;

    private ConverterBuilder(Converter converter)
    {
        this.converter = converter;
        this.options = new ConverterOptions();
    }

    public static ConverterBuilder convertXmlToYaml()
    {
        return new ConverterBuilder(new XmlToYamlConverter());
    }

    public static ConverterBuilder convertYamlToXml()
    {
        return new ConverterBuilder(new YamlToXmlConverter());
    }

    public ConverterBuilder toFile(File toFile)
    {
        this.toFile = toFile;
        return this;
    }

    public ConverterBuilder toWriter(Writer writer)
    {
        this.toWriter = writer;
        return this;
    }

    public ConverterBuilder fromFile(File fromFile)
    {
        this.fromFile = fromFile;
        return this;
    }

    public ConverterBuilder fromReader(Reader fromReader)
    {
        this.fromReader = fromReader;
        return this;
    }

    public ConverterBuilder indentSpaces(int spaces)
    {
        String tab = "";
        for (int x=0; x<spaces; x++)
        {
            tab += " ";
        }
        this.options.indent(tab);
        return this;
    }

    public ConverterBuilder logWith(Log log)
    {
        this.options.log(log);
        return this;
    }

    public void convert() throws IOException, InvalidFormatException
    {
        Reader reader = fromReader;
        Writer writer = toWriter;
        try
        {
            if (reader == null)
            {
                reader = new FileReader(fromFile);
            }
            if (writer == null)
            {
                writer = new FileWriter(toFile);
            }

            String text = converter.convert(reader, options);
            if (text != null && text.trim().length() > 0)
            {
                IOUtils.write(text, writer);
            }
            else
            {
                throw new InvalidFormatException("No converted text", null);
            }

        }
        finally
        {
            IOUtils.closeQuietly(reader);
            IOUtils.closeQuietly(writer);
        }
    }
}