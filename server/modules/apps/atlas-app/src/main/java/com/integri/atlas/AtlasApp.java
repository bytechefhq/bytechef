/* (C)2016-2018 */
package com.integri.atlas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;

@SpringBootApplication
public class AtlasApp {

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(AtlasApp.class);
        springApplication.addListeners(new ApplicationPidFileWriter());
        springApplication.run(args);
    }
}
