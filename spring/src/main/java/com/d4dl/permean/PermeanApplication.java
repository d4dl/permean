package com.d4dl.permean;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan("com.d4dl.permean.data")
public class PermeanApplication {

	public static void main(String[] args) {
		SpringApplication.run(PermeanApplication.class, args);
	}
}
