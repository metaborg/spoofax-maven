package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Inject;
import org.apache.commons.vfs2.FileName;
import org.apache.commons.vfs2.FileObject;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenProjectService implements IProjectService {
    private static final long serialVersionUID = 4023956968975951264L;
    private static final Logger log = LoggerFactory.getLogger(MavenProjectService.class);

    private final IProject project;

    @Inject public MavenProjectService(SpoofaxMavenProject project) {
        this.project = project;
    }

    @Override
    public IProject get(FileObject resource) {
        FileName resourceName = resource.getName();
        FileName projectName = project.location().getName();
        if ( !(projectName.equals(resourceName) ||
                projectName.isDescendent(resourceName)) ) {
            log.warn("Resource {} outside project {}.", resourceName, projectName);
        }
        return project;
    }
 
}
