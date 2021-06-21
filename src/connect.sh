nodeNum=$1
# 启动所有下线服务
docker ps -a | grep Exited | awk '{print $1}' |xargs docker start
for i in `seq 1 ${nodeNum}`
do
  port=`expr 100 + ${i}`35
  curl -X POST localhost:${port}/connect/ip4/114.115.137.118/tcp/1634/p2p/16Uiu2HAm4grratwAahiyGrH5qRwuMTPDePqRXHWMv42zdcT3VVKH
  curl -X POST localhost:${port}/connect/ip4/54.210.29.30/tcp/31308/p2p/16Uiu2HAmRHY7h1cdpmB6S48MGfwecG3kfhLT9cXpE8SokS9o1Cck
  curl -X POST localhost:${port}/connect/ip4/54.210.29.30/tcp/31300/p2p/16Uiu2HAmUC3GsAfnZhVyYz5suYYCkKE9TCvJnLWT75EeoqpDarCM
  curl -X POST localhost:${port}/connect/ip4/39.173.176.4/tcp/27047/p2p/16Uiu2HAm8dAWoutmQRVTPDUMVTcEcd2ELgDXBqqUWbKkH47k7Svx
  curl -X POST localhost:${port}/connect/ip4/112.13.205.243/tcp/17024/p2p/16Uiu2HAm2zPyD93BmPozkxctoiX2UrNji9GiCBKppEXssfk5KiCN
  curl -X POST localhost:${port}/connect/ip4/36.158.231.20/tcp/40026/p2p/16Uiu2HAmUeb6JfkJfSWdK4MsYQcEXyf4vFKeeXs3rEaimAjDPjwz
  curl -X POST localhost:${port}/connect/ip4/36.158.231.4/tcp/40047/p2p/16Uiu2HAmR3nnYbnFYeasmBNiRgzwiKJmnT1PLGCRJBV97xggRohN
  curl -X POST localhost:${port}/connect/ip4/183.60.41.47/tcp/10321/p2p/16Uiu2HAm52weM84oHNc8G4sWWvk4axWL85MCaM5YBswQJ9fVzuCT
  curl -X POST localhost:${port}/connect/ip4/122.9.111.129/tcp/1634/p2p/16Uiu2HAmLR1suVRtNWZGhR3x3EXZ9ikY4ZVYWr1zmE4cigPu2Wz6
  curl -X POST localhost:${port}/connect/ip4/8.209.216.186/tcp/2634/p2p/16Uiu2HAmQmiuwfrxdNE5UkMfbKsb4M5EJuesTTA4nrhd89Z5Xb4D
done
