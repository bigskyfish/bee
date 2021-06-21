-- 创建数据库
create database bee if not exists bee
default character set utf8
default collate utf8_general_ci;
-- 进入bee
USE bee;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
-- 建表
-- ----------------------------
-- Table structure for server
-- ----------------------------
DROP TABLE IF EXISTS `server`;
CREATE TABLE `server` (
  `id` int(11) NOT NULL DEFAULT '0' COMMENT '主键',
  `ip` varchar(20) NOT NULL COMMENT 'ip地址',
  `user` varchar(50) NOT NULL DEFAULT 'root' COMMENT '账户',
  `psd` varchar(50) NOT NULL COMMENT '密码',
  `port` int(11) NOT NULL DEFAULT '22' COMMENT '端口',
  `endpoint` varchar(100) NOT NULL COMMENT '关联的swapPoint',
  `node_num` int(11) NOT NULL DEFAULT '1' COMMENT '节点数',
  `cpu_num` int(11) DEFAULT NULL COMMENT 'cpu核数',
  `memory` varchar(20) DEFAULT NULL COMMENT '存储',
  `band_width` varchar(20) DEFAULT NULL COMMENT '带宽',
  `detail` varchar(200) DEFAULT NULL COMMENT '其他描述信息',
  `status` int(11) DEFAULT -1 COMMENT '服务器联通情况（检查网络）\n-1: 未安装\n0：未连通（ping不同）\n1：已连通',
  PRIMARY KEY (`id`),
  UNIQUE KEY `server_ip_uindex` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据库信息';
-- 设置主键自增
ALTER TABLE `server` MODIFY id INTEGER auto_increment;


-- ----------------------------
-- Table structure for node
-- ----------------------------
DROP TABLE IF EXISTS `node`;
CREATE TABLE `node` (
    `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
    `node_ip` varchar(20) NOT NULL COMMENT '节点所在ip',
    `node_id` int(11) DEFAULT NULL COMMENT '节点所在编号',
    `node_name` varchar(20) DEFAULT NULL COMMENT '节点名称（对应包名）',
    `node_start_time` datetime DEFAULT NULL COMMENT '节点启动时间',
    `node_status` int(11) DEFAULT '0' COMMENT '节点状态0：默认待引导 -1为下线 1为正常运行',
    `node_address` varchar(100) DEFAULT NULL COMMENT '节点地址',
    `node_private_key` varchar(200) DEFAULT NULL COMMENT '节点私钥',
    `node_ticket_num` int(11) DEFAULT '0' COMMENT '出票情况',
    `node_detail` varchar(300) DEFAULT NULL COMMENT '节点的详细信息',
    PRIMARY KEY (`id`),
    UNIQUE KEY `node_u_index` (`node_ip`, `node_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='节点';
-- 设置主键自增
ALTER TABLE `node` MODIFY id INTEGER auto_increment;


SET FOREIGN_KEY_CHECKS = 1;

