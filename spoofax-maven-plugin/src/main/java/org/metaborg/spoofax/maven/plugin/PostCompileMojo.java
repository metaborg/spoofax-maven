package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.ant.AntSLF4JLogger;

@Mojo(name = "post-compile", defaultPhase = LifecyclePhase.COMPILE)
public class PostCompileMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.compile.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();

        final MetaBuildInput input = new MetaBuildInput(getMetaborgProject(), getProjectSettings());

        try {
            metaBuilder.compilePostJava(input, null, new AntSLF4JLogger());
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
