package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;

@Mojo(name="generate-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    public void execute() throws MojoFailureException {
        if ( skip ) return;
        super.execute();
        getLog().info("Generating Spoofax sources");
        ProjectSettings ps = getProjectSettings();
        SpoofaxHelper spoofax = SpoofaxHelper.get(getProject(), getPlugin(),
                getLog(), false);
        List<File> sources = Arrays.asList(
                ps.getSyntaxDirectory(),
                ps.getTransDirectory()
        );
        sources.addAll(getAdditionalSources());
        spoofax.compileDirectories(sources, getPardonedLanguages());
    }

}
