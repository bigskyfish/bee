#!/bin/bash
chmod 777 /mnt/beeCli/restart.sh
chmod 777 /mnt/beeCli/beeRestart.sh
yum install -y epel-release
yum list jq
yum install -y jq
yum install -y wget
cd /mnt/beeCli
echo "执行获取密钥的执行程序"
wget https://github.com/ethersphere/exportSwarmKey/releases/download/v0.1.0/export-swarm-key-linux-386
chmod a+x export-swarm-key-linux-386
echo "执行下载bee程序脚本"
index=1
for line in $(</mnt/beeCli/version.txt)
do
  case $index in
	2|4|6)
	  let index++
	  echo $index"==="$line
	  rpm -i $line
	  ;;
	*)
	  let index++
	  echo $index"==="$line
	  wget $line
	  ;;
	esac
done
while getopts "n:" opt;
do
   case $opt in
    n)
      index=1
      node=$OPTARG
      while(( $index<=$node ))
      do
        cd /mnt/bee$index
        nohup bee start --config /mnt/bee$index/bee-config.yaml >>/mnt/bee$index/beeSetup.log 2>$1 &
        echo "=====启动中====="
        let "index++"
      done
      ;;
    \?)
      echo "invalid arg" ;;
  esac
done
exit 0