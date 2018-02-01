#!/bin/bash

set -e

local_dir="$(dirname "$0")"

echo "==============="
echo "REMOVING REDASH"
echo "---------------"

sudo docker-compose -f ${local_dir}/docker-compose.production.yml rm -f -s
