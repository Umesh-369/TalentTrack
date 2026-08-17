package com.umesh.talenttrack;

import org.springframework.boot.SpringApplication;

public class TestTalenttrackApplication {

	public static void main(String[] args) {
		SpringApplication.from(TalenttrackApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
