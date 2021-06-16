package com.floatcloud.beefz.pojo;

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
public class BeeVersionPojo {

    private String beeClefVersion;
    private String beeClefRpm;
    private String beeVersion;
    private int beginIndex = 1;
    private String beeRpm;

}
