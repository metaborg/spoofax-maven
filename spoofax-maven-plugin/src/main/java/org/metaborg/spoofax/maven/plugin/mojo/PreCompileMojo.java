package org.metaborg.spoofax.maven.plugin.mojo;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.SpoofaxLanguageSpecBuildInput;

@Mojo(name = "pre-compile", defaultPhase = LifecyclePhase.COMPILE)
public class PreCompileMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.compile.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();
        discoverLanguages();

        final SpoofaxLanguageSpecBuildInput metaInput = createBuildInput();

        final MavenProject project = getProject();
        if(project == null) {
            throw new MojoExecutionException("Maven project is null, cannot build project");
        }
        project.addCompileSourceRoot(getLanguageSpec().paths().strJavaFolder().getName().getPath());

        try {
            SpoofaxInit.spoofaxMeta().metaBuilder.compilePreJava(metaInput);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
