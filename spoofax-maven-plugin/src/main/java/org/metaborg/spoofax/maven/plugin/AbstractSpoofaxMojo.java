package org.metaborg.spoofax.maven.plugin;

import com.google.inject.Guice;
import com.google.inject.Injector;
import java.io.File;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxMavenModule;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {

    private static final String CONTEXT_ID = "spoofax-maven-plugin.spoofax";

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true)
    private File buildDirectory;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true)
    private File javaOutputDirectory;

    private IProject spoofaxProject;

    public File getBasedir() {
        return basedir;
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
    
    public Injector getSpoofax() throws MojoFailureException {
        Injector spoofax;
        if ( (spoofax = (Injector) project.getContextValue(CONTEXT_ID)) == null ) {
            getLog().info("Initialising shared Spoofax core");
            project.setContextValue(CONTEXT_ID,
                    spoofax = Guice.createInjector(new SpoofaxMavenModule(project, plugin)));
        } else {
            getLog().info("Using shared Spoofax core");
        }
        return spoofax;

    }

    public IProject getSpoofaxProject() throws MojoFailureException {
        if ( spoofaxProject == null ) {
            Injector spoofax = getSpoofax();
            IResourceService resourceService = spoofax.getInstance(IResourceService.class);
            IProjectService projectService = spoofax.getInstance(IProjectService.class);
            spoofaxProject = projectService.get(resourceService.resolve(basedir));
            if ( spoofaxProject == null ) {
                throw new MojoFailureException("Cannot find project instance.");
            }
        }
        return spoofaxProject;
    }

    public File getAbsoluteFile(@Nullable File file) {
        if ( file == null ) {
            return basedir;
        }
        return file.isAbsolute() ? file : new File(basedir, file.getPath());
    }

    public File getAbsoluteFile(@Nullable String path) {
        if ( path == null ) {
            return basedir;
        }
        return getAbsoluteFile(new File(path));
    }

}