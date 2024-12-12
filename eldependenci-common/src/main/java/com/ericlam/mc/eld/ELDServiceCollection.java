package com.ericlam.mc.eld;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Provider;

import com.ericlam.mc.eld.common.CommonCommandNode;
import com.ericlam.mc.eld.common.CommonRegistry;
import com.ericlam.mc.eld.components.Configuration;
import com.ericlam.mc.eld.components.GroupConfiguration;
import com.ericlam.mc.eld.components.GroupLangConfiguration;
import com.ericlam.mc.eld.components.Overridable;
import com.ericlam.mc.eld.configurations.ELDConfigManager;
import com.ericlam.mc.eld.registration.CommandRegistry;
import com.ericlam.mc.eld.registration.ListenerRegistry;
import com.ericlam.mc.eld.registrations.ELDCommandRegistry;
import com.ericlam.mc.eld.registrations.ELDListenerRegistry;

public abstract class ELDServiceCollection<CommandNode extends CommonCommandNode<?>, Listener, Plugin> implements ServiceCollection {

	final Map<Class<?>, Object> customInstallation;

	final ELDConfigManager configManager;

	private final ELDCommonModule module;

	final Set<HierarchyNode<? extends CommandNode>> commands;
	final Set<Class<? extends Listener>> listeners;
	final LifeCycle<Plugin> lifeCycleHook;

	static Set<MCPlugin> DISABLED = new HashSet<>();

	public ELDServiceCollection(ELDCommonModule module, MCPlugin plugin, Map<Class<?>, Object> customInstallation, ConfigHandler handler) {
		this.module = module;
		this.customInstallation = customInstallation;
		this.configManager = new ELDConfigManager(module, plugin, handler);

		var en = getComponents(plugin);
		var registryCls = en.getKey();
		var lifeCycleCls = en.getValue();

		var registry = registryCls.isInterface() ? new EmptyCommandRegistry() : this.toInstance(registryCls);
		this.lifeCycleHook = lifeCycleCls.isInterface() ? new EmptyLifeCycle() : this.toInstance(lifeCycleCls);

		//register command
		var cmdregistry = new ELDCommandRegistry<CommandNode>();
		registry.registerCommand(cmdregistry);
		this.commands = cmdregistry.getNodes();

		//register listeners
		var listenerRegistry = new ELDListenerRegistry<Listener>();
		registry.registerListeners(listenerRegistry);

		this.listeners = listenerRegistry.getListenersCls();
	}

	public abstract Map.Entry<
			Class<? extends CommonRegistry<CommandNode, Listener>>,
			Class<? extends LifeCycle<Plugin>>
			> getComponents(MCPlugin plugin);

	@Override
	public ServiceCollection addSingleton(Class<?> singleton) {
		if (Modifier.isAbstract(singleton.getModifiers()))
			throw new IllegalStateException("Singleton class cannot be an abstract class");
		module.bindSingleton(singleton);
		return this;
	}

	@Override
	public <T, L extends T> ServiceCollection bindService(Class<T> service, Class<L> implementation) {
		if (!Modifier.isAbstract(service.getModifiers()))
			throw new IllegalStateException("Service class must be an abstract class");
		module.bindService(service, implementation);
		return this;
	}

	@Override
	public <T, P extends Provider<T>> ServiceCollection bindServiceProvider(Class<T> service, Class<P> provider) {
		module.addServiceProvider(service, provider);
		return this;
	}

	@Override
	public ServiceCollection bindFactory(Class<?> factory, Class<?>... implementations) {
		if (!factory.isInterface()) throw new IllegalStateException("Factory class must be an interface");
		for (var method : factory.getMethods()) {
			if (Void.class.isAssignableFrom(method.getReturnType()) || void.class.isAssignableFrom(method.getReturnType())) {
				throw new IllegalStateException("Factory method cannot return void: " + method);
			}

		}
		for (var impl : implementations) {
			if (impl.isInterface() || Modifier.isAbstract(impl.getModifiers())) {
				throw new IllegalStateException("Factory implementation must not be an interface or abstract class: " + impl);
			}
		}
		module.bindFactory(factory, Set.of(implementations));
		return this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getInstallation(Class<T> cls) {
		return Optional.ofNullable(customInstallation.get(cls)).map(r -> (T) r).orElseThrow(() -> new IllegalStateException("unknown installation: " + cls));
	}

	@Override
	public <T extends Overridable, L extends T> ServiceCollection overrideService(Class<T> service, Class<L> implementation) {
		if (!Modifier.isAbstract(service.getModifiers()))
			throw new IllegalStateException("Service class must be an abstract class");
		module.overrideService(service, implementation);
		return this;
	}

	@Override
	public <T, L extends T> ServiceCollection addService(Class<T> service, Class<L> implementation) {
		if (!Modifier.isAbstract(service.getModifiers()))
			throw new IllegalStateException("Service class must be an abstract class");
		module.addService(service, implementation);
		return this;
	}

	@Override
	public <T> ServiceCollection addServices(Class<T> service, Map<String, Class<? extends T>> implementations) {
		if (!Modifier.isAbstract(service.getModifiers()))
			throw new IllegalStateException("Service class must be an abstract class");
		module.addServices(service, implementations);
		return this;
	}

	@Override
	public <T extends Configuration> ServiceCollection addConfiguration(Class<T> config) {
		configManager.loadConfig(config);
		return this;
	}

	@Override
	public <T extends GroupConfiguration> ServiceCollection addGroupConfiguration(Class<T> config) {
		this.configManager.loadConfigPool(config);
		return this;
	}

	@Override
	public <T extends GroupLangConfiguration> ServiceCollection addMultipleLanguages(Class<T> lang) {
		this.configManager.loadLanguagePool(lang);
		return this;
	}

	protected <T> T toInstance(Class<T> registry) {
		try {
			var con = registry.getConstructor();
			return con.newInstance();
		} catch (Exception e) {
			if (e instanceof NoSuchMethodException) {
				throw new IllegalStateException(registry.getSimpleName() + " must have public no-args constructor");
			}
			throw new RuntimeException(e);
		}
	}

	public final class EmptyLifeCycle implements LifeCycle<Plugin> {

		@Override
		public void onEnable(Plugin plugin) {
		}

		@Override
		public void onDisable(Plugin plugin) {
		}
	}

	public final class EmptyCommandRegistry implements CommonRegistry<CommandNode, Listener> {

		@Override
		public void registerCommand(CommandRegistry<CommandNode> registry) {
		}

		@Override
		public void registerListeners(ListenerRegistry<Listener> registry) {
		}
	}
}
