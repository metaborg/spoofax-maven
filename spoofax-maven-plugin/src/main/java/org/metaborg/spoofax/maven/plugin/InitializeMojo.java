package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.spoofax.generator.project.ProjectSettings;

@Mojo(name = "initialize", defaultPhase = LifecyclePhase.INITIALIZE,
        requiresDependencyResolution = ResolutionScope.COMPILE)
public class InitializeMojo extends AbstractSpoofaxLifecycleMojo {

    @Parameter(property = "spoofax.initialise.skip", defaultValue = "false")
    boolean skip;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) { return; }
        super.execute();
        ProjectSettings ps = getProjectSettings();
        mkdirs(ps.getOutputDirectory());
        mkdirs(ps.getLibDirectory());
        mkdirs(ps.getGeneratedSourceDirectory());
        mkdirs(ps.getGeneratedSyntaxDirectory());
    }
 
    private void mkdirs(File dir) {
        if ( !dir.exists() ) {
            getLog().info("Creating: "+dir.getPath());
            dir.mkdirs();
        }
    }

}
