package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class GetJavacErrorCount extends FailOnErrorTask {
	private IProject workspaceProject = null;

	private String projectName;

	private String propertyName = "JavacErrorCount";

	final MonitorHelper provider = new MonitorHelper(this);

	final IProgressMonitor monitor = this.provider.createProgressGroup();

	int UNKNOWN_ERRORS = -1;

	public void execute() throws BuildException {
		super.execute();
		int errorCount = this.UNKNOWN_ERRORS;
		validateAttributes(this.monitor);
		try {
			this.monitor.setTaskName("GetJavacErrorCount: " + this.projectName);
			errorCount = getJavacErrorCount(this.workspaceProject);
			getProject().setUserProperty(this.propertyName, Integer.toString(errorCount));
		} catch (Exception e) {
			handleError(e.getMessage(), e);
		} finally {
			this.monitor.done();
			this.provider.dispose();
		}
	}

	private int getJavacErrorCount(IProject prj) {
		this.workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
		try {
			IMarker[] markerList = prj.findMarkers("org.eclipse.jdt.core.problem", true, 2);
			if (markerList == null || markerList.length == 0)
				return 0;
			IMarker marker = null;
			int numErrors = 0;
			for (int i = 0; i < markerList.length; i++) {
				marker = markerList[i];
				int severity = marker.getAttribute("severity", 2);
				if (severity == 2)
					numErrors++;
			}
			return numErrors;
		} catch (CoreException e) {
			handleError(ResourceHandler.getString("Common.coreException", e.getMessage()), (Exception) e);
			return this.UNKNOWN_ERRORS;
		}
	}

	public void setProjectName(String name) {
		this.projectName = name;
	}

	public void setPropertyName(String name) {
		this.propertyName = name;
	}

	protected void validateAttributes(IProgressMonitor monitor1) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"projectName\":" + this.projectName);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"propertyName\":" + this.propertyName);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.projectName == null)
			handleError(ResourceHandler.getString("Common.missingProjectName"));
		this.workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
		if (this.workspaceProject == null)
			handleError(ResourceHandler.getString("Common.projectNameNull", this.projectName));
		if (!this.workspaceProject.exists())
			handleError(ResourceHandler.getString("Common.projectNameNotInWorkspace", this.projectName));
		if (!this.workspaceProject.isOpen())
			handleError(ResourceHandler.getString("Common.projectNotOpen", this.projectName));
	}
}
