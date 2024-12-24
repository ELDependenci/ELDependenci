package com.ericlam.mc.eldtest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Inject;

public class TestFactory {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface Factory {
	}

	public interface TestingFactory {
		TestService2 create(String name);
	}

	public static class TestService {
		public void print(String name) {
			System.out.println("Hello " + name);
		}
	}

	public static class TestService2 {
		@Inject
		public TestService testService;
		public final String name;

		public TestService2(String name) {
			this.name = name;
		}

		public void print() {
			testService.print(name);
		}
	}

	public static class TestService3 {

		@Inject
		private TestingFactory factory;

		public void test() {
			TestService2 service = factory.create("test");
			System.out.println(service.name);
		}
	}
}
