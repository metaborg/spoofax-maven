package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractRunAntMojo {

    @Override
    public void execute() throws MojoExecutionException {
        super.execute();
        executeAntTarget("clean");
    }
    
}
