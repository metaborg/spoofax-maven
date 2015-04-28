package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="post-compile",
        defaultPhase = LifecyclePhase.COMPILE)
public class PostCompileMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        AntHelper ant = new AntHelper(this);
        ant.executeTarget("package");
    }

}
