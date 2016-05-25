package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.file.FileCollection

class SpoofaxLanguageDirectories {

    final Project project
    final SpoofaxSourceSet sourceSet
    final String name

    private List<?> srcPaths
    private Object outputPath

    SpoofaxLanguageDirectories(final SpoofaxSourceSet sourceSet, final String name) {
        this.project = sourceSet.project
        this.sourceSet = sourceSet
        this.name = name
        this.srcPaths = [new File(sourceSet.project.rootDir,"src/${sourceSet.name}/${name}")]
        this.outputPath = new File(sourceSet.project.buildDir,"generated/${sourceSet.name}/${name}")
    }
 
    void setSrcDirs(Iterable<?> srcPaths) {
        this.srcPaths.clear()
        this.srcPaths.addAll(srcPaths)
    }
 
    void srcDir(Object srcPath) {
        this.srcPaths.add(srcPath)
    }
 
    void srcDirs(Object... srcPaths) {
        this.srcPaths.addAll(srcPaths)
    }

    def getSrcDirs() {
        srcPaths
    }
 
    FileCollection getSrcFiles() {
        project.files(srcPaths)
    }
 
    void setOutputDir(Object outputPath) {
        this.outputPath = outputPath
    }
 
    File getOutputDir() {
        project.file(outputPath)
    }

}
