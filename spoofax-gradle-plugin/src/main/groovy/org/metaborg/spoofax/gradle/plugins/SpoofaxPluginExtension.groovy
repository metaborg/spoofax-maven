package org.metaborg.spoofax.gradle.plugins

class SpoofaxPluginExtension {
 
    Collection<String> buildDependencies
    
    Collection<String> srcDirs // FIXME: should this be per language?

}