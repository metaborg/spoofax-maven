package org.metaborg.spoofax.maven.plugin;

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
import org.metaborg.core.build.NewBuildInputBuilder;
import org.metaborg.spoofax.core.project.configuration.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.core.project.settings.SpoofaxProjectSettings;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
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

//        final SpoofaxProjectSettings settings = getProjectSettings();
        final LanguageSpecBuildInput metaInput = createBuildInput();

        try {
            metaBuilder.generateSources(metaInput, new FileAccess());
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        try {
            final NewBuildInputBuilder inputBuilder = new NewBuildInputBuilder(getLanguageSpec());
            // @formatter:off
            final BuildInput input = inputBuilder
                .withDefaultIncludePaths(true)
                .withSourcesFromDefaultSourceLocations(true)
                .withSelector(new SpoofaxIgnoresSelector())
                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, true, true, logger))
                .withThrowOnErrors(true)
                .withPardonedLanguageStrings(getLanguageSpecConfig().pardonedLanguages())
                .addTransformGoal(new CompileGoal())
                .build(dependencyService, languagePathService)
                ;
            // @formatter:on

            processorRunner.build(input, null, null).schedule().block();
        } catch(MetaborgException | InterruptedException e) {
            throw new MojoExecutionException("Generating sources failed unexpectedly", e);
        } catch(MetaborgRuntimeException e) {
            throw new MojoFailureException("Generating sources failed", e);
        }
    }
}
