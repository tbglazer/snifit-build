package il.co.fibi.snifit.ant.extras;

import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jst.jsp.core.internal.java.search.JSPIndexManager;
import org.eclipse.osgi.util.NLS;

public class HeadlessWorkspaceSettingsHelper {
	public static boolean userEnabledAutoBuild = false;

	public static boolean userEnabledAutoBuildValue = false;

	public static boolean userEnabledMaxFileStateSize = false;

	public static long userEnabledMaxFileStateSizeValue = -1L;

	private final IEclipsePreferences debugPreferenceNode;

	private final IWorkspace workspace;

	private IWorkspaceDescription wd = null;

	private boolean originalAutoBuildEnabledValue = true;

	private boolean originalStatusHandlersEnabledValue = true;

	private long originalMaxFileStateSizeValue = 1000000L;

	private static final String name = "HeadlessWorkspaceSettings: ";

	public HeadlessWorkspaceSettingsHelper() {
		this.workspace = ResourcesPlugin.getWorkspace();
		this.wd = this.workspace.getDescription();
		this.debugPreferenceNode = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
		saveWorkspaceSettings();
		boolean disableIndexers = Boolean.getBoolean("DISABLE_WORKSPACE_INDEXERS");
		if (disableIndexers)
			shutdownJavaIndexer();
	}

	@SuppressWarnings("restriction")
	private void saveWorkspaceSettings() {
		this.originalMaxFileStateSizeValue = this.wd.getMaxFileStateSize();
		this.originalAutoBuildEnabledValue = this.wd.isAutoBuilding();
		System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_INITIAL, new Object[] { name,
				Boolean.toString(this.wd.isAutoBuilding()), String.valueOf(this.wd.getMaxFileStateSize()) }));
		this.originalStatusHandlersEnabledValue = this.debugPreferenceNode
				.getBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, true);
		if (this.originalStatusHandlersEnabledValue)
			this.debugPreferenceNode.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false);
		try {
			if (this.originalAutoBuildEnabledValue)
				this.wd.setAutoBuilding(false);
			if (this.originalMaxFileStateSizeValue >= 0L)
				this.wd.setMaxFileStateSize(-1L);
			if (this.originalAutoBuildEnabledValue || this.originalMaxFileStateSizeValue > 0L)
				this.workspace.setDescription(this.wd);
			System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_TEMP, new Object[] { name,
					Boolean.toString(this.wd.isAutoBuilding()), String.valueOf(this.wd.getMaxFileStateSize()) }));
		} catch (CoreException exc) {
			System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_SAVE_FAILED,
					new Object[] { name, exc.getMessage() }));
			this.wd = null;
		}
	}

	@SuppressWarnings("restriction")
	public void restore() {
		if (this.wd == null) {
			System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_PREVIOUS_FAILED_INIT, name));
			return;
		}
		try {
			this.debugPreferenceNode.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS,
					this.originalStatusHandlersEnabledValue);
			boolean workspaceChanged = false;
			if (userEnabledAutoBuild) {
				this.wd.setAutoBuilding(userEnabledAutoBuildValue);
				workspaceChanged = true;
			} else if (this.originalAutoBuildEnabledValue) {
				this.wd.setAutoBuilding(this.originalAutoBuildEnabledValue);
				workspaceChanged = true;
			}
			if (userEnabledMaxFileStateSize) {
				this.wd.setMaxFileStateSize(userEnabledMaxFileStateSizeValue);
				workspaceChanged = true;
			} else if (this.originalMaxFileStateSizeValue >= 0L) {
				this.wd.setMaxFileStateSize(this.originalMaxFileStateSizeValue);
				workspaceChanged = true;
			}
			if (workspaceChanged)
				this.workspace.setDescription(this.wd);
			System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_RESTORED_AUTO_BUILD,
					new String[] { name, Boolean.toString(this.wd.isAutoBuilding()),
							String.valueOf(this.wd.getMaxFileStateSize()) }));
		} catch (CoreException exc) {
			System.out.println(NLS.bind(NLSMessageConstants.HEADLESS_WORKSPACE_SETTINGS_RESTORE_FAILED,
					new String[] { name, exc.getMessage() }));
		}
	}

	@SuppressWarnings("restriction")
	private void shutdownJavaIndexer() {
		Runnable r = () -> {
			try {
				JavaModelManager.getIndexManager().shutdown();
				JSPIndexManager.getDefault().stop();
				JavaModelManager.getJavaModelManager().shutdown();
			} catch (Exception e) {
			}
		};
		(new Thread(r, "shutdownIndexersThread")).start();
	}
}
