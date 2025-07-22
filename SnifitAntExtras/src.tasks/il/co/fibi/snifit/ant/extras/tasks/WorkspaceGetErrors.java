package il.co.fibi.snifit.ant.extras.tasks;

import java.util.Vector;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

public class WorkspaceGetErrors extends FailOnErrorTask {
	private String propertyCountName = "WorkspaceErrorCount";

	private String propertyMessagesName = "WorkspaceErrorMessages";

	private boolean showErrors = true;

	private boolean countValidationErrors = true;

	private int severity = 2;

	private String severityString = "ERROR";

	private int UNKNOWN_ERRORS = -1;

	private boolean isQuiet = false;

	private Vector<String> v = null;

	public int errorCount = this.UNKNOWN_ERRORS;

	public String errorMessages = "?FAILURE?";

	public void execute() throws BuildException {
		super.execute();
		try {
			getProject().setUserProperty(this.propertyCountName, Integer.toString(this.errorCount));
			validateAttributes(this.monitor);
			loopThroughProjects();
			getProject().setUserProperty(this.propertyCountName, Integer.toString(this.errorCount));
			getProject().setUserProperty(this.propertyMessagesName, this.errorMessages);
		} finally {
			this.monitor.done();
			this.provider.dispose();
		}
	}

	final MonitorHelper provider = new MonitorHelper(this);

	final IProgressMonitor monitor = this.provider.createProgressGroup();

	protected void loopThroughProjects() throws BuildException {
		this.v = new Vector<String>();
		this.errorCount = 0;
		this.errorMessages = "";
		try {
			IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
			IProject[] inOrderProjects = getProjectsInOrder(projects);
			for (int i = 0; i < inOrderProjects.length; i++) {
				IProject moduleProject = inOrderProjects[i];
				if (moduleProject.isOpen() && moduleProject.isAccessible()) {
					int e = getErrors(moduleProject);
					if (!this.isQuiet)
						System.out.println(ResourceHandler.getString("WorkspaceGetErrors.error",
								(Object[]) new String[] { Integer.toString(e), moduleProject.getName() }));
				}
			}
		} catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null)
				msg = "Exception==null";
			this.errorCount = this.UNKNOWN_ERRORS;
			this.errorMessages = "?FAILURE?";
			handleError(ResourceHandler.getString("WorkspaceGetErrors.exception", msg), e);
		}
		if (this.monitor != null)
			this.monitor.done();
		for (int j = 0; j < this.errorCount; j++) {
			if (j == 0) {
				this.errorMessages = this.v.elementAt(j);
			} else {
				this.errorMessages = String.valueOf(this.errorMessages) + "\n" + (String) this.v.elementAt(j);
			}
		}
		if (this.errorCount > 0)
			handleError(ResourceHandler.getString("WorkspaceGetErrors.failOnError",
					(Object[]) new String[] { Integer.toString(this.errorCount), this.v.elementAt(0) }));
		this.v = null;
	}

	private int getErrors(IProject prj) {
		if (!prj.isOpen() && this.showErrors)
			System.out.println(ResourceHandler.getString("WorkspaceGetErrors.ignoreClosedProject", prj.getName()));
		int numErrors = 0;
		String errMsg = null;
		try {
			IMarker[] markerList = prj.findMarkers("org.eclipse.core.resources.problemmarker", true, 2);
			if (markerList == null || markerList.length == 0)
				return 0;
			IMarker marker = null;
			for (int i = 0; i < markerList.length; i++) {
				marker = markerList[i];
				if (!marker.exists())
					break;
				int level = marker.getAttribute("severity", this.severity);
				if (level >= this.severity) {
					String msg = marker.getAttribute("message", "NO-MESSAGE");
					String lvl = "?????: ";
					if (level == 2) {
						lvl = "ERROR: ";
					} else if (level == 1) {
						lvl = "WARN:  ";
					} else if (level == 0) {
						lvl = "INFO:  ";
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
							lvl = "VALID: ";
						}
					}
					if (this.showErrors) {
						int lineNumber = marker.getAttribute("lineNumber", 0);
						System.out.println(ResourceHandler.getString("WorkspaceGetErrors.showErrors1",
								(Object[]) new String[] { Integer.toString(i), marker.getType(),
										Integer.toString(lineNumber),
										marker.getResource().getFullPath().toOSString() }));
						System.out.println(ResourceHandler.getString("WorkspaceGetErrors.showErrors2",
								(Object[]) new String[] { Integer.toString(i), msg }));
					}
					this.v.addElement(String.valueOf(lvl) + msg);
				}
			}
			this.errorCount += numErrors;
		} catch (CoreException e) {
			String coreExceptionMessage = ResourceHandler.getString("WorkspaceGetErrors.coreException", e.getMessage());
			handleError(coreExceptionMessage, (Exception) e);
		}
		if (this.errorCount > 0)
			handleError(ResourceHandler.getString("WorkspaceGetErrors.failOnError",
					(Object[]) new String[] { Integer.toString(this.errorCount), errMsg }));
		return numErrors;
	}

	private IProject[] getProjectsInOrder(IProject[] projects) {
		IWorkspace.ProjectOrder projectOrder = ResourcesPlugin.getWorkspace().computeProjectOrder(projects);
		return projectOrder.projects;
	}

	public void setshowerrors(boolean b) {
		this.showErrors = b;
	}

	public void setCountValidationErrors(boolean b) {
		this.countValidationErrors = b;
	}

	public void setquiet(boolean b) {
		this.isQuiet = b;
	}

	public void setpropertycountname(String name) {
		this.propertyCountName = name;
	}

	public void setpropertymessagesname(String name) {
		this.propertyMessagesName = name;
	}

	public void setseveritylevel(String s) {
		this.severityString = s;
	}

	protected void validateAttributes(IProgressMonitor monitor1) throws BuildException {
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
}
