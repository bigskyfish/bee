#!/bin/bash
while getopts "p:" opt; do
  case $opt in
    p)
        passwd=$OPTARG
        sed -i '' 's/_bee_config_password_/'+$passwd+'/g' /mnt/bee/bee-config.yaml
        sed -i '' 's/_bee_config_password_/'+$passwd+'/g' /mnt/bee/transferPrivateKey.sh ;;
    \?)
        echo "invalid arg" ;;
  esac
done
yum install -y wget
cd /mnt/bee
wget -c -t 5 -T 30 https://github.com/ethersphere/bee-clef/releases/download/v0.4.12/bee-clef_0.4.12_amd64.rpm
rpm -i bee-clef_0.4.12_amd64.rpm
# wget https://github.com/ethersphere/bee/releases/download/v0.6.0/bee_0.6.0_arm64.rpm
wget -c -t 5 -T 30 https://github.com/ethersphere/bee/releases/download/v0.6.1/bee_0.6.1_386.rpm
rpm -i bee_0.6.1_386.rpm
goon=1
while (goon -eq 1)
do
  bee start --config /mnt/bee/bee-config.yaml
  systemctl status bee.service &>/dev/null
  if [ $? -eq 0 ]
  then
    sh /mnt/bee/transferPrivateKey.sh
    goon=0
  else
    echo "====bee服务未质押，再次调用启动==="
    cat /dev/null /mnt/bee/beeSetup.log
  fi
done
echo "====bee节点已启动===="
