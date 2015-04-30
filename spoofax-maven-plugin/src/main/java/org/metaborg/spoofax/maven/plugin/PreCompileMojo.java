package org.metaborg.spoofax.maven.plugin;

import org.metaborg.spoofax.maven.plugin.impl.AntHelper;
import java.io.IOException;
import java.util.Arrays;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.ProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectSettings;
import org.metaborg.spoofax.maven.plugin.impl.SpoofaxHelper;
import org.metaborg.spoofax.maven.plugin.impl.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.impl.AbstractSpoofaxMojo;

@Mojo(name="pre-compile",
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(lifecycle = "spoofax-unpack-build-dependencies",
        phase = LifecyclePhase.INITIALIZE)
public class PreCompileMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        ProjectSettings ps = getProjectSettings();
        getProject().addCompileSourceRoot(ps.getEditorJavaDirectory().getAbsolutePath());
        generateCommon();
        AntHelper ant = new AntHelper(this);
        SpoofaxHelper spoofax = new SpoofaxHelper(getProject(), getPlugin(), getLog());
        ant.executeTarget("generate-sources-pre-gen");
        getLog().info("Compiling editor services.");
        spoofax.compileDirectory(Arrays.asList(
            ps.getEditorDirectory()
        ));
        ant.executeTarget("generate-sources-post-gen");
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
