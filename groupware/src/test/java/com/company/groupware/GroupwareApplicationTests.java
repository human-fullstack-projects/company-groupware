package com.company.groupware;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GroupwareApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private com.company.groupware.repository.EmployeeRepository employeeRepository;

	@Test
	void contextLoads() {
		System.out.println("=== EMPLOYEES ===");
		employeeRepository.findAll().forEach(e -> {
			String dName = e.getDepartment() != null ? e.getDepartment().getDeptName() : "부서없음";
			String gName = e.getGrade() != null ? e.getGrade().getGradeName() : "직급없음";
			System.out.println("EMP: " + e.getEmplId() + " | " + e.getEmplName() + " | " + dName + " | " + gName);
		});
	}

}
