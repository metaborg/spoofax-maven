package org.metaborg.spoofax.maven.plugin.impl;

import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.core.project.IMavenProjectService;
import org.metaborg.core.project.IProject;
import org.metaborg.core.resource.IResourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class MavenProjectService implements IMavenProjectService {
    private static final Logger log = LoggerFactory.getLogger(MavenProjectService.class);

    private final MavenProject mavenProject;
    private final FileObject basedir;


    @Inject public MavenProjectService(MavenProject mavenProject, IResourceService resourceService) {
        this.mavenProject = mavenProject;
        this.basedir = resourceService.resolve(mavenProject.getBasedir());
    }


    @Override public MavenProject get(IProject project) {
        final FileName projectName = basedir.getName();
        if(!basedir.equals(project.location())) {
            log.warn("Project {} different from Maven project {}.", project.location().getName(), projectName);
        }
        return mavenProject;
    }
}
