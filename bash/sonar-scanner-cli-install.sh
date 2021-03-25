#!/bin/bash

inst_dir=/opt/sonar-scanner

if [ -d $inst_dir ]; then
    mkdir -p $inst_dir
fi

apt-get update
apt-get install unzip wget 

mkdir /tmp/sonarqube -p
cd /tmp/sonarqube
wget https://binaries.sonarsource.com/Distribution/sonar-scanner-cli/sonar-scanner-cli-4.3.0.2102-linux.zip
unzip sonar-scanner-cli-4.3.0.2102-linux.zip
mv sonar-scanner-4.3.0.2102-linux $inst_dir
rm -rf /tmp/sonarqube

sed -i 's/#sonar.host.url=http:\/\/localhost:9000/sonar.host.url=https:\/\/sonarqube.url.com/g' /opt/sonar-scanner/conf/sonar-scanner.properties
sed -i 's/#sonar.sourceEncoding=UTF-8/sonar.sourceEncoding=UTF-8/g' $inst_dir/conf/sonar-scanner.properties

#echo 'export PATH="'$inst_dir'/bin:$PATH"' >> /etc/profile.d/sonar-scanner.sh
ln -s $inst_dir/bin/sonar-scanner /usr/local/bin/

source /etc/profile.d/sonar-scanner.sh