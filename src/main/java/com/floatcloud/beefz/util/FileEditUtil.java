package com.floatcloud.beefz.util;

import com.floatcloud.beefz.constant.FileConstant;
import com.floatcloud.beefz.pojo.ServerConfigPojo;
import com.sun.deploy.net.MessageHeader;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author floatcloud
 */
public class FileEditUtil {


    private static List<ServerConfigPojo> serverConfigPojos = new ArrayList<>(500);
    
    static {
        String filePath = System.getProperty("user.dir") + File.separator +"src" + File.separator + FileConstant.CSV_PATH;
        try {
            FileInputStream inputStream = new FileInputStream(filePath);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            boolean first = true;
            while ((line = bufferedReader.readLine()) != null) {
                if (first) {
                    first = false;
                } else {
                    String[] serverInfos = line.split(",");
                    ServerConfigPojo configPojo = new ServerConfigPojo();
                    configPojo.setIp(serverInfos[0]);
                    configPojo.setUser(serverInfos[1]);
                    configPojo.setPassword(serverInfos[2]);
                    configPojo.setPort(Integer.valueOf(serverInfos[3]));
                    serverConfigPojos.add(configPojo);
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }
    
    public static List<ServerConfigPojo> getServers(){
        return serverConfigPojos;
    }
    
    



    public static String appendMsg(String filePath, String content) {
        int len = 0;
        StringBuilder str = new StringBuilder();
        File file = new File(filePath);
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while( (line=bufferedReader.readLine())!=null ) {
                if (len != 0) {
                    str.append("\r\n"+line);
                } else {
                    str.append(line);
                }
                len++;
            }
            str.append(content);
            bufferedReader.close();
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str.toString();

    }


    /**
     * 获取 服务列表
     * @return
     */
    public static List<ServerConfigPojo> getServerList(File file) throws IOException {
        List<ServerConfigPojo> result = new ArrayList<>(200);
        FileInputStream inputStream = new FileInputStream(file);
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        String line;
        boolean first = true;
        while((line = bufferedReader.readLine()) != null){
            if(first){
                first = false;
            } else {
                String[] strs = line.split(",");
                ServerConfigPojo configPojo = new ServerConfigPojo();
                configPojo.setIp(strs[0]);
                configPojo.setUser(strs[1]);
                configPojo.setPassword(strs[2]);
                result.add(configPojo);
            }
        }
        return result;
    }
}
