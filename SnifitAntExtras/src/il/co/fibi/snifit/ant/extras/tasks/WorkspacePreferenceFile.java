package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

public class WorkspacePreferenceFile extends FailOnErrorTask {
	private String preferenceFileName = null;

	@Deprecated
	private boolean useEclipsePrefs = false;

	@Deprecated
	private boolean eclipsePrefsSetByAttribute = false;

	private boolean overwrite = false;

	private File preferenceFile = null;

	@Deprecated
	public void setUseEclipsePrefs(boolean value) {
		this.useEclipsePrefs = value;
		this.eclipsePrefsSetByAttribute = true;
	}

	public void setPreferenceFileName(String name) {
		this.preferenceFileName = name;
	}

	public void setOverwrite(boolean value) {
		this.overwrite = value;
	}

	@SuppressWarnings("deprecation")
	public void execute() throws BuildException {
	    System.out.println("[DEBUG] in execute(), getProject() = " + getProject());
	    System.out.println("Ant Task classloader = " + Task.class.getClassLoader());
	    System.out.println("My class classloader = " + this.getClass().getClassLoader());
	    System.out.println("instanceof Task? = " + (this instanceof Task));
	    System.out.println("[DEBUG] this.getClass(): " + this.getClass());
	    System.out.println("[DEBUG] this.getClass().getSuperclass(): " + this.getClass().getSuperclass());
	    System.out.println("[DEBUG] getProject() == null: " + (getProject() == null));
	    
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		validateAttributes(monitor);
		try {
			this.useEclipsePrefs = true; //determineUseOfEclipsePrefs(this.useEclipsePrefs, this.eclipsePrefsSetByAttribute);
			if (this.useEclipsePrefs) {
				IScopeContext[] allScopes = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE };
				IPreferenceFilter[] filters = PreferenceUtilities.createPreferenceFilter(allScopes);
				PreferenceUtilities.readAndApplyPreferences(this.preferenceFile, filters);
			} else {
				BufferedReader rdr = getFileReader(this.preferenceFileName);
				String line = readLine(rdr);
				String runtimeTypeId = null;
				String targetLocation = null;
				String targetName = null;
				String targetId = targetName;
				while (line != null) {
					int i = line.indexOf('.');
					int j = line.indexOf('=');
					WorkspacePreferenceSet task = null;
					if (i > 0 && j > i) {
						String type = line.substring(0, i);
						String key = line.substring(i + 1, j);
						String value = line.substring(j + 1);
						if (type.equalsIgnoreCase("antbuild")) {
							System.out.println(ResourceHandler.getString("WorkspacePreferenceFile.keyValue",
									(Object[]) new String[] { key, value }));
							if (key.equalsIgnoreCase("overwrite")) {
								if (value.equalsIgnoreCase("true")) {
									setOverwrite(true);
								} else if (value.equalsIgnoreCase("true")) {
									setOverwrite(false);
								} else {
									handleError(ResourceHandler
											.getString("WorkspacePreferenceFile.invalidOverwriteValue", line));
								}
							} else if (key.equalsIgnoreCase("failonerror")) {
								if (value.equalsIgnoreCase("true")) {
									setFailOnError(true);
								} else if (value.equalsIgnoreCase("false")) {
									setFailOnError(false);
								} else {
									handleError(ResourceHandler.getString("WorkspacePreferenceFile.invalidFailOnError",
											line));
								}
							} else {
								handleError(ResourceHandler.getString("WorkspacePreferenceFile.unknownBuildPreference",
										(Object[]) new String[] { key, line }));
							}
						} else if (type.equalsIgnoreCase("targetRuntime")) {
							if (key.equalsIgnoreCase("runtimeTypeId")) {
								runtimeTypeId = value;
								getProject().setUserProperty("RuntimeTargetTypeId", value);
							} else if (key.equalsIgnoreCase("targetLocation")) {
								targetLocation = value;
								getProject().setUserProperty("RuntimeTargetLocation", value);
							} else if (key.equalsIgnoreCase("targetId")) {
								targetId = value;
								System.out.println(
										ResourceHandler.getString("WorkspacePreferenceFile.setTargetId", targetId));
							} else if (key.equalsIgnoreCase("targetName")) {
								targetName = value;
								getProject().setUserProperty("RuntimeTargetName", value);
							} else {
								String msg = ResourceHandler.getString("WorkspacePreferenceFile.unknownTargetRuntime",
										(Object[]) new String[] { key, line });
								handleError(msg);
							}
							if (targetName != null) {
								if (runtimeTypeId == null || targetLocation == null) {
									String msg = ResourceHandler
											.getString("WorkspacePreferenceFile.missingRuntimeTypeAndTarget");
									handleError(msg);
								}
								TargetRuntimeCreate targetRT = new TargetRuntimeCreate();
								targetRT.setProject(getProject());
								targetRT.setRuntimeTypeId(runtimeTypeId);
								targetRT.setTargetLocation(targetLocation);
								targetRT.setTargetName(targetName);
								if (targetId != null) {
									targetRT.setTargetId(targetId);
								} else {
									targetRT.setTargetId(targetName);
								}
								targetRT.execute();
								runtimeTypeId = null;
								targetLocation = null;
								targetName = null;
								targetId = null;
							}
						} else if (type.equalsIgnoreCase("compiler")) {
							task = new WorkspacePreferenceSet();
							type = "compiler";
						} else if (type.equalsIgnoreCase("builder")) {
							task = new WorkspacePreferenceSet();
							type = "builder";
						} else if (type.equalsIgnoreCase("classpath")) {
							task = new WorkspacePreferenceSet();
							type = "classpath";
						} else if (type.equalsIgnoreCase("classpathVariable")) {
							task = new WorkspacePreferenceSet();
							type = "classpathVariable";
						} else if (type.equalsIgnoreCase("webtoolsValidation")) {
							task = new WorkspacePreferenceSet();
							type = "webtoolsValidation";
						} else {
							task = new WorkspacePreferenceSet();
							task.setPreferenceType(type);
						}
						if (task != null) {
							task.setProject(getProject());
							task.setPreferenceType(type);
							task.setPreferenceName(key);
							task.setPreferenceValue(value);
							task.setOverwrite(this.overwrite);
							task.setFailOnError(isFailOnError());
							task.execute();
						}
					} else {
						String msg = ResourceHandler.getString("WorkspacePreferenceFile.missingSeparator", line);
						handleError(msg);
					}
					line = readLine(rdr);
				}
				closeRdr(rdr);
			}
		} catch (CoreException coreEx) {
			handleError(coreEx.getStatus().getMessage(), (Exception) coreEx);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	@Deprecated
	private static String readLine(BufferedReader rdr) {
		if (rdr == null)
			return null;
		String txt = "";
		while (txt.equals("")) {
			try {
				txt = rdr.readLine();
				if (txt == null)
					return null;
				txt = txt.trim();
				if (txt.startsWith("#")) {
					System.out.println(ResourceHandler.getString("WorkspacePreferenceFile.comment", txt));
					txt = "";
				}
			} catch (IOException iOException) {
			}
		}
		return txt;
	}

	@Deprecated
	public static BufferedReader getFileReader(String filename) {
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(new File(filename)));
		} catch (IOException iOException) {
			System.out.println(ResourceHandler.getString("WorkspacePreferenceFile.ioException", filename));
			return null;
		}
		return rdr;
	}

	@Deprecated
	private static void closeRdr(BufferedReader rdr) {
		try {
			if (rdr != null)
				rdr.close();
		} catch (IOException e) {
			System.out
					.println(ResourceHandler.getString("WorkspacePreferenceFile.closeBufferException", e.getMessage()));
		}
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.preferenceFileName == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.missingPreferenceFileName"));
		this.preferenceFile = new File(this.preferenceFileName);
		if (!this.preferenceFile.isFile())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileIsNotAFile",
					this.preferenceFile.getAbsolutePath()));
		if (!this.preferenceFile.exists())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileDoesNotExist",
					this.preferenceFile.getAbsolutePath()));
		if (!this.preferenceFile.canRead())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileCannotBeOpened",
					this.preferenceFile.getAbsolutePath()));
	}

	@Deprecated
	private boolean determineUseOfEclipsePrefs(boolean usingEclipsePrefs, boolean setByAttribute) {
		if (setByAttribute)
			return usingEclipsePrefs;
		boolean globalValue = false;
		String enableEclipsePreferenceSupportProperty = getProject().getProperty("useEclipsePrefs");
		if (enableEclipsePreferenceSupportProperty != null)
			globalValue = Boolean.valueOf(enableEclipsePreferenceSupportProperty).booleanValue();
		return globalValue;
	}
}
