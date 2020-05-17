pipeline {
  environment {
    registry = "private.registry.com" // DNS-name or ip-address of private docker registry
    registryCredential = 'registry_creds' // ID of credential for private doecker registry stored in Jenkins
    image = "example02" // image name 
    image_tag = "latest" // tag of image
    branch_name = "master" // branch of github
    git_url = "https://github.com/reponame/example02.git" // no comments )
  }

  agent any

    stages {
        stage('Clone Github repo') {
            steps {
                git branch: branch_name, url: git_url
            }
        }
        stage('Build image') {
            steps {
            script {
                (dockerImage = docker.build(registry + image + ":" + image_tag, "--no-cache ."))
            }
            }
        }
        stage('Push image to Private Docker Registry') {
            steps {
            script {
                docker.withRegistry("https://" + registry, registryCredential) {
                dockerImage.push()
                }
            }
            }
        }
        stage('Deploy via remote SSH') {
            steps {
            script {
                def remote = [:]
                remote.name = 'hostname' // hostname of destination server
                remote.host = '255.255.255.1' // ip-address of destination server
                remote.allowAnyHosts = true
                if (deploy_prod == 'true') { // if "Boolean parameter" in Jenkins parameterized job, called deploy_prod, was chacked before build ("Build with parameters" button), then go to /srv/prod on destination server
                    wdir = '/srv/prod' 
                    // use SSH credentials with ID "Jenkins"
                    withCredentials([sshUserPrivateKey(credentialsId: 'Jenkins', keyFileVariable: 'sshKeyfile', usernameVariable: 'sshUsername')]) {
                    remote.user = sshUsername
                    remote.identityFile = sshKeyfile
                    // use Docker Private Registry credentials with ID "registryCredential" in Jenkins
                    withCredentials([usernamePassword(credentialsId: registryCredential, usernameVariable: 'registryUsername', passwordVariable: 'registryPassword')]) {
                    sshCommand remote: remote,
                        command: "set -x\n" +
                        "cd $wdir\n" + // got to folder with docker-compose.yml
                        "docker login -u $registryUsername -p $registryPassword https://$registry\n" + // login to Docker Private Registry
                        "docker-compose pull extender\n" + // pull & up service
                        "docker-compose up -d extender" 
                        }
                    }
                }
                if (deploy_stage == "true") { 
                    wdir = '/srv/stage'
                    withCredentials([sshUserPrivateKey(credentialsId: 'Jenkins', keyFileVariable: 'sshKeyfile', usernameVariable: 'sshUsername')]) {
                    remote.user = sshUsername
                    remote.identityFile = sshKeyfile
                    withCredentials([usernamePassword(credentialsId: registryCredential, usernameVariable: 'registryUsername', passwordVariable: 'registryPassword')]) {
                    sshCommand remote: remote,
                        command: "set -x\n" +
                        "cd $wdir\n" +
                        "docker login -u $registryUsername -p $registryPassword https://$registry\n" +
                        "docker-compose pull extender\n" +
                        "docker-compose up -d extender"
                            }
                        }
                    }
                }
            }
        }
    }
}
