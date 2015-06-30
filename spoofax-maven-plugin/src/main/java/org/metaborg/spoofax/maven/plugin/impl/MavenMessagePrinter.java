package org.metaborg.spoofax.maven.plugin.impl;

import org.apache.maven.plugin.logging.Log;
import org.metaborg.spoofax.core.messages.IMessage;
import org.metaborg.spoofax.core.messages.IMessagePrinter;


public class MavenMessagePrinter implements IMessagePrinter {
    private final Log log;


    public MavenMessagePrinter(Log log) {
        this.log = log;
    }


    @Override public void print(IMessage message) {
        switch(message.severity()) {
            case ERROR:
                log.error(message.message(), message.exception());
                break;
            case WARNING:
                log.warn(message.message(), message.exception());
                break;
            case NOTE:
                log.info(message.message(), message.exception());
                break;
            default:
                log.debug(message.message(), message.exception());
                break;
        }
    }
}
