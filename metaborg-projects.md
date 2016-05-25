# Spoofax Projects

> NB. This is a proposal, not implemented, and subject to change. 

There are two types of Spoofax projects: _language specifications_,
and _libraries_. Projects that use a Spoofax language compiler, but do
not produce a Spoofax artifact are called _client_ projects.

## Language Specification Project

A language specification project produces a language component, which
contributes to a language implementation.

### Layout

A language specification project must have the following file layout.

* `editor/`: Editor service configuration (*.esv)
  * `LANGUAGE.main.esv`: Main language configuration (required)
* `include/`: Project build artifacts (removed on Clean)
* `lib/`: External resources (e.g. *.str, *.jar)
* `metaborg.yaml`: Project configuration (required)
* `src-gen/`: Generated sources (removed on Clean)
* `syntax/`: Syntax definition (*.sdf, *.sdf3)
* `trans/`: Other language files (e.g. *.str, *.nab, *.ts)

### Configuration

A Spoofax language project is configured by a `metaborg.yaml` file.

```yaml
---
type: languagespec
group: org.metaborg
id: org.metaborg.meta.lang.nabl2
name: nabl2
version: ${metaborgVersion}
metaborgVersion: 2.0.0-SNAPSHOT
buildDependencies:
  languages:
  - org.metaborg:org.metaborg.meta.lang.esv:${metaborgVersion}
  - org.metaborg:org.metaborg.meta.lang.template:${metaborgVersion}
dependencies:
  libraries:
  - org.metaborg:org.metaborg.meta.lib.analysis:${metaborgVersion}
generates:
- language: Stratego
  directory: .
targetDependencies:
  libraries:
  - org.metaborg:org.metaborg.meta.lib.analysis2:${metaborgVersion}
```

See below for an overview of the different dependency types.

## Library Project

A library project contributes to a language specification project.

### Layout

A library project must have the following file layout.

* `metaborg.yaml`: Project configuration (required)

### Configuration

A Spoofax library project is configured by a `metaborg.yaml` file.

```yaml
---
type: library
group: org.metaborg
id: org.metaborg.meta.lib.analysis2
version: ${metaborgVersion}
metaborgVersion: 2.0.0-SNAPSHOT
buildDependencies:
  languages:
  - org.metaborg:org.metaborg.meta.lang.template:${metaborgVersion}
dependencies:
  classpath:
    compile:
    - org.jgrapht:jgrapht-core:0.9.1
    - org.metaborg:org.strategoxt.strj:${metaborgVersion}
    runtime: []
  libraries:
  - org.metaborg:org.metaborg.meta.lib.analysis:${metaborgVersion}
exports:
- type: classpath
  file: include/libanalysis2.jar
- type: ctree
  file: include/libanalysis2.ctree
- type: signature
  language: Stratego
  directory: src-gen/signatures/
  includes: **/*.str
- type: signature
  language: ds
  directory: src-gen/ds-signatures/
  includes: **/*.ds
- type: signature
  language: SDF
  directory: include
  file: include/libanalysis2.def
```

See below for an overview of the different dependency types.

## Client Project

### Default Project layout

Depending on the build system, the default source and output locations
are the following.

| Build System | Source Location     | Output Location 
|--------------|---------------------|-----------------------------------------
| Maven        | `src/main/LANGUAGE` | `target/generated-sources/main/LANGUAGE`
| Gradle       | `src/main/LANGUAGE` | `build/generated/main/LANGUAGE`

Since Spoofax does not know about the languages available in your
project, you need to configure source folders of other plugins to find
any generated sources. For example, if you use Gradle, and your
`LANGUAGE` generates Java sources in `build/generated/LANGUAGE/java`,
you can use the following `build.gradle`.

```gradle
apply plugin: 'spoofax'
apply plugin: 'java'

# Configure Spoofax compile languages
spoofax {
    buildDependencies {
        language 'GROUP.ID:QUALIFIED.LANGUAGE.ID:VERSION'
        language('GROUP.ID:QUALIFIED.LANGUAGE.ID:VERSION') {
            main {
                srcDirs = [ 'src/main/LANGUAGE' ]
                srcDir 'src/main/LANGUAGE'
                outputDir = 'build/generated/LANGUAGE'
            }
        }
    }
}

# Inform Java plugin about generated sources
sourceSets {
    main {
        java {
            srcDir spoofax.sourceSets.main.LANGUAGE.outputDir+'/java'
        }
    }
}

# Ensure sources are generated before Java compilation
compileJava.dependsOn 'compileSpoofax'
```

## Dependencies

### Classpath

Classpath dependencies are declared as *compile*, or *runtime*
dependencies. The following table shows when dependencies are active
(compile or run time).

| scope    | compile time | run time |
|----------|--------------|----------|
| compile  | yes          | yes      |
| runtime  | no           | yes      |

The following table shows how transitivity works. On the left are the
scopes of direct dependencies. At the top the scopes of transitive
dependencies. In the cells are the scopes a transitive dependency in
that column gets through a direct dependency in the scope of that row.

| direct\transitive | compile             | runtime |
|-------------------|---------------------|---------|
| compile           | compile<sup>1</sup> | runtime |
| runtime           | runtime             | runtime |

<sup>1</sup> Arguably this could be *runtime*. However, since classes
expose dependencies in parameters and super types, we use *compile*
here. This is the same policy used by Maven.

When a classpath dependency is active at

* *compile time*, classes are included in the compilation classpath
  for Java sources
* *run time*, classes are loaded in the Stratego interpreter

### Spoofax Library

When a Spoofax Library dependency is active at

* *compile time*, then

  | type      | description                                | transitive
  |-----------|--------------------------------------------|-----------
  | classpath | Classes are added to compilation classpath | yes
  | ctree     | -                                          | -
  | source    | Files are analyzed and compiled            | no
  | signature | Files are analyzed, but not compiled       | yes

* *run time*, then

  | type      | description                                    | transitive
  |-----------|------------------------------------------------|-----------
  | classpath | Classes are loaded in the Stratego interpreter | yes
  | ctree     | CTree is loaded in the Stratego interpreter    | yes
  | source    | -                                              | -
  | signature | -                                              | -

The scopes are the same as used in Maven, and therefore we reuse them
in the POM for published artifacts.
