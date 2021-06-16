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
  `node_num` int(11) NOT NULL DEFAULT '1' COMMENT '节点数',
  `cpu_num` int(11) DEFAULT NULL COMMENT 'cpu核数',
  `memory` varchar(20) DEFAULT NULL COMMENT '存储',
  `band_width` varchar(20) DEFAULT NULL COMMENT '带宽',
  `detail` varchar(200) DEFAULT NULL COMMENT '其他描述信息',
  `status` int(11) DEFAULT NULL COMMENT '服务器联通情况（检查网络）\n0：未连通（ping不同）\n1：已连通',
  PRIMARY KEY (`id`),
  UNIQUE KEY `server_ip_uindex` (`ip`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='数据库信息';
-- 设置主键自增
ALTER TABLE `server` MODIFY id INTEGER auto_increment;

SET FOREIGN_KEY_CHECKS = 1;

