package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name="pre-compile",
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(lifecycle = "spoofax-unpack-build-dependencies",
        phase = LifecyclePhase.INITIALIZE)
public class PreCompileMojo extends AbstractAntMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        super.execute();
        executeTarget("generate-sources");

        findArtifact(getPlugin().getGroupId(),"make-permissive-jar");
        findArtifact(getPlugin().getGroupId(),"aster-jar");
        findArtifact(getPlugin().getGroupId(),"strategoxt-min-jar");
    }
 
    private File findArtifact(String groupId, String artifactId)
            throws MojoExecutionException {
        for ( Artifact artifact : getPlugin().getArtifacts() ) {
            if ( artifact.getGroupId().equalsIgnoreCase(groupId) &&
                    artifact.getArtifactId().equalsIgnoreCase(artifactId) &&
                    artifact.getType().equals("jar") ) {
                return artifact.getFile();
            }
        }
        throw new MojoExecutionException("Could not find "+groupId+":"+artifactId);
    }

}
