def executeCommand(command) {
    def output = sh(script: command, returnStdout: true).trim()
    return output != null ? output.trim() : null
}

pipeline {
    agent any
    
    environment {
        DOCKER_HOST = 'tcp://dind:2375'
        IMAGE_REGISTRY = 'localhost:5000'
        IMAGE_NAME = 'nanobot-api'
        IMAGE_TAG = 'latest'
        KUBE_NAMESPACE = 'default'
        KUBE_DEPLOYMENT_NAME = 'nanobot-api'
        KUBE_YAML_FILE = 'nanobot-api-deployment.yaml'
    }

    stages {
        stage('Test Docker') {
            steps {
                sh 'docker --version'
            }
        }
        
        stage('Checkout') {
            steps {
                script {
                    checkout([$class: 'GitSCM', 
                        branches: [[name: '*/main']],
                        userRemoteConfigs: [[
                            url: 'https://github.com/piscatus/nanobot-api.git',
                            credentialsId: '5d65a488-3ab7-458c-bd21-f60eda4291f6'
                        ]]
                    ])
                }
            }
        }

        stage('Build Image') {
            steps {
                script {
                    // Build Docker image
                    def buildCommand = "docker build -t $IMAGE_NAME:$IMAGE_TAG ."
                    executeCommand(buildCommand)
                }
            }
        }

        stage('Tag Image') {
            steps {
                script {
                    // Tag Docker image for local storage
                    def tagCommand = "docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                    executeCommand(tagCommand)
                }
            }
        }

        stage('Push Image') {
            steps {
                script {
                    // Push the Docker image to the local registry
                    def pushCommand = "docker push ${REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}"
                    executeCommand(pushCommand)
                }
            }
        }

        /**
        stage('Print Kubernetes Info') {
            steps {
                script {
                    executeCommand('kubectl cluster-info')
                }
            }
        }
        **/

        /**
        stage('Check Kubernetes Cluster') {
            steps {
                script {
                    // Check if the Kubernetes cluster is accessible
                    def clusterExistsCommand = "kubectl get nodes"
                    def clusterExistsOutput = executeCommand(clusterExistsCommand)

                    if (clusterExistsOutput != null && clusterExistsOutput.contains('Ready')) {
                        echo "Kubernetes cluster is accessible."
                    } else {
                        echo "Kubernetes cluster is not accessible. Aborting."
                        error "Kubernetes cluster is not accessible."
                    }
                }
            }
        }
        **/

        /**
        stage('Deploy to Kubernetes') {
            steps {
                script {
                    // Apply Kubernetes deployment
                    def deployCommand = "kubectl apply -f $KUBE_YAML_FILE -n $KUBE_NAMESPACE"
                    executeCommand(deployCommand)
                }
            }
        }
        **/

    }

    post {
        success {
            echo 'Pipeline succeeded! Your application is deployed.'
        }
        failure {
            echo 'Pipeline failed. Check the logs for more details.'
        }
    }
}
