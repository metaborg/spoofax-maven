package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSourcesMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    public void execute() throws MojoFailureException {
        if ( skip ) return;
        super.execute();
        getLog().info("Generating Spoofax sources");
        getProject().addCompileSourceRoot(getGeneratedSourceDirectory().getAbsolutePath());
        SpoofaxHelper spoofax = new SpoofaxHelper(getProject(), getPlugin(), getLog());
        spoofax.compileDirectory(new File[] {
            getSyntaxDirectory(),
            getTransDirectory()
        });
        spoofax.compileDirectory(new File[] {
            getGeneratedSourceDirectory()
        });
    }

}
