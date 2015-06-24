package org.metaborg.spoofax.maven.plugin.impl;

import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.IMavenProjectService;
import org.metaborg.spoofax.core.project.IProjectService;

import com.google.inject.Singleton;

public class MavenSpoofaxModule extends SpoofaxModule {
    private final MavenProject project;


    public MavenSpoofaxModule(MavenProject project) {
        this.project = project;
    }


    @Override protected void bindProject() {
        bind(IProjectService.class).to(MetaborgProjectService.class).in(Singleton.class);
        bind(IMavenProjectService.class).to(MavenProjectService.class).in(Singleton.class);
        bind(MavenProject.class).toInstance(project);
    }
}
