package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Inject;
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
            return ((SpoofaxMavenProject) project).runtimeLanguages();
        }
        log.error("Project should be a Maven project, something went wrong.");
        return Iterables2.empty();
    }

    @Override
    public Iterable<ILanguage> compileDependencies(IProject project) {
        if ( project instanceof SpoofaxMavenProject ) {
            return ((SpoofaxMavenProject) project).compileLanguages();
        }
        log.error("Project should be a Maven project, somewting went wrong.");
        return languageService.getAllActive();
    }

}
