package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class AbstractSpoofaxMojo extends AbstractMojo {

    public enum Format {
        ctree,
        jar
    }

    @Parameter(alias = "lang.name", defaultValue = "${project.name}", readonly = true, required = true)
    private String langName;

    @Parameter(alias = "lang.format", defaultValue = "ctree", readonly = true, required = true)
    private Format langFormat;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private File buildDirectory;

    private File includeDirectory;
    private File transDirectory;
    private File srcgenDirectory;
    private File syntaxDirectory;
    private File dependencyDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        includeDirectory = new File(basedir, "include");
        transDirectory = new File(basedir, "trans");
        srcgenDirectory = new File(basedir, "src-gen");
        syntaxDirectory = new File(basedir, "syntax");
        dependencyDirectory = new File(buildDirectory, "dependency");
    }

    protected String getLangName() {
        return langName;
    }

    protected Format getLangFormat() {
        return langFormat;
    }

    protected File getBasedir() {
        return basedir;
    }

    protected MavenProject getProject() {
        return project;
    }

    protected PluginDescriptor getPlugin() {
        return plugin;
    }

    protected File getBuildDirectory() {
        return buildDirectory;
    }

    protected File getIncludeDirectory() {
        return includeDirectory;
    }

    protected File getTransDirectory() {
        return transDirectory;
    }

    protected File getSrcgenDirectory() {
        return srcgenDirectory;
    }

    protected File getSyntaxDirectory() {
        return syntaxDirectory;
    }

    protected File getDependencyDirectory() {
        return dependencyDirectory;
    }

}
