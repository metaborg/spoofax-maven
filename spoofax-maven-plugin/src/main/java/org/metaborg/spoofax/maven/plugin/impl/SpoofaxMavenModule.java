package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Singleton;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.SpoofaxModule;
import org.metaborg.spoofax.core.project.IDependencyService;
import org.metaborg.spoofax.core.project.IProjectService;

public class SpoofaxMavenModule extends SpoofaxModule {

    private final MavenProject project;
    private final PluginDescriptor plugin;

    public SpoofaxMavenModule(MavenProject project, PluginDescriptor plugin) {
        this.project = project;
        this.plugin = plugin;
    }

    @Override
    protected void bindDependency() {
        bind(IDependencyService.class).to(MavenDependencyService.class).in(Singleton.class);
    }

    @Override
    protected void bindProject() {
        bind(IProjectService.class).to(MavenProjectService.class).in(Singleton.class);
        bind(SpoofaxMavenProject.class).in(Singleton.class);
    }

    @Override
    protected void bindBuilder() {
        super.bindBuilder();
        bind(SpoofaxMetaBuilder.class).in(Singleton.class);
    }

    @Override
    protected void bindOther() {
        bind(MavenProject.class).toInstance(project);
        bind(PluginDescriptor.class).toInstance(plugin);
    }

}
