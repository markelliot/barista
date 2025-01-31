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

import static com.markelliot.barista.Logging.DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

public final class LoggingTests {

    private String originalLoggingEnvValue = null;

    @BeforeEach
    void saveOriginalLoggingPropertyValue() {
        originalLoggingEnvValue = System.getProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY);
        System.clearProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY);
    }

    @AfterEach
    void restoreOriginalLoggingPropertyValue() {
        if (originalLoggingEnvValue == null) {
            System.clearProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY);
        } else {
            System.setProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY, originalLoggingEnvValue);
        }
    }

    private static void validateBaristaDefaultLoggerConfiguration(Configuration config) {
        assertEquals(1, config.getAppenders().size());
        Appender stdoutAppender = config.getAppender("stdout");
        assertInstanceOf(PatternLayout.class, stdoutAppender.getLayout());
        assertEquals(Logging.PATTERN, ((PatternLayout) stdoutAppender.getLayout()).getConversionPattern());
    }

    private static Configuration reloadLog4jConfiguration() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        context.reconfigure();
        return context.getConfiguration();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void confirmBaristaDefaultLoggingIfPropertyUnset(String systemPropertyValue) {
        if (systemPropertyValue == null) {
            System.clearProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY);
        } else {
            System.setProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY, systemPropertyValue);
        }
        Configuration config = reloadLog4jConfiguration();
        validateBaristaDefaultLoggerConfiguration(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"false", "0", "no", "off", "disabled"})
    void confirmBaristaDefaultLoggingIfPropertySetFalsey(String systemPropertyValue) {
        System.setProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY, systemPropertyValue);
        Configuration config = reloadLog4jConfiguration();
        validateBaristaDefaultLoggerConfiguration(config);
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "1", "yes", "on", "enabled"})
    void confirmLog4jDefaultLoggingIfPropertySetTruth(String systemPropertyValue) {
        System.setProperty(DISABLE_LOGGING_DEFAULTS_SYSTEM_PROPERTY, systemPropertyValue);
        Configuration config = reloadLog4jConfiguration();
        assertInstanceOf(DefaultConfiguration.class, config);
    }
}
