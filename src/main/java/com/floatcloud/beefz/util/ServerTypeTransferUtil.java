package com.floatcloud.beefz.util;

import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.floatcloud.beefz.dao.Server;

/**
 * @author floatcloud
 */
public class ServerTypeTransferUtil {


    public static ServerConfigPojo transferServerConfigPojo(Server server){
        ServerConfigPojo serverConfigPojo = new ServerConfigPojo();
        serverConfigPojo.setIp(server.getIp());
        serverConfigPojo.setUser(server.getUser());
        serverConfigPojo.setPassword(server.getPsd());
        serverConfigPojo.setPort(server.getPort());
        serverConfigPojo.setEndPoint(server.getEndpoint());
        serverConfigPojo.setNodeNum(server.getNodeNum());
        return serverConfigPojo;
    }


    public static Server transferServer(ServerConfigPojo serverConfigPojo){
        Server server = new Server();
        server.setIp(serverConfigPojo.getIp());
        server.setUser(serverConfigPojo.getUser());
        server.setPsd(serverConfigPojo.getPassword());
        server.setPort(serverConfigPojo.getPort());
        server.setEndpoint(serverConfigPojo.getEndPoint());
        server.setNodeNum(serverConfigPojo.getNodeNum());
        return server;
    }
}
