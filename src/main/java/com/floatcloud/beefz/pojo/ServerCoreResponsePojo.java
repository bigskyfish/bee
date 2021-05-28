package com.floatcloud.beefz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author aiyuner
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServerCoreResponsePojo {

    private String address;
    private String ip;
    private String privateKey;

}
