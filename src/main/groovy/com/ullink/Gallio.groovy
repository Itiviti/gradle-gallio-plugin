package com.ullink

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils
import org.gradle.api.GradleException;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.ConventionTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class Gallio extends ConventionTask {
    def gallioHome
    def gallioVersion
    def openCoverHome
    def openCoverVersion
    boolean ignoreFailures = false
    List testAssemblies
    List targetAssemblies
    boolean useClrV2
    boolean force32bits
    def coverageTool
    def verbosity

    Gallio() {
        inputs.files {
            def ret = [ getTestAssemblies() ]
            if (getTargetAssemblies() != null) {
                ret += getTargetAssemblies()
            }
            ret
        }
        outputs.files {
            def ret = [ getTestReportFile() ]
            if (coverageTool != null) {
                ret += getCoverageReportFile()
            }
            ret
        }
    }
    
    File gallioBinFile(String file) {
        new File(project.file(getGallioHome()), "bin/${file}")
    }

    def getOpenCoverConsole(){
        assert getOpenCoverHome(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        File openCoverExec = new File(project.file(getOpenCoverHome()), "OpenCover.Console.exe")
        assert openCoverExec.isFile(), "You must install OpenCover and set opencover.home property or OPENCOVER_HOME env variable"
        openCoverExec
    }

    def getGallioEcho(){
        assert getGallioHome(), "You must install Gallio and set gallio.home property or GALLIO_HOME env variable"
        File gallioOrigExec = gallioBinFile('Gallio.Echo.exe')
        assert gallioOrigExec.isFile(), "You must install Gallio and set gallio.home property or GALLIO_HOME env variable"
        File gallioExec = gallioOrigExec
        if (force32bits) {
            gallioExec = gallioBinFile('Gallio.Echo.x86.exe')
            if (!gallioExec.isFile()) {
                FileUtils.copyFile(gallioOrigExec, gallioExec)
                // Using CorFlags would be mucho better, but hey, it comes at a price (SDK installed as repackaging is forbidden)
                // CorFlags MyAssembly.exe /32BIT+
                RandomAccessFile raf = new RandomAccessFile(gallioExec, "rw")
                try {
                    raf.seek(0x218)
                    raf.writeByte(0x0B)
                } finally {
                    raf.close()
                }
                FileUtils.copyFile(gallioBinFile('Gallio.Echo.exe.config'), gallioBinFile('Gallio.Echo.x86.exe.config'))
            }
        }
        if (useClrV2) {
            gallioExec = gallioBinFile(force32bits ? 'Gallio.Echo.x86.v2.exe' : 'Gallio.Echo.v2.exe')
            if (!gallioExec.isFile()) {
                FileUtils.copyFile(gallioBinFile(force32bits ? 'Gallio.Echo.x86.exe' : 'Gallio.Echo.exe'), gallioExec)
                File destCfg = gallioBinFile(force32bits ? 'Gallio.Echo.x86.v2.exe.config' : 'Gallio.Echo.v2.exe.config')
                FileUtils.copyFile(gallioBinFile('Gallio.Echo.exe.config'), destCfg)
                destCfg.withWriter('UTF-8', { writer ->
                    gallioBinFile('Gallio.Echo.exe.config').eachLine('UTF-8', { line ->
                        if (!line.contains('v4.0.30319')) {
                            writer.write(line)
                        }
                    })
                })
            }
        }
        gallioExec
    }

    def getOutputFolder() {
        new File(project.buildDir, 'gallio')
    }

    def getReportsFolder() {
        new File(outputFolder, 'reports')
    }

    def getTestReportFile() {
        new File(reportsFolder, 'gallio.xml')
    }

    def getCoverageReportFile() {
        new File(reportsFolder, 'opencover.xml')
    }

    def getTargetFolder() {
        new File(outputFolder, 'work')
    }

    @TaskAction
    def build() {
        targetFolder.mkdirs()
        reportsFolder.mkdirs()
        def commandLineArgs = [ ]

        commandLineArgs += ['/nl','/np','/rt:Xml', '/rnf:gallio']
        commandLineArgs += "/rd:${reportsFolder}"
        commandLineArgs += "/wd:${targetFolder}"
        String verb = verbosity
        if (!verb) {
            if (logger.debugEnabled) {
                verb = 'Debug'
            } else if (logger.infoEnabled) {
                verb = 'Normal'
            } else { // 'quiet'
                verb = 'Quiet'
            }
        }
        if (verb) {
            commandLineArgs += '/v:'+verb
        }

        getTestAssemblies().each {
            commandLineArgs += project.file(it)
        }

        if (coverageTool == 'OpenCover') {
            def gallioCommandLineArgs = commandLineArgs
            commandLineArgs = [ openCoverConsole, '-register:user', '-mergebyhash']
            commandLineArgs += ["-target:${gallioEcho}", "-targetargs:${gallioCommandLineArgs.join(' ')}", "-targetdir:${outputFolder}"]
            getTargetAssemblies().each {
                commandLineArgs += "+[${FilenameUtils.getBaseName(project.file(it).name)}]"
            }
            commandLineArgs += "-output:${coverageReportFile}"
        } else {
            commandLineArgs = [ gallioEcho ] + commandLineArgs
        }

        def mbr = project.exec {
            commandLine = commandLineArgs
            ignoreExitValue = ignoreFailures
        }

        switch (mbr.exitValue) {
            case 0:
            case 16:
                break;
            case 1: // gallio test failed
            case -3: // opencover test failed
                // ok & failure
                if (ignoreFailures) break;
            default:
                // nok
                throw new GradleException("Gallio execution failed (ret=${mbr.exitValue})");
        }
    }
}
