package org.metaborg.spoofax.maven.plugin.impl;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.project.IProject;
import org.metaborg.core.project.IProjectService;
import org.metaborg.core.resource.IResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class MetaborgProjectService implements IProjectService {
    private static final Logger log = LoggerFactory.getLogger(MetaborgProjectService.class);

    private final FileObject basedir;
    private final IProject project;


    @Inject public MetaborgProjectService(MavenProject mavenProject, IResourceService resourceService) {
        this.basedir = resourceService.resolve(mavenProject.getBasedir());
        this.project = new IProject() {
            @Override public FileObject location() {
                return basedir;
            }
        };
    }


    @Override public IProject get(FileObject resource) {
        final FileName resourceName = resource.getName();
        final FileName projectName = basedir.getName();
        if(!(projectName.equals(resourceName) || projectName.isDescendent(resourceName))) {
            log.warn("Resource {} outside Maven project {}.", resourceName, projectName);
        }
        return project;
    }
}
