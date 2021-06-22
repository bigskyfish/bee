host=$1
local=$2
nodeNum=$3
for i in `seq 1 ${nodeNum}`
do
  port1=`expr 100 + $i`33
  port2=`expr 100 + $i`35
  now=`date +"%Y-%m-%d %H:%M:%S"`
  if [[ -z $(curl -s http://localhost:${port1}) ]];
  then
    # 服务下线
    echo "${now} : 节点 bee${i} 处于下线状态。" >> /mnt/beeCli/beeStop.log
    beeStop="${beeStop}${i}"
  else
    cheque=$(curl -s http://localhost:${port2}/chequebook/cheque | jq '.peers | length')
    if [[ -n ${cheque} ]];
    then
      success="${success}${i},${cheque}"
    fi
  fi
  # shellcheck disable=SC2053
  if [[ ${i} == ${nodeNum} ]];
  then
    echo "${beeStop}===${success}"
    curl http://${host}/v2/api/update/bee/status -X POST -d "ip=${local}&stop=${beeStop}&running=${success}"
  else
    beeStop="${beeStop},"
    success="${success};"
  fi
done