package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectDependenciesResolver;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.tree.DependencyNode;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilder;
import org.apache.maven.shared.dependency.tree.DependencyTreeBuilderException;
import org.apache.maven.shared.dependency.tree.traversal.DependencyNodeVisitor;
import org.metaborg.core.MetaborgException;
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

    @Component(hint = "default") private DependencyTreeBuilder dependencyTreeBuilder;
    @Component private RepositorySystem repoSystem;
    @Component private ProjectDependenciesResolver projectDependenciesResolver;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true) private File basedir;
    @Parameter(defaultValue = "${project}", readonly = true) private MavenProject project;
    @Parameter(defaultValue = "${plugin}", readonly = true, required = true) private PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true) private File buildDirectory;
    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true) private File javaOutputDirectory;

    @Parameter(defaultValue = "${localRepository}", readonly = true) private ArtifactRepository localRepository;
    @Parameter(defaultValue = "${project.remoteProjectRepositories}") private List<ArtifactRepository> projectRepos;
    @Parameter(defaultValue = "${project.remotePluginRepositories}") private List<ArtifactRepository> pluginRepos;

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
        if(project != null) {
            spoofaxInjector = (Injector) project.getContextValue(INJECTOR_ID);
        } else {
            spoofaxInjector = null;
        }

        if(spoofaxInjector == null) {
            getLog().info("Initialising shared Spoofax core");
            final Injector injector = Guice.createInjector(new MavenSpoofaxModule(project));
            final Injector metaInjector = injector.createChildInjector(new MavenSpoofaxMetaModule());
            spoofaxInjector = metaInjector;
            if(project != null) {
                project.setContextValue(INJECTOR_ID, metaInjector);
            }
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


    public @Nullable MavenProject getProject() {
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


    public void discoverLanguages() throws MojoExecutionException {
        if(project == null) {
            throw new MojoExecutionException("Cannot discover language without a project");
        }

        final Boolean discovered = (Boolean) project.getContextValue(DISCOVERED_ID);
        if(discovered != null && discovered) {
            return;
        }

        getLog().info("Collecting language dependencies");

        final Iterable<Artifact> dependencies;
        try {
            final Iterable<Artifact> allDependencies = allDependencies();
            dependencies = resolveArtifacts(allDependencies);
        } catch(DependencyTreeBuilderException e) {
            throw new MojoExecutionException("Resolving dependencies failed", e);
        }

        getLog().info("Loading language components from dependencies");

        boolean error = false;
        for(Artifact dependency : dependencies) {
            if(loadComponents(dependency) == null) {
                error = true;
            }
        }

        if(error) {
            throw new MojoExecutionException("Error(s) occurred while discovering languages");
        }

        project.setContextValue(DISCOVERED_ID, true);
    }

    /**
     * Get the dependency tree so that we also see dependencies that have been omitted by Maven. Maven does conflict
     * resolution so that it only has to load a single version of the artifact in the JVM, which makes sense for Java,
     * but not for Spoofax. We actually want to load multiple versions of the same language for bootstrapping purposes.
     */
    private Iterable<Artifact> allDependencies() throws DependencyTreeBuilderException {
        final Set<Artifact> dependencies = Sets.newHashSet();
        final DependencyNode node =
            dependencyTreeBuilder.buildDependencyTree(project, localRepository, new ArtifactFilter() {
                @Override public boolean include(Artifact artifact) {
                    return true;
                }
            });
        node.accept(new DependencyNodeVisitor() {
            @Override public boolean visit(DependencyNode node) {
                final Artifact artifact = node.getArtifact();
                if(artifact.getType().equalsIgnoreCase(SpoofaxMavenConstants.PACKAGING_TYPE)) {
                    dependencies.add(artifact);
                }
                return true;
            }

            @Override public boolean endVisit(DependencyNode node) {
                return true;
            }
        });
        dependencies.remove(project.getArtifact());
        return dependencies;
    }

    /**
     * Omitted dependencies in the dependency tree are not resolved. Resolve them manually and return the resolved
     * artifacts.
     */
    private Iterable<Artifact> resolveArtifacts(Iterable<Artifact> dependencies) {
        final Set<Artifact> artifacts = Sets.newHashSet();
        for(Artifact dependency : dependencies) {
            if(dependency.isResolved()) {
                artifacts.add(dependency);
            } else {
                final ArtifactResolutionRequest request = new ArtifactResolutionRequest();
                request.setArtifact(dependency);
                // HACK: setting remote repositories causes ClassCastException in Maven, disable for now..
                // request.setRemoteRepositories(projectRepos);
                request.setLocalRepository(localRepository);
                final ArtifactResolutionResult result = repoSystem.resolve(request);
                artifacts.addAll(result.getArtifacts());
            }
        }
        return artifacts;
    }

    /**
     * Loads language components given an artifact.
     * 
     * @param artifact
     *            Artifact to load language components from.
     * @return Loaded components, or null if an error occured.
     */
    private Iterable<ILanguageComponent> loadComponents(Artifact artifact) {
        final File file = artifact.getFile();
        if(file != null && file.exists()) {
            final String url = (file.isDirectory() ? "file:" : "zip:") + file.getPath();
            final FileObject artifactLocation = resourceService.resolve(url);

            try {
                if(!artifactLocation.exists()) {
                    getLog().error("Artifact location" + artifactLocation + " does not exist, cannot load languages");
                    return null;
                }

                // When running in Eclipse using M2E, artifact location will point to the target/classes/
                // directory which is empty. Try again with the packaged artifact.
                final FileObject targetLocation = artifactLocation.getParent();
                final String filename =
                    artifact.getArtifactId() + "-" + artifact.getBaseVersion() + "." + artifact.getType();
                final FileObject packageLocation = targetLocation.resolveFile(filename);
                final FileObject packageFile = resourceService.resolve("zip:" + packageLocation.getName().getPath());

                final Iterable<ILanguageComponent> components;
                if(packageFile.exists()) {
                    components = languageDiscoveryService.discover(packageFile);
                } else {
                    components = languageDiscoveryService.discover(artifactLocation);
                }

                if(Iterables.isEmpty(components)) {
                    getLog().error("No languages were discovered in " + artifact);
                    return null;
                }

                for(ILanguageComponent component : components) {
                    getLog().info("Loaded " + component);
                }

                return components;
            } catch(FileSystemException | MetaborgException e) {
                getLog().error("Unexpected error while discovering languages in " + artifact, e);
                return null;
            }
        }

        getLog().error("Artifact " + artifact + " has no files, cannot load languages");
        return null;
    }
}
