package org.metaborg.spoofax.maven.plugin;

import com.google.inject.Inject;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.project.MavenProject;
import org.metaborg.spoofax.core.project.IProject;
import org.metaborg.spoofax.core.project.IProjectService;
import org.metaborg.spoofax.core.resource.IResourceService;

public class MavenProjectService implements IProjectService {

    private final IProject project;

    @Inject
    public MavenProjectService(IResourceService resourceService, MavenProject project) {
        final FileObject basedir = resourceService.resolve(project.getBasedir());
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
