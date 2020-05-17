#!groovy
//Не запускать более одной сборки данной джибы в текущий момент времени
properties([disableConcurrentBuilds()])

pipeline {

  agent {
    label 'master'
  }
  //использовать инструмент из глоальной конфигурациии Jenkins
  tools { maven 'maven' }

  //проверять изменения из SCM https://www.jenkins.io/doc/book/pipeline/syntax/#triggers
  triggers { pollSCM('* * * * *') }

  options {
        // Хранить логи и аретфакты из последних десяти сборок
        buildDiscarder(logRotator(numToKeepStr: '10', artifactNumToKeepStr: '10'))
        // В выводе сборки выводить временные отметки
        timestamps()
    }

  stages {
    stage ('Build project') {
      steps {
        script{
          sh '''
            mvn clean install
          '''  

        }
      }
    }
    stage('Deploy on Remote host via SSH') {
      steps {
        script {
          def remote = [:]
          remote.name = 'BackEnd'
          remote.host = SERVER_ADD
          remote.allowAnyHosts = true
          withCredentials([usernamePassword(credentialsId: 'ID-CRED', passwordVariable: 'password', usernameVariable: 'userName')]) {
            remote.user = userName
            remote.password = password
           //Create folders for backup
            sshCommand remote: remote, command: "powershell.exe \"if (Test-Path C:\\temp_jar) { Write-Host Folder already exist} else {New-Item C:\\temp_jar -ItemType Directory}\"" 
            sshCommand remote: remote, command: "powershell.exe \"if (Test-Path C:\\temp_jar\\back) { Write-Host Folder already exist} else {New-Item C:\\temp_jar\\back -ItemType Directory}\"" 
            sshCommand remote: remote, command: "scp root@192.168.254.12:/var/lib/jenkins/workspace/pf_backend/target/mdmAPI-1.0-SNAPSHOT-site.jar C:\\temp_jar"
            sshCommand remote: remote, command: "scp root@192.168.254.12:/var/lib/jenkins/workspace/pf_backend/target/mdmAPI-1.0-SNAPSHOT.jar C:\\temp_jar"
            //stop Tomcat
            sshCommand remote: remote, command: "powershell.exe \"Start-Job { Stop-Service Tomcat8 } | Receive-Job -Wait\""
            sshCommand remote: remote, command: "powershell.exe Start-Sleep -s 30"
            //make backup
            sshCommand remote: remote, command: "cmd /c \"copy \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps\\ROOT\\WEB-INF\\lib\\mdmAPI-1.0-SNAPSHOT-site.jar\" C:\\temp_jar\\back\\\""
            sshCommand remote: remote, command: "cmd /c \"copy \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps.api\\ROOT\\WEB-INF\\lib\\mdmAPI-1.0-SNAPSHOT.jar\" C:\\temp_jar\\back\\\""
            //copy new jars
            sshCommand remote: remote, command: "cmd /c \"copy C:\\temp_jar\\mdmAPI-1.0-SNAPSHOT-site.jar \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps\\ROOT\\WEB-INF\\lib\\\""
            sshCommand remote: remote, command: "cmd /c \"copy C:\\temp_jar\\mdmAPI-1.0-SNAPSHOT.jar \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps.api\\ROOT\\WEB-INF\\lib\\\""
            // restart Tomcat
            sshCommand remote: remote, command: "powershell.exe \"Start-Job { Start-Service Tomcat8 } | Receive-Job -Wait\""
            // Wait after restart Tomcat
            sshCommand remote: remote, command: "powershell.exe Start-Sleep -s 40"
            // Test connect to site after Restart
            sshCommand remote: remote, command: "cmd /c \"del C:\\temp_jar\\status.txt\""
            sshCommand remote: remote, command: "curl -k -w \"%{http_code}\" \"https://localhost/main?sysname=logon\" -o /Null >> C:\\temp_jar\\status.txt"
            sshCommand remote: remote, command: "powershell.exe \"if ([bool]((Get-Content -Path \"C:\\temp_jar\\status.txt\") -like '200')) { write-host \"Allritght!\" } else { write-host \"After copy jar status is not 200\"; Copy-Item -Path \"C:\\temp_jar\\back\\mdmAPI-1.0-SNAPSHOT-site.jar\" -Destination \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps\\ROOT\\WEB-INF\\lib\\\" -Force; Copy-Item -Path \"C:\\temp_jar\\back\\mdmAPI-1.0-SNAPSHOT.jar\" -Destination \"C:\\Program Files\\Apache Software Foundation\\Tomcat 8.0\\webapps.api\\ROOT\\WEB-INF\\lib\\\" -Force; Start-Job { Restart-Service Tomcat8 } | Receive-Job -Wait}\""
            // Final test connect
            sshCommand remote: remote, command: "cmd /c \"del C:\\temp_jar\\status.txt\""
            sshCommand remote: remote, command: "curl -k -w \"%{http_code}\" \"https://localhost/main?sysname=logon\" -o /Null >> C:\\temp_jar\\status.txt"
            sshCommand remote: remote, command: "powershell.exe \"if ([bool]((Get-Content -Path \"C:\\temp_jar\\status.txt\") -like '200')) { write-host \"Allritght!\" } else { write-host \"After Reсovery Status is not 200\" }\""
          }
        }
      }
      post{
        success {
          echo 'Job Success!'
          script {
          if (env.Send_notification == 'true'){
            mail(body: "Run ${JOB_NAME}-#${BUILD_NUMBER} succeeded. To get more details, visit the build results page: ${BUILD_URL}.",
              from: 'admin@jenkins.com',
              replyTo: '',
              сс: '',
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
            mail(body: "Run ${JOB_NAME}-#${BUILD_NUMBER} failed. To get more details, visit the build results page: ${BUILD_URL}.",
              from: 'admin@jenkins.com',
              replyTo: '',
              сс: '',
              bcc: '',
              subject: "${JOB_NAME} ${BUILD_NUMBER} failed",
              to: "${ReplyTo}")                
            }
          }
        }
      }
    }
  }
}