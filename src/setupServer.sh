#!/bin/bash
passwd=Mnb1234567.
while getopts "p:" opt; do
  case $opt in
    p)
        passwd=$OPTARG
        chmod 777 /mnt/bee/bee-config.yaml
        sed -i "s*_bee_config_password_*"$passwd"*g" /mnt/bee/bee-config.yaml
        chmod 777 /mnt/bee/transferPrivateKey.sh
        sed -i "s*_bee_config_password_*"$passwd"*g" /mnt/bee/transferPrivateKey.sh
        echo "替换字符串值成功" ;;
    \?)
        echo "invalid arg" ;;
  esac
done
chmod 777 /mnt/bee/restart.sh
chmod 777 /mnt/bee/beeRestart.sh
yum install -y epel-release
yum list jq
yum install -y jq
yum install -y wget
cd /mnt/bee
echo "执行获取密钥的执行程序"
wget https://github.com/ethersphere/exportSwarmKey/releases/download/v0.1.0/export-swarm-key-linux-386
chmod a+x export-swarm-key-linux-386
echo "执行下载bee程序脚本"
index=1
for line in $(</mnt/bee/version.txt)
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
echo "unalias cp" >> ~/.bash_profile
source ~/.bash_profile
cp -r -f /mnt/bee/bee-config.yaml /etc/bee/bee.yaml
# bee start --config /mnt/bee/bee-config.yaml
bee start --config /mnt/bee/bee-config.yaml
echo "=====启动bee中====="
./export-swarm-key-linux-386 /root/.bee/keys/ $passwd > /mnt/bee/privateKey.key
echo "=====密钥提取成功====="
exit 0