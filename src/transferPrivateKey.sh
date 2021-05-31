cd /root/.bee/keys
wget https://github.com/ethersphere/exportSwarmKey/releases/download/v0.1.0/export-swarm-key-linux-386
chmod a+x export-swarm-key-linux-386
./export-swarm-key-linux-386 /root/.bee/keys/ _bee_config_password_ > /mnt/bee/privateKey.key