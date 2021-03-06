package com.dslplatform.compiler.client.parameters;

import com.dslplatform.compiler.client.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public enum TempPath implements CompileParameter {
	INSTANCE;

	@Override
	public String getAlias() { return "temp"; }
	@Override
	public String getUsage() { return "path"; }

	private static final String CACHE_NAME = "temp_path_cache";

	public static File getTempProjectPath(final Context context) {
		return context.load(CACHE_NAME);
	}

	public static File getTempRootPath(final Context context) {
		final File temp = context.load(CACHE_NAME);
		return context.contains(INSTANCE) ? temp : temp.getParentFile();
	}

	private static boolean prepareSystemTempPath(final Context context) {
		try {
			final String projectLocation = System.getProperty("user.dir");
			final File parentFolder = new File(projectLocation).getParentFile();
			final String projectName = parentFolder == null
				? "root"
				: parentFolder.getName();
			final String rnd = UUID.randomUUID().toString();
			final File temp = File.createTempFile(rnd, ".dsl-test");
			final File dslPlatformPath = new File(temp.getParentFile().getAbsolutePath(), "DSL-Platform");
			final File path = new File(dslPlatformPath, projectName);
			if (!temp.delete()) {
				context.error("Unable to remove temporary created file: " + temp.getAbsolutePath());
				return false;
			}
			if (path.exists()) {
				Utils.deletePath(path);
			} else if (!path.mkdirs()) {
				context.error("Error creating temporary path in: " + path.getAbsolutePath());
				return false;
			}
			context.cache(CACHE_NAME, path);
			return true;
		} catch (IOException e) {
			context.error("Error preparing system temporary path.");
			context.error(e);
			return false;
		}
	}

	private static boolean prepareCustomPath(final Context context, final File path) {
		try {
			Utils.deletePath(path);
			context.cache(CACHE_NAME, path);
			return true;
		} catch (IOException e) {
			context.error("Error preparing custom temporary path.");
			context.error(e);
			return false;
		}
	}

	@Override
	public boolean check(final Context context) {
		if (context.contains(INSTANCE)) {
			final String value = context.get(INSTANCE);
			if (value != null && value.length() > 0) {
				final File path = new File(value);
				if (!path.exists()) {
					context.error("Temporary path provided (" + value + "), but doesn't exists. Please create it or use system path.");
					return false;
				}
				if (!path.isDirectory()) {
					context.error("Temporary path provided, but it's not a directory: " + value);
					return false;
				}
				final File[] tempFiles = path.listFiles();
				if (tempFiles != null && tempFiles.length > 0) {
					context.error("Temporary path contains files.");
					if (!context.canInteract()) {
						context.error("Please manage the path you have assigned as temporary.");
						return false;
					}
					final String delete = context.ask("Delete files in temporary path? (y/N):");
					if (!"y".equalsIgnoreCase(delete)) {
						return false;
					} else {
						return prepareCustomPath(context, path);
					}
				}
				context.cache(CACHE_NAME, path);
				return true;
			}
		}
		return prepareSystemTempPath(context);
	}

	@Override
	public void run(final Context context) {
	}

	@Override
	public String getShortDescription() {
		return "Use custom temporary path instead of system default";
	}

	@Override
	public String getDetailedDescription() {
		return "Files downloaded from DSL Platform will be stored to temporary path.\n" +
				"When unspecified /DSL-Platform folder in system default temporary path will be used.";
	}
}
