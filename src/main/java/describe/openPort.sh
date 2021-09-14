openOrNot=$(firewall-cmd --zone=public --query-port=18090/tcp)
if [ "${openOrNot}" == no ];then
  firewall-cmd --zone=public --add-port=18090/tcp --permanent  && firewall-cmd --reload
fi
firewall-cmd --zone=public --query-port=18090/tcp