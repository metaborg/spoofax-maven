package org.metaborg.spoofax.maven.plugin;

import org.metaborg.spoofax.maven.plugin.impl.AntHelper;
import java.io.IOException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.ProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectSettings;

@Mojo(name="pre-compile",
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(lifecycle = "spoofax-unpack-build-dependencies",
        phase = LifecyclePhase.INITIALIZE)
public class PreCompileMojo extends AbstractSpoofaxLifecycleMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        ProjectSettings ps = getProjectSettings();
        getProject().addCompileSourceRoot(ps.getJavaDirectory().getPath());
        generateCommon();
        AntHelper ant = AntHelper.get(this);
        ant.executeTarget("generate-sources");
    }

    private void generateCommon() throws MojoFailureException {
        ProjectGenerator cg = new ProjectGenerator(getProjectSettings());
        try {
            cg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate library files.", ex);
        }
    }

}
