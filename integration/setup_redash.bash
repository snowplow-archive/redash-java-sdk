#!/bin/bash

set -e

local_dir="$(dirname "$0")"

echo "==============="
echo "STARTING REDASH"
echo "---------------"

sudo docker-compose -f ${local_dir}/docker-compose.production.yml run --rm server create_db
sleep 5
sudo docker-compose -f ${local_dir}/docker-compose.production.yml up -d
sleep 5
sudo docker ps

echo "================"
echo "SETUP ADMIN USER"
echo "----------------"

curl -XPOST http://localhost:80/setup \
    -F "name=Admin" \
    -F "email=admin@snowplowanalytics.com" \
    -F "password=password" \
    -F "org_name=Snowplow"

echo "=========================="
echo "EXTRACT ADMIN USER API KEY"
echo "--------------------------"

curl -s -c ${local_dir}/cookies.txt -d "email=admin@snowplowanalytics.com&password=password" http://localhost:80/login

admin_api_key=$(curl -b ${local_dir}/cookies.txt \
    -c ${local_dir}/cookies.txt \
    -d "email=admin@snowplowanalytics.com&password=password" \
    -H "Content-Type: application/json" \
    -XGET http://localhost:80/api/users/1 | \
    python -c "import sys, json; print json.load(sys.stdin)['api_key']")

echo "admin_api_key=${admin_api_key}" > ./src/test/resources/redash_dynamic.properties

rm ${local_dir}/cookies.txt

echo "=================="
echo "SETUP DEFAULT USER"
echo "------------------"

set +e
sudo docker exec -i integration_server_1 /app/manage.py users create default@snowplowanalytics.com Default --admin --password=password
set -e
