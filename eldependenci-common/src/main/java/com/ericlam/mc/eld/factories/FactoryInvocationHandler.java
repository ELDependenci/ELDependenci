package com.ericlam.mc.eld.factories;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;

import javax.inject.Named;

import com.google.inject.Injector;

public final class FactoryInvocationHandler implements InvocationHandler {

	private final Set<Class<?>> implementations;
	private final Injector injector;

	public FactoryInvocationHandler(Set<Class<?>> implementations, Injector injector) {
		this.implementations = implementations;
		this.injector = injector;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		var implementationCls = findImplementations(method);
		var constructorArgs = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
		try {
			var constructor = implementationCls.getConstructor(constructorArgs);
			var instance = constructor.newInstance(args);
			injector.injectMembers(instance);
			return instance;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("No such constructor found for " + implementationCls.getName() + " with args: " + Arrays.toString(constructorArgs), e);
		}
	}

	private Class<?> findImplementations(Method method) {
		var returnType = method.getReturnType();
		if (isNotAbstract(returnType)) return returnType;
		var possibles = implementations.stream()
				.filter(returnType::isAssignableFrom)
				.filter(cls -> isNamedMatch(method, cls))
				.toList();
		if (possibles.isEmpty()) {
			throw new IllegalArgumentException("No implementation found for " + returnType.getName());
		} else if (possibles.size() > 1) {
			throw new IllegalArgumentException("Multiple implementations found for " + returnType.getName() + ": " + possibles);
		}
		return possibles.getFirst();
	}

	private static boolean isNotAbstract(Class<?> cls) {
		return !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers());
	}

	private static boolean isNamedMatch(Method method, Class<?> impl) {
		var methodNamed = method.getAnnotation(Named.class);
		var implNamed = impl.getAnnotation(Named.class);
		if (methodNamed == null && implNamed == null) return true;
		if (methodNamed == null || implNamed == null) return false;
		return methodNamed.value().equals(implNamed.value());
	}
}
