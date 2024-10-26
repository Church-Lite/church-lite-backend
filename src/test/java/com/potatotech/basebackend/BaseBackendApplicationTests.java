package com.potatotech.basebackend;

import com.potatotech.StarterApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


class BaseBackendApplicationTests {

	@Test
	void contextLoads() {
		System.out.println(StarterApplication.class.getPackage());
	}

}
