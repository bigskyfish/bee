# 重启所有停止的容器
docker ps -a | grep Exited | awk '{print $1}' |xargs docker start
host=$1
local=$2
node=$3
for((i=1;i<=${node};i++))
do
  docker cp bee${i}:/home/bee/.bee /mnt/bee${i}/data/
  port=`expr 100 + $i`35
  address=$(curl -s http://localhost:$port/addresses | jq '.ethereum')
  # address2=$(curl -s http://localhost:$port/chequebook/address)
  if [[ ! -n ${address} ]]
  then
    address=$(cat /mnt/bee${i}/data/keys/swarm.key | jq ".address")
    address=0x${address}
  fi
  echo ${address}
  sleep 1
  if [ $i == ${node} ]
  then
    data="${data}${i},${address}"
    # 向服务端发送地址信息
    curl http://${host}/v2/api/address -X POST -d "ip=${local}&address=${data}"
  else
    data="${data}${i},${address};"
  fi
done