while getopts "d:p:v:" opt;
do
  case $opt in
    d)
      unbee=$OPTARG
      chmod 777 /mnt/bee/uninstall.sh
      sh /mnt/bee/uninstall.sh ;;
    p)
      passwd=$OPTARG
      nohup sh /mnt/bee/setupServer.sh -p $passwd  >>/mnt/bee/beeSetup.log 2>&1 & ;;
    \?)
      echo "invalid arg" ;;
  esac
done
exit