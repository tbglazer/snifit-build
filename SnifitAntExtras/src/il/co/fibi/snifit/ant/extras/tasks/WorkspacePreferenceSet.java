package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.JavaCore;

public class WorkspacePreferenceSet extends FailOnErrorTask {
	private boolean overwrite = true;

	private String preferenceType = null;

	private String preferenceName = null;

	private String preferenceValue = null;

	private String preferenceQualifier = null;

	private IScopeContext preferenceScope = null;

	@Deprecated
	private boolean useEclipsePrefs = false;

	@Deprecated
	private boolean eclipsePrefsSetByAttribute = false;

	public void setOverwrite(boolean b) {
		this.overwrite = b;
	}

	public void setPreferenceName(String name) {
		this.preferenceName = name;
	}

	public void setPreferenceValue(String value) {
		this.preferenceValue = value;
	}

	public void setPreferenceType(String value) {
		this.preferenceType = value;
	}

	public void setPreferenceQualifier(String qualifer) {
		this.preferenceQualifier = qualifer;
	}

	public void setPreferenceScope(String scope) {
		if (scope != null)
			if (scope.equalsIgnoreCase("instance")) {
				this.preferenceScope = InstanceScope.INSTANCE;
			} else if (scope.equalsIgnoreCase("configuration")) {
				this.preferenceScope = ConfigurationScope.INSTANCE;
			} else {
				this.preferenceScope = DefaultScope.INSTANCE;
			}
	}

	@Deprecated
	public void setUseEclipsePrefs(boolean value) {
		this.useEclipsePrefs = value;
		this.eclipsePrefsSetByAttribute = true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		this.useEclipsePrefs = determineUseOfEclipsePrefs(this.useEclipsePrefs, this.eclipsePrefsSetByAttribute);
		validateAttributes(monitor);
		try {
			if (!this.useEclipsePrefs) {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				if (workspace == null) {
					handleError(ResourceHandler.getString("WorkspacePreferenceSet.nullWorkspae"));
					return;
				}
				String pref = null;
				String preferenceKEY = null;
				if (!this.preferenceType.equalsIgnoreCase("webtoolsValidation")) {
					Preferences store = JavaCore.getPlugin().getPluginPreferences();
					preferenceKEY = "org.eclipse.jdt.core." + this.preferenceType + "." + this.preferenceName;
					pref = store.getString(preferenceKEY);
					if (pref == null || pref.equalsIgnoreCase("") || this.overwrite) {
						store.setValue(preferenceKEY, this.preferenceValue);
					} else if (isFailOnError()) {
						handleError(ResourceHandler.getString("WorkspacePreferenceSet.overwriteFailed",
								(Object[]) new String[] { this.preferenceType, this.preferenceName,
										this.preferenceValue }));
					} else {
						System.out.println(ResourceHandler.getString("WorkspacePreferenceSet.overwriteSkipped",
								(Object[]) new String[] { this.preferenceType, this.preferenceName,
										this.preferenceValue, pref }));
						this.preferenceValue = pref;
					}
				}
				System.out.println(ResourceHandler.getString("WorkspacePreferenceSet.finished",
						(Object[]) new String[] { preferenceKEY, this.preferenceValue }));
			} else {
				if (this.preferenceScope == null) {
					this.preferenceScope = (IScopeContext) new DefaultScope();
				}
				if (this.preferenceQualifier == null) {
					this.preferenceQualifier = "";
				}
				PreferenceUtilities.setPreference(this.preferenceScope, this.preferenceQualifier,
					this.preferenceName, this.preferenceValue);
			}
		} catch (CoreException coreEx) {
			handleError(ResourceHandler.getString("Common.coreException", coreEx.getStatus().getMessage()),
					(Exception) coreEx);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (!this.useEclipsePrefs)
			if (this.preferenceType == null) {
				handleError(ResourceHandler.getString("WorkspacePreferenceSet.missingPreferenceType",
						(Object[]) new String[] { "useEclipsePrefs", getProject().getName() }));
			} else if (this.preferenceType.equalsIgnoreCase("compiler")) {
				this.preferenceType = "compiler";
			} else if (this.preferenceType.equalsIgnoreCase("builder")) {
				this.preferenceType = "builder";
			} else if (this.preferenceType.equalsIgnoreCase("classpath")) {
				this.preferenceType = "classpath";
			} else if (this.preferenceType.equalsIgnoreCase("classpathVariable")) {
				this.preferenceType = "classpathVariable";
			} else if (this.preferenceType.equalsIgnoreCase("webtoolsValidation")) {
				this.preferenceType = "webtoolsValidation";
			} else {
				handleError(ResourceHandler.getString("WorkspacePreferenceSet.unknownPreferenceType", this.preferenceType));
			}
		if (this.preferenceName == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceSet.missingPreferenceName"));
		if (this.preferenceValue == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceSet.missingPreferenceValue"));
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
