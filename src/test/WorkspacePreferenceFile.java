package mataf.snifit.tasks;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IExportedPreferences;
import org.eclipse.core.runtime.preferences.IPreferenceFilter;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.runtime.preferences.PreferenceFilterEntry;
import org.osgi.service.prefs.BackingStoreException;

public class WorkspacePreferenceFile extends FailOnErrorTask {
	private String preferenceFileName = null;

	private File preferenceFile = null;

	public void setPreferenceFileName(String name) {
		this.preferenceFileName = name;
	}

	public WorkspacePreferenceFile() {
		setTaskName("workspacePreferenceFile");
	}

	@Override
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		validateAttributes(monitor);
		try {
			log("WorkspacePreferenceFile started");
			IScopeContext[] allScopes = { InstanceScope.INSTANCE, ConfigurationScope.INSTANCE };
			IPreferenceFilter[] filters = createPreferenceFilter(allScopes);
			boolean successfulImport = false;
			successfulImport = readAndApplyPreferences(this.preferenceFile, filters);
			log("WorkspacePreferenceFile ended with " + (successfulImport ? "success" : "error"));
		} catch (CoreException coreEx) {
			handleError(coreEx.getStatus().getMessage(), coreEx);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}
	
	private IPreferenceFilter[] createPreferenceFilter(IScopeContext[] scopes) {
		final String[] scopeNames = new String[scopes.length];
		int i = 0;
		while (i < scopes.length) {
			scopeNames[i] = scopes[i].getName();
			i++;
		}
		IPreferenceFilter[] filters = new IPreferenceFilter[1];
		filters[0] = new IPreferenceFilter() {
			public String[] getScopes() {
				return scopeNames;
			}
			public Map<String, PreferenceFilterEntry[]> getMapping(String scope) {
				return null;
			}
		};
		return filters;
	}

	private boolean readAndApplyPreferences(File preferenceFile, IPreferenceFilter[] preferenceFilters) throws CoreException {
		boolean successfulImport = false;
		if (preferenceFilters != null && preferenceFilters.length > 0) {
			try (FileInputStream fis = new FileInputStream(preferenceFile);){
				IExportedPreferences prefs = Platform.getPreferencesService().readPreferences(fis);
				Platform.getPreferencesService().applyPreferences(prefs, preferenceFilters);
				Platform.getPreferencesService().getRootNode().flush();
				successfulImport = true;
			} catch (BackingStoreException | IOException ex) {
				throw  getCoreException(IStatus.ERROR, ex);
			}
		} else {
			throw getCoreException(4, "No filters were specified while reading the file '" + preferenceFile, null);
		}
		return successfulImport;
	}
	
	private CoreException getCoreException(int severity, Exception ex) {
		return getCoreException(severity, ex.getMessage(), ex);
	}
	
	private CoreException getCoreException(int severity, String message, Exception ex) {
		Status status = new Status(severity, "com.ibm.etools.j2ee.ant", message, ex);
		return new CoreException(status);
	}
	
	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.preferenceFileName == null)
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.missingPreferenceFileName"));
		this.preferenceFile = new File(this.preferenceFileName);
		if (!this.preferenceFile.isFile())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileIsNotAFile", this.preferenceFile.getAbsolutePath()));
		if (!this.preferenceFile.exists())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileDoesNotExist", this.preferenceFile.getAbsolutePath()));
		if (!this.preferenceFile.canRead())
			handleError(ResourceHandler.getString("WorkspacePreferenceFile.preferenceFileCannotBeOpened", this.preferenceFile.getAbsolutePath()));
	}
}
