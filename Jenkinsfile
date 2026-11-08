pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
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