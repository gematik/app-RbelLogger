@Library('gematik-jenkins-shared-library') _
// GitHub
def REPO_URL = 'https://github.com/gematik/app-RbelLogger.git'
def BRANCH = 'master'

pipeline {

    options {
        disableConcurrentBuilds()
        skipDefaultCheckout()
        buildDiscarder logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5')
    }
    agent { label 'Docker-Publish' }

    tools {
        maven 'Default'
    }
    stages {

        stage('Checkout') {
            steps {
                git branch: BRANCH,
                        url: REPO_URL
            }
        }

        stage('Build') {
            steps {
                mavenBuild()
                archiveArtifacts allowEmptyArchive: true, artifacts: '**/target/*.jar,**/target/*.war', fingerprint: true
            }
        }

        stage('Unit Test') {
            steps {
                mavenTest()
            }
        }

        stage('Publish to MavenCentral') {
            steps {
                mavenDeploy("pom.xml", "-Pexternal")
            }
        }
    }
}