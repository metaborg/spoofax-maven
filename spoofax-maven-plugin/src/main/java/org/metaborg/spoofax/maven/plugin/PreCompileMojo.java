package org.metaborg.spoofax.maven.plugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.meta.core.LanguageSpecBuildInput;

@Mojo(name = "pre-compile", defaultPhase = LifecyclePhase.COMPILE)
public class PreCompileMojo extends AbstractSpoofaxLifecycleMojo {
    @Parameter(property = "spoofax.compile.skip", defaultValue = "false") private boolean skip;

    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        if(skip || skipAll) {
            return;
        }
        super.execute();
        discoverLanguages();

//        final SpoofaxProjectSettings settings = getProjectSettings();
//        final MetaBuildInput input = new MetaBuildInput(getMetaborgProject(), settings);
        final LanguageSpecBuildInput metaInput = createBuildInput();

        final MavenProject project = getProject();
        if (project == null)
            throw new RuntimeException("Maven project is null.");
        project.addCompileSourceRoot(getLanguageSpecPaths().strJavaFolder().getName().getPath());

        try {
            metaBuilder.compilePreJava(metaInput);
        } catch(Exception e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }
}
