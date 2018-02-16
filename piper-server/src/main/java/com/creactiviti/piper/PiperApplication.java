
package com.creactiviti.piper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.system.ApplicationPidFileWriter;

@SpringBootApplication
public class PiperApplication {

	public static void main(String[] args) {
		SpringApplication springApplication = new SpringApplication(PiperApplication.class);
		springApplication.addListeners(new ApplicationPidFileWriter());
		springApplication.run(args);
	}
}
