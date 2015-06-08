package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Inject;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.meta.core.IMavenProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenMojoProjectService implements IProjectService, IMavenProjectService {
    private static final long serialVersionUID = 4023956968975951264L;
    private static final Logger log = LoggerFactory.getLogger(MavenMojoProjectService.class);

    private final MavenProject mavenProject;
    private final FileObject basedir;
    private final IProject project;

    @Inject public MavenMojoProjectService(MavenProject mavenProject,
            IResourceService resourceService) {
        this.mavenProject = mavenProject;
        this.basedir = resourceService.resolve(mavenProject.getBasedir());
        this.project = new IProject() {
            @Override
            public FileObject location() {
                return basedir;
            }
        };
    }

    @Override
    public IProject get(FileObject resource) {
        FileName resourceName = resource.getName();
        FileName projectName = basedir.getName();
        if ( !(projectName.equals(resourceName) ||
                projectName.isDescendent(resourceName)) ) {
            log.warn("Resource {} outside Maven project {}.", resourceName,
                    projectName);
        }
        return project;
    }

    @Override
    public MavenProject get(IProject project) {
        FileName projectName = basedir.getName();
        if ( !basedir.equals(project.location()) ) {
            log.warn("Project {} different from Maven project {}.",
                    project.location().getName(), projectName);
        }
        return mavenProject;
    }
 
}
