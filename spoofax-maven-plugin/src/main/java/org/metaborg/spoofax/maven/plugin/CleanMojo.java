package org.metaborg.spoofax.maven.plugin;

import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.processing.IProcessorRunner;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoredDirectories;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.meta.core.SpoofaxMetaBuilder;

@Mojo(name = "clean", defaultPhase = LifecyclePhase.CLEAN)
public class CleanMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "clean.skip", defaultValue = "false") private boolean skip;


    @Override public void execute() throws MojoFailureException {
        if(skip) {
            return;
        }
        super.execute();

        final IProcessorRunner<?, ?, ?> processor = getSpoofax().getInstance(IProcessorRunner.class);
        final SpoofaxMetaBuilder metaBuilder = getSpoofax().getInstance(SpoofaxMetaBuilder.class);
        final ProjectSettings projectSettings = getProjectSettings();
        final CleanInput input = new CleanInput(getSpoofaxProject(), SpoofaxIgnoredDirectories.excludeFileSelector());

        try {
            processor.clean(input, null).schedule().block();
            metaBuilder.clean(projectSettings);
            FileUtils.deleteDirectory(getDependencyDirectory());
            FileUtils.deleteDirectory(getDependencyMarkersDirectory());
        } catch(IOException | InterruptedException e) {
            throw new MojoFailureException("Failed to clean", e);
        }
    }
}
