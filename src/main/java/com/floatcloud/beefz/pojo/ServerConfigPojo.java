package com.floatcloud.beefz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author floatcloud
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerConfigPojo {

    private String ip;
    private String user;
    private String password;

}
