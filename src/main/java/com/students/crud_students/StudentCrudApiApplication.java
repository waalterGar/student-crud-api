package com.students.crud_students;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.students.crud_students.repository")
public class StudentCrudApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(StudentCrudApiApplication.class, args);
	}

}
