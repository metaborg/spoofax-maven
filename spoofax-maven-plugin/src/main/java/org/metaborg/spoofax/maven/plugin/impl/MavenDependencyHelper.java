package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import java.util.List;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.language.LanguageVersion;
import static org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo.TYPE_SPOOFAX_LANGUAGE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDependencyHelper {
    private static final Logger log = LoggerFactory.getLogger(MavenDependencyHelper.class);

    private final List<LanguageDependency> runtimeDependencies;
    private final List<LanguageDependency> compileDependencies;
 
    public MavenDependencyHelper(MavenProject project) {
        runtimeDependencies = Lists.newArrayList();
        fillRuntimeDependencies(project);
        compileDependencies = Lists.newArrayList();
        fillCompileDependencies(project);
    }

    private void fillRuntimeDependencies(MavenProject project) {
        for ( Dependency dependency : project.getModel().getDependencies() ) {
            if ( TYPE_SPOOFAX_LANGUAGE.equalsIgnoreCase(dependency.getType()) ) {
                runtimeDependencies.add(new LanguageDependency(dependency.getArtifactId(),
                        LanguageVersion.parse(dependency.getVersion())));
            }
        }
    }

    private void fillCompileDependencies(MavenProject project) {
        for ( Plugin plugin : project.getModel().getBuild().getPlugins() ) {
            if ( "spoofax-maven-plugin".equals(plugin.getArtifactId()) ) {
                for ( Dependency dependency : plugin.getDependencies() ) {
                    if ( TYPE_SPOOFAX_LANGUAGE.equalsIgnoreCase(dependency.getType()) ) {
                        compileDependencies.add(
                                new LanguageDependency(dependency.getArtifactId(),
                                        LanguageVersion.parse(dependency.getVersion())));
                    }
                }
            }
        }
    }

    public Iterable<LanguageDependency> getRuntimeDependencies() {
        return runtimeDependencies;
    }

    public Iterable<LanguageDependency> getCompileDependencies() {
        return compileDependencies;
    }

}
