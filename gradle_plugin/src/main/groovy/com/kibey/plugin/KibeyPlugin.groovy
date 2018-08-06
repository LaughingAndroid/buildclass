package com.kibey.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.android.build.gradle.AppExtension

class KibeyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        Logs.LOG_FILE = new File(project.rootDir.getPath() + "/.test/log/" + System.currentTimeMillis())
        if (!Logs.LOG_FILE.getParentFile().exists()) {
            Logs.LOG_FILE.getParentFile().mkdirs()
        }

        Logs.d("========================")
        Logs.d("KibeyPlugin " + project.name)
        Logs.d("========================")

        def android = project.extensions.getByType(AppExtension)
        def myTransform = new PluginTransform(project)
        android.registerTransform(myTransform)

        project.task('testPlugin') {
            doLast {
                Logs.d('+++++++++++++++++++++transformPath task')
            }
        }
    }
}

