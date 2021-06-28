host=$1
local=$2
nodeNum=$3

CPU_us=$(vmstat | awk '{print $13}' | sed -n '$p')
CPU_sy=$(vmstat | awk '{print $14}' | sed -n '$p')
CPU_id=$(vmstat | awk '{print $15}' | sed -n '$p')
CPU_wa=$(vmstat | awk '{print $16}' | sed -n '$p')
CPU_st=$(vmstat | awk '{print $17}' | sed -n '$p')

CPU1=`cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}'`
sleep 5
CPU2=`cat /proc/stat | grep 'cpu ' | awk '{print $2" "$3" "$4" "$5" "$6" "$7" "$8}'`
IDLE1=`echo $CPU1 | awk '{print $4}'`
IDLE2=`echo $CPU2 | awk '{print $4}'`
CPU1_TOTAL=`echo $CPU1 | awk '{print $1+$2+$3+$4+$5+$6+$7}'`
CPU2_TOTAL=`echo $CPU2 | awk '{print $1+$2+$3+$4+$5+$6+$7}'`
IDLE=`echo "$IDLE2-$IDLE1" | bc`
CPU_TOTAL=`echo "$CPU2_TOTAL-$CPU1_TOTAL" | bc`
cpuRate=`echo "scale=4;($CPU_TOTAL-$IDLE)/$CPU_TOTAL*100" | bc | awk '{printf "%.2f",$1}'`

# 内存数据
total=$(free -m | sed -n '2p' | awk '{print $2}')
used=$(free -m | sed -n '2p' | awk '{print $3}')
free=$(free -m | sed -n '2p' | awk '{print $4}')
shared=$(free -m | sed -n '2p' | awk '{print $5}')
buff=$(free -m | sed -n '2p' | awk '{print $6}')
cached=$(free -m | sed -n '2p' | awk '{print $7}')
memoryRate=`echo "scale=2;$used/$total" | bc | awk -F. '{print $2}'`

# 磁盘
diskRate=0
DEV=`df -hP | grep '^/dev/*' | cut -d' ' -f1 | sort`
for I in $DEV
do
dev=`df -Ph | grep $I | awk '{print $1}'`
size=`df -Ph | grep $I | awk '{print $2}'`
used=`df -Ph | grep $I | awk '{print $3}'`
free=`df -Ph | grep $I | awk '{print $4}'`
rate=`df -Ph | grep $I | awk '{print $5}'`
mount=`df -Ph | grep $I | awk '{print $6}'`
diskRate=$rate
done
echo $diskRate

# 带宽
bandWidth=0

curl http://${host}/v2/api/update/server/status -X POST -d "ip=${local}&cpu=${cpuRate}&memory=${memoryRate}&disk=${diskRate}&bandWidth=${bandWidth}"

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