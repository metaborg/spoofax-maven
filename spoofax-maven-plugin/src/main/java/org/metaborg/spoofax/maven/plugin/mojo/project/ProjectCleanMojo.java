package org.metaborg.spoofax.maven.plugin.mojo.project;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.metaborg.core.MetaborgException;
import org.metaborg.core.build.CleanInput;
import org.metaborg.core.build.CleanInputBuilder;
import org.metaborg.spoofax.core.resource.SpoofaxIgnoresSelector;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;

@Mojo(name = "project-clean", defaultPhase = LifecyclePhase.CLEAN,
    requiresDependencyResolution = ResolutionScope.COMPILE, requiresDependencyCollection = ResolutionScope.COMPILE)
public class ProjectCleanMojo extends AbstractSpoofaxMojo {
    @Parameter(property = "clean.skip", defaultValue = "false") private boolean skip;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            if(skip) {
                return;
            }
            super.execute();
            discoverLanguages();
    
            final CleanInput input;
            try {
                final CleanInputBuilder inputBuilder = new CleanInputBuilder(project());
                // @formatter:off
                input = inputBuilder
                    .withSelector(new SpoofaxIgnoresSelector())
                    .build(SpoofaxInit.spoofax().dependencyService)
                    ;
                // @formatter:on
            } catch(MetaborgException e) {
                throw new MojoExecutionException("Building clean input failed unexpectedly", e);
            }
    
            try {
                SpoofaxInit.spoofax().processorRunner.clean(input, null, null).schedule().block();
            } catch(InterruptedException e) {
                // Ignore
            }
        } finally {
            SpoofaxInit.close();
        }
    }
}
