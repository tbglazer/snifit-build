package il.co.fibi.snifit.ant.extras.tasks;

import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

public class ProjectGetErrors extends FailOnErrorTask {
	private String projectName;

	private String propertyCountName = "ProjectErrorCount";

	private String propertyMessagesName = "ProjectErrorMessages";

	private boolean isQuiet = false;

	private boolean countValidationErrors = true;

	private boolean showErrors = false;

	private int severity = 2;

	private String severityString = "ERROR";

	private Vector<String> v = null;

	private IProject workspaceProject = null;

	private int UNKNOWN_ERRORS = -1;

	public String messages = "?FAILURE?";

	public ProjectGetErrors() {
		setTaskName("projectGetErrors");
	}

	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		getProject().setUserProperty(this.propertyCountName, Integer.toString(this.UNKNOWN_ERRORS));
		validateAttributes(monitor);
		try {
			int errorCount = getErrorCount(this.workspaceProject);
			getProject().setUserProperty(this.propertyCountName, Integer.toString(errorCount));
			getProject().setUserProperty(this.propertyMessagesName, this.messages);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	public int getErrorCount(IProject workspaceProject1) {
		int errorCount = 0;
		this.messages = "";
		this.v = new Vector<String>();
		String errMsg = null;
		try {
			IMarker[] markerList = workspaceProject1.findMarkers("org.eclipse.core.resources.problemmarker", true, 2);
			if (markerList == null || markerList.length == 0) {
				errorCount = 0;
				return errorCount;
			}
			IMarker marker = null;
			int numErrors = 0;
			int totalDisplayErrors = 0;
			for (int j = 0; j < markerList.length; j++) {
				marker = markerList[j];
				if (!marker.exists())
					break;
				int level = marker.getAttribute("severity", this.severity);
				if (level >= this.severity) {
					totalDisplayErrors++;
					String msg = marker.getAttribute("message", "NO-MESSAGE");
					String errorLevelName = "UNKNOWN";
					if (level == 2) {
						errorLevelName = "ERROR";
						errorCount++;
					} else if (level == 1) {
						errorLevelName = "WARNING";
					} else if (level == 0) {
						errorLevelName = "INFORMATION";
					}
					if (this.countValidationErrors) {
						numErrors++;
						if (errMsg == null)
							errMsg = msg;
					} else {
						String type = marker.getType().toLowerCase();
						if (type.indexOf(".validat") < 0) {
							numErrors++;
							if (errMsg == null)
								errMsg = msg;
						} else {
							errorLevelName = "VALIDATION";
						}
					}
					this.v.addElement(String.valueOf(errorLevelName) + ":  " + msg);
					if (this.showErrors) {
						int lineNumber = marker.getAttribute("lineNumber", 0);
						log(ResourceHandler.getString("ProjectGetErrors.separator"));
						log(ResourceHandler.getString("ProjectGetErrors.displayErrors1",
								new String[] { Integer.toString(totalDisplayErrors), errorLevelName,
										marker.getResource().getFullPath().toOSString() }));
						log(ResourceHandler.getString("ProjectGetErrors.displayErrors2",
								new String[] { Integer.toString(lineNumber), marker.getType() }));
						log(msg);
					}
				}
			}
			errorCount = numErrors;
		} catch (CoreException e) {
			handleError(ResourceHandler.getString("Common.coreException", e.getMessage()), e);
		}
		for (int i = 0; i < errorCount; i++) {
			if (i == 0) {
				this.messages = this.v.elementAt(i);
			} else {
				this.messages = String.valueOf(this.messages) + "\n" + this.v.elementAt(i);
			}
		}
		this.v = null;
		if (errorCount > 0)
			handleError(ResourceHandler.getString("ProjectGetErrors.displayFailOnError",
					new String[] { this.projectName, Integer.toString(errorCount), errMsg }));
		return errorCount;
	}

	public void setProjectName(String name) {
		this.projectName = name;
	}

	public void setShowerrors(boolean b) {
		this.showErrors = b;
	}

	public void setCountValidationErrors(boolean b) {
		this.countValidationErrors = b;
	}

	public void setQuiet(boolean b) {
		this.isQuiet = b;
	}

	public void setPropertycountname(String name) {
		this.propertyCountName = name;
	}

	public void setPropertymessagesname(String name) {
		this.propertyMessagesName = name;
	}

	public void setSeveritylevel(String s) {
		this.severityString = s;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.projectName == null)
			handleError(ResourceHandler.getString("Common.missingProjectName"));
		this.workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
		if (this.workspaceProject == null) {
			handleError(ResourceHandler.getString("Common.projectNameNull", this.projectName));
			return;
		}
		if (!this.workspaceProject.exists()) {
			handleError(ResourceHandler.getString("Common.projectNameNotInWorkspace", this.projectName));
			return;
		}
		if (!this.workspaceProject.isOpen()) {
			handleError(ResourceHandler.getString("Common.projectNotOpen", this.projectName));
			return;
		}
		if (this.severityString.equalsIgnoreCase("ERROR")) {
			this.severity = 2;
		} else if (this.severityString.equalsIgnoreCase("WARNING") || this.severityString.equalsIgnoreCase("WARN")) {
			this.severity = 1;
		} else if (this.severityString.equalsIgnoreCase("INFO")
				|| this.severityString.equalsIgnoreCase("INFORMATION")) {
			this.severity = 0;
		} else {
			handleError(ResourceHandler.getString("Common.invalidSeverityLevel", this.severityString));
		}
	}

	public boolean isQuiet() {
		return this.isQuiet;
	}
}
