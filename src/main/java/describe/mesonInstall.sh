mkdir -p  /root/meson && cd /root/meson || exit
if [ ! -d /root/meson/meson-linux-amd64 ];then
 wget 'https://coldcdn.com/api/cdn/f2cobx/terminal/v2.5.1/meson-linux-amd64.tar.gz' &&\
 tar -zxf meson-linux-amd64.tar.gz
fi
cd ./meson-linux-amd64 || exit
cat >config.txt<<EOF
token = 7P357FgZe3QqsAEPa9iLEg==
port = 18080
spacelimit = 100
EOF
./meson service-install && ./meson service-start && ./meson service-status