while getopts "d:p:" opt;
do
  case $opt in
    d)
      unbee=$OPTARG
      chmod 777 /mnt/bee/uninstall.sh
      sh /mnt/bee/uninstall.sh
      ;;
    p)
      passwd=$OPTARG
      chmod 777 /mnt/bee/setupServer.sh
      nohup sh /mnt/bee/setupServer.sh -p $passwd  >>/mnt/bee/beeSetup.log &
      ;;
    \?)
      echo "invalid arg"
      ;;
  esac
done