package com.example.ems;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@RequiredArgsConstructor
public class EmsApplication {

	private final PasswordEncoder passwordEncoder;
	@PostConstruct
	public void test(){
		System.out.println(passwordEncoder.encode("password123"));
	}
	public static void main(String[] args) {
		SpringApplication.run(EmsApplication.class, args);
	}


}
