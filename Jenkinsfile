pipeline {
    agent any

    tools {
        jdk 'JDK17'
        maven 'Maven3'
    }

    stages {
        stage('Build') {
            steps {
                bat 'mvn -B clean compile'
            }
        }
        stage('Test') {
            steps {
                bat 'mvn -B test'
            }
        }
        stage('Package') {
            steps {
                bat 'mvn -B package -DskipTests'
            }
        }
    }
}