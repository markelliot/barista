/*
 * (c) Copyright 2021 Mark Elliot. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.markelliot.barista;

import java.net.URI;
import java.util.Locale;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.Order;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.core.config.plugins.Plugin;

@Plugin(name = "BaristaLogging", category = ConfigurationFactory.CATEGORY)
@Order(1_000_000)
final class Logging extends ConfigurationFactory {

    static final String DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY = "barista.logging.disableDefaults";
    static final String PATTERN = "%d [%t] %level: %msg%n%throwable";
    static final String LOGGER_PROP_PREFIX = "log4j2.logger.";

    private static final String STDOUT = "stdout";
    private static final String[] SUPPORTED_TYPES = {"*"};

    private static boolean isTrueish(String value) {
        return value != null && value.matches("(?i)^(true|1|yes|on|enabled)$");
    }

    static BuiltConfiguration createConfiguration(String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        if (isTrueish(System.getProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY, null))) {
            return null;
        }
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName(name);

        LayoutComponentBuilder layout = builder.newLayout("PatternLayout").addAttribute("pattern", PATTERN);

        AppenderComponentBuilder appenderBuilder = builder.newAppender(STDOUT, "CONSOLE")
                .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                .add(layout);

        builder.add(appenderBuilder);
        builder.add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(appenderBuilder.getName())));

        // Apply logger-level overrides from system properties:
        applyLoggerOverrides(builder);

        return builder.build();
    }

    private static void applyLoggerOverrides(ConfigurationBuilder<BuiltConfiguration> builder) {
        Properties props = System.getProperties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(LOGGER_PROP_PREFIX)) {
                String loggerName = key.substring(LOGGER_PROP_PREFIX.length()).trim();
                String value = props.getProperty(key);
                Level level = levelOrDefault(value, null);
                if (loggerName.isEmpty() || level == null) continue;

                // Add or override this logger at the requested level
                builder.add(builder.newLogger(loggerName, level)
                        // keep additive so logs still reach the console unless user changes it
                        .addAttribute("additivity", true)
                        .add(builder.newAppenderRef(STDOUT)));
            }
        }
    }

    private static Level levelOrDefault(String maybeLevel, Level dflt) {
        if (maybeLevel == null) return dflt;
        try {
            return Level.valueOf(maybeLevel.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ignored) {
            return dflt;
        }
    }

    @Override
    public Configuration getConfiguration(final LoggerContext loggerContext, final ConfigurationSource source) {
        return getConfiguration(loggerContext, source.toString(), null);
    }

    @Override
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final String name, final URI configLocation) {
        ConfigurationBuilder<BuiltConfiguration> builder = newConfigurationBuilder();
        return createConfiguration(name, builder);
    }

    @Override
    protected String[] getSupportedTypes() {
        return SUPPORTED_TYPES;
    }
}
