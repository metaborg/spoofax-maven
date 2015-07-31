package org.metaborg.spoofax.maven.plugin.impl;

import org.apache.maven.project.MavenProject;
import org.metaborg.core.MetaborgModule;
import org.metaborg.core.project.IProjectService;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.IMavenProjectService;

import com.google.inject.Singleton;

public class MavenSpoofaxModule extends SpoofaxModule {
    private final MavenProject project;


    public MavenSpoofaxModule(MavenProject project) {
        this.project = project;
    }


    /**
     * Overrides {@link MetaborgModule#bindProject()} for non-dummy implementation of project service.
     */
    @Override protected void bindProject() {
        bind(IProjectService.class).to(MetaborgProjectService.class).in(Singleton.class);
    }

    /**
     * Overrides {@link SpoofaxModule#bindMavenProject()} for non-dummy implementation of Maven project service.
     */
    @Override protected void bindMavenProject() {
        bind(IMavenProjectService.class).to(MavenProjectService.class).in(Singleton.class);
        bind(MavenProject.class).toInstance(project);
    }
}
