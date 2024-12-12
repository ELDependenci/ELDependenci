package com.ericlam.mc.eld;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Qualifier;

import com.ericlam.mc.eld.components.Configuration;
import com.ericlam.mc.eld.components.Overridable;
import com.ericlam.mc.eld.factories.ELDFactoryProvider;
import com.ericlam.mc.eld.services.ELDMessageService;
import com.ericlam.mc.eld.services.MessageService;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Scopes;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

@SuppressWarnings({ "unchecked", "rawtypes" })
public final class ELDCommonModule implements Module {

	private final Set<Class<?>> singleton = ConcurrentHashMap.newKeySet();
	private final Map<Class<?>, Class> services = new ConcurrentHashMap<>();
	private final Map<Class<?>, Map<String, Class>> servicesMulti = new ConcurrentHashMap<>();
	private final Map<Class<?>, Set<Class>> servicesSet = new ConcurrentHashMap<>();
	private final Map<Class, Object> instances = new ConcurrentHashMap<>();
	private final Map<Class, Configuration> configs = new ConcurrentHashMap<>();
	private final Map<Class, Class<? extends Provider>> serviceProviders = new ConcurrentHashMap<>();
	private final Map<Class, Provider> factories = new ConcurrentHashMap<>();

	private final List<Module> modules = new ArrayList<>();

	public final MCPlugin plugin;

	private boolean defaultSingleton = true;

	public ELDCommonModule(MCPlugin plugin) {
		this.plugin = plugin;
	}

	public void setDefaultSingleton(boolean defaultSingleton) {
		this.defaultSingleton = defaultSingleton;
	}

	private void setScope(ScopedBindingBuilder bindingBuilder) {
		if (defaultSingleton) {
			bindingBuilder.in(Scopes.SINGLETON);
		}
	}

	@Override
	public void configure(Binder binder) {

		// bind internal service
		binder.bind(MessageService.class).to(ELDMessageService.class).in(Scopes.SINGLETON);

		modules.forEach(binder::install);
		singleton.forEach(cls -> setScope(binder.bind(cls)));
		serviceProviders.forEach((service, provider) -> setScope(binder.bind(service).toProvider(provider)));
		factories.forEach((factory, provider) -> setScope(binder.bind(factory).toProvider(provider)));
		services.forEach((service, impl) -> setScope(binder.bind(service).to(impl)));
		configs.forEach((cls, config) -> binder.bind(cls).toInstance(config));
		instances.forEach((cls, ins) -> {
			if (ins instanceof MCPlugin mcPlugin) {
				binder.bind(MCPlugin.class).annotatedWith(Names.named(mcPlugin.getName())).toInstance(mcPlugin);
			}
			binder.bind(cls).toInstance(ins);
		});
		servicesMulti.forEach((service, map) -> {
			var binding = MapBinder.newMapBinder(binder, String.class, service);

			map.forEach((key, impl) -> {
				setScope(binder.bind(impl)); // only set scope here
				binding.addBinding(key).to(impl);
				binder.bind(service).annotatedWith(Names.named(key)).to(impl);
			});
		});
		servicesSet.forEach((services, cls) -> {
			var binding = Multibinder.newSetBinder(binder, services);
			cls.forEach(c -> {
				setScope(binder.bind(c)); // only set scope here
				binding.addBinding().to(c);
				Optional<Annotation> qualifierOpt = Arrays.stream(c.getAnnotations()).filter(a -> a.annotationType().isAnnotationPresent(Qualifier.class)).findAny();
				if (c.isAnnotationPresent(Named.class)) {
					Named named = (Named) c.getAnnotation(Named.class);
					binder.bind(services).annotatedWith(named).to(c);
				} else if (qualifierOpt.isPresent()) {
					Annotation annotation = qualifierOpt.get();
					binder.bind(services).annotatedWith(annotation).to(c);
				}
			});
		});
	}

	void bindSingleton(Class<?> singleton) {
		this.singleton.add(singleton);
	}

	<T, R extends T> void bindService(Class<T> service, Class<R> implement) {
		if (services.containsKey(service) || servicesMulti.containsKey(service) || servicesSet.containsKey(service)) {
			plugin.getLogger().warning("Service " + service.getName() + " has already registered and cannot be registered again.");
			return;
		}
		this.services.putIfAbsent(service, implement);
	}

	<T> void addServices(Class<T> service, Map<String, Class<? extends T>> implementations) {
		if (services.containsKey(service) || servicesSet.containsKey(service)) {
			plugin.getLogger().warning("Service " + service.getName() + " has already registered and cannot be registered again.");
			return;
		}
		this.servicesMulti.putIfAbsent(service, new LinkedHashMap<>());
		var map = new HashMap<String, Class>(implementations);
		this.servicesMulti.get(service).putAll(map);
	}

	<T, L extends T> void addService(Class<T> service, Class<L> implement) {
		if (services.containsKey(service) || servicesMulti.containsKey(service)) {
			plugin.getLogger().warning("Service " + service.getName() + " has already registered and cannot be registered again.");
			return;
		}
		this.servicesSet.putIfAbsent(service, new LinkedHashSet<>());
		this.servicesSet.get(service).add(implement);
	}

	<T extends Overridable, L extends T> void overrideService(Class<T> service, Class<L> implement) {
		this.services.put(service, implement);
	}

	<T extends ELDPlugin> void bindPluginInstance(Class<? extends ELDPlugin> cls, T instance) {
		this.instances.putIfAbsent(cls, instance);
	}

	public <T> void bindInstance(Class<T> cls, T instance) {
		this.instances.putIfAbsent(cls, instance);
	}

	public void bindConfig(Class<? extends Configuration> cls, Configuration c) {
		this.configs.put(cls, c);
	}

	void addModule(Module module) {
		this.modules.add(module);
	}

	<T, P extends Provider<T>> void addServiceProvider(Class<T> service, Class<P> provider) {
		this.serviceProviders.put(service, provider);
	}

	<T> void bindFactory(Class<T> factory, Set<Class<?>> implementations) {
		this.factories.put(factory, new ELDFactoryProvider<>(factory, implementations));
	}
}
