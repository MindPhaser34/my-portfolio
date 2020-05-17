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
        string(name: 'JDBC_HOST', defaultValue: "jdbc:mysql://localhost:3306/service01", description: "address of db")
        string(name: 'JDBC_USERNAME', defaultValue: "admin", description: "db username")
        string(name: 'JDBC_PASSWORD', defaultValue: "admin123", description: "db password")
        string(name: 'APPLICATION_HOST', defaultValue: "http://255.255.255.1:8181", description: "URL of the app")
        string(name: 'ROLE_ADMIN', defaultValue: "Admin", description: "Name of role with admin privileges")
        string(name: 'ROLE_USER', defaultValue: "Admin_group", description: "Default user privileges")
    }

    stages {
        stage('Define variables') {
            agent {
                label JENKINS_NODE_LABEL
            }
            steps {
                script {
                    echo "All envs: ${params}"

                    //GET THE IP OF NODE WITH APPLICATION
                    appNodeIP = env.JENKINS_NODE_IP
                    if (!appNodeIP?.trim()) {
                        //CHECK IF IP NOT EMPTY
                        echo "Error: Node '${JENKINS_NODE_LABEL}' has empty JENKINS_NODE_IP var."
                        sh "exit 1"
                    } else {
                        echo "Node: [${JENKINS_NODE_LABEL}] IP: ${appNodeIP}"
                    }

                    applicationParams = [
                          'application.host'               : APPLICATION_HOST,
                          'jdbc.url'                       : JDBC_HOST,
                          'jdbc.username'                  : JDBC_USERNAME,
                          'jdbc.password'                  : JDBC_PASSWORD,
                          'role.admin'                     : ROLE_ADMIN,
                          'role.user'                      : ROLE_USER
                    ]                
                }
            }
        }

        stage('Create docker image and push it to docker registry ') {
            agent {
                label BUILD_MACHINE_LABEL // Build-machine Node 
            }
            steps {
                //clone service from external repo
                git branch: GIT_BRANCH, credentialsId: creds_id, url: 'git@git.repo.ru:repo1/service01.git'
                
                script {
                    //add custom parameters to application
                    String inputApplicationPropertiesPath = '/tmp/service/app.properties'
                    String outputApplicationPropertiesPath = './src/main/resources/app.properties'
                    log.info("Save config into [${outputApplicationPropertiesPath}]")

                    writeFile file: outputApplicationPropertiesPath, text: reoUtils.replaceParamsInResourceFile(inputApplicationPropertiesPath, applicationParams)
                    // replace "token" with generated token of SonarQube
                    sh "mvn clean package sonar:sonar -Dsonar.host.url=http://localhost:9000 -Dsonar.login=token -Dsonar.projectName=service01"
                    
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