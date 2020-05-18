#!/bin/bash
# Скрипт выполняет мониторинг папки с дампами БД Postgresql. Когда туда прилетает упакованый дамп, то мы останавливаем контейнер с БД, проливаем дамп,поднимаем контейнер с БД и перезапускаем контейнер с CRM
# предварительно установить выполнить:
# apt-get install inotify-tools -y && apt-get install mutt
# mkdir -p /var/log/db_dumplogs

dirlogs=/var/log/db_dumplogs
DIR='/mnt/db_dumps'

# ставим папку на мониторинг
inotifywait -m -r -e create -e moved_to $DIR |
while read; do
	date=`date +%Y-%m-%d`
	
	# Узнаём время начала работы скрипта
	starttime=`date +%s`
	date &>> $dirlogs/dump_log_$date.txt
	
	# Удаляем логи старше 5-ти дней
	find $dirlogs -type f -name "*.txt" -mtime +5 -print0 | xargs -0 rm -rf &>> $dirlogs/dump_log_$date.txt

	# Останавливаем CRM-приложение
	docker stop crm-app-1 &>> $dirlogs/dump_log_$date.txt

	# Удаляем старую БД, создаём новую чистую БД и даём права на неё
	PGPASSWORD=Gfhjkm psql -U db_name -h localhost -p 5432 pg_user -c "revoke all on database db_user from db_name" -c "drop database db_name" -c "create database db_name" -c "grant all privileges on database db_user to db_name" &>> $dirlogs/dump_log_$date.txt
	
	# Заливаем дамп из архива в БД и пишем процесс в лог
	echo "Dump uploading for "postgres-db-1"" &>> $dirlogs/dump_log_$date.txt
	gunzip -q -c /mnt/db_dump/db_dump-*.sql.tar.gz | PGPASSWORD=Gfhjkm_1 psql -U postgres -h localhost -p 15432 db_name &>> $dirlogs/dump_log_$date.txt
	echo "Uploading is SUCCSESS for "postgres-db-1"" &>> $dirlogs/dump_log_$date.txt
	
	# Отправляем логи на почту
	echo  "crm-app-1 - Dump of base is uploaded!" &>> $dirlogs/dump_log_$date.txt
	
	# Поднимаем приложение заново
	docker start crm-app-1 &>> $dirlogs/dump_log_$date.txt
	
	# Проверяем доступность в течении 5-ти минут (300 сек)
	CURL_RETURN_CODE=0
	CURL_OUTPUT=`curl -k -s -LI -o /dev/null -w '%{http_code}' -m 300 http://localhost:8071/metrics/admin/healthcheck` || CURL_RETURN_CODE=$?
	if [ $CURL_RETURN_CODE -ne 0 ]; then  
		echo "Curl connection failed with return code - $CURL_RETURN_CODE ✗" &>> $dirlogs/dump_log_$date.txt
	else
		echo "Curl connection SUCCESS ✓" &>> $dirlogs/dump_log_$date.txt
		# Check http code for curl operation/response in  CURL_OUTPUT
		if [ $CURL_OUTPUT -ne 200 ]; then
			echo "Curl operation failed due to server return code - $CURL_OUTPUT" &>> $dirlogs/dump_log_$date.txt
		fi
	fi
	
	echo "CURL = 200 OK!" $dirlogs/dump_log_$date.txt
done
	
