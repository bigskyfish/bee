yum install -y wget
# 1. 下载docker-ce的repo
curl https://download.docker.com/linux/centos/docker-ce.repo -o /etc/yum.repos.d/docker-ce.repo
# 2. 安装依赖
yum install -y https://download.docker.com/linux/fedora/30/x86_64/stable/Packages/containerd.io-1.2.6-3.3.fc30.x86_64.rpm
# 3. 安装docker-ce
yum -y  install docker-ce  docker-ce-cli --nobest
# 4.启动 docker
systemctl start docker
# 5.设置开机自动启动
systemctl enable --now docker
cd /mnt/
mkdir beeCli
cd beeCli/
yum install -y epel-release
yum list jq
yum install -y jq
echo "执行获取密钥的执行程序"
wget https://github.com/ethersphere/exportSwarmKey/releases/download/v0.1.0/export-swarm-key-linux-386 -o /mnt/beeCli/export-swarm-key-linux-386
chmod a+x export-swarm-key-linux-386
# 添加端口

