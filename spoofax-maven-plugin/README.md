# Spoofax Maven Plugin

THis repository contains the sources for a Maven plugin that allows
the compilation of Spoofax langauges using only Maven.

## Installing

Run `mvn install` to install this plugin.

## Bootstrapping meta langauges from Eclipse build

Some of the meta-languages need to be bootstrapped (usually
`TemplateLang`/`SDF3`, `NaBL` and `TS`). Special POM files are
provided for this in the examples. To bootstrap, e.g. `NaBL`, you'll
need to do the following:

 * Make sure you have a checkout of the language repository.
 * Build the language using a Eclipse-based Spoofax installation.
 * Copy `examples/nabl/pom.bootstrap-language.xml` to the project
   root.
 * Run `mvn -f pom.bootstrap-language.xml install`.

## Building a language with Maven

See `examples\Entity` for an example language using the Maven
plugin. The project has the `spoofax-language` packaging, which is
provided by the plugin. Meta-languages that are necessary during the
build are declared as plugin dependencies.

## Limitations

 * The plugin does not support the standard Maven directory layout, but
   is hardcoded to use the classic Spoofax language directory layout.
 * Many of the meta-languages have non-trivial builds, currently they
   cannot be build using this plugin yet. For now, they have to be
   bootstrapped from Eclipse-based builds.
