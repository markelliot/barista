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

    private static final String STDOUT = "stdout";
    private static final String[] SUPPORTED_TYPES = {"*"};

    static BuiltConfiguration createConfiguration(
            String name, ConfigurationBuilder<BuiltConfiguration> builder) {
        builder.setStatusLevel(Level.ERROR);
        builder.setConfigurationName(name);

        LayoutComponentBuilder layout =
                builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%d [%t] %level: %msg%n%throwable");

        AppenderComponentBuilder appenderBuilder =
                builder.newAppender(STDOUT, "CONSOLE")
                        .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                        .add(layout);

        builder.add(appenderBuilder);
        builder.add(
                builder.newRootLogger(Level.INFO)
                        .add(builder.newAppenderRef(appenderBuilder.getName())));

        return builder.build();
    }

    @Override
    public Configuration getConfiguration(
            final LoggerContext loggerContext, final ConfigurationSource source) {
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
