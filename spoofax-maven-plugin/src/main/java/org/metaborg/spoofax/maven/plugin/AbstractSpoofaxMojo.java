package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.language.ILanguage;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.spoofax.core.build.dependency.SpoofaxMavenConstants;
import org.metaborg.spoofax.maven.plugin.impl.MavenSpoofaxMetaModule;
import org.metaborg.spoofax.maven.plugin.impl.MavenSpoofaxModule;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {
    private static final String CONTEXT_ID = "spoofax-maven-plugin.spoofax";

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true, required = true) private MavenProject project;
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true) private PluginDescriptor plugin;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true) private File buildDirectory;
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true) private File javaOutputDirectory;

    private IProject spoofaxProject;


    public File getBasedir() {
        return basedir;
    }

    public FileObject getBasedirLocation() {
        return getSpoofax().getInstance(IResourceService.class).resolve(basedir);
    }

    public MavenProject getProject() {
        return project;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }

    public File getBuildDirectory() {
        return getAbsoluteFile(buildDirectory);
    }

    public File getJavaOutputDirectory() {
        return getAbsoluteFile(javaOutputDirectory);
    }

    public File getDependencyDirectory() {
        return new File(getBuildDirectory(), "spoofax/dependency");
    }

    public File getDependencyMarkersDirectory() {
        return new File(getBuildDirectory(), "spoofax/dependency-markers");
    }

    public Injector getSpoofax() {
        Injector spoofax = (Injector) project.getContextValue(CONTEXT_ID);
        if(spoofax == null) {
            getLog().info("Initialising shared Spoofax core");
            final Injector spoofaxInjector = Guice.createInjector(new MavenSpoofaxModule(project));
            final Injector spoofaxMetaInjector = spoofaxInjector.createChildInjector(new MavenSpoofaxMetaModule());
            spoofax = spoofaxMetaInjector;
            project.setContextValue(CONTEXT_ID, spoofax);
            IResourceService resourceService = spoofax.getInstance(IResourceService.class);
            ILanguageDiscoveryService languageDiscoveryService = spoofax.getInstance(ILanguageDiscoveryService.class);
            Set<Artifact> dependencyArtifacts = Sets.newHashSet();
            dependencyArtifacts.addAll(project.getDependencyArtifacts());
            dependencyArtifacts.addAll(plugin.getArtifacts());
            discoverLanguages(dependencyArtifacts, resourceService, languageDiscoveryService);
        }
        return spoofax;
    }

    private void discoverLanguages(Iterable<Artifact> artifacts, IResourceService resourceService,
        ILanguageDiscoveryService languageDiscoveryService) {
        for(Artifact artifact : artifacts) {
            if(SpoofaxMavenConstants.PACKAGING_TYPE.equalsIgnoreCase(artifact.getType())) {
                File file = artifact.getFile();
                if(file != null && file.exists()) {
                    String url = (file.isDirectory() ? "file:" : "zip:") + file.getPath();
                    FileObject artifactLocation = resourceService.resolve(url);
                    try {
                        Iterable<ILanguage> languages = languageDiscoveryService.discover(artifactLocation);
                        if(Iterables.isEmpty(languages)) {
                            getLog().error("No languages discovered in " + artifactLocation);
                        }
                    } catch(Exception ex) {
                        getLog().error("Error discovering languages in " + artifactLocation, ex);
                    }
                } else {
                    getLog().warn("Artifact " + artifact + " has no file(s), not resolved?");
                }
            }
        }
    }

    public IProject getSpoofaxProject() throws MojoFailureException {
        if(spoofaxProject == null) {
            Injector spoofax = getSpoofax();
            IResourceService resourceService = spoofax.getInstance(IResourceService.class);
            IProjectService projectService = spoofax.getInstance(IProjectService.class);
            spoofaxProject = projectService.get(resourceService.resolve(basedir));
            if(spoofaxProject == null) {
                throw new MojoFailureException("Cannot find project instance.");
            }
        }
        return spoofaxProject;
    }

    public File getAbsoluteFile(@Nullable File file) {
        if(file == null) {
            return basedir;
        }
        return file.isAbsolute() ? file : new File(basedir, file.getPath());
    }

    public File getAbsoluteFile(@Nullable String path) {
        if(path == null) {
            return basedir;
        }
        return getAbsoluteFile(new File(path));
    }
}
