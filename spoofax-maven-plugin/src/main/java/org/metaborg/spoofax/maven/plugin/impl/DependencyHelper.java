package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.util.FileUtils;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo.TYPE_SPOOFAX_LANGUAGE;

public class DependencyHelper {

    private final MavenProject project;
    private final PluginDescriptor plugin;

    private final List<Artifact> dependencyArtifacts;
    private final List<Artifact> compileArtifacts;
    private final List<Artifact> compileDependencyArtifacts;
 
    public DependencyHelper(MavenProject project, PluginDescriptor plugin) {
        this.project = project;
        this.plugin = plugin;
        List<Artifact> projectArtifacts = filterArtifacts(
                project.getDependencyArtifacts(), Arrays.asList(Artifact.SCOPE_COMPILE));
        dependencyArtifacts = Lists.newArrayList();
        List<Dependency> directProjectDependencies = project.getModel().getDependencies();
        List<String> directProjectDependencyIds = getDependencyIds(directProjectDependencies);
        for ( Artifact artifact : projectArtifacts ) {
            if ( directProjectDependencyIds.contains(getId(artifact)) ) {
                dependencyArtifacts.add(artifact);
            }
        }
        List<Artifact> pluginArtifacts = filterArtifacts(
                plugin.getArtifacts(), Arrays.asList(Artifact.SCOPE_COMPILE, Artifact.SCOPE_RUNTIME));
        compileArtifacts = Lists.newArrayList();
        compileDependencyArtifacts = Lists.newArrayList();
        List<String> directPluginDependencyIds = getPluginDependencyIds();
        for ( Artifact artifact : pluginArtifacts ) {
            if ( directPluginDependencyIds.contains(getId(artifact)) ) {
                compileArtifacts.add(artifact);
            } else {
                compileDependencyArtifacts.add(artifact);
            }
        }
    }

    private List<Artifact> filterArtifacts(Collection<Artifact> artifacts,
            Collection<String> scopes) {
        List<Artifact> artifactsInScope = Lists.newArrayList();
        for ( Artifact artifact : artifacts ) {
            if ( TYPE_SPOOFAX_LANGUAGE.equals(artifact.getType()) &&
                    scopes.contains(artifact.getScope()) ) {
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
        return Collections.unmodifiableList(dependencyArtifacts);
    }

    public List<Artifact> getCompileArtifacts() {
        return Collections.unmodifiableList(compileArtifacts);
    }

    public List<Artifact> getCompileDependencyArtifacts() {
        return Collections.unmodifiableList(compileDependencyArtifacts);
    }

    public static Collection<Artifact> unpack(Collection<Artifact> artifacts,
            File directory, Log log) throws MojoFailureException {
        try {
            List<Artifact> unpackedArtifacts = Lists.newArrayListWithExpectedSize(artifacts.size());
            for (Artifact artifact : artifacts) {
                File artifactDirectory = new File(directory,
                        String.format("%s-%s", artifact.getGroupId(), artifact.getArtifactId()));
                File artifactFile = artifact.getFile();
                log.info(String.format("Unpacking %s to %s.", artifactFile, artifactDirectory));
                ZipUnArchiver unzip = new ZipUnArchiver(artifactFile);
                if ( !artifactDirectory.exists() || artifactDirectory.lastModified() < artifactFile.lastModified() ) {
                    FileUtils.deleteDirectory(artifactDirectory);
                    artifactDirectory.mkdirs();
                    unzip.extract("", artifactDirectory);
                }
                DefaultArtifact unpackedArtifact = new DefaultArtifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getVersion(),
                        artifact.getScope(),
                        artifact.getType(),
                        artifact.getClassifier(),
                        artifact.getArtifactHandler());
                unpackedArtifact.setFile(artifactDirectory);
                unpackedArtifacts.add(unpackedArtifact);
            }
            return unpackedArtifacts;
        } catch (IOException | ArchiverException ex) {
            throw new MojoFailureException("Cannot unpack dependencies.");
        }
    }

}
