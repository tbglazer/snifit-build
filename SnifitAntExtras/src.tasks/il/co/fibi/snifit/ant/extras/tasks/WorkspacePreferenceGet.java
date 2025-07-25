package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jdt.core.JavaCore;

public class WorkspacePreferenceGet extends FailOnErrorTask {
	private String preferenceType = null;

	private String preferenceQualifier = null;

	private String defaultValue = null;

	private String preferenceName = null;

	private IScopeContext[] preferenceScopes = null;

	private String propertyName = null;

	@Deprecated
	private boolean useEclipsePrefs = false;

	@Deprecated
	private boolean eclipsePrefsSetByAttribute = false;

	public void setPreferenceQualifier(String qualifier) {
		this.preferenceQualifier = qualifier;
	}

	public void setDefaultValue(String value) {
		this.defaultValue = value;
	}

	public void setPreferenceName(String value) {
		this.preferenceName = value;
	}

	public void setPropertyName(String value) {
		this.propertyName = value;
	}

	public void setPreferenceType(String type) {
		this.preferenceType = type;
	}

	@Deprecated
	public void setUseEclipsePrefs(boolean value) {
		this.useEclipsePrefs = value;
		this.eclipsePrefsSetByAttribute = true;
	}

	@SuppressWarnings("deprecation")
	@Override
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
					handleError(ResourceHandler.getString("WorkspacePreferenceGet.nullWorkspace"));
					return;
				}
				String pref = null;
				String preferenceKEY = null;
				if (!this.preferenceType.equalsIgnoreCase("webtoolsValidation")) {
					Preferences store = JavaCore.getPlugin().getPluginPreferences();
					preferenceKEY = "org.eclipse.jdt.core." + this.preferenceType + "." + this.preferenceName;
					pref = store.getString(preferenceKEY);
					if (pref == null || pref.equalsIgnoreCase("")) {
						String msg = ResourceHandler.getString("WorkspacePreferenceGet.logPreferences",
								(Object[]) new String[] { this.preferenceType, this.preferenceName });
						handleError(msg);
					}
				}
				getProject().setUserProperty(this.propertyName, pref);
				System.out.println(ResourceHandler.getString("WorkspacePreferenceGet.finished",
						(Object[]) new String[] { preferenceKEY, pref, this.propertyName }));
			} else {
				if (this.preferenceScopes == null) {
					this.preferenceScopes = new IScopeContext[] { (IScopeContext) new DefaultScope() };
				}
				if (this.preferenceQualifier == null) {
					this.preferenceQualifier = "";
				}
				String preferenceValue = PreferenceUtilities.getPreference(this.preferenceScopes,
						this.preferenceQualifier, this.preferenceName, this.defaultValue);
				if (preferenceValue != null) {
					if (this.propertyName != null) {
						getProject().setUserProperty(this.propertyName, preferenceValue);
					}
				}
			}
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (!this.useEclipsePrefs) {
			if (this.preferenceType == null) {
				String msg = ResourceHandler.getString("WorkspacePreferenceGet.missingPreferenceType",
						(Object[]) new String[] { "useEclipsePrefs", getProject().getName() });
				handleError(msg);
			}
			if (this.preferenceType.equalsIgnoreCase("compiler")) {
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
				handleError(
						ResourceHandler.getString("WorkspacePreferenceGet.unknownPreferenceType", this.preferenceType));
			}
		}
		if (this.preferenceName == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceGet.missingPreferenceName"));
		if (this.propertyName == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceGet.missingPropertyName"));
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
