package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

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
    
    public SpoofaxHelper getSpoofaxHelper() throws MojoFailureException {
        SpoofaxHelper spoofaxHelper;
        if ( (spoofaxHelper = (SpoofaxHelper) project.getContextValue(CONTEXT_ID)) == null ) {
            getLog().info("Initialising shared Spoofax core");
            project.setContextValue(CONTEXT_ID,
                    spoofaxHelper = new SpoofaxHelper(project, plugin,
                            getDependencyDirectory(), getLog()));
        } else {
            getLog().info("Using shared Spoofax core");
        }
        return spoofaxHelper;

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
