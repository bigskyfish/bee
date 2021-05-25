cd /mnt
mkdir bee
cd bee
yum install -y wget
echo "=======下载bee-clef程序========="
wget https://github.com/ethersphere/bee-clef/releases/download/v0.4.12/bee-clef_0.4.12_amd64.rpm
echo "=======下载bee-clef程序成功========="
echo "=======安装bee-clef程序========="
rpm -i bee-clef_0.4.12_amd64.rpm
echo "=======查看bee-clef服务情况========="
systemctl status bee-clef
echo "=======安装bee-clef程序成功========="
echo "=======下载bee程序========="
wget https://github.com/ethersphere/bee/releases/download/v0.6.0/bee_0.6.0_arm64.rpm
echo "=======下载bee程序成功========="
echo "=======安装bee程序========="
rpm -i bee_0.6.0_arm64.rpm
echo "=======安装bee程序成功========="
bee start --config bee-config.yaml

