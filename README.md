
## bee的bee-config.yaml 端口以及文件说明

#### 多节点部署

###### 目录结构
- /mnt/bee 包下包含所有配置文件
- /mnt/bee1 /mnt/bee2 一直到 /mnt/beeN 为N个节点的安装目录
- N 个节点的安装目录中
    - 包含 /data 文件夹，用于存储 bee运行后的 key
    - bee-config.yaml 配置文件

端口说明
bee
- 默认启动端口 ：1633
- p2p端口 ：1634
- 监听端口 ：1635

改造后如下：
- 包 beeCli 存放启动脚本
- 包 bee1 端口为： 10133、10134、10135
- 包 bee2 端口为： 10233、10234、10235
  。。。
- 包 bee12 端口： 11233、11234、11235




## 脚本说明与更新

#### 单节点手动部署脚本执行顺序

- 第一步： 创建文件夹，并上传脚本

```shell
mkdir -p /mnt/beeCli
```

- 将sh脚本上传至该文件夹（多种方式，xshell,也可以直接采用scp命令，
  这里展示下scp命令）
```shell script
scp -r 【存放上传文件的文件夹的绝对路径】 root@【上传服务器ip】:/mnt/beeCli
```
回车后，接着输入服务器root账户密码，显示上传成功
示例：
```shell script
scp -r /Users/floatcloud/Miner root@192.168.0.1:/mnt/beeCli
```

- 第二步：脚本执行

```shell script
cd /mnt/beeCli
# 如果文件权限不够，加授权，有读写执行权限，下面授权命令则省略
chmod 777 【相应文件】
# 也可以对整个文件夹的文件授权
chmod 777 /mnt/beeCli/*

#### 脚本执行
sh /mnt/beeCli/setup.sh 【项目地址】【bee启动密码建议与服务器密码一致】 【创建节点的开始节点数起始为1】  【创建节点的总节点数】 【交易gas费，目前建议5000】 
```
示例：
```shell script
sh /mnt/beeCli/setup.sh https://goerli.infura.io/v3/026d3df94fad47abaf0fac718e4951be Mnb123456. 1 10 5000
```
- 第三步：启动后，执行获取密钥脚本
```shell script
cd /mnt/beeCli
# 传入的为节点总数
sh /mnt/beeCli/transferPrivateKey.sh -n 【节点总数】
```

- 最后说明
1. 密钥、地址均在对应包类的 privateKey.key 文件
2. bee 的启动日志记录，在响应包的 beeSetup.log

---

待后续更新


