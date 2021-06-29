-- 创建数据库
create database bee
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
  `status` int(3) DEFAULT -1 COMMENT '服务器联通情况（检查网络）\n-1: 未安装\n0：未连通（ping不同）\n1：已连通',
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


-- V2.0 监控指标增加
ALTER TABLE `bee`.`server`
ADD COLUMN `run_time` datetime(0) NULL COMMENT '服务器开始运行的起始时间' AFTER `endpoint`,
ADD COLUMN `cpu_used` varchar(10) NULL COMMENT 'cpu使用率' AFTER `band_width`,
ADD COLUMN `memory_used` varchar(10) NULL COMMENT '内存使用率' AFTER `cpu_used`,
ADD COLUMN `all_disk` varchar(20) NULL COMMENT '磁盘大小' AFTER `memory_used`,
ADD COLUMN `disk_used` varchar(255) NULL COMMENT '磁盘使用率' AFTER `all_disk`;

ALTER TABLE `bee`.`node`
ADD COLUMN `connect_peers` int(10) NULL AFTER `node_private_key`,
ADD COLUMN `bzz_num` varchar(50) NULL AFTER `connect_peers`,
ADD COLUMN `run_minute` int(10) DEFAULT 0  AFTER `bzz_num`;


SET FOREIGN_KEY_CHECKS = 1;

