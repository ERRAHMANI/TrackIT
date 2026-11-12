pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    environment {
        // Sous Windows, Java écrit la console en cp1252 : les accents des journaux s'affichaient « bloqu�e ».
        // On force l'UTF-8 pour Maven et pour la JVM des tests (variable lue par toute JVM démarrée).
        JAVA_TOOL_OPTIONS = '-Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8'
    }

    stages {
        stage('Frontend') {
            steps {
                bat 'node --version'
                bat 'node scripts/verifier-frontend.mjs frontend'
            }
        }
        stage('Build') {
            steps {
                dir('backend') {
                    bat 'mvn -B clean compile'
                }
            }
        }
        stage('Test') {
            steps {
                dir('backend') {
                    bat 'mvn -B test'
                }
            }
        }
        stage('Package') {
            steps {
                dir('backend') {
                    bat 'mvn -B package -DskipTests'
                }
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
        }
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
        }
    }
}