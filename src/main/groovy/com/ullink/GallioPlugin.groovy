package com.ullink

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaBasePlugin;

class GallioPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.tasks.withType(Gallio).whenTaskAdded { Gallio task ->
            task.conventionMapping.map "gallioVersion", { '3.4.14.0' }
            task.conventionMapping.map "gallioHome", {
                if (System.getenv()['GALLIO_HOME']) {
                    return System.getenv()['GALLIO_HOME']
                }
                setupGallioRepository(project)
                def version = task.getGallioVersion()
                downloadGallio(project, version)
            }
            task.conventionMapping.map "openCoverVersion", { '4.5.1604' }
            task.conventionMapping.map "openCoverHome", {
                if (System.getenv()['OPENCOVER_HOME']) {
                    return System.getenv()['OPENCOVER_HOME']
                }
                setupOpenCoverRepository(project)
                def version = task.getOpenCoverVersion()
                downloadOpenCover(project, version)
            }
            task.conventionMapping.map "targetAssemblies", {
                if (project.plugins.hasPlugin('msbuild')) {
                    task.dependsOn project.tasks.msbuild
                    project.tasks.msbuild.projects.findAll { !(it.key =~ 'test')}.collect { it.value.getProjectPropertyPath('TargetPath') }
                }
            }
            task.conventionMapping.map "testAssemblies", {
                if (project.plugins.hasPlugin('msbuild')) {
                    task.dependsOn project.tasks.msbuild
                    project.tasks.msbuild.projects.findAll { it.key =~ 'test' }.collect { it.value.getProjectPropertyPath('TargetPath') }
                }
            }
        }
        
        project.apply plugin: 'repositories'

		Task gallio = project.task('gallio', type: Gallio)

		gallio.group = JavaBasePlugin.VERIFICATION_GROUP
		gallio.description = 'Compiles the project jar into a .Net assembly.'
    }

    File downloadGallio(Project project, String version) {
        def dest = new File(project.gradle.gradleUserHomeDir, 'gallio')
        if (!dest.exists()) {
            dest.mkdirs()
        }
        def ret = new File(dest, "gallio-${version}")
        if (!ret.exists()) {
            project.logger.info "Downloading & Unpacking Gallio ${version}"
            def dep = project.dependencies.create(group: 'mb-unit', name: 'GallioBundle', version: version) {
                artifact {
                    name = 'GallioBundle'
                    type = 'zip'
                }
            }
            File zip = project.configurations.detachedConfiguration(dep).singleFile
            if (!zip.isFile()) {
                throw new GradleException("Gallio zip file '${zip}' doesn't exist")
            }
            project.ant.unzip(src: zip, dest: ret)
        }
        ret
    }

    File downloadOpenCover(Project project, String version) {
        def dest = new File(project.gradle.gradleUserHomeDir, 'opencover')
        if (!dest.exists()) {
            dest.mkdirs()
        }
        def ret = new File(dest, "opencover-${version}")
        if (!ret.exists()) {
            project.logger.info "Downloading & Unpacking OpenCover ${version}"
            def dep = project.dependencies.create(group: 'shaunwilde', name: 'opencover', version: version) {
                artifact {
                    name = 'opencover'
                    type = 'zip'
                }
            }
            File zip = project.configurations.detachedConfiguration(dep).singleFile
            if (!zip.isFile()) {
                throw new GradleException("OpenCover zip file '${zip}' doesn't exist")
            }
            project.ant.unzip(src: zip, dest: ret)
        }
        ret
    }

    void setupGallioRepository(Project project) {
        if (!project.repositories.findByName('googlecode-mb-unit')) {
            project.repositories.googlecode('mb-unit')
        }
    }

    void setupOpenCoverRepository(Project project) {
        if (!project.repositories.findByName('bitbucket-shaunwilde')) {
            project.repositories.bitbucket('shaunwilde', '[artifact].[revision].[ext]')
        }
    }
}

