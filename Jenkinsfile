pipeline {
    agent any

    environment {
        DOCKER_HUB_USER = 'devarshtpatel'  
        IMAGE_NAME      = 'eduscope'
        IMAGE_TAG    = "${BUILD_NUMBER}"
        FULL_IMAGE   = "${DOCKER_HUB_USER}/${IMAGE_NAME}:${IMAGE_TAG}"
        LATEST_IMAGE = "${DOCKER_HUB_USER}/${IMAGE_NAME}:latest"
    }

    triggers {
        pollSCM('H/1 * * * *') 
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Image') {
            steps {
                sh "docker build -t ${FULL_IMAGE} -t ${LATEST_IMAGE} ."
            }
        }

        stage('Push to Docker Hub') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USER',
                    passwordVariable: 'DOCKER_PASS'
                )]) {
                    sh """
                        echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
                        docker push ${FULL_IMAGE}
                        docker push ${LATEST_IMAGE}
                        docker logout
                    """
                }
            }
        }

        stage('Cleanup') {
            steps {
                sh """
                    docker rmi ${FULL_IMAGE} || true
                    docker rmi ${LATEST_IMAGE} || true
                """
            }
        }
    }

    post {
        success {
            echo "✅ Build #${BUILD_NUMBER} pushed as ${FULL_IMAGE}"
        }
        failure {
            echo "❌ Build #${BUILD_NUMBER} failed. Check the logs above."
        }
    }
}
