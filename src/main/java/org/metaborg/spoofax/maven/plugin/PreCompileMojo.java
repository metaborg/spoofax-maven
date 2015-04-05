package org.metaborg.spoofax.maven.plugin;

import com.google.inject.Injector;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.metaborg.spoofax.core.stratego.IStrategoRuntimeService;
import org.slf4j.impl.StaticLoggerBinder;
import org.spoofax.interpreter.core.InterpreterException;
import org.spoofax.interpreter.terms.IStrategoString;
import org.spoofax.interpreter.terms.ITermFactory;
import org.strategoxt.HybridInterpreter;
import org.strategoxt.IncompatibleJarException;

@Mojo(name="pre-compile",
        defaultPhase = LifecyclePhase.COMPILE)
@Execute(lifecycle = "spoofax-unpack-build-dependencies",
        phase = LifecyclePhase.INITIALIZE)
public class PreCompileMojo extends AbstractSpoofaxMojo {

    @Parameter(property = "spoofax.compile.skip", defaultValue = "false")
    private boolean skip;

    private HybridInterpreter runtime;

    @Override
    public void execute() throws MojoExecutionException {
        if ( skip ) { return; }
        StaticLoggerBinder.getSingleton().setMavenLog(this.getLog());
        getLog().info("Compiling Spoofax sources");
        initSpoofax();
        copy_sdf2();
        pack_sdf();
    }
 
    private void initSpoofax() throws MojoExecutionException {
        Injector spoofax = getSpoofax();
        IStrategoRuntimeService runtimeService = spoofax.getInstance(IStrategoRuntimeService.class);
        runtime = runtimeService.genericRuntime();
        try {
            runtime.loadJars(findArtifact(getPlugin().getGroupId(),"make-permissive-jar").toURI().toURL());
            runtime.loadJars(findArtifact(getPlugin().getGroupId(),"aster-jar").toURI().toURL());
            runtime.loadJars(findArtifact(getPlugin().getGroupId(),"strategoxt-min-jar").toURI().toURL());
        } catch (SecurityException | IncompatibleJarException | IOException ex) {
            throw new MojoExecutionException("", ex);
        }
        runtime.init();
    }

    private File findArtifact(String groupId, String artifactId)
            throws MojoExecutionException {
        for ( Artifact artifact : getPlugin().getArtifacts() ) {
            if ( artifact.getGroupId().equalsIgnoreCase(groupId) &&
                    artifact.getArtifactId().equalsIgnoreCase(artifactId) &&
                    artifact.getType().equals("jar") ) {
                return artifact.getFile();
            }
        }
        throw new MojoExecutionException("Could not find "+groupId+":"+artifactId);
    }

    private void copy_sdf2() throws MojoExecutionException {
        try {
            FileUtils.copyDirectory(getSyntaxDirectory(),
                    getGeneratedSyntaxDirectory(), "**/*.sdf", "");
        } catch (IOException ex) {
            throw new MojoExecutionException("", ex);
        }
    }

    private void pack_sdf() throws MojoExecutionException {
        invokeStrategy("main_pack_sdf_0_0", new String[]{
            "-i", new File(getGeneratedSyntaxDirectory(),getLanguageName()+".sdf").getPath(),
            "-o", new File(getOutputDirectory(),getLanguageName()+".def").getPath(),
            "-I", getGeneratedSyntaxDirectory().getPath(),
            "-I", getLibDirectory().getPath()
        });
    }

    private void invokeStrategy(String name, String... args)
            throws MojoExecutionException {
        getLog().info("Invoking "+name+" "+Arrays.toString(args));
        final ITermFactory factory = runtime.getFactory();
        final IStrategoString[] sargs = new IStrategoString[args.length+1];
        sargs[0] = factory.makeString(name);
        for ( int i = 0; i < args.length; i++ ) {
            sargs[i+1] = factory.makeString(args[i]);
        }
        runtime.setCurrent(factory.makeList(sargs));
        try {
            runtime.invoke(name);
        } catch (InterpreterException ex) {
            throw new MojoExecutionException("",ex);
        }
    }

}
