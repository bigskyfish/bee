# shellcheck disable=SC2126
diskOn=$(df -h | grep /root | grep /dev/sdb | wc -l)
if [ "${diskOn}" != "1" ];then
  echo "执行磁盘重新挂载"
  sh /root/meson/LinuxVMDiskAutoInitialize.sh
fi
mkdir -p  /root/meson && cd /root/meson || exit
if [ ! -d /root/meson/meson-linux-amd64 ];then
 wget 'https://coldcdn.com/api/cdn/f2cobx/terminal/v2.5.1/meson-linux-amd64.tar.gz' &&\
 tar -zxf meson-linux-amd64.tar.gz
fi
cd /root/meson/meson-linux-amd64 || exit
cat >config.txt<<EOF
token = LViy39eSOmO3AQVdUCXxqw==
port = 18090
spacelimit = 80
EOF
if [ ! -d  /root/meson/meson-linux-amd64/dailylog ];then
  /root/meson/meson-linux-amd64/meson service-install
fi
mesonOn=$(ps -ef | grep /root/meson/meson-linux-amd64/meson | grep -v auto | wc -l)
if [ "${mesonOn}" == "1" ];then
  /root/meson/meson-linux-amd64/meson service-stop
fi
./meson service-start && ./meson service-status