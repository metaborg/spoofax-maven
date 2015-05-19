package org.metaborg.spoofax.maven.plugin;

import java.io.File;
import java.io.IOException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaborg.spoofax.generator.eclipse.EclipseProjectGenerator;
import org.metaborg.spoofax.generator.project.ProjectException;
import org.metaborg.spoofax.generator.project.ProjectSettings;

@Mojo(name = "generate-eclipse-sources",
        defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class GenerateEclipseSourcesMojo extends AbstractMojo {

    @Parameter(property = "spoofax.generate-sources.skip", defaultValue = "false")
    private boolean skip;

    @Parameter(defaultValue = "${project.name}")
    private String name;

    @Parameter(defaultValue = "${basedir}", readonly = true, required = true)
    private File basedir;

    @Override
    public void execute() throws MojoFailureException {
        if ( skip ) return;
        generateCommon();
    }
    
    private void generateCommon() throws MojoFailureException {
        try {
            ProjectSettings ps = new ProjectSettings(name, basedir);
            EclipseProjectGenerator epg = new EclipseProjectGenerator(ps);
            epg.generateAll();
        } catch (IOException | ProjectException ex) {
            throw new MojoFailureException("Failed to generate library files.", ex);
        }
    }

}
