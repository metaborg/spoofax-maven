package org.metaborg.spoofax.maven.plugin;

import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.CleanInputBuilder;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "clean.skip", defaultValue = "false") private boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip) {
            return;
        }
        super.execute();
        discoverLanguages();

        final CleanInput input;
        try {
            final CleanInputBuilder inputBuilder = new CleanInputBuilder(getLanguageSpec());
            // @formatter:off
            input = inputBuilder
                .withSelector(new SpoofaxIgnoresSelector())
                .build(dependencyService)
                ;
            // @formatter:on
        } catch(MetaborgException e) {
            throw new MojoExecutionException("Building clean input failed unexpectedly", e);
        }
//        final SpoofaxProjectSettings projectSettings = getProjectSettings();

        try {
            final LanguageSpecBuildInput metaInput = createBuildInput();

            processorRunner.clean(input, null, null).schedule().block();
            metaBuilder.clean(metaInput);
        } catch(IOException | InterruptedException e) {
            throw new MojoExecutionException("Cleaning project failed unexpectedly", e);
        }
    }
}
