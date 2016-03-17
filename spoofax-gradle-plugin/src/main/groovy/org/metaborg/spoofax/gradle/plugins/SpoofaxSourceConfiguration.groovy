package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.metaborg.core.build.BuildInput
import org.metaborg.core.build.BuildInputBuilder
import org.metaborg.core.language.LanguageImplementation
import org.metaborg.util.log.LoggerUtils

class SpoofaxSourceConfiguration {
    private static def logger = LoggerUtils.logger(SpoofaxSourceConfiguration)

    private final Project project
    final LanguageImplementation languageImpl

    private List<?> srcPaths
    private Object outputPath

    SpoofaxSourceConfiguration(final Project project,
            final LanguageImplementation languageImpl, final String name) {
        this.project = project
        this.languageImpl = languageImpl
        def languageName = languageImpl.belongsTo.name()
        srcPaths = ["src/${name}/${languageName}"]
        outputPath = "build/generated/${languageName}/${name}"
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
    
    ConfigurableFileCollection getSrcFiles() {
        project.files(srcPaths)
    }
    
    void setOutputDir(Object outputPath) {
        this.outputPath = outputPath
    }
    
    File getOutputDir() {
        project.file(outputPath)
    }

    BuildInput getBuildInput() {
        new BuildInputBuilder(null)
    }
    
}