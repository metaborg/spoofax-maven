package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxMetaBuilder;

@Mojo(name="pre-compile",
        defaultPhase = LifecyclePhase.COMPILE)
public class PreCompileMojo extends AbstractSpoofaxLifecycleMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        ProjectSettings ps = getProjectSettings();
        getProject().addCompileSourceRoot(ps.getJavaDirectory().getPath());
        SpoofaxMetaBuilder metaBuilder = getSpoofax().getInstance(SpoofaxMetaBuilder.class);
        metaBuilder.compilePreJava(getMetaBuildInput());
    }

}