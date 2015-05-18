package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "run")
public class RunMojo extends AbstractSpoofaxMojo {

    @Parameter(defaultValue = "false")
    boolean skip;

    @Parameter(readonly = true, required = true)
    private String name;

    @Parameter(readonly = true)
    private String[] args;

    public String[] getArgs() {
        return args == null ? new String[0] : args;
    }

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        getSpoofaxHelper().runStrategy(name, getArgs());;
    }
    
}
