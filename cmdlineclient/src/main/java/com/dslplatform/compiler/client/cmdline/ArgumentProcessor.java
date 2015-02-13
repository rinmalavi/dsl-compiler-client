package com.dslplatform.compiler.client.cmdline;

import com.dslplatform.compiler.client.response.GenerateMigrationSQLResponse;

import java.io.IOException;

public interface ArgumentProcessor {

    /**
     * iterates though actions and performs them.
     * @throws IOException
     */
    public void process() throws IOException;

    /**
     * Informs if current DSL is syntactically correct.
     */
    public boolean parseDSL() throws IOException;

    /**
     * Outputs last dsl to the standard output.
     */
    public void lastDSL() throws IOException;

    /**
     * Informs a user of the changes made to the DSL.
     */
    public void getChanges() throws IOException;

    /**
     * Upgrades the managed project with a given dsl.
     *
     * @return true if successful
     */
    public boolean upgrade() throws IOException;

    /**
     * Generates client source for connecting to the managed revenj instance
     */
    public boolean generateSources() throws IOException;

    /**
     * Requests for Unmanaged source.
     *
     * @return is operation successful
     */
    public boolean unmanagedSource() throws IOException;

    /**
     * Compiles C# sources provided at {@value }
     *
     * @return is operation successful
     */
    public boolean compileCSServer() throws IOException;

    /**
     * Requests a migration based on the last migration in the provided database at the moment, or null if database is new, and the dsl provided in the parameters.
     * Will output migration to a file.
     *
     * @return migration or null if failed.
     */
    public GenerateMigrationSQLResponse sqlMigration() throws IOException;

    /**
     * Applies a migration sql to the database
     * migrationSQL is read from the disk if existing, otherwise user is prompted to request it.
     */
    public boolean upgradeUnmanagedDatabase() throws IOException;

    /**
     * Aggregation of all tasks will perform following:
     * Parse and diff DSL - display information to user
     * <p>
     * Get Migration SQL - prompt user should it continue in case migration is destructive
     * Apply migration SQL
     * <p>
     * Get CS Sources and compile them.
     * <p>
     * Deploy to mono service.
     */
    public boolean deployUnmanagedServer() throws IOException;
}
