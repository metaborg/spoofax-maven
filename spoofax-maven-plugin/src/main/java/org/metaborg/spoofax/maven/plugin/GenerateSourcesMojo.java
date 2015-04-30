package org.metaborg.spoofax.maven.plugin;

import java.util.Arrays;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name="generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateSourcesMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    public void execute() throws MojoFailureException {
        if ( skip ) return;
        super.execute();
        getLog().info("Generating Spoofax sources");
        ProjectSettings ps = getProjectSettings();
        getProject().addCompileSourceRoot(ps.getGeneratedSourceDirectory().getAbsolutePath());
        SpoofaxHelper spoofax = new SpoofaxHelper(getProject(), getPlugin(), getLog());
        spoofax.compileDirectory(Arrays.asList(
            ps.getSyntaxDirectory(),
            ps.getTransDirectory()
        ));
        spoofax.compileDirectory(Arrays.asList(
            ps.getGeneratedSourceDirectory()
        ));
    }

}
