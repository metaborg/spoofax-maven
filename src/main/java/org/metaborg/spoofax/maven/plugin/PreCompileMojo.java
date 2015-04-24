package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.common.CommonGenerator;

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
        generateCommon();
        AntHelper ant = new AntHelper(this);
        SpoofaxHelper spoofax = new SpoofaxHelper(this);
        ant.executeTarget("generate-sources-pre-gen");
        getLog().info("Compiling editor services.");
        spoofax.compileDirectory(new File[] {
            getEditorDirectory()
        });
        ant.executeTarget("generate-sources-post-gen");
    }
 
    private void generateCommon() throws MojoFailureException {
        CommonGenerator cg = new CommonGenerator(getBasedir(), getName());
        try {
            cg.generateAll();
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to generate library files.", ex);
        }
    }

}
