import os

import paramiko
import sys
import json
 
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
        # 创建文件夹
        self.mkdir(remote_dir)
        # 得到服务器文件的路径
        remote_file = os.path.join(remote_path, item)
        # 上传文件
        print("*** Upload ", local_file, " to ", remote_file, " success ***")
        self.client.put(local_file, remote_file)
 
    def mkdir(self, path):
        """
        根据路径递归创建文件夹
        :param path: 文件夹路径
        :return:
        """
        if self.exists(path):
            return
        self.mkdir(os.path.dirname(path))
        self.client.mkdir(path)

if __name__ == '__main__':
    print("******* program start *******")
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
        passwordw = "\r\npassword: " + password
        privatekey = " " + password + " > /mnt/bee/privateKey.txt"
        port = server["port"]  # 端口
        remote_path = server["remote_path"]  # 服务器目标路径
        local_path = server["local_path"]  # 本地文件路径
        excludes = server["excludes"]  # 排除文件或文件夹夹路径
        sftp = SFTP(ip , port, username, password)
        sftp.connect()
        print("******* ", ip, " start upload*******")
        if not sftp.exists(remote_path):
            sftp.mkdir(remote_path)
        item_list = os.listdir(local_path)
        # 遍历目录下所有文件
        while len(item_list) > 0:
            item = item_list.pop()
            # 如果文件或文件夹在排除名单内，跳过本次循环
            if item in excludes:
                continue
            filename = os.path.join(local_path, item)
            beeline = []
            keyline = []
            print("文件：", filename)
            if "bee-config.yaml" in filename:
                beefile = open(filename, "r")
                while 1:
                    line = beefile.readline()
                    if not line:
                        break
                    beeline.append(line)
                beefile.close()
                with open(filename, "a") as filestr:
                    filestr.write(passwordw)
            if "transferprivateKey.sh" in filename:
                beefile = open(filename, "r")
                while 1:
                    line = beefile.readline()
                    if not line:
                        break
                    keyline.append(line)
                beefile.close()
                with open(filename, "a") as filestr:
                    filestr.write(privateKey)
            # 如果为目录，将目录下的文件加入遍历队列中
            if os.path.isdir(filename):
                path_list = []
                for i in os.listdir(filename):
                    path_list.append(os.path.join(item, i))
                item_list.extend(path_list)
                continue
            sftp.upload(filename, item, remote_path)
            if "bee-config.yaml" in filename:
                beefile2 = open(filename, "w")
                for i in beeline:
                    beefile2.write(i)
                beefile2.close()
            if "transferprivateKey.sh" in filename:
                beefile2 = open(filename, "w")
                for i in keyline:
                    beefile2.write(i)
                beefile2.close()
        print("******* ", ip, " end upload *******")
    print("******* program finish *******")