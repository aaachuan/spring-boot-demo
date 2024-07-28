package com.xkcoding.server.autoconfigure;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.InetSocketAddress;

@Configuration
@EnableConfigurationProperties(AaachuanServerProperties.class)
public class AaachuanServerAutoConfiguration {

    private Logger logger = LoggerFactory.getLogger(AaachuanServerAutoConfiguration.class);


    @Bean
    @ConditionalOnClass(HttpServer.class)
    public HttpServer httpServer(AaachuanServerProperties serverProperties) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(serverProperties.getPort()), 0);
        server.start();
        logger.info("[httpServer][启动服务器成功，端口为:{}]", serverProperties.getPort());

        return server;
    }
}
