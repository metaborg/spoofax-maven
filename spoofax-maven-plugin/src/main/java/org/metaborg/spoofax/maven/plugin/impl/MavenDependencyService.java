package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.List;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.ILanguageService;
import org.metaborg.spoofax.core.project.IDependencyService;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.util.iterators.Iterables2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenDependencyService implements IDependencyService {
    private static final Logger log = LoggerFactory.getLogger(MavenDependencyService.class);
 
    private final ILanguageService languageService;

    @Inject
    public MavenDependencyService(ILanguageService languageService) {
        this.languageService = languageService;
    }

    @Override
    public Iterable<ILanguage> runtimeDependencies(IProject project) {
        if ( project instanceof SpoofaxMavenProject ) {
            return getLanguages(((SpoofaxMavenProject) project).getRuntimeDependencies());
        }
        log.error("Project should be a Maven project, something went wrong.");
        return Iterables2.empty();
    }

    @Override
    public Iterable<ILanguage> compileDependencies(IProject project) {
        if ( project instanceof SpoofaxMavenProject ) {
            return getLanguages(((SpoofaxMavenProject) project).getCompileDependencies());
        }
        log.error("Project should be a Maven project, something went wrong.");
        return languageService.getAllActive();
    }

    private Iterable<ILanguage> getLanguages(Iterable<LanguageDependency> dependencies) {
        List<ILanguage> languages = Lists.newArrayList();
        for ( LanguageDependency dependency : dependencies ) {
            ILanguage language = languageService.getWithId(dependency.id(), dependency.version());
            if ( language != null ) {
                languages.add(language);
                continue;
            }
            language = languageService.getWithId(dependency.id());
            if ( language != null ) {
                log.warn("Cannot find dependency {}, using version {}.",
                        dependency, language.version());
                languages.add(language);
                continue;
            }
            log.error("Cannot find dependency {}, make sure it is loaded.",
                    dependency);
        }
        return languages;
    }

}
