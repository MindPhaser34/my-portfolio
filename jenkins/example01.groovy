pipeline {
  environment {
    registry = "private.registry.com" // DNS-name or ip-address of private docker registry
    registryCredential = 'registry_creds' // ID of credential for private doecker registry stored in Jenkins
    image = "example01" // image name 
    image_tag = "latest" // tag of image
    branch_name = "master" // branch of github
    git_url = "https://github.com/reponame/example01.git" // no comments )
  }

  agent any

  stages {
    stage('Clone Git repo') {
      steps {
        git branch: branch_name, url: git_url
      }
    }
    stage('Build image from Docker file') {
      steps {
        script {
          (dockerImage = docker.build(registry + image + ":" + image_tag, "--no-cache -f ./Dockerfile ."))
        }
      }
    }
    stage('Push image to Registry') {
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
          remote.name = 'hostname'
          remote.host = '255.255.255.1'
          remote.allowAnyHosts = true
          withCredentials([sshUserPrivateKey(credentialsId: 'Jenkins', keyFileVariable: 'sshKeyfile', usernameVariable: 'sshUsername')]) {
            remote.user = sshUsername
            remote.identityFile = sshKeyfile
            withCredentials([usernamePassword(credentialsId: registryCredential, usernameVariable: 'registryUsername', passwordVariable: 'registryPassword')]) {
              sshCommand remote: remote,
                command: "set -x\n" +
                  "docker login -u $registryUsername -p $registryPassword https://$registry\n" +
                  "docker pull " + registry + image + ":" + image_tag + "\n" +
                  "docker run --rm " + registry + image + ":" + image_tag
            }
          }
        }
      }
    }
  }
}
