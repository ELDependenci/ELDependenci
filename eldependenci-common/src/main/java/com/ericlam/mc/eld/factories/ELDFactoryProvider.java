package com.ericlam.mc.eld.factories;

import java.lang.reflect.Proxy;
import java.util.Set;

import javax.inject.Provider;

import com.google.inject.Inject;
import com.google.inject.Injector;

public final class ELDFactoryProvider<T> implements Provider<T> {

	@Inject
	private Injector injector;

	private final Class<T> factoryCls;
	private final Set<Class<?>> implementations;

	public ELDFactoryProvider(Class<T> factoryCls, Set<Class<?>> implementations) {
		this.factoryCls = factoryCls;
		this.implementations = implementations;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T get() {
		return (T) Proxy.newProxyInstance(
				this.getClass().getClassLoader(),
				new Class[]{ factoryCls },
				new FactoryInvocationHandler(implementations, injector)
		);
	}

}
