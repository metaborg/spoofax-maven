package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {

    public static final Format DEFAULT_FORMAT = Format.ctree;

    public enum Format {
        ctree,
        jar
    }

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "ctree")
    private Format format;

    @Parameter(defaultValue = "${project.artifactId}")
    private String id;

    @Parameter
    private String[] sdfArgs;

    @Parameter
    private String[] strategoArgs;

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

    public String getName() {
        return name;
    }

    public Format getFormat() {
        return format;
    }

    public String getPackageName() {
        return id != null && !id.isEmpty() ?
                id : getName().toLowerCase();
    }

    public String getPackagePath() {
        return getPackageName().replace('.', '/');
    }

    public String[] getSdfArgs() {
        return sdfArgs == null ? new String[0] : sdfArgs;
    }

    public String[] getStrategoArgs() {
        return strategoArgs == null ? new String[0] : strategoArgs;
    }

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
        return buildDirectory;
    }

    public File getGeneratedSourceDirectory() {
        return new File(basedir, "src-gen");
    }

    public File getOutputDirectory() {
        return new File(basedir, "include");
    }

    public File getIconsDirectory() {
        return new File(basedir, "icons");
    }

    public File getJavaOutputDirectory() {
        return javaOutputDirectory;
    }

    public File getDependencyDirectory() {
        return new File(buildDirectory, "spoofax/dependency");
    }

    public File getDependencyMarkersDirectory() {
        return new File(buildDirectory, "spoofax/dependency-markers");
    }

    public File getAntDirectory() {
        return new File(buildDirectory, "spoofax/ant");
    }

    public File getNativeDirectory() throws MojoFailureException {
        File dependencyDirectory = getDependencyDirectory();
        if ( SystemUtils.IS_OS_WINDOWS ) {
            return new File(dependencyDirectory, "native/cygwin");
        } else if ( SystemUtils.IS_OS_MAC_OSX ) {
            return new File(dependencyDirectory, "native/macosx");
        } else if ( SystemUtils.IS_OS_LINUX ) {
            return new File(dependencyDirectory, "native/linux");
        } else {
            throw new MojoFailureException("Unsupported platform "+SystemUtils.OS_NAME);
        }
    }

    public File getDistDirectory() {
        return new File(getDependencyDirectory(), "dist");
    }

    public File getLibDirectory() {
        return new File(basedir, "lib");
    }

    public File getSyntaxDirectory() {
        return new File(basedir, "syntax");
    }

    public File getEditorDirectory() {
        return new File(basedir, "editor");
    }

    public File getEditorJavaDirectory() {
        return new File(getEditorDirectory(), "java");
    }

    public File getJavaTransDirectory() {
        return new File(getEditorJavaDirectory(), "trans");
    }

    public File getGeneratedSyntaxDirectory() {
        return new File(getGeneratedSourceDirectory(), "syntax");
    }

    public File getTransDirectory() {
        return new File(basedir, "trans");
    }

    public File getCacheDirectory() {
        return new File(basedir, ".cache");
    }

    @Override
    public void execute() throws MojoFailureException {
        // this doesn't work sometimes, looks like another implementation
        // of StaticLoggerBinder is pulled in when the plugin is run?
        // StaticLoggerBinder.getSingleton().setMavenLog(getLog());
        if ( !NameUtil.isValidIdentifier(getName()) ) {
            throw new MojoFailureException("Invalid language name "+getName());
        }
        if ( !NameUtil.isValidPackage(getPackageName()) ) {
            throw new MojoFailureException("Invalid language name "+getPackageName());
        }
    }

}
