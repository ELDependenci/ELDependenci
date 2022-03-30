package com.ericlam.mc.eld.configurations;

import com.ericlam.mc.eld.ELDependenci;
import com.ericlam.mc.eld.components.GroupLangConfiguration;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.ericlam.mc.eld.configurations.ELDConfigManager.ConfigUtils.setField;

public class SimpleGroupLang<T extends GroupLangConfiguration> implements GroupLang<T>, PreLoadable {

    private final static Logger LOGGER = ELDependenci.getProvidingPlugin(ELDependenci.class).getSLF4JLogger();

    private final File folder;
    private final Class<T> groupType;
    private final String defaultLang;
    private final Plugin plugin;

    private final Map<String, T> cached = new ConcurrentHashMap<>();

    public SimpleGroupLang(File folder, Class<T> groupType, Plugin plugin, String defaultLang) {
        this.folder = folder;
        this.groupType = groupType;
        this.defaultLang = defaultLang;
        this.plugin = plugin;
    }

    @Override
    public synchronized void loadAll() {
        File[] child = folder.listFiles(f -> f.getName().endsWith(".yml"));
        if (child == null) return;
        Arrays.stream(child)
                .parallel()
                .forEach(f -> {
                    String locale = FilenameUtils.getBaseName(f.getName());
                    if (cached.containsKey(locale)) {
                        return;
                    }
                    try {
                        T data = loadLanguage(locale, f);
                        cached.put(locale, data);
                    } catch (Exception e) {
                        LOGGER.warn("Error while loading " + locale + ".yml: " + e.getMessage(), e);
                    }
                });
    }

    @Override
    public synchronized Optional<T> getByLocale(String locale) {
        if (cached.containsKey(locale)) return Optional.ofNullable(cached.get(locale));
        try {
            var file = new File(folder, locale + ".yml");
            if (!file.exists()) return Optional.empty();
            T ins = loadLanguage(locale, file);
            this.cached.put(locale, ins);
            return Optional.of(ins);
        } catch (Exception e) {
            LOGGER.warn("Error while loading default language: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    private T loadLanguage(String locale, File file) throws Exception {
        YamlConfiguration yamlConfiguration = YamlConfiguration.loadConfiguration(file);
        T ins = groupType.getConstructor().newInstance();
        var controllerField = GroupLangConfiguration.class.getDeclaredField("lang");
        var localeField = GroupLangConfiguration.class.getDeclaredField("locale");
        setField(controllerField, new ELDConfigManager.MessageGetterImpl(ins, yamlConfiguration, file, plugin), ins);
        setField(localeField, locale, ins);
        return ins;
    }

    @Override
    public synchronized T getDefault() {
        var lang = getDefaultLanguage(defaultLang);
        Validate.notNull(lang);
        return lang;
    }

    @Override
    public synchronized void fetchById(String locale) {
        this.cached.remove(locale);
    }

    @Override
    public synchronized void fetch() {
        this.cached.clear();
    }

    private T getDefaultLanguage(String defaultLang) {
        try {
            var file = new File(folder, defaultLang + ".yml");
            if (!file.exists())
                throw new IllegalStateException("Default Language file cannot be non-exist !");
            return loadLanguage(defaultLang, file);
        } catch (Exception e) {
            throw new IllegalStateException("Error while loading default language: " + e.getMessage(), e);
        }
    }
}