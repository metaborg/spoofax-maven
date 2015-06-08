package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Singleton;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.meta.core.IMavenProjectService;
import org.metaborg.spoofax.meta.core.SpoofaxMetaModule;

public class SpoofaxMavenModule extends SpoofaxMetaModule {

    private final MavenProject project;

    public SpoofaxMavenModule(MavenProject project) {
        this.project = project;
    }

    @Override
    protected void bindProject() {
        bind(IProjectService.class).to(MavenMojoProjectService.class).in(Singleton.class);
        bind(IMavenProjectService.class).to(MavenMojoProjectService.class).in(Singleton.class);
        bind(MavenProject.class).toInstance(project);
    }

}
