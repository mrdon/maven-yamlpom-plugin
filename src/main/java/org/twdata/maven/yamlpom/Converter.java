package org.twdata.maven.yamlpom;

import java.io.Reader;
import java.io.IOException;
import java.io.Writer;

/**
 *
 */
public interface Converter
{
    String convert(Reader from, ConverterOptions options) throws InvalidFormatException, IOException;
}
