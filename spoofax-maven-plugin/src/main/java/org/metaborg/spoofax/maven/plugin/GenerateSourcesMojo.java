package org.metaborg.spoofax.maven.plugin;

import java.io.OutputStream;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.core.SpoofaxRuntimeException;
import org.metaborg.spoofax.core.build.BuildInput;
import org.metaborg.spoofax.core.build.BuildInputBuilder;
import org.metaborg.spoofax.core.build.ConsoleBuildMessagePrinter;
import org.metaborg.spoofax.core.build.IBuilder;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoredDirectories;
import org.metaborg.spoofax.core.source.ISourceTextService;
import org.metaborg.spoofax.core.transform.CompileGoal;
import org.metaborg.spoofax.meta.core.MetaBuildInput;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;
import org.metaborg.util.log.LoggingOutputStream;

import com.google.inject.Injector;

@Mojo(name = "generate-sources", defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateSourcesMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException {
        if(skip) {
            return;
        }
        super.execute();

        getLog().info("Generating Spoofax sources");

        final Injector spoofax = getSpoofax();
        final MetaBuildInput metaInput = getMetaBuildInput();

        final SpoofaxMetaBuilder metaBuilder = spoofax.getInstance(SpoofaxMetaBuilder.class);
        try {
            metaBuilder.generateSources(metaInput);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }

        final ISourceTextService sourceTextService = spoofax.getInstance(ISourceTextService.class);
        final OutputStream logOutputStream =
            new LoggingOutputStream(org.slf4j.LoggerFactory.getLogger(GenerateSourcesMojo.class), false);

        final BuildInputBuilder inputBuilder = new BuildInputBuilder(getSpoofaxProject());
        // @formatter:off
        final BuildInput input = inputBuilder
            .withDefaultIncludeLocations(true)
            .withResourcesFromDefaultSourceLocations(true)
            .withSelector(SpoofaxIgnoredDirectories.includeFileSelector())
            .withMessagePrinter(new ConsoleBuildMessagePrinter(sourceTextService, logOutputStream, true, true))
            .withThrowOnErrors(true)
            .withPardonedLanguageStrings(metaInput.pardonedLanguages)
            .addGoal(new CompileGoal())
            .build(spoofax)
            ;
        // @formatter:on

        final IBuilder<?, ?, ?> builder = spoofax.getInstance(IBuilder.class);
        try {
            builder.build(input);
        } catch(SpoofaxRuntimeException e) {
            throw new MojoFailureException("Error generating sources", e);
        }
    }
}
