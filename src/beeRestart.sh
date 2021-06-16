while getopts "n:" opt;
do
   case $opt in
    n)
      index=1
      node=$OPTARG
      while(( $index<=$node ))
      do
        cd /mnt/bee$index
        nohup bee start --config /mnt/bee$index/bee-config.yaml >>/mnt/bee$index/beeSetup.log 2>$1 &
        echo "=====node"$node"启动中====="
        let "index++"
      done
      ;;
    \?)
      echo "invalid arg" ;;
  esac
done