package barista;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.api.FilterComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LayoutComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.LoggerComponentBuilder;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;

final class Logging {
    private static final String STDOUT = "stdout";

    public static void configure() {
        ConfigurationBuilder<BuiltConfiguration> builder =
                ConfigurationBuilderFactory.newConfigurationBuilder()
                        .setStatusLevel(Level.ERROR)
                        .setConfigurationName("barista");

        LayoutComponentBuilder layout =
                builder.newLayout("PatternLayout")
                        .addAttribute("pattern", "%d [%t] %-5level: %msg%n%throwable");
        FilterComponentBuilder filter =
                builder.newFilter("MarkerFilter", Result.DENY, Result.NEUTRAL)
                        .addAttribute("marker", "FLOW");

        AppenderComponentBuilder appenderBuilder =
                builder.newAppender(STDOUT, "CONSOLE")
                        .addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT)
                        .add(layout)
                        .add(filter);

        LoggerComponentBuilder logger =
                builder.newLogger("org.apache.logging.log4j", Level.DEBUG)
                        .add(builder.newAppenderRef(STDOUT))
                        .addAttribute("additivity", false);

        builder.add(appenderBuilder)
                .add(logger)
                .add(builder.newRootLogger(Level.INFO).add(builder.newAppenderRef(STDOUT)));

        Configurator.initialize(builder.build());
        // TODO(markelliot): we may want to stop the logging context on server shutdown
    }
}
