package org.metaborg.spoofax.gradle.plugins

import org.gradle.api.Project
import org.metaborg.core.language.LanguageImplementation
import org.metaborg.util.log.LoggerUtils

class SpoofaxLanguageConfiguration {
    private static def logger = LoggerUtils.logger(SpoofaxLanguageConfiguration)
        
    final LanguageImplementation languageImpl
    final SpoofaxSourceConfiguration main
 
    SpoofaxLanguageConfiguration(final Project project,
            final LanguageImplementation languageImpl) {
        this.languageImpl = languageImpl
        this.main =  new SpoofaxSourceConfiguration(project,languageImpl,
                SpoofaxGradleConstants.SPOOFAX_MAIN_CONFIGURATION)
    }
 
    void main(Closure closure) {
        main.with closure
    }
 
}