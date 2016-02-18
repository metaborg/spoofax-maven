package org.metaborg.spoofax.maven.plugin.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.MetaborgRuntimeException;
import org.metaborg.core.action.CompileGoal;
import org.metaborg.core.build.BuildInput;
import org.metaborg.core.build.BuildInputBuilder;
import org.metaborg.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.SpoofaxLanguageSpecBuildInput;
import org.metaborg.util.file.FileAccess;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxLifecycleMojo {
    private static final ILogger logger = LoggerUtils.logger(GenerateSourcesMojo.class);

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();
        discoverLanguages();

        getLog().info("Generating Spoofax sources");

        final SpoofaxLanguageSpecBuildInput metaInput = createBuildInput();

        try {
            SpoofaxInit.spoofaxMeta().metaBuilder.generateSources(metaInput, new FileAccess());
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        try {
            final BuildInputBuilder inputBuilder = new BuildInputBuilder(getLanguageSpec());
            // @formatter:off
            final BuildInput input = inputBuilder
                .withDefaultIncludePaths(true)
                .withSourcesFromDefaultSourceLocations(true)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(SpoofaxInit.spoofax().sourceTextService, true, true, logger))
                .withThrowOnErrors(true)
                .withPardonedLanguageStrings(getLanguageSpec().config().pardonedLanguages())
                .addTransformGoal(new CompileGoal())
                .build(SpoofaxInit.spoofax().dependencyService, SpoofaxInit.spoofax().languagePathService)
                ;
            // @formatter:on

            SpoofaxInit.spoofax().processorRunner.build(input, null, null).schedule().block();
        } catch(MetaborgException | InterruptedException e) {
            throw new MojoExecutionException("Generating sources failed unexpectedly", e);
        } catch(MetaborgRuntimeException e) {
            throw new MojoFailureException("Generating sources failed", e);
        }
    }
}
