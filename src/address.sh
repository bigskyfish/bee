# 重启所有停止的容器
docker ps -a | grep Exited | awk '{print $1}' |xargs docker start
node=$1
resultArray=()
for((i=1;i<=${node};i++))
do
  port=`expr 100 + $i`35
  address1=$(curl -s http://localhost:$port/addresses | jq '.ethereum')
  # address2=$(curl -s http://localhost:$port/chequebook/address)
  msg=${address1}
  echo $msg
  resultArray[${i}]=${msg}
done
echo $result
