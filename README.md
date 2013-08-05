# Gradle Gallio Plugin [![Build Status](https://buildhive.cloudbees.com/job/Ullink/job/gradle-gallio-plugin/badge/icon)](https://buildhive.cloudbees.com/job/gluck/job/gradle-gallio-plugin/)

This plugin allows to use [Gallio](https://code.google.com/p/mb-unit/) to execute .Net unit tests.
Gallio can execute various unit test framework flavors, including NUnit.
Gallio activity has decreased a lot, but the tooling works, and it's the only (as of now) supported format for importing into [Sonar](http://www.sonarsource.org/).

Below task is provided by the plugin:

## gallio

This task will download (if applicable) Gallio distribution, and runs the given unit tests.

Simplest usage:

    buildscript {
        repositories {
            mavenCentral()
        }
    
        dependencies {
            classpath "com.ullink.gradle:gradle-gallio-plugin:1.1"
        }
    }
    
    apply plugin:'gallio'

    repositories {
        mavenCentral()
    }

    gallio {
        testProject = 'my-project-test.csproj'
    }

More options to come...

# License

All these plugins are licensed under the [Creative Commons � CC0 1.0 Universal](http://creativecommons.org/publicdomain/zero/1.0/) license with no warranty (expressed or implied) for any purpose.
