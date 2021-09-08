package com.floatcloud.beefz.base.util;

import com.floatcloud.beefz.base.pojo.BaseServerConfigPojo;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author floatcloud
 */
public class TransferFileUtils {


    public static List<BaseServerConfigPojo> transferFileToServerConfig(String filePath){
        List<BaseServerConfigPojo> servers = new ArrayList<>();
        try ( FileInputStream inputStream = new FileInputStream(filePath);
              BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
            String line;
            boolean first = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (first) {
                    first = false;
                } else {
                    String[] serverInfos = line.split(",");
                    BaseServerConfigPojo configPojo = new BaseServerConfigPojo();
                    configPojo.setIp(serverInfos[0]);
                    configPojo.setUser(serverInfos[1]);
                    configPojo.setPassword(serverInfos[2]);
                    configPojo.setPort(Integer.valueOf(serverInfos[3]));
                    servers.add(configPojo);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
        return servers;
    }
}
