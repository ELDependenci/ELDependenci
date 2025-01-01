package com.ericlam.mc.eld.services.logging;

import com.ericlam.mc.eld.implement.ELDConfig;
import com.ericlam.mc.eld.misc.DebugLogger;
import com.ericlam.mc.eld.services.LoggingService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public final class ELDLoggingService implements LoggingService {

    private final Map<String, DebugLogger> loggerMap = new ConcurrentHashMap<>();

    private final ELDConfig config;
    private final Logger parent;

    public ELDLoggingService(ELDConfig config, Logger parent) {
        this.config = config;
        this.parent = parent;
    }

    @Override
    public DebugLogger getLogger(Class<?> cls) {
        return this.getLogger(cls.getName());
    }

    @Override
    public DebugLogger getLogger(String name) {
        return Optional.ofNullable(loggerMap.get(name)).orElseGet(() -> {
            var logger = new ELDLogger(name, config, parent);
            loggerMap.put(name, logger);
            return logger;
        });
    }

    @Override
    public void changeDebugMode(boolean debugEnabled) throws IOException {
        config.debugLogging = debugEnabled;
        config.getController().save();
    }

}
