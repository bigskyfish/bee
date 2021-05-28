import os

import paramiko
import sys
import json

from concurrent.futures import ThreadPoolExecutor
import threading
 
class SFTP(object):
    """
    SFTP 工具类
    """
 
    def __init__(self, host, port, username, password):
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.manager = None
        self.client = None
 
    def connect(self):
        """
        连接sftp服务器
        """
        manager = paramiko.Transport((self.host, self.port))
        manager.connect(username=self.username, password=self.password)
        self.manager = manager
        client = paramiko.SFTPClient.from_transport(manager)
        self.client = client
 
    def quit(self):
        """
        断开sftp服务器连接
        """
        if not self.client:
            self.client.close()
 
        if not self.manager:
            self.manager.close()
 
    def exists(self, remote_path):
        """
        判断服务器上路径是否存在
        :param remote_path: 服务器路径
        :return:
        """
        try:
            self.client.stat(remote_path)
            return True
        except FileNotFoundError:
            return False
    def upload(self, local_file, item, remote_path=''):
        """
        上传文件到服务器
        :param local_file: 本地文件路径
        :param item: 文件相对路径
        :param remote_path: 服务器文件夹路径
        :return:
        """
        if not os.path.isfile(local_file):
            raise FileNotFoundError('File %s is not found' % local_file)
        # 得到服务器文件所在文件夹的路径
        remote_dir = os.path.join(remote_path, os.path.split(item)[0])
        # 得到服务器文件的路径
        remote_file = os.path.join(remote_path, item)
        # 上传文件
        print("*** Upload ", local_file, " to ", remote_file, " success ***")
        self.client.put(local_file, remote_file)

    def sshconn(self, shellMsg):
        ssh = paramiko.SSHClient()
        # 允许连接不在know_hosts文件中的主机
        ssh._transport = self.manager
        # ssh.connect(hostname=self.host,port=self.port,username=self.username,password=self.password)
        # 执行服务器中的shell脚本
        stdin, stdout, stderr = ssh.exec_command(shellMsg)
        result = stdout.read().decode()
        print(result)



if __name__ == '__main__':
    print("******* program start *******")
    remote_path = "/mnt/bee"
    deleteArg = sys.argv[1]
    pool = ThreadPoolExecutor(max_workers=200)
    servers = None
    # 获取脚本所在位置
    script_path = os.path.dirname(os.path.realpath(sys.argv[0]))
    with open(script_path + "/servers.json", "r", encoding="utf-8") as json_file:
        servers = json.load(json_file)
        json_file.close()
    for server in servers["servers"]:
        ip = server["ip"]  # host
        username = server["username"]  # 用户名
        password = server["password"]  # 密码
        port = server["port"]  # 端口
        local_path = server["local_path"]  # 本地文件路径
        print(username + "::::" + password)
        sftp = SFTP(ip , port, username, password)
        sftp.connect()
        print("******* ", ip, " start upload*******")
        if not sftp.exists(remote_path):
            sftp.sshconn("cd /mnt && mkdir bee")
        item_list = os.listdir(local_path)
        # 遍历目录下所有文件
        while len(item_list) > 0:
            item = item_list.pop()
            filename = os.path.join(local_path, item)
            beeline = []
            keyline = []
            print("文件：", filename)
            # 如果为目录，将目录下的文件加入遍历队列中
            if os.path.isdir(filename):
                path_list = []
                for i in os.listdir(filename):
                    path_list.append(os.path.join(item, i))
                item_list.extend(path_list)
                continue
            sftp.upload(filename, item, remote_path)
        print("******* ", ip, " end upload *******")
        # 多线程执行
        shellMsg = 'chmod 777 /mnt/bee/setup.sh && sh /mnt/bee/setup.sh -p ' + password
        if deleteArg == 1:
            shellMsg = shellMsg + ' -d'
        beefuture = pool.submit(sftp.sshconn, shellMsg)
        def getResult(future):
            print(future.result())
        beefuture.add_done_callback(getResult)
    print("******* program finish *******")
    pool.shutdown()