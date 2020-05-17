@Library('jenkins-libs') _

pipeline {
    parameters { // parameters for buttons in Jenkins
        string(name: 'REGISTRY_URL', defaultValue: 'https://private.registry.com', description: 'Docker registry for docker images')
        string(name: 'REGISTRY_CREDS', defaultValue: 'registry_creds', description: 'ID of Docker registry credentials in Jenkins')
        string(name: 'IMAGE', defaultValue: null, description: 'Docker registry for docker images')
        string(name: 'IMAGE_TAG', defaultValue: 'latest', description: 'ID of Docker registry credentials in Jenkins')
        string(name: 'GIT_BRANCH', defaultValue: 'master', description: 'Git brach with service')
        string(name: 'JENKINS_NODE_LABEL', defaultValue: "node01", description: 'Destianation node for service')
        string(name: 'BUILD_MACHINE_LABEL', defaultValue: "bm-node01", description: 'Jenkins build-machine node')
        string(name: 'APP_NAME', defaultValue: "service01", description: "App name") 
    }

    stages {
        stage('Create docker image and push it to docker registry ') {
            agent {
                label BUILD_MACHINE_LABEL // Build-machine Node 
            }
            steps {
                //clone service from external repo with main app and completed Dockerfile
                git branch: GIT_BRANCH, credentialsId: creds_id, url: 'git@git.repo.ru:repo1/service01.git'
                
                script {

                    sh "mvn clean package"
                    
                    dockerImage = docker.build(REGISTRY_URL + IMAGE + ":" + IMAGE_TAG, " .") //build docker image
                    docker.withRegistry(REGISTRY_URL, REGISTRY_CREDS) { //push it to private docker registry
                    dockerImage.push()
                    }
                    sleep(time:30,unit:"SECONDS")
                }
            }
        }

        stage('Pull docker container and start') {
            agent {
                label JENKINS_NODE_LABEL
            }
            steps {
                script {
                    docker.withRegistry(REGISTRY_URL, REGISTRY_CREDS) {
                        sh "docker pull " + REGISTRY_URL + IMAGE + ":" + IMAGE_TAG
                    }
                    try {
                        sh "docker rm --force service01"
                        echo "Container service01 removed"
                    } catch (error) {
                        echo "Container service01 not removed, because: " + error
                    }
                    sh "docker run --name service01 -d " + REGISTRY_URL + IMAGE + ":" + IMAGE_TAG
                }
            }
        }
    }
}