package com.floatcloud.beefz.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PrivateKeyPojo {

    private String address;
    private String privatekey;
    private String id;
    private String version;
}
