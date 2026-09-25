package com.github.fmqa.spu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpuApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpuApplication.class, args);
	}

}
