#!/bin/bash

# Checking snapshot-job status in kubernetes and sending report in telegram. In our example, we check snapshot of KubeDB Postgresql-server

TG_TOKEN="XXX"
CHAT_ID="XXX" 
CUR_DATE=$(date +%Y%m%d-%H -u)
SNAPSHOT=$(kubectl get snap -n prod --selector="kubedb.com/kind=Postgres,kubedb.com/name=pgsql-server" | grep pgsql-server-"$CUR_DATE" | awk '{ print $1 }')
STATUS=$(kubectl get snap -n prod --selector="kubedb.com/kind=Postgres,kubedb.com/name=pgsql-server" | grep pgsql-server-"$CUR_DATE" | awk '{ print $3 }')
MESSAGE_BADNAME="❌ Snapshot doesn't create! ❌\n Name: pgsql-server-$CUR_DATE\n"
MESSAGE_STATE="❌ Snapshot have bad status! ❌\n Name: pgsql-server-$CUR_DATE\nStatus: $STATUS"

if [ "$SNAPSHOT" != "pgsql-server-${CUR_DATE}" ]; then
    curl --connect-timeout 10 -s --header 'Content-Type: application/json' --request 'POST' --data "{\"chat_id\":\"${CHAT_ID}\",\"text\":\"${MESSAGE_BADNAME}\" \"Status\":\"$STATUS\"}" "https://api.telegram.org/bot${TG_TOKEN}/sendMessage"
else
    while [ "$STATUS" = "Running" ]; do
        sleep 30
        STATUS=$(kubectl get snap -n prod --selector="kubedb.com/kind=Postgres,kubedb.com/name=pgsql-server" | grep pgsql-server-"$CUR_DATE" | awk '{ print $3 }')
    done
    if [ "$STATUS" != "Succeeded" ]; then
        curl --connect-timeout 10 -s --header 'Content-Type: application/json' --request 'POST' --data "{\"chat_id\":\"${CHAT_ID}\",\"text\":\"${MESSAGE_STATE}\"}" "https://api.telegram.org/bot${TG_TOKEN}/sendMessage"
    else
        TIME=$(kubectl get snap -n prod --selector="kubedb.com/kind=Postgres,kubedb.com/name=pgsql-server" | grep pgsql-server-"$CUR_DATE" | awk '{ print $4 }')
        STATUS=$(kubectl get snap -n prod --selector="kubedb.com/kind=Postgres,kubedb.com/name=pgsql-server" | grep pgsql-server-"$CUR_DATE" | awk '{ print $3 }')
        curl --connect-timeout 10 -s --header 'Content-Type: application/json' --request 'POST' --data "{\"chat_id\":\"${CHAT_ID}\",\"text\":\"✅ Snapshot is done! ✅ \nName: pgsql-server-$CUR_DATE \nStatus: $STATUS\nTime for create: $TIME\"}" "https://api.telegram.org/bot${TG_TOKEN}/sendMessage"
    fi
fi