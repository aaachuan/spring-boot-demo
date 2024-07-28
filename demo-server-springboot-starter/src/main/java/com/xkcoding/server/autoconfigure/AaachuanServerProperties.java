package com.xkcoding.server.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aaachuan.server")
public class AaachuanServerProperties {

    private static final Integer DEFAULT_PORT = 8000;

    private Integer port = DEFAULT_PORT;

    public Integer getPort() {
        return port;
    }

    public AaachuanServerProperties setPort(Integer port) {
        this.port = port;
        return this;
    }

}
