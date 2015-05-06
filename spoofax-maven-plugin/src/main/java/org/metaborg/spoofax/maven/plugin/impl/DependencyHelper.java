package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactFilterException;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxMojo.TYPE_SPOOFAX_LANGUAGE;

public class DependencyHelper {

    private final MavenProject project;
    private final PluginDescriptor plugin;

    private final List<Artifact> dependencyArtifacts;
    private final List<Artifact> compileArtifacts;
    private final List<Artifact> compileDependencyArtifacts;
    
    public DependencyHelper(MavenProject project, PluginDescriptor plugin)
            throws MojoFailureException {
        this.project = project;
        this.plugin = plugin;
        try {
            dependencyArtifacts = filterArtifacts(
                    project.getArtifacts(), Artifact.SCOPE_COMPILE);

            List<Artifact> pluginArtifacts = filterArtifacts(
                    plugin.getArtifacts(), Artifact.SCOPE_RUNTIME);
            compileArtifacts = Lists.newArrayList();
            compileDependencyArtifacts = Lists.newArrayList();
            List<String> directDependencyKeys = getPluginDependencyIds();
            for ( Artifact artifact : pluginArtifacts ) {
                if ( directDependencyKeys.contains(getId(artifact)) ) {
                    compileArtifacts.add(artifact);
                } else {
                    compileDependencyArtifacts.add(artifact);
                }
            }
        } catch (ArtifactFilterException ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    private List<Artifact> filterArtifacts(Collection<Artifact> artifacts, String scope)
            throws ArtifactFilterException {
        ScopeFilter filter = new ScopeFilter(scope, null);
        List<Artifact> artifactsInScope = Lists.newArrayList();
        for ( Artifact artifact : artifacts ) {
            if ( TYPE_SPOOFAX_LANGUAGE.equals(artifact.getType()) && 
                    filter.isArtifactIncluded(artifact) ) {
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

    public List<Artifact> getDependencyArtifacts() {
        return dependencyArtifacts;
    }

    public List<Artifact> getCompileArtifacts() {
        return compileArtifacts;
    }

    public List<Artifact> getCompileDependencyArtifacts() {
        return compileDependencyArtifacts;
    }

}
