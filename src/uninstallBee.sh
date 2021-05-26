if [ "$1" -eq 1 ]; then
  # 判断是否已安装apt-get
  curl https://raw.githubusercontent.com/dvershinin/apt-get-centos/master/apt-get.sh -o /usr/local/bin/apt-get
  chmod 0755 /usr/local/bin/apt-get
fi
# 停止服务
systemctl stop bee.service
systemctl stop bee-clef.service
# 移除开机自启
systemctl disable bee.service
systemctl disable bee-clef.service
# 清除package
/usr/local/bin/apt-get remove -y bee
/usr/local/bin/apt-get remove -y bee-clef
# 清除包文件
rm -rf /var/lib/bee/
rm -rf /etc/bee/
rm -rf /var/lib/bee-clef/
rm -rf /etc/bee-clef/
# 卸载地址信息
sudo rm -rf /root/.bee/
