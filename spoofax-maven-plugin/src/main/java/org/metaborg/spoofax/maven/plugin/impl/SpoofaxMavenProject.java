package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.io.File;
import java.util.List;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxMavenProject implements IProject {
    private final static Logger log = LoggerFactory.getLogger(SpoofaxMavenProject.class);

    private final FileObject location;
    private final IResourceService resourceService;
    private final ILanguageService languageService;
    private final ILanguageDiscoveryService languageDiscoveryService;
    private final Iterable<ILanguage> dependencies;
    private final Iterable<ILanguage> compileLanguages;

    @Inject
    public SpoofaxMavenProject(MavenProject project,
            PluginDescriptor plugin,
            IResourceService resourceService,
            ILanguageService languageService,
            ILanguageDiscoveryService languageDiscoveryService) {
        this.languageService = languageService;
        this.languageDiscoveryService = languageDiscoveryService;
        this.resourceService = resourceService;
        location = resourceService.resolve(project.getBasedir());
        MavenDependencyHelper dh = new MavenDependencyHelper(project, plugin);
        this.dependencies = discoverFromArtifacts(dh.getLibraryArtifacts());
        this.compileLanguages = discoverFromArtifacts(dh.getCompileArtifacts());
        discoverFromArtifacts(dh.getCompileDependencyArtifacts());
    }

    private Iterable<ILanguage> discoverFromArtifacts(Iterable<Artifact> artifacts) {
        List<Iterable<ILanguage>> languages = Lists.newArrayList();
        for ( Artifact artifact : artifacts ) {
            File file = artifact.getFile();
            if ( file != null && file.exists() ) {
                String url = file.isDirectory() ? String.format("file:%s",file.getPath()) : String.format("zip:%s",file.getPath());
                FileObject artifactLocation = resourceService.resolve(url);
                if ( languageService.get(artifactLocation.getName()) == null ) {
                    try {
                        Iterable<ILanguage> discovered = languageDiscoveryService.discover(artifactLocation);
                        languages.add(discovered);
                    } catch (Exception ex) {
                        log.warn("Error discovering languages in {}",artifactLocation,ex);
                    }
                } else {
                    log.debug("Skipping already loaded language in {}",artifactLocation);
                }
            } else {
                log.warn("Artifact {} has no associated file, dependencies not resolved?", artifact.getId());
            }
        }
        return Iterables.concat(languages);
    }

    @Override
    public FileObject location() {
        return location;
    }
 
    public Iterable<ILanguage> runtimeLanguages() {
        return dependencies;
    }

    public Iterable<ILanguage> compileLanguages() {
        return compileLanguages;
    }
 
}
