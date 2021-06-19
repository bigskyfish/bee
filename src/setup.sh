yum install -y epel-release
yum list jq
yum install -y jq
yum install -y wget
imageVersion=$1
psd=$2
swapPoint=$3
nodeNum=$4
for i in `seq 1 ${nodeNum}`
do
  port1=`expr 100 + ${i}`33
  port2=`expr 100 + ${i}`34
  port3=`expr 100 + ${i}`35
  name=bee${i}
  mkdir -p /mnt/${name}/data
  sudo docker run -d --privileged=true -p ${port3}:1635 -p ${port2}:1634 -p ${port1}:1633 --name ${name} -it ${imageVersion} start --welcome-message="Bzzzz bzzz ${name}" --password="${psd}" --swap-endpoint ${swapPoint} --debug-api-enable
  echo "${name}容器创建成功!" 
done
