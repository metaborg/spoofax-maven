package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;
import org.sugarj.common.Log;

import build.pluto.builder.BuildManagers;
import build.pluto.builder.BuildRequest;
import build.pluto.buildspoofax.Main;

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

        Log.log.setLoggingLevel(Log.ALWAYS);
        try {
            BuildManagers.build(new BuildRequest<>(Main.factory, new Main.Input(getProjectSettings(), getProject()
                .getCompileClasspathElements())));
        } catch(DependencyResolutionRequiredException e) {
            throw new MojoFailureException("Generating sources failed", e);
        }

        // final SpoofaxProjectSettings settings = getProjectSettings();
        // final MetaBuildInput metaInput = new MetaBuildInput(getMetaborgProject(), settings);
        //
        // try {
        // metaBuilder.generateSources(metaInput);
        // } catch(Exception e) {
        // throw new MojoFailureException(e.getMessage(), e);
        // }
        //
        // try {
        // final BuildInputBuilder inputBuilder = new BuildInputBuilder(getMetaborgProject());
//            // @formatter:off
//            final BuildInput input = inputBuilder
//                .withDefaultIncludePaths(true)
//                .withSourcesFromDefaultSourceLocations(true)
//                .withSelector(new SpoofaxIgnoresSelector())
//                .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, true, true, logger))
//                .withThrowOnErrors(true)
//                .withPardonedLanguageStrings(settings.pardonedLanguages())
//                .addTransformGoal(new CompileGoal())
//                .build(dependencyService, languagePathService)
//                ;
//            // @formatter:on
        //
        // processorRunner.build(input, null, null).schedule().block();
        // } catch(MetaborgException | InterruptedException e) {
        // throw new MojoExecutionException("Generating sources failed unexpectedly", e);
        // } catch(MetaborgRuntimeException e) {
        // throw new MojoFailureException("Generating sources failed", e);
        // }
    }
}
