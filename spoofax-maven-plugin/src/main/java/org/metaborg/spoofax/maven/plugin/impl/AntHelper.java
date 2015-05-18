package org.metaborg.spoofax.maven.plugin.impl;

import com.google.common.collect.Lists;
import org.metaborg.spoofax.maven.plugin.AbstractSpoofaxLifecycleMojo;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.FileObject;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DefaultLogger;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.apache.tools.ant.PropertyHelper;
import org.metaborg.spoofax.core.resource.IResourceService;
import org.metaborg.spoofax.generator.project.ProjectSettings;

public class AntHelper {

    private static final String CONTEXT_ID = "spoofax-maven-plugin.ant";

    private final AbstractSpoofaxLifecycleMojo mojo;
    private final ProjectSettings projectSettings;
    private final Project antProject;

    private AntHelper(AbstractSpoofaxLifecycleMojo mojo) throws MojoFailureException {
        this.mojo = mojo;
        this.projectSettings = mojo.getProjectSettings();
        prepareAntFiles();
        File buildFile = new File(mojo.getAntDirectory(), "build.main.xml");
        this.antProject = initProject(buildFile);
        setLogger();
        setProperties();
        parseBuildFile(buildFile);
    }

    private void prepareAntFiles() throws MojoFailureException {
        File antDirectory = mojo.getAntDirectory();
        try {
            if ( !antDirectory.exists() ) {
                antDirectory.mkdirs();
            }
            copyResource("build.main.xml", antDirectory);
            copyResource("build.generated.xml", antDirectory);
        } catch (IOException ex) {
            throw new MojoFailureException("Failed to copy Ant files.", ex);
        }
    }

    private void copyResource(String name, File directory) throws IOException {
        try (FileWriter writer = new FileWriter(new File(directory, name))) {
            IOUtils.copy(getClass().getResourceAsStream(name), writer);
        }
    }

    private Project initProject(File buildFile) throws BuildException {
        Project antProject = new Project();
        antProject.setBaseDir(projectSettings.getBaseDir());
        antProject.setProperty("ant.file", buildFile.getAbsolutePath());
        antProject.init();
        return antProject;
    }

    private void setLogger() {
        DefaultLogger consoleLogger = new DefaultLogger();
        consoleLogger.setErrorPrintStream(System.err);
        consoleLogger.setOutputPrintStream(System.out);
        if ( mojo.getLog().isDebugEnabled() ) {
            consoleLogger.setMessageOutputLevel(Project.MSG_DEBUG);
        } else if ( mojo.getLog().isInfoEnabled() ) {
            consoleLogger.setMessageOutputLevel(Project.MSG_INFO);
        } else if ( mojo.getLog().isWarnEnabled()) {
            consoleLogger.setMessageOutputLevel(Project.MSG_WARN);
        } else if ( mojo.getLog().isErrorEnabled()) {
            consoleLogger.setMessageOutputLevel(Project.MSG_ERR);
        }
        consoleLogger.setMessageOutputLevel(Project.MSG_WARN);
        antProject.addBuildListener(consoleLogger);
    }

    private void setProperties() throws MojoFailureException {
        PropertyHelper ph = PropertyHelper.getPropertyHelper(antProject);

        ph.setUserProperty("nativepath", mojo.getNativeDirectory().getAbsolutePath());
        ph.setUserProperty("distpath", mojo.getDistDirectory().getAbsolutePath());

        ph.setUserProperty("lang.name", projectSettings.name());
        ph.setUserProperty("lang.strname", projectSettings.strategoName());
        ph.setUserProperty("lang.format", projectSettings.format().name());
        ph.setUserProperty("lang.package.name", projectSettings.packageName());
        ph.setUserProperty("lang.package.path", projectSettings.packagePath());

        ph.setUserProperty("sdf.args", formatArgs(buildSdfArgs()));
        ph.setUserProperty("stratego.args", formatArgs(buildStrategoArgs()));

        if ( mojo.getExternalDef() != null ) {
            ph.setUserProperty("externaldef", mojo.getExternalDef());
        }
        if ( mojo.getExternalJar() != null ) {
            ph.setUserProperty("externaljar", mojo.getExternalJar());
        }
        if ( mojo.getExternalJarFlags() != null ) {
            ph.setUserProperty("externaljarflags", mojo.getExternalJarFlags());
        }
    }

    private List<String> buildSdfArgs() throws MojoFailureException {
        List<String> args = Lists.newArrayList(mojo.getSdfArgs());
        SpoofaxHelper spoofax = mojo.getSpoofaxHelper();
        IResourceService resourceService = spoofax.getResourceService();
        for ( FileObject path : spoofax.getLanguageSourcesAndIncludes(Languages.SDF) ) {
            if ( path.getName().getExtension().equals("def") ) {
                args.add("-Idef");
                args.add(resourceService.localFile(path).getPath());
            } else {
                args.add("-I");
                args.add(resourceService.localFile(path).getPath());
            }
        }
        return args;
    }

    private List<String> buildStrategoArgs() throws MojoFailureException {
        List<String> args = Lists.newArrayList(mojo.getStrategoArgs());
        SpoofaxHelper spoofax = mojo.getSpoofaxHelper();
        IResourceService resourceService = spoofax.getResourceService();
        for ( FileObject path : spoofax.getLanguageSourcesAndIncludes(Languages.Stratego) ) {
            args.add("-I");
            args.add(resourceService.localFile(path).getPath());
        }
        return args;
    }

    private void parseBuildFile(File buildFile) throws BuildException {
        ProjectHelper helper = ProjectHelper.getProjectHelper();
        antProject.addReference("ant.projectHelper", helper);
        helper.parse(antProject, buildFile);
    }

    public void executeTarget(String name) {
        antProject.executeTarget(name);
    }

    private String formatArgs(List<String> args) {
        String ret = "";
        for ( String arg : args ) {
            ret += " "+formatArg(arg);
        }
        return ret;
    }

    private String formatArg(String arg) {
        return StringUtils.containsWhitespace(arg) ?
            "\""+arg+"\"" : arg;
    }


    public static AntHelper get(AbstractSpoofaxLifecycleMojo mojo)
            throws MojoFailureException {
        Log log = mojo.getLog();
        MavenProject project = mojo.getProject();
        AntHelper antHelper;
        if ( (antHelper = (AntHelper) project.getContextValue(CONTEXT_ID)) == null ) {
            log.info("Initialising shared Ant");
            project.setContextValue(CONTEXT_ID,
                    antHelper = new AntHelper(mojo));
        } else {
            log.info("Using shared Ant");
        }
        return antHelper;
    }


}
