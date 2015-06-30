package org.metaborg.spoofax.maven.plugin;

import java.io.IOException;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.utils.io.FileUtils;
import org.metaborg.spoofax.core.build.IBuilder;
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

        final IBuilder<?, ?, ?> builder = getSpoofax().getInstance(IBuilder.class);
        final SpoofaxMetaBuilder metaBuilder = getSpoofax().getInstance(SpoofaxMetaBuilder.class);
        final ProjectSettings projectSettings = getProjectSettings();
        try {
            builder.clean(projectSettings.location());
            metaBuilder.clean(projectSettings);
            FileUtils.deleteDirectory(getDependencyDirectory());
            FileUtils.deleteDirectory(getDependencyMarkersDirectory());
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to clean", ex);
        }
    }
}
