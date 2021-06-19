#!/bin/bash
#监控容器的运行状态
nerName=$(docker ps -a | awk '{ print $1}' | tail -n +2)
for i in ${nerName[@]}
do
  name=$i
  #当前时间
  now=`date +"%Y-%m-%d %H:%M:%S"`
  # 查看进程是否存在
  exist=`docker inspect --format '{{.State.Running}}' ${name}`
  if [ "${exist}" != "true" ]
    then {
      echo "${now} 重启docker容器，容器名称：${name}" >> /mnt/beeCli/dockerStop.log
      docker start ${name}
    }
  fi
done
