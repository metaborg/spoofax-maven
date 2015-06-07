package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import java.io.File;
import java.util.Set;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageDiscoveryService;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.resource.IResourceService;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo.TYPE_SPOOFAX_LANGUAGE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpoofaxMavenProject implements IProject {
    private final static Logger log = LoggerFactory.getLogger(SpoofaxMavenProject.class);

    private final FileObject location;
    private final MavenDependencyHelper dh;
    private final IResourceService resourceService;
    private final ILanguageDiscoveryService languageDiscoveryService;

    @Inject
    public SpoofaxMavenProject(MavenProject project,
            PluginDescriptor plugin,
            IResourceService resourceService,
            ILanguageDiscoveryService languageDiscoveryService) {
        this.resourceService = resourceService;
        this.languageDiscoveryService = languageDiscoveryService;
        location = resourceService.resolve(project.getBasedir());
        Set<Artifact> dependencyArtifacts = Sets.newHashSet();
        dependencyArtifacts.addAll(project.getDependencyArtifacts());
        dependencyArtifacts.addAll(plugin.getArtifacts());
        discoverLanguages(dependencyArtifacts);
        dh = new MavenDependencyHelper(project);
    }

    private void discoverLanguages(Iterable<Artifact> artifacts) {
        for ( Artifact artifact : artifacts ) {
            if ( TYPE_SPOOFAX_LANGUAGE.equalsIgnoreCase(artifact.getType()) ) {
                File file = artifact.getFile();
                if ( file != null && file.exists() ) {
                    String url = (file.isDirectory() ? "file:" : "zip:")+file.getPath();
                    FileObject artifactLocation = resourceService.resolve(url);
                    try {
                        Iterable<ILanguage> languages = languageDiscoveryService.discover(artifactLocation);
                        if ( Iterables.isEmpty(languages) ) {
                            log.error("No languages discovered in {}", artifactLocation);
                        }
                    } catch (Exception ex) {
                        log.error("Error discovering languages in {}", artifactLocation, ex);
                    }
                } else {
                    log.warn("Artifact {} has no file(s), not resolved?");
                }
            }
        }
    }

    @Override
    public FileObject location() {
        return location;
    }

    public Iterable<LanguageDependency> getRuntimeDependencies() {
        return dh.getRuntimeDependencies();
    }

    public Iterable<LanguageDependency> getCompileDependencies() {
        return dh.getCompileDependencies();
    }
 
}
