pipeline {
  environment {
    registry = "private.registry.com" // DNS-name or ip-address of private docker registry
    registryCredential = 'registry_creds' // ID of credential for private doecker registry stored in Jenkins
    image = "example03" // image name 
    image_tag = "latest" // tag of image
    branch_name = "master" // branch of github
    git_url = "https://github.com/reponame/example03.git" // no comments )
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
          dockerImage = docker.build(registry + image + ":" + image_tag, "--no-cache .")
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
          if (ENV_ID == "prod") { // if "Choice Parameter" in Jenkins parameterized job, called ENV_ID, was chosen before build ("Build with parameters" button), then use this parameters
            DB_USER = 'user1'
            DB_PASSWORD = 'Qwerty1'
            DB_NAME = 'prod-db'
            DB_HOST = '172.17.0.1'
            DB_PORT = '5432'
            docker_network = 'prod_default'
          } 
          if (ENV_ID == "stage") { // else
            DB_USER = 'user2'
            DB_PASSWORD = 'Qwerty2'
            DB_NAME = 'stage-db'
            DB_HOST = '172.17.0.1'
            DB_PORT = '5432'
            docker_network = 'stage_default'
          }
          // use SSH credentials with ID "Jenkins"
          withCredentials([sshUserPrivateKey(credentialsId: 'Jenkins', keyFileVariable: 'sshKeyfile', usernameVariable: 'sshUsername')]) {
            remote.user = sshUsername
            remote.identityFile = sshKeyfile
            // use Docker Private Registry credentials with ID "registryCredential" in Jenkins
            withCredentials([usernamePassword( credentialsId: registryCredential, usernameVariable: 'registryUsername', passwordVariable: 'registryPassword')]) {
                    sshCommand remote: remote,
                        command: "set -x\n" +
                        "docker login -u $registryUsername -p $registryPassword https://$registry\n" +
                        "docker pull " + registry + image + ":" + image_tag + "\n" +
                        // create container with parameters
                        "docker run --rm -e DB_USER=$DB_USER -e DB_NAME=$DB_NAME -e DB_PASSWORD=$DB_PASSWORD -e DB_HOST=$DB_HOST -e DB_PORT=$DB_PORT -e IS_DEV=true --network $docker_network " + registry + image + ":" + image_tag
            }
          }
        }
      }
    }
  }
}
