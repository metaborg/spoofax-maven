package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Singleton;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.maven.plugin.impl.MavenProjectService;

public class SpoofaxMavenModule extends SpoofaxModule {

    private final MavenProject project;

    public SpoofaxMavenModule(MavenProject project) {
        this.project = project;
    }

    @Override
    protected void bindProject() {
        bind(IProjectService.class).to(MavenProjectService.class).in(Singleton.class);
    }

    @Override
    protected void bindOther() {
        bind(MavenProject.class).toInstance(project);
    }

}
