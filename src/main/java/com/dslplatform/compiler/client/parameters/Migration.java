package com.dslplatform.compiler.client.parameters;

import com.dslplatform.compiler.client.*;
import com.dslplatform.compiler.client.json.JsonObject;
import com.dslplatform.compiler.client.json.JsonValue;

import java.io.File;
import java.io.IOException;
import java.util.*;

public enum Migration implements CompileParameter {
	INSTANCE;

	@Override
	public String getAlias() { return "migration"; }
	@Override
	public String getUsage() { return null; }

	private final static String DESCRIPTION_START = "/*MIGRATION_DESCRIPTION";
	private final static String DESCRIPTION_END = "MIGRATION_DESCRIPTION*/";

	private static final String MIGRATION_FILE_NAME = "migration_file";

	public static File getMigrationFile(final Context context) {
		return context.load(MIGRATION_FILE_NAME);
	}

	public static String[] extractDescriptions(final String sql) throws ExitException {
		final int start = sql.indexOf(DESCRIPTION_START);
		final int end = sql.indexOf(DESCRIPTION_END);
		if (end > start) {
			return sql.substring(start + DESCRIPTION_START.length(), end).split("\n");
		}
		return new String[0];
	}

	@Override
	public boolean check(final Context context) {
		if (context.contains(INSTANCE)) {
			if (!context.contains(DbConnection.INSTANCE)) {
				context.error("Connection string is required to create a migration script");
				return false;
			}
			if (context.contains(SqlPath.INSTANCE)) {
				final String value = context.get(SqlPath.INSTANCE);
				if (value == null || value.length() == 0) {
					return true;
				}
				final File sqlPath = new File(value);
				if (!sqlPath.exists()) {
					context.error("Path for SQL migration script provided (" + value + ") but not found");
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public void run(final Context context) throws ExitException {
		if (context.contains(Migration.INSTANCE)) {
			final DbConnection.DatabaseInfo dbInfo = DbConnection.getDatabaseDslAndVersion(context);
			final String value = context.get(SqlPath.INSTANCE);
			final File path;
			if (!context.contains(SqlPath.INSTANCE) || value == null || value.length() == 0) {
				path = TempPath.getTempProjectPath(context);
			} else {
				path = new File(value);
			}
			if (!path.exists()) {
				context.error("Error accessing SQL path (" + path.getAbsolutePath() + ").");
				throw new ExitException();
			}
			final String script;
			if (context.contains(DslCompiler.INSTANCE)) {
				script = offlineMigration(context, dbInfo);
			} else {
				script = onlineMigration(context, dbInfo);
			}
			final File sqlFile = new File(path.getAbsolutePath(), "sql-migration-" + (new Date().getTime()) + ".sql");
			try {
				Utils.saveFile(sqlFile, script);
			} catch (IOException e) {
				context.error("Error saving migration script to " + sqlFile.getAbsolutePath());
				context.error(e);
				throw new ExitException();
			}
			context.show("Migration saved to " + sqlFile.getAbsolutePath());
			if (script.length() > 0) {
				final String[] descriptions = extractDescriptions(script);
				for (int i = 1; i < descriptions.length; i++) {
					context.log(descriptions[i]);
				}
			} else {
				context.show("No database changes detected.");
			}
			context.cache(MIGRATION_FILE_NAME, sqlFile);
		}
	}

	private String onlineMigration(final Context context, final DbConnection.DatabaseInfo dbInfo) throws ExitException {
		final Map<String, String> currentDsl = DslPath.getCurrentDsl(context);
		final String url =
				"Platform.svc/unmanaged/postgres-migration?version=" + dbInfo.compilerVersion
						+ "&postgres=" + dbInfo.postgresVersion;
		final JsonObject arg =
				new JsonObject()
						.add("Old", Utils.toJson(dbInfo.dsl))
						.add("New", Utils.toJson(currentDsl));
		context.show("Downloading SQL migration...");
		final Either<String> response = DslServer.put(url, context, arg);
		if (!response.isSuccess()) {
			context.error("Error creating online SQL migration:");
			context.error(response.whyNot());
			throw new ExitException();
		}
		return response.get().startsWith("\"") && response.get().endsWith("\"")
				? JsonValue.readFrom(response.get()).asString()
				: response.get();
	}

	private String offlineMigration(final Context context, final DbConnection.DatabaseInfo dbInfo) throws ExitException {
		final List<File> currentDsl = DslPath.getDslPaths(context);
		context.show("Creating SQL migration...");
		final Either<String> migration = DslCompiler.migration(context, dbInfo.postgresVersion, currentDsl);
		if (!migration.isSuccess()) {
			context.error("Error creating local SQL migration:");
			context.error(migration.whyNot());
			throw new ExitException();
		}
		return migration.get();
	}

	@Override
	public String getShortDescription() {
		return "Create SQL migration from previous DSL to the current one";
	}

	@Override
	public String getDetailedDescription() {
		return "DSL Platform will compare previously applied DSL with the current one and provide a migration SQL script.\n" +
				"Developer can inspect migration (although it contains a lot of boilerplate due to Postgres dependency graph),\n" +
				"to check if the requested migration matches what he had in mind.\n" +
				"Every migration contains description of the important changes to the database.\n" +
				"\n" +
				"Postgres migrations are transactional due to Transactional DDL feature of the Postgres.\n" +
				"\n" +
				"While for most migrations ownership of the database is sufficient, some require superuser access (Enum changes, strange primary keys, ...).";
	}
}
