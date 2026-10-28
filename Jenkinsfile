pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
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
        success {
            archiveArtifacts artifacts: 'backend/target/*.jar', fingerprint: true
        }
    }
}