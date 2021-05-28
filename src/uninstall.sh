# 停止服务
systemctl stop bee.service
systemctl stop bee-clef.service
# 移除开机自启
systemctl disable bee.service
systemctl disable bee-clef.service
# 清除package
yum remove -y bee
yum remove -y bee-clef
# 清除包文件
rm -rf /var/lib/bee/
rm -rf /etc/bee/
rm -rf /var/lib/bee-clef/
rm -rf /etc/bee-clef/
# 卸载地址信息
rm -rf /root/.bee/
