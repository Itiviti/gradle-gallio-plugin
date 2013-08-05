package com.ullink

import static org.junit.Assert.*
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class GallioPluginTest {
    @Test
    public void gallioPluginAddsGallioTasksToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'gallio'
        assertTrue(project.tasks.gallio instanceof Gallio)
        project.gallio {
            gallioHome = 'abc'
        }
        assertEquals('abc', project.tasks.gallio.gallioHome)
    }
}
