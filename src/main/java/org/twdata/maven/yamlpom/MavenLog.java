package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.logging.Log;

/**
 *
 */
public class MavenLog implements org.twdata.maven.yamlpom.Log {

    private final Log log;

    public MavenLog(Log log) {
        this.log = log;
    }

    public void info(String msg) {
        log.info(msg);
    }

    public void error(String msg) {
        log.error(msg);
    }
}
