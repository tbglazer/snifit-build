package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.ant.extras.HeadlessWorkspaceSettingsHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.osgi.service.prefs.BackingStoreException;

public class PreferenceUtilities {
	public static final String ECLIPSE_PREFERENCE_SUPPORT = "useEclipsePrefs";

	private static final String AUTO_BUILD_PREFERENCE_KEY = "/instance/org.eclipse.core.resources/description.autobuilding";

	private static final String MAX_FILE_STATE_SIZE_PREFERENCE_KEY = "/instance/org.eclipse.core.resources/description.maxfilestatesize";

	private static final String BUNDLE_ID = "com.ibm.etools.j2ee.ant";

	public static final IPreferenceFilter[] createPreferenceFilter(IScopeContext[] scopes) {
		final String[] scopeNames = new String[scopes.length];
		int i = 0;
		while (i < scopes.length) {
			scopeNames[i] = scopes[i].getName();
			i++;
		}
		IPreferenceFilter[] filters = new IPreferenceFilter[1];
		filters[0] = new IPreferenceFilter() {
			@Override
			public String[] getScopes() {
				return scopeNames;
			}

			@Override
			public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
				return null;
			}
		};
		return filters;
	}

	public static final boolean readAndApplyPreferences(File preferenceFile, IPreferenceFilter[] preferenceFilters)
			throws CoreException {
		boolean successfulImport = false;
		FileInputStream fis = null;
		if (preferenceFilters != null && preferenceFilters.length > 0) {
			File prefFile = massagePreferenceFile(preferenceFile);
			try {
				fis = new FileInputStream(prefFile);
				IExportedPreferences prefs = Platform.getPreferencesService().readPreferences(fis);
				Platform.getPreferencesService().applyPreferences((IEclipsePreferences) prefs, preferenceFilters);
				Platform.getPreferencesService().getRootNode().flush();
				successfulImport = true;
			} catch (BackingStoreException backingStoreEx) {
				CoreException coreEx = getCoreException(4, (Exception) backingStoreEx);
				throw coreEx;
			} catch (FileNotFoundException fileNotFoundEx) {
				CoreException coreEx = getCoreException(4, fileNotFoundEx);
				throw coreEx;
			} finally {
				if (fis != null) {
					try {
						fis.close();
					} catch (IOException ioEx) {
						CoreException coreEx = getCoreException(4, ioEx);
						throw coreEx;
					}
					if (prefFile != null && !prefFile.equals(preferenceFile))
						prefFile.delete();
				}
			}
		} else {
			CoreException coreEx = getCoreException(4,
					"No filters were specified while reading the file '" + preferenceFile, null);
			throw coreEx;
		}
		return successfulImport;
	}

	private static final File massagePreferenceFile(File preferenceFile) throws CoreException {
		File newPrefFile;
		Properties preferences = loadPreferenceFile(preferenceFile);
		if (!preferences.isEmpty()) {
			boolean preferencesChanged = false;
			String autoBuild = preferences.getProperty("/instance/org.eclipse.core.resources/description.autobuilding");
			if (autoBuild != null) {
				preferences.remove("/instance/org.eclipse.core.resources/description.autobuilding");
				HeadlessWorkspaceSettingsHelper.userEnabledAutoBuild = true;
				HeadlessWorkspaceSettingsHelper.userEnabledAutoBuildValue = Boolean.valueOf(autoBuild).booleanValue();
				preferencesChanged = true;
			}
			String maxFileStateSize = preferences
					.getProperty("/instance/org.eclipse.core.resources/description.maxfilestatesize");
			if (maxFileStateSize != null) {
				preferences.remove("/instance/org.eclipse.core.resources/description.maxfilestatesize");
				HeadlessWorkspaceSettingsHelper.userEnabledMaxFileStateSize = true;
				HeadlessWorkspaceSettingsHelper.userEnabledMaxFileStateSizeValue = Long.valueOf(maxFileStateSize)
						.longValue();
				preferencesChanged = true;
			}
			if (preferencesChanged) {
				IPath stateLocation = AntCorePlugin.getPlugin().getStateLocation().append("preferenceFile.epf");
				newPrefFile = stateLocation.toFile();
				storePreferenceFile(preferences, newPrefFile);
			} else {
				newPrefFile = preferenceFile;
			}
		} else {
			newPrefFile = preferenceFile;
		}
		return newPrefFile;
	}

	private static final Properties loadPreferenceFile(File preferenceFile) throws CoreException {
		Properties preferences = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(preferenceFile);
			preferences.load(fis);
		} catch (FileNotFoundException fileNotFoundEx) {
			CoreException coreEx = getCoreException(4, fileNotFoundEx);
			throw coreEx;
		} catch (InvalidPropertiesFormatException invalidPropFormatEx) {
			CoreException coreEx = getCoreException(4, invalidPropFormatEx);
			throw coreEx;
		} catch (IOException ioEx) {
			CoreException coreEx = getCoreException(4, ioEx);
			throw coreEx;
		} finally {
			if (fis != null)
				try {
					fis.close();
				} catch (IOException ioEx) {
					CoreException coreEx = getCoreException(4, ioEx);
					throw coreEx;
				}
		}
		return preferences;
	}

	private static final void storePreferenceFile(Properties preferences, File preferenceFile) throws CoreException {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(preferenceFile);
			preferences.store(fos, (String) null);
		} catch (FileNotFoundException fileNotFoundEx) {
			CoreException coreEx = getCoreException(4, fileNotFoundEx);
			throw coreEx;
		} catch (IOException ioEx) {
			CoreException coreEx = getCoreException(4, ioEx);
			throw coreEx;
		} finally {
			if (fos != null)
				try {
					fos.close();
				} catch (IOException ioEx) {
					CoreException coreEx = getCoreException(4, ioEx);
					throw coreEx;
				}
		}
	}

	public static final String getPreference(IScopeContext[] scopeContexts, String qualifier, String key,
			String defaultValue) {
		return Platform.getPreferencesService().getString(qualifier, key, defaultValue, scopeContexts);
	}

	public static final boolean setPreference(IScopeContext scopeContext, String qualifier, String key, String newValue)
			throws CoreException {
		boolean successfulSet = false;
		if (key != null) {
			StringBuffer fullKey = new StringBuffer("/");
			fullKey.append(scopeContext.getName());
			fullKey.append("/");
			fullKey.append(qualifier);
			fullKey.append("/");
			fullKey.append(key);
			String fullKeyString = fullKey.toString();
			if ("/instance/org.eclipse.core.resources/description.autobuilding".equals(fullKeyString)) {
				HeadlessWorkspaceSettingsHelper.userEnabledAutoBuild = true;
				HeadlessWorkspaceSettingsHelper.userEnabledAutoBuildValue = Boolean.valueOf(newValue).booleanValue();
			} else if ("/instance/org.eclipse.core.resources/description.maxfilestatesize".equals(fullKeyString)) {
				HeadlessWorkspaceSettingsHelper.userEnabledMaxFileStateSize = true;
				HeadlessWorkspaceSettingsHelper.userEnabledMaxFileStateSizeValue = Long.valueOf(newValue).longValue();
			} else {
				IEclipsePreferences iEclipsePreferences = scopeContext.getNode(qualifier);
				if (iEclipsePreferences != null) {
					iEclipsePreferences.put(key, newValue);
					try {
						iEclipsePreferences.flush();
						successfulSet = true;
					} catch (BackingStoreException backingStoreEx) {
						CoreException coreEx = getCoreException(4, (Exception) backingStoreEx);
						throw coreEx;
					}
				}
			}
		}
		return successfulSet;
	}

	private static final CoreException getCoreException(int severity, Exception ex) {
		return getCoreException(severity, ex.getMessage(), ex);
	}

	private static final CoreException getCoreException(int severity, String message, Exception ex) {
		Status status = new Status(severity, "com.ibm.etools.j2ee.ant", message, ex);
		return new CoreException((IStatus) status);
	}
}
