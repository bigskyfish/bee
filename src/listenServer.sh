#!/bin/bash
hostUri=$1
localIp=$2
nodeNum=$3
serverStatus=$(curl -I -m 3 -o /dev/null -s -w %{http_code} www.baidu.com)
echo $serverStatus


