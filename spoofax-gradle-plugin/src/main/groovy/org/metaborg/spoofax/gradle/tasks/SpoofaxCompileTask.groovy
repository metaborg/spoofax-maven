package org.metaborg.spoofax.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskAction
import org.metaborg.core.action.CompileGoal
import org.metaborg.core.action.EndNamedGoal
import org.metaborg.core.action.ITransformGoal
import org.metaborg.core.action.NamedGoal

class SpoofaxCompileTask extends DefaultTask {

    private ITransformGoal goal
    FileCollection sourceFiles
    File outputDir
 
    SpoofaxCompileTask() {
        this.goal = new CompileGoal()
    }
    
    @TaskAction
    def compile() {
        throw new GradleException("Compilation not implemented yet!")
    }
    
    void setGoal(String name) {
        this.goal = new EndNamedGoal(name)
    }
    
    void setGoal(List<String> names) {
        this.goal = new NamedGoal(names)
    }

}
