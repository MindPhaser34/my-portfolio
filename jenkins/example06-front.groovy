#!groovy
//Не запускать более одной сборки данной джибы в текущий момент времени
properties([disableConcurrentBuilds()])

pipeline {
  agent any

  //set it in Manage Jenkins -> Configure Global Tools
  tools {nodejs "nodejs"}

  stages {
    stage ('Build project') {
      steps {
        script{
          sh '''
            npm install
            npm run build
            tar -zcf build.tar.gz build
          '''
          archiveArtifacts artifacts: 'build.tar.gz', fingerprint: true
          }
        }
      }
    stage('Deploy on Remote host via SSH') {
      steps {
        unarchive mapping: ['build.tar.gz': "build.tar.gz"]
        sshagent(['SSH-ID-CRED']) {
          script {
            sh "echo \" Start make \"build_bck\" and switch nginx to this folder\""
            first_stage = sh(returnStatus: true, script: """
              ssh -T -o StrictHostKeyChecking=no root@$SERVER_IP 'bash -s' <<'EOF'
              set -x
              STATUS=\$(curl -k -s -LI -o /dev/null -w '%{http_code}\n' https://localhost);
              
              cd $SERVER_DEPLOY_DIR
              git pull origin $GIT_BRANCH
              npm i

              if [ -d $SERVER_DEPLOY_DIR/build_bck/ ]; then rm -rf $SERVER_DEPLOY_DIR/build_bck/; fi
              cp -r $SERVER_DEPLOY_DIR/build/ $SERVER_DEPLOY_DIR/build_bck/

              nginx -t 2>/dev/null > /dev/null
              if [[ \$? == 1 ]]; then
                echo "wrong nginx config after pull and backup"
                exit 1
              else 
                cp /etc/nginx/sites-enabled/$NGINX_CONFIG /tmp/$NGINX_CONFIG 
                sed -i "s|$SERVER_DEPLOY_DIR/build;|$SERVER_DEPLOY_DIR/build_bck;|g" /etc/nginx/sites-enabled/$NGINX_CONFIG
                nginx -s reload
              fi
              
              if [ \$STATUS != 200 ]; then
                cp /tmp/$NGINX_CONFIG /etc/nginx/sites-enabled/$NGINX_CONFIG 
                rm -rf $SERVER_DEPLOY_DIR/build_bck/ && rm -rf /tmp/$NGINX_CONFIG && rm -rf $SERVER_DEPLOY_DIR/build.tar.gz
                exit 1
              else
                echo "Status ok!"
                nginx -s reload
              fi
            """)
            if (first_stage == 1) {
              sh "echo \"After swithed to build_bck deploy was FAILED\""
              sh "exit ${first}"
            } else {
              sh "echo \"All right! Start unpacking build!\""
              second_stage = sh(returnStatus: true, script: """
                scp -o StrictHostKeyChecking=no build.tar.gz root@$SERVER_IP:$SERVER_DEPLOY_DIR
                ssh -T -o StrictHostKeyChecking=no root@$SERVER_IP 'bash -s' <<'EOF'
                set -x
                STATUS=\$(curl -k -s -LI -o /dev/null -w '%{http_code}\n' https://localhost);

                if [ -d $SERVER_DEPLOY_DIR/build/ ]; then rm -rf $SERVER_DEPLOY_DIR/build; fi
                tar -xf $SERVER_DEPLOY_DIR/build.tar.gz -C $SERVER_DEPLOY_DIR/
                cp /tmp/$NGINX_CONFIG /etc/nginx/sites-enabled/$NGINX_CONFIG
                nginx -s reload

                if [ \$STATUS != 200 ]; then 
                  sed -i "s|$SERVER_DEPLOY_DIR/build;|$SERVER_DEPLOY_DIR/build_bck;|g" /etc/nginx/sites-enabled/$NGINX_CONFIG
                  exit 1
                else    
                  rm -rf $SERVER_DEPLOY_DIR/build_bck/ && rm -rf /tmp/$NGINX_CONFIG && rm -rf $SERVER_DEPLOY_DIR/build.tar.gz
                  exit 0
                fi
                """)
              echo "${second}"
              if (second_stage == "1") {
                echo "After swithed to build_bck deploy was FAILED"
                sh "exit ${second}"
              } else {
                echo "Job Complete"
                }
              }
            }
          }
        }
      post{
        //Configure SMTP notifications in "Manage Jenkins" -> "System Configure"
        success {
          echo 'Job Success!'
          script {
          if (env.Send_notification == 'true'){
            mail(body: "Run ${JOB_NAME}-#${BUILD_NUMBER} succeeded. To get more details, visit the build results page: ${BUILD_URL}.",
              from: 'admin@jenkins.com',
              replyTo: '${ReplyTo}',
              cc: '',
              bcc: '',
              subject: "${JOB_NAME} ${BUILD_NUMBER} succeeded",
              to: "${ReplyTo}")       
            }         
          }
        }
        failure {
          echo 'Job FAILURE!!!'
          script{
            if (env.Send_notification == 'true'){
            mail(body: "Run ${JOB_NAME}-#${BUILD_NUMBER} succeeded. To get more details, visit the build results page: ${BUILD_URL}.",
              from: 'admin@jenkins.com',
              replyTo: '',
              cc: '',
              bcc: '',
              subject: "${JOB_NAME} ${BUILD_NUMBER} succeeded",
              to: "${ReplyTo}")       
            }
          }
        }
      }
    }
  }
}