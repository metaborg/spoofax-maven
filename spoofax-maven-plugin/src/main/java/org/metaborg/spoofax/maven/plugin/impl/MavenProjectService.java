package org.metaborg.spoofax.maven.plugin.impl;

import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.core.resource.IResourceService;

public class MavenProjectService implements IProjectService {
    private static final long serialVersionUID = 4023956968975951264L;

    private final IProject project;
    private final FileObject basedir;

    @Inject
    public MavenProjectService(IResourceService resourceService, MavenProject project) {
        basedir = resourceService.resolve(project.getBasedir());
        this.project = new IProject() {
            @Override
            public FileObject location() {
                return basedir;
            }
        };
    }

    @Override
    public IProject get(FileObject resource) {
        return project;
    }
    
}
