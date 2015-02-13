package com.dslplatform.compiler.client.cmdline;

import com.dslplatform.compiler.client.api.config.PropertyLoader;
import com.dslplatform.compiler.client.api.config.StreamLoader;
import com.dslplatform.compiler.client.cmdline.parser.Arguments;
import com.dslplatform.compiler.client.cmdline.parser.ArgumentsReader;
import com.dslplatform.compiler.client.cmdline.parser.ArgumentsValidator;
import com.dslplatform.compiler.client.cmdline.parser.CachingArgumentsProxy;
import com.dslplatform.compiler.client.io.PathExpander;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Main {

    public static void main(String... argv) {
        //System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", arguments.getLoggingLevel().level);
        if (argv.length == 0) {
            System.out.println("Not enough arguments to process the command!");
            printHelp(null);
            return;
        }
        final Logger logger = LoggerFactory.getLogger("dsl-clc");

        final PropertyLoader propertyLoader = new PropertyLoader(logger,
                new StreamLoader(logger, new PathExpander(logger)));

        final Arguments arguments;
        try {
            arguments = new CachingArgumentsProxy(
                    logger,
                    new ArgumentsValidator(logger,
                            new ArgumentsReader(logger, propertyLoader).readArguments(argv)));

            final ArgumentProcessor argumentProcessor = new ActionDefinition(logger, arguments);
            argumentProcessor.process();
        } catch (Exception e) {
            printHelp(e);
        }
    }

    public static void printHelp(Exception e) {
        printError(e);

        final InputStreamReader manis = new InputStreamReader(Main.class.getResourceAsStream("/man"));
        BufferedReader bufferedReader = new BufferedReader(manis);

        try {
            String line;
            while ( (line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException ie) {
            printError(ie);
        } finally {
            try {
                manis.close();
            } catch (IOException e1) {
                printError(e1);
            }
        }
    }

    private static void printError(Exception e) {
        if (e != null) {
            System.out.print("An error has occurred while processing your parameters: ");
            System.out.println(e.getMessage());
            System.out.println();
        }
    }
}
