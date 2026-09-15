package com.company.groupware;

import com.company.groupware.service.EmployeeService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GroupwareApplication {

	public static void main(String[] args) {
		SpringApplication.run(GroupwareApplication.class, args);
	}

	// 서버 시작 시 관리자 계정이 하나도 없으면 root/root1234 계정을 자동 생성
	@Bean
	CommandLineRunner ensureDefaultAdmin(EmployeeService employeeService) {
		return args -> employeeService.ensureDefaultAdmin();
	}

}
