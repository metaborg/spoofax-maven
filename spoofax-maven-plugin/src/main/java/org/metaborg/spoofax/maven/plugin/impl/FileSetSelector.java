package org.metaborg.spoofax.maven.plugin.impl;

import java.util.Collection;

import org.apache.commons.vfs2.FileSelectInfo;
import org.apache.commons.vfs2.FileSelector;
import org.apache.commons.vfs2.FileType;
import org.apache.maven.shared.utils.io.SelectorUtils;

public class FileSetSelector implements FileSelector {
    private final Collection<String> includes;
    private final Collection<String> excludes;


    public FileSetSelector(Collection<String> includes, Collection<String> excludes) {
        this.includes = includes;
        this.excludes = excludes;
    }


    @Override public boolean includeFile(FileSelectInfo fileInfo) throws Exception {
        if(FileType.FOLDER.equals(fileInfo.getFile().getType())) {
            return false;
        }
        String relativeName = fileInfo.getBaseFolder().getName().getRelativeName(fileInfo.getFile().getName());
        for(String exclude : excludes) {
            if(SelectorUtils.matchPath(exclude, relativeName)) {
                return false;
            }
        }
        if(includes.isEmpty()) {
            return true;
        }
        for(String include : includes) {
            if(SelectorUtils.matchPath(include, relativeName)) {
                return true;
            }
        }
        return false;
    }

    @Override public boolean traverseDescendents(FileSelectInfo fileInfo) throws Exception {
        return true;
    }
}
