#!/bin/bash
while getopts "p:" opt; do
  case $opt in
    p)
        passwd=$OPTARG
        chmod 777 /mnt/bee/bee-config.yaml
        sed -i "s/_bee_config_password_/"+$passwd+"/g" /mnt/bee/bee-config.yaml
        chmod 777 /mnt/bee/transferPrivateKey.sh
        sed -i "s/_bee_config_password_/"+$passwd+"/g" /mnt/bee/transferPrivateKey.sh
        echo "替换字符串值成功" ;;
    \?)
        echo "invalid arg" ;;
  esac
done
yum install -y wget
cd /mnt/bee
while read -r beeclfversion beeclfrpm beeversion beerpm
do
  echo $beeclfversion+"==="+$beeclfrpm+"==="+$beeversion+"==="+$beerpm
  wget -c -t 5 -T 30 $beeclfversiondir=/mnt/bee
  rpm -i $beeclfrpm
  wget -c -t 5 -T 30 $beeversion
  rpm -i $beerpm
done < /mnt/bee/version.txt
bee start --config /mnt/bee/bee-config.yaml
echo "=====启动bee中====="
sh /mnt/bee/transferPrivateKey.sh
echo "======获取密钥======="
goon=1
while [ $goon -eq 1 ]
do
  systemctl status bee.service &>/dev/null
  if [ $? -eq 0 ]
  then
    ${goon}=0
    times=$(date "+%Y-%m-%d %H:%M:%S")
    echo "=====启动成功：时间为 " + times >> /mnt/bee/beeSetup.log
  else
    echo "====bee服务未质押，再次调用启动==="
    bee start --config /mnt/bee/bee-config.yaml
    echo "=====启动bee中====="
  fi
done
echo "====bee节点已启动===="
