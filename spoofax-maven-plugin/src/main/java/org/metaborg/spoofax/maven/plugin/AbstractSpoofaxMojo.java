package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.build.dependency.IDependencyService;
import org.metaborg.core.build.paths.ILanguagePathService;
import org.metaborg.core.language.ILanguageComponent;
import org.metaborg.core.language.ILanguageDiscoveryService;
import org.metaborg.core.language.ILanguageService;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.metaborg.core.source.ISourceTextService;
import org.metaborg.spoofax.core.project.SpoofaxMavenConstants;
import org.metaborg.spoofax.core.project.settings.ISpoofaxProjectSettingsService;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.metaborg.spoofax.maven.plugin.impl.MavenSpoofaxMetaModule;
import org.metaborg.spoofax.maven.plugin.impl.MavenSpoofaxModule;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Injector;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {
    private static final String INJECTOR_ID = "spoofax-maven-plugin.injector";
    private static final String DISCOVERED_ID = "spoofax-maven-plugin.discovered";

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true, required = true) private MavenProject project;
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true) private PluginDescriptor plugin;
    @Parameter(defaultValue = "${project.build.directory}", readonly = true) private File buildDirectory;
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true) private File javaOutputDirectory;

    private Injector spoofaxInjector;

    private ILanguageDiscoveryService languageDiscoveryService;

    protected IResourceService resourceService;
    protected ILanguageService languageService;
    protected ILanguagePathService languagePathService;
    protected IDependencyService dependencyService;
    protected IProjectService projectService;
    protected ISpoofaxProjectSettingsService projectSettingsService;
    protected ISourceTextService sourceTextService;
    protected IStrategoRuntimeService strategoRuntimeService;
    protected SpoofaxMetaBuilder metaBuilder;
    protected IProcessorRunner<?, ?, ?> processor;

    private IProject spoofaxProject;


    @Override public void execute() throws MojoExecutionException, MojoFailureException {
        spoofaxInjector = (Injector) project.getContextValue(INJECTOR_ID);
        if(spoofaxInjector == null) {
            getLog().info("Initialising shared Spoofax core");
            final Injector injector = Guice.createInjector(new MavenSpoofaxModule(project));
            final Injector metaInjector = injector.createChildInjector(new MavenSpoofaxMetaModule());
            spoofaxInjector = metaInjector;
            project.setContextValue(INJECTOR_ID, metaInjector);
        }

        languageDiscoveryService = spoofaxInjector.getInstance(ILanguageDiscoveryService.class);

        resourceService = spoofaxInjector.getInstance(IResourceService.class);
        languageService = spoofaxInjector.getInstance(ILanguageService.class);
        languagePathService = spoofaxInjector.getInstance(ILanguagePathService.class);
        dependencyService = spoofaxInjector.getInstance(IDependencyService.class);
        projectService = spoofaxInjector.getInstance(IProjectService.class);
        projectSettingsService = spoofaxInjector.getInstance(ISpoofaxProjectSettingsService.class);
        sourceTextService = spoofaxInjector.getInstance(ISourceTextService.class);
        strategoRuntimeService = spoofaxInjector.getInstance(IStrategoRuntimeService.class);
        metaBuilder = spoofaxInjector.getInstance(SpoofaxMetaBuilder.class);
        processor = spoofaxInjector.getInstance(IProcessorRunner.class);

        spoofaxProject = projectService.get(resourceService.resolve(basedir));
        if(spoofaxProject == null) {
            throw new MojoFailureException("Cannot find Spoofax project");
        }
    }


    public MavenProject getProject() {
        return project;
    }

    public PluginDescriptor getPlugin() {
        return plugin;
    }


    public File getBasedir() {
        return basedir;
    }

    public FileObject getBasedirLocation() {
        return resourceService.resolve(basedir);
    }

    public IProject getSpoofaxProject() {
        return spoofaxProject;
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


    public void discoverLanguages() {
        final Boolean discovered = (Boolean) project.getContextValue(DISCOVERED_ID);
        if(discovered != null && discovered) {
            return;
        }

        final Set<Artifact> artifacts = Sets.newHashSet();
        artifacts.addAll(project.getDependencyArtifacts());
        artifacts.addAll(plugin.getArtifacts());
        for(Artifact artifact : artifacts) {
            if(SpoofaxMavenConstants.PACKAGING_TYPE.equalsIgnoreCase(artifact.getType())) {
                final File file = artifact.getFile();
                if(file != null && file.exists()) {
                    String url = (file.isDirectory() ? "file:" : "zip:") + file.getPath();
                    final FileObject artifactLocation = resourceService.resolve(url);
                    try {
                        Iterable<ILanguageComponent> components = languageDiscoveryService.discover(artifactLocation);
                        if(Iterables.isEmpty(components)) {
                            // When running in Eclipse using M2E, artifact location will point to the target/classes/
                            // directory which is empty. Try again with the packaged artifact.
                            final FileObject targetLocation = artifactLocation.getParent();
                            final String filename =
                                artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + artifact.getType();
                            final FileObject packageLocation = targetLocation.resolveFile(filename);
                            final FileObject packageFile =
                                resourceService.resolve("zip:" + packageLocation.getName().getPath());
                            components = languageDiscoveryService.discover(packageFile);
                        }
                        if(Iterables.isEmpty(components)) {
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

        project.setContextValue(DISCOVERED_ID, true);
    }
}
