package org.twdata.maven.yamlpom;

/**
 *
 */
public class ConverterOptions
{
    private String indent = "  ";
    private Log log;


    public String getIndent()
    {
        return indent;
    }

    public ConverterOptions indent(String indent)
    {
        this.indent = indent;
        return this;
    }

    public Log getLog()
    {
        return log;
    }

    public ConverterOptions log(Log log)
    {
        this.log = log;
        return this;
    }
}
