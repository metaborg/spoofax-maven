package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name="generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxLifecycleMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) return;
        super.execute();
        getLog().info("Generating Spoofax sources");
        SpoofaxHelper spoofax = getSpoofaxHelper();
        spoofax.transformFiles(new CompileGoal(), getPardonedLanguages());
    }

}
