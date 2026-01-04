package com.northgod.server;

import com.northgod.server.config.DatabasePreInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ServerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ServerApplication.class);
        // 显式注册ApplicationContextInitializer，确保它被执行
        application.addInitializers(new DatabasePreInitializer());
        application.run(args);
    }

}
