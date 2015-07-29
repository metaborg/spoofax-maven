package org.metaborg.spoofax.maven.plugin;

import java.io.OutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.core.transform.CompileGoal;
import org.metaborg.spoofax.core.project.SpoofaxProjectSettings;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.util.log.LoggingOutputStream;

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip) {
            return;
        }
        super.execute();
        discoverLanguages();

        getLog().info("Generating Spoofax sources");

        final SpoofaxProjectSettings settings = getProjectSettings();
        final MetaBuildInput metaInput = new MetaBuildInput(getSpoofaxProject(), settings);

        try {
            metaBuilder.generateSources(metaInput);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        final OutputStream logOutputStream =
            new LoggingOutputStream(org.slf4j.LoggerFactory.getLogger(GenerateSourcesMojo.class), false);

        try {
            final BuildInputBuilder inputBuilder = new BuildInputBuilder(getSpoofaxProject());
            // @formatter:off
            final BuildInput input = inputBuilder
                .withDefaultIncludePaths(true)
                .withSourcesFromDefaultSourceLocations(true)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, logOutputStream, true, true))
                .withThrowOnErrors(true)
                .withPardonedLanguageStrings(settings.pardonedLanguages())
                .addTransformGoal(new CompileGoal())
                .build(dependencyService, languagePathService)
                ;
            // @formatter:on

            processor.build(input, null, null).schedule().block();
        } catch(MetaborgException | InterruptedException e) {
            throw new MojoExecutionException("Generating sources failed unexpectedly", e);
        } catch(MetaborgRuntimeException e) {
            throw new MojoFailureException("Generating sources failed", e);
        }
    }
}
