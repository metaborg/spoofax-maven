package org.metaborg.spoofax.maven.plugin.impl;

import org.metaborg.spoofax.meta.core.SpoofaxMetaModule;
import org.metaborg.spoofax.meta.core.ant.AntRunnerService;
import org.metaborg.spoofax.meta.core.ant.IAntRunnerService;

import com.google.inject.Singleton;

public class MavenSpoofaxMetaModule extends SpoofaxMetaModule {
    @Override protected void configure() {
        super.configure();

        bind(IAntRunnerService.class).to(AntRunnerService.class).in(Singleton.class);
    }
}
