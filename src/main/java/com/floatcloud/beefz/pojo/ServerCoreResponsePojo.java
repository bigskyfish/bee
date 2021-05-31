package com.floatcloud.beefz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author aiyuner
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ServerCoreResponsePojo {

    private String address;
    private String ip;
    private String privateKey;

}
