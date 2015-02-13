package com.dslplatform.compiler.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class Shell {

    private final static Logger innerLogger = LoggerFactory.getLogger(Shell.class);

    public static boolean makeExecutable(final File file) {
        return makeExecutable(file, innerLogger);
    }

    public static boolean makeExecutable(final File file, final  Logger logger) {
        String runScript = "chmod u+x " + file.getPath();
        try {
            return execute(runScript, logger);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean execute(final String runScript) throws IOException, InterruptedException {
        return execute(runScript, innerLogger);
    }

    public static boolean execute(final String runScript, final Logger logger) throws IOException, InterruptedException {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(runScript);
            final Process finalProcess = process;

            /* Consume output stream */
            new Thread() {
                public void run() {
                    final InputStream in = finalProcess.getInputStream();
                    try {
                        final InputStreamReader inputStreamReader = new InputStreamReader(in);
                        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = bufferedReader.readLine()) != null) logger.trace(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        close(in);
                    }
                }
            }.start();

            /* Consume error stream */
            new Thread() {
                public void run() {
                    final InputStream in = finalProcess.getErrorStream();
                    try {
                        final InputStreamReader inputStreamReader = new InputStreamReader(in);
                        final BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String line;
                        while ((line = bufferedReader.readLine()) != null) logger.error(line);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        close(in);
                    }
                }
            }.start();

            process.waitFor();

            return true;
        } finally {
            if (process != null) {
                try {
                    process.getOutputStream().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                process.destroy();
            }
        }

    }

    private static void close(final InputStream inputStream) {
        try {
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

