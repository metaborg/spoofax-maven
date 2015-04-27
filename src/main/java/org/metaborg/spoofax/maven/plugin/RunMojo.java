package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

@Mojo(name = "run")
public class RunMojo extends AbstractMojo {

    @Parameter(readonly = true, required = true)
    String name;

    @Parameter(readonly = true)
    String[] args;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    private PluginDescriptor plugin;

    public String[] getArgs() {
        return args == null ? new String[0] : args;
    }

    @Override
    public void execute() throws MojoFailureException {
        SpoofaxHelper spoofax = new SpoofaxHelper(project, plugin, getLog());
        spoofax.runStrategy(name, getArgs());
    }
    
}
