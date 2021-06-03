package com.floatcloud.beefz.pojo;

import com.floatcloud.beefz.sysenum.ServerStatusEnum;
import lombok.Data;
import lombok.ToString;

/**
 * @author aiyuner
 */
@Data
@ToString
public class ServerCoreResponsePojo {

    private String address;
    private String ip;
    private String privateKey;
    private ServerStatusEnum statusEnum;
    private String status;

    public ServerCoreResponsePojo(Builder builder){
        this.address = builder.address;
        this.ip = builder.ip;
        this.privateKey = builder.privateKey;
        this.statusEnum = builder.statusEnum;
        this.status = builder.status;
    }

    public static class Builder{
        private String address;
        private String ip;
        private String privateKey;
        private ServerStatusEnum statusEnum = ServerStatusEnum.UN_INSTANCE;
        private String status = ServerStatusEnum.UN_INSTANCE.getType();

        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        public Builder withIp(String ip) {
            this.ip = ip;
            return this;
        }

        public Builder withPrivateKey(String privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder withStatusEnum(ServerStatusEnum statusEnum) {
            this.statusEnum = statusEnum;
            return this;
        }

        public Builder withStatus(String status) {
            this.status = status;
            return this;
        }

        public ServerCoreResponsePojo build(){
            return new ServerCoreResponsePojo(this);
        }

    }
}
