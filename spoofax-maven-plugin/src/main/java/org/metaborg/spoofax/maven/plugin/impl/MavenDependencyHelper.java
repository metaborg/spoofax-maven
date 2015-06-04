package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo.TYPE_SPOOFAX_LANGUAGE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDependencyHelper {
    private static final Logger log = LoggerFactory.getLogger(MavenDependencyHelper.class);

    private final MavenProject project;
    private final PluginDescriptor plugin;

    private final Set<Artifact> libraryArtifacts;
    private final Set<Artifact> compileArtifacts;
    private final Set<Artifact> compileDependencyArtifacts;
 
    public MavenDependencyHelper(MavenProject project, PluginDescriptor plugin) {
        this.project = project;
        this.plugin = plugin;
        List<Artifact> projectArtifacts =
                filterSpoofaxLanguageArtifacts(project.getDependencyArtifacts());
        libraryArtifacts = Sets.newHashSet();
        List<Dependency> directProjectDependencies = project.getModel().getDependencies();
        List<String> directProjectDependencyIds = getDependencyIds(directProjectDependencies);
        for ( Artifact artifact : projectArtifacts ) {
            if ( true || directProjectDependencyIds.contains(getId(artifact)) ) {
                libraryArtifacts.add(artifact);
            }
        }
        List<Artifact> pluginArtifacts =
                filterSpoofaxLanguageArtifacts(plugin.getArtifacts());
        compileArtifacts = Sets.newHashSet();
        compileDependencyArtifacts = Sets.newHashSet();
        List<String> directPluginDependencyIds = getPluginDependencyIds();
        for ( Artifact artifact : pluginArtifacts ) {
            if ( directPluginDependencyIds.contains(getId(artifact)) ) {
                compileArtifacts.add(artifact);
            } else {
                compileDependencyArtifacts.add(artifact);
            }
        }
        compileDependencyArtifacts.removeAll(libraryArtifacts);
    }

    private List<Artifact> filterSpoofaxLanguageArtifacts(Collection<Artifact> artifacts) {
        List<Artifact> artifactsInScope = Lists.newArrayList();
        for ( Artifact artifact : artifacts ) {
            if ( TYPE_SPOOFAX_LANGUAGE.equals(artifact.getType()) ) {
                artifactsInScope.add(artifact);
            }
        }
        return artifactsInScope;
    }

    private List<String> getPluginDependencyIds() {
        for ( Plugin modelPlugin : project.getModel().getBuild().getPlugins() ) {
            String pluginId = getId(plugin);
            String modelPluginId = getId(modelPlugin);
            if ( pluginId.equals(modelPluginId) ) {
                return getDependencyIds(modelPlugin.getDependencies());
            }
        }
        return Collections.emptyList();
    }

    private List<String> getDependencyIds(Collection<Dependency> dependencies) {
        List keys = Lists.newArrayList();
        for ( Dependency dependency : dependencies ) {
            keys.add(getId(dependency));
        }
        return keys;
    }

    private static String getId(PluginDescriptor descriptor) {
        return String.format("%s:%s",
                descriptor.getGroupId(),
                descriptor.getArtifactId());
    }

    private static String getId(Plugin plugin) {
        return String.format("%s:%s",
                plugin.getGroupId(),
                plugin.getArtifactId());
    }

    private static String getId(Dependency dependency) {
        return String.format("%s:%s:%s:%s",
                dependency.getGroupId(),
                dependency.getArtifactId(),
                dependency.getType(),
                dependency.getClassifier());
    }

    private static String getId(Artifact artifact) {
        return String.format("%s:%s:%s:%s",
                artifact.getGroupId(),
                artifact.getArtifactId(),
                artifact.getType(),
                artifact.getClassifier());
    }

    public Set<Artifact> getLibraryArtifacts() {
        return Collections.unmodifiableSet(libraryArtifacts);
    }

    public Set<Artifact> getCompileArtifacts() {
        return Collections.unmodifiableSet(compileArtifacts);
    }

    public Set<Artifact> getCompileDependencyArtifacts() {
        return Collections.unmodifiableSet(compileDependencyArtifacts);
    }

}
