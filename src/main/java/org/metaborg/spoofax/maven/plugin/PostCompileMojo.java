package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="post-compile",
        defaultPhase = LifecyclePhase.COMPILE)
public class PostCompileMojo extends AbstractAntMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        executeTarget("package");
    }

}
