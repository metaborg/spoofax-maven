package org.metaborg.spoofax.maven.plugin.mojo.language.manual;

import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.MetaborgConstants;
import org.metaborg.core.language.LanguageIdentifier;
import org.metaborg.core.language.LanguageVersion;
import org.metaborg.core.project.ProjectException;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo;
import org.metaborg.spoofax.maven.plugin.SpoofaxInit;
import org.metaborg.spoofax.meta.core.config.ISpoofaxLanguageSpecConfig;
import org.metaborg.spoofax.meta.core.generator.GeneratorSettings;
import org.metaborg.spoofax.meta.core.generator.eclipse.EclipsePluginGenerator;
import org.metaborg.util.prompt.Prompter;

@Mojo(name = "generate-eclipse", requiresDirectInvocation = true, requiresProject = false)
public class GenerateEclipseProjectMojo extends AbstractSpoofaxMojo {
    @Parameter(required = false) private String groupId;
    @Parameter(required = false) private String id;
    @Parameter(required = false) private String version;
    @Parameter(required = false) private String name;
    @Parameter(defaultValue = "${plugin.version}", required = false) private String metaborgVersion;


    @Override public void execute() throws MojoFailureException, MojoExecutionException {
        try {
            super.execute();
    
            final FileObject baseDir = basedirLocation();
            final MavenProject project = mavenProject();
    
            try {
                if(project == null || project.getFile() == null) {
                    generateFromPrompt(baseDir);
                } else if("spoofax-language".equals(project.getPackaging())) {
                    generateFromProject(project);
                } else {
                    getLog().error("Found existing project " + project.getName()
                        + ", but it is not of packaging type 'spoofax-language'");
                }
            } catch(FileSystemException e) {
                throw new MojoExecutionException("Generating project failed unexpectedly", e);
            }
        } finally {
            SpoofaxInit.close();
        }
    }

    private void generateFromPrompt(FileObject baseDir) throws MojoFailureException, FileSystemException {
        final PrintStream out = System.out;

        out.println("Generating Eclipse plugin project from scratch");

        final Prompter prompter;
        try {
            prompter = Prompter.get();
        } catch(IOException ex) {
            throw new MojoFailureException("Must run interactively", ex);
        }

        out.println(
            "The language name, id, and version you enter must be the same as for the Spoofax language project");

        String groupId = this.groupId;
        while(groupId == null || groupId.isEmpty()) {
            groupId = prompter.readString("Group ID (e.g. 'org.metaborg')");
            if(!LanguageIdentifier.validId(groupId)) {
                System.err.println("Please enter a valid id");
                groupId = null;
            }
        }

        String id = this.id;
        while(id == null || id.isEmpty()) {
            id = prompter.readString("ID (e.g. 'org.metaborg.meta.lang.sdf')");
            if(!LanguageIdentifier.validId(id)) {
                System.err.println("Please enter a valid id");
                id = null;
            }
        }

        LanguageVersion version = null;
        if(this.version != null && LanguageVersion.valid(this.version)) {
            version = LanguageVersion.parse(this.version);
        }
        while(version == null) {
            final String versionString = prompter.readString("Version (e.g. '1.5.0-SNAPSHOT')");
            if(!LanguageVersion.valid(versionString)) {
                System.err.println("Please enter a valid version");
                version = null;
            } else {
                version = LanguageVersion.parse(versionString);
            }
        }

        String name = null;
        while(name == null || name.isEmpty()) {
            name = prompter.readString("Name (e.g. 'SDF')");
            if(!LanguageIdentifier.validId(name)) {
                System.err.println("Please enter a valid name");
                name = null;
            }
        }

        String metaborgVersion = this.metaborgVersion;
        while(metaborgVersion == null || metaborgVersion.isEmpty()) {
            metaborgVersion =
                prompter.readString("Version for MetaBorg artifacts [" + MetaborgConstants.METABORG_VERSION + "]");
            if(metaborgVersion.isEmpty()) {
                metaborgVersion = MetaborgConstants.METABORG_VERSION;
            }
        }

        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        final FileObject newBaseDir = baseDir.resolveFile(EclipsePluginGenerator.siblingName(id));
        generate(identifier, name, metaborgVersion, newBaseDir);
    }

    private void generateFromProject(MavenProject project) throws MojoFailureException, FileSystemException {
        System.out.println("Generating Eclipse plugin project from existing Spoofax language project");
        final String id = project.getArtifactId();
        final String groupId = project.getGroupId();
        final LanguageVersion version = LanguageVersion.parse(project.getVersion());
        final LanguageIdentifier identifier = new LanguageIdentifier(groupId, id, version);
        final String name = project.getName();
        final FileObject baseDir = SpoofaxInit.spoofax().resourceService.resolve(project.getBasedir().getParentFile());
        final FileObject newBaseDir = baseDir.resolveFile(EclipsePluginGenerator.siblingName(id));
        generate(identifier, name, project.getParent().getVersion(), newBaseDir);
    }

    private void generate(LanguageIdentifier identifier, String name, String metaborgVersion, FileObject baseDir)
        throws MojoFailureException {
        try {
            final ISpoofaxLanguageSpecConfig config = SpoofaxInit.spoofaxMeta().languageSpecConfigBuilder()
                .withIdentifier(identifier).withName(name).build(baseDir);
            final GeneratorSettings generatorSettings = new GeneratorSettings(baseDir, config);
            generatorSettings.setMetaborgVersion(metaborgVersion);

            final EclipsePluginGenerator newGenerator = new EclipsePluginGenerator(generatorSettings);
            newGenerator.generateAll();
        } catch(IOException ex) {
            throw new MojoFailureException("Failed to generate project files", ex);
        } catch(ProjectException ex) {
            throw new MojoFailureException("Invalid project settings", ex);
        }
    }
}
