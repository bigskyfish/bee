cd /mnt/beeCli/
wget https://github.com/ethersphere/exportSwarmKey/releases/download/v0.1.0/export-swarm-key-linux-386
chmod a+x export-swarm-key-linux-386
while getopts "n:" opt;
do
   case $opt in
    n)
      index=1
      node=$OPTARG
      while(( $index<=$node ))
      do
        ./export-swarm-key-linux-386 /mnt/bee$index/data/keys/ _bee_config_password_ > /mnt/bee$index/privateKey.key
         let "index++"
      done
      ;;
    \?)
      echo "invalid arg" ;;
  esac
done