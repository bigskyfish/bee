#!/bin/bash

#This shell is produced by EBS@cmss
#data: 2021-6-2

export PATH=/usr/local/sbin/:/usr/local/bin:/sbin:/bin:/usr/sbin:/usr/bin:/root/bin
clear
echo -e "\n\033[36mStep 1: Initializing script and check root privilege\033[0m"
if [ "$(id -u)" = "0" ];then
	echo -e "\033[33mIs running, please wait!\033[0m"
	yum -y install e4fsprogs > /dev/null 2>&1
	yum -y install psmisc > /dev/null 2>&1
	echo -e "\033[32mSuccess, the script is ready to run!\033[0m"
else
	echo -e "\033[31mError, this script must be run as root!\n\033[0m"
	exit 1
fi


echo -e "\n\033[36mStep 2: Show all active disks:\033[0m"
fdisk -l 2>/dev/null | grep -o "Disk /dev/.*" | sed '1d' | grep -vE "root|swap"
Disk=/dev/sdb
echo "Disk Value : ${Disk}"
#analysis usr's input
until fdisk -l 2>/dev/null | grep -o "Disk /dev/.*" | sed '1d' | grep -vE "root|swap" | grep -w "Disk $Disk" &>/dev/null && [[ $Disk != "/dev" ]];do
#until fdisk -l 2>/dev/null | grep -o "Disk /dev/.*" | sed '1d' | grep -vE "root|swap" | grep -w "Disk $Disk" &>/dev/null;do
	Disk=/dev/sdb
done

while mount | grep $Disk > /dev/null 2>&1;do
	echo -e "\033[31m\nYour disk has been mounted:\033[0m"
	mount | grep "$Disk"
	echo -e -n "\033[31m\nForce umounting?[y/n]:\033[0m"
	read Umount
	until [ $Umount == y -o $Umount == n ];do
		echo -e -n "\033[31mError input, please try again [y/n]:\033[0m"
		read Umount
	done
	if [ $Umount == n ];then
		exit
	else
		echo -e "\033[33mIs running, please wait!\033[0m"
		#awk will be wrong with " ",replace it with ''
		for i in $(mount | grep "$Disk" | awk '{print $3}');do
			fuser -km $i >/dev/null
			umount $i >/dev/null
			#use '\' to recognize '/' is a path's symbol
			temp=$(echo $Disk | sed 's;/;\\\/;g')
			sed -i -e "/^$temp/d" /etc/fstab
			sleep 2
		done
		echo -e "\033[32mSuccessfully umount this disk!\033[0m"
	fi

	echo -e -n "\n\033[31mReady to begin to format the disk? [y/n]:\033[0m"
	read Choice
	until [ $Choice == y -o $Choice == n ];do
		echo -e -n "\033[31mError input, please try again [y/n]:\033[0m"
		read Choice
	done
	if [ $Choice == n ];then
		exit
	else
		echo -e "\033[33mIs running, Please wait!\033[0m"
		dd if=/dev/zero of=$Disk bs=512 count=1 &>/dev/null
		sleep 2
	sync
	fi
	echo -e "\033[32mThis disk had been formatted\033[0m"
done

echo -e "\n\033[36mStep 4: This disk is formatting\033[0m"
echo -e "\033[33mIs running, please wait!\033[0m"
fdisk_mkfs() {

echo y | mkfs.ext4 ${1}
}
fdisk_mkfs $Disk > /dev/null 2>&1
echo -e "\033[32mSuccess, the disk has been formatted!\033[0m"
echo -e "\n\033[36mStep 5: Make a directory and mount it\033[0m"
echo -e "\n\033[36mWarning:Files that previously existed in this directory will be hidden!\033[0m"
Mount=/root
	mkdir -p $Mount > /dev/null 2>&1
	mount ${Disk} $Mount
	echo -e "\033[32mSuccess, the mount is completed!\033[0m"
	echo -e "\n\033[36mStep 6: Write configuration to /etc/fstab and mount device\033[0m"
	echo ${Disk} $Mount 'ext4 defaults 0 0' >> /etc/fstab
	echo -e "\033[32mSuccess, the /etc/fstab is Write!\033[0m"
	echo -e "\n\033[36mStep 7: Show information about the file system on which each FILE resides\033[0m"
	df -h
	sleep 2
	echo -e "\n\033[36mStep 8: Show the write configuration to /etc/fstab\033[0m"
	cat /etc/fstab



LinuxVMDiskAutoInitialize.sh