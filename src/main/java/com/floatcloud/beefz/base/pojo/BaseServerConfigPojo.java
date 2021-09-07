package com.floatcloud.beefz.base.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author floatcloud
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class BaseServerConfigPojo {

    private String ip;
    private String user;
    private String password;
    private Integer port;

}
