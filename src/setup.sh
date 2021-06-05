endpoint=$1
psd=$2
node=1
echo "====psd==="$psd
chmod 777 /mnt/beeCli/bee-config.yaml
chmod 777 /mnt/beeCli/transferPrivateKey.sh
sed -i "s*_bee_config_password_*"$psd"*g" /mnt/beeCli/bee-config.yaml
sed -i "s*_swap_endpoint_http_*"$endpoint"*g" /mnt/beeCli/bee-config.yaml
sed -i "s*_bee_config_password_*"$psd"*g" /mnt/beeCli/transferPrivateKey.sh
node=$3
index=1
cd /mnt/
while(( $index<=$node ))
  do
    if [ ! -d bee$index ]; then
      mkdir bee$index
      cd bee$index
      mkdir data
    fi
    let baseport=100+index
    cp /mnt/beeCli/bee-config.yaml /mnt/bee$index
    cp /mnt/beeCli/transferPrivateKey /mnt/bee$index
    cd /mnt/bee$index
    chmod 777 /mnt/bee$index/bee-config.yaml
    sed -i "s*_bee_config_path_*"/mnt/bee$index"*g" /mnt/bee$index/bee-config.yaml
    sed -i "s*_bee_port_*"$baseport"*g" /mnt/bee$index/bee-config.yaml
    cd /mnt/
    let "index++"
  done
echo "======节点数====="$node
nohup sh /mnt/beeCli/setupServer.sh -n $node /dev/null &
exit