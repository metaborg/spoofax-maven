package org.metaborg.spoofax.maven.plugin.impl;

import org.metaborg.spoofax.core.language.LanguageVersion;

public class LanguageIdentifier {
    
    private final String name;
    private final LanguageVersion version;

    public LanguageIdentifier(String name, LanguageVersion version) {
        this.name = name;
        this.version = version;
    }

    public String name() {
        return name;
    }

    public LanguageVersion version() {
        return version;
    }

}
