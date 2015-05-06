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
        getProject().addCompileSourceRoot(ps.getJavaDirectory().getAbsolutePath());
        generateCommon();
        AntHelper ant = new AntHelper(this);
        ant.executeTarget("generate-sources-pre-gen");
        compileEditorServices();
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

    private void compileEditorServices()
            throws MojoFailureException {
        ProjectSettings ps = getProjectSettings();
        SpoofaxHelper spoofax = SpoofaxHelper.get(getProject(), getPlugin(),
                getLog(), false);
        getLog().info("Compiling editor services.");
        spoofax.compileDirectories(Arrays.asList(
                ps.getEditorDirectory()
        ), getPardonedLanguages());
    }
 
}
