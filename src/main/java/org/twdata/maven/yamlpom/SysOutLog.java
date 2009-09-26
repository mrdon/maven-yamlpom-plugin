package org.twdata.maven.yamlpom;

/**
 *
 */
public class SysOutLog implements Log {
    public void info(String msg) {
        System.out.println(msg);
    }

    public void error(String msg) {
        System.err.println(msg);
    }
}
