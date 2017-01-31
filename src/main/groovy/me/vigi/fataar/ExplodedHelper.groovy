package me.vigi.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree

/**
 * Created by Vigi on 2017/1/20.
 */
class ExplodedHelper {

    /**
     * iterate over all AndroidManifest.xml to resolve aar dependencies
     */
    static FileTree resolveAllManifests(Project project) {
        def explodedRoot = project.file(project.buildDir.path + '/intermediates' + '/exploded-aar')
        def manifests = project.fileTree(explodedRoot) {
            include '**/AndroidManifest.xml'
            exclude '**/aapt/AndroidManifest.xml'
        }
        return manifests
    }

    static void processIntoJars(Project project, Collection<ResolvedArtifact> artifacts, File folderOut) {
        for (artifact in artifacts) {
            if ('aar'.equals(artifact.type)) {
                def aarRoot = project.file(project.buildDir.path + '/intermediates' + '/exploded-aar')
                def mid = artifact.moduleVersion.id
                def mRoot = project.file(aarRoot.path + '/' + mid.group + '/' + mid.name + '/' + mid.version)
                if (!mRoot.exists()) {
                    println 'vigi-->[warning]' + mRoot.path + ' not found!'
                    continue
                }
                println 'vigi-->copy aar from: ' + mRoot
                def prefix = mid.name + '-' + mid.version
                project.copy {
                    from("$mRoot.path/jars/classes.jar")
                    into folderOut
                    rename { prefix + '.jar' }
                }
                project.copy {
                    from("$mRoot.path/jars") {
                        include '*.jar'
                        exclude 'classes.jar'
                    }
                    from("$mRoot.path/jars/libs") {
                        include '*.jar'
                    }
                    from("$mRoot.path/libs") {
                        include '*.jar'
                    }
                    into folderOut
                    rename { prefix + '-' + it }
                }
            }
            if ('jar'.equals(artifact.type)) {
                if (!artifact.file.exists()) {
                    println 'vigi-->[warning]' + artifact.file.path + ' not found!'
                    continue
                }
                println 'vigi-->copy jar from: ' + artifact.file
                project.copy {
                    from(artifact.file)
                    into folderOut
                }
            }
        }
    }

    static void processIntoClasses(Project project, Collection<ResolvedArtifact> artifacts, File folderOut) {
        def aarRoot = project.file(project.buildDir.path + '/intermediates' + '/exploded-aar')
        for (artifact in artifacts) {
            FileCollection jars = null
            if ('aar'.equals(artifact.type)) {
                def mid = artifact.moduleVersion.id
                def mRoot = project.file(aarRoot.path + '/' + mid.group + '/' + mid.name + '/' + mid.version)
                if (!mRoot.exists()) {
                    println 'vigi-->[warning]' + mRoot.path + ' not found!'
                    continue
                }
                jars = project.fileTree(mRoot) {
                    include 'jars/*.jar'
                    include 'jars/libs/*.jar'
                    include 'libs/*.jar'
                }
            }
            if ('jar'.equals(artifact.type)) {
                if (!artifact.file.exists()) {
                    println 'vigi-->[warning]' + artifact.file.path + ' not found!'
                    continue
                }
                jars = project.files(artifact.file)
            }
            for (jar in jars) {
                println 'vigi-->copy classes from: ' + jar
                project.copy {
                    from project.zipTree(jar)
                    into folderOut
                    include '**/*.class'
                    exclude 'META-INF/'
                }
            }
        }
    }
}