### Examples of simple piplines for Jenkins

In this repo you can see some examples of jenkins groovy-piplines. You can use it as template to your projects.
- [**example01.groovy**](./example01.groovy) - Simple jenkins pipline with clone repo, build image with dockerfile, push to private docker registry, pull from them and delpoy it via SSH.

- [**example02.groovy**](./example02.groovy) - Same as example01.groovy but with using "Boolean parameter" in Jenkins parameterized job and docker-compose in the end.

- [**example03.groovy**](./example03.groovy) - Same as example01.groovy but with using "Choice Parameter" in Jenkins parameterized job and custom parameter's in "docker run" command depends of "Choice Parameter".

- [**example04.groovy**](./example04.groovy) - Simple pipline with build java-applications by maven, put it into docker-image, push to private docker registry, pull from them and delpoy to connected node by agent. You must already have pom.xml and add nodes to Jenkins (Manage Jenkins - Manage Nodes - New Node).

- [**example05.groovy**](./example05.groovy) - Simple pipline to build java-applications with parameters by maven, test quality of code with SonarQube, put it into docker-image, push to private docker registry, pull from them and delpoy to connected node by agent. You must already have pom.xml and add nodes to Jenkins (Manage Jenkins - Manage Nodes - New Node).

- [**example06-back.groovy**](./example06-back.groovy) and [**example06-front.groovy**](./example06-front.groovy)- CI for Backend (Building Java App and deploying it on Windows host - yes it's strange decision of our partner but it's interesting experience)) and Frontend (Building Node.JS app and deploying in linux) with notifications.

