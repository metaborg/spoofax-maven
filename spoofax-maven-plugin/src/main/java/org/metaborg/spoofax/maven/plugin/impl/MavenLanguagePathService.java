package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.ListMultimap;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.metaborg.spoofax.core.context.IContext;
import org.metaborg.spoofax.core.context.ILanguagePathService;
import org.metaborg.spoofax.core.language.ILanguage;
import org.metaborg.spoofax.core.language.LanguagePathFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenLanguagePathService implements ILanguagePathService {
    private static final Logger log = LoggerFactory.getLogger(MavenLanguagePathService.class);

    public static final String ALL_LANGUAGES = "";

    private final ListMultimap<String, FileObject> sources;
    private final ListMultimap<String, FileObject> includes;

    public MavenLanguagePathService() {
        this.sources = ArrayListMultimap.create();
        this.includes = ArrayListMultimap.create();
    }

    @Override
    public Iterable<FileObject> getSources(IContext context, ILanguage language) {
        return getSources(language.name());
    }

    @Override
    public Iterable<FileObject> getIncludes(IContext context, ILanguage language) {
        return MavenLanguagePathService.this.getIncludes(language.name());
    }

    public Iterable<FileObject> getSources(String languageName) {
        return Iterables.concat(
                getOrDefault(sources.asMap(), languageName, Collections.EMPTY_LIST),
                getOrDefault(sources.asMap(), ALL_LANGUAGES, Collections.EMPTY_LIST));
    }

    public Iterable<FileObject> getIncludes(String languageName) {
        return Iterables.concat(
                getOrDefault(includes.asMap(), languageName, Collections.EMPTY_LIST),
                getOrDefault(includes.asMap(), ALL_LANGUAGES, Collections.EMPTY_LIST));
    }

    public void addSources(String language, FileObject directory) {
        sources.put(language, directory);
    }

    public void addSources(ILanguage language, FileObject basedir) {
        sources.putAll(getSources(language, basedir));
    }

    public void addSources(Iterable<ILanguage> languages, FileObject basedir) {
        for ( ILanguage language : languages ) {
            addSources(language, basedir);
        }
    }

    public void addIncludes(String language, FileObject directory) {
        includes.put(language, directory);
    }

    public void addIncludes(ILanguage language) {
        includes.putAll(getIncludes(language));
    }

    public void addIncludes(Iterable<ILanguage> languages) {
        for ( ILanguage language : languages ) {
            addIncludes(language);
        }
    }

    private ListMultimap<String,FileObject> getIncludes(ILanguage language) {
        final ListMultimap<String,FileObject> path = ArrayListMultimap.create();
        LanguagePathFacet lcf = language.facet(LanguagePathFacet.class);
        if ( lcf != null ) {
            for ( Entry<String,String> entry : lcf.includes.entries() ) {
                try {
                    FileObject element = language.location().resolveFile(entry.getValue());
                    path.put(entry.getKey(), element);
                } catch (FileSystemException ex) {
                    log.warn("Cannot resolve path element {}",entry.getValue(), ex);
                }
            }
        }
        return path;
    }

    private ListMultimap<String,FileObject> getSources(ILanguage language, FileObject basedir) {
        final ListMultimap<String,FileObject> path = ArrayListMultimap.create();
        LanguagePathFacet lcf = language.facet(LanguagePathFacet.class);
        if ( lcf != null ) {
            for ( Entry<String,String> entry : lcf.sources.entries() ) {
                try {
                    FileObject element = basedir.resolveFile(entry.getValue());
                    path.put(entry.getKey(), element);
                } catch (FileSystemException ex) {
                    log.warn("Cannot resolve path element {}",entry.getValue(), ex);
                }
            }
        }
        return path;
    }

    private <K,V> V getOrDefault(Map<K,V> map, K key, V defaultValue) {
        return map.containsKey(key) ? map.get(key) : defaultValue;
    }
}
