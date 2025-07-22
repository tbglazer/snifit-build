package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.NLSMessageConstants;
import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Ant;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProjectSetBuild extends FailOnErrorTask {
	private String projectSetFileName = null;

	private String buildFileName = "build.xml";

	private String buildTarget = null;

	private String propertyBuiltProjectNames = "BuiltProjectNames";

	private boolean useBuildXML = false;

	private boolean missingProjects = false;

	private String propertyCountName = "ProjectErrorCount";

	private String propertyMessagesName = "ProjectErrorMessages";

	private String buildTypeString = "INCREMENTAL";

	private boolean isQuiet = false;

	private boolean summary = false;

	private boolean showErrors = true;

	private boolean countValidationErrors = true;

	private String severityString = "ERROR";

	private boolean debugCompilation;

	private boolean disableValidators = false;

	public ProjectSetBuild() {
		setTaskName("projectSetBuild");
	}

	public void execute() throws BuildException {
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		super.execute();
		validateAttributes(monitor);
		try {
			log(ResourceHandler.getString("Common.readingProjectSet", this.projectSetFileName));
			List<String> projects = readProjectSet();
			if (projects == null || projects.size() == 0) {
				String msg = null;
				if (this.missingProjects) {
					msg = ResourceHandler.getString("ProjectSetBuild.missingProjectReferences",
							this.projectSetFileName);
				} else {
					msg = ResourceHandler.getString("Common.noProjectReferences", this.projectSetFileName);
				}
				log(ResourceHandler.getString("Common.message", msg));
				handleError(msg);
				return;
			}
			log(ResourceHandler.getString("ProjectSetBuild.projectsToBuild", Integer.toString(projects.size())));
			String projectNames = "";
			projectNames = BuildProjects(projects);
			getProject().setUserProperty(this.propertyBuiltProjectNames, projectNames);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	private String BuildProjects(List<String> projects) {
		String projectNames = "";
		for (int i = 0; i < projects.size(); i++) {
			String projName = projects.get(i);
			if (projName == null || projName.length() == 0) {
				String msg = ResourceHandler.getString("Common.antProjectSetProjectNull");
				log(ResourceHandler.getString("Common.message", msg));
				handleError(msg);
				return "FAILED";
			}
			if (this.useBuildXML) {
				IProject workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projName);
				if (workspaceProject == null) {
					log(ResourceHandler.getString("Common.message",
							ResourceHandler.getString("Common.projectNameNull", projName)));
					handleError(ResourceHandler.getString("Common.projectNameNull", projName));
					return "FAILED";
				}
				if (!workspaceProject.exists()) {
					log(ResourceHandler.getString("Common.message",
							ResourceHandler.getString("Common.projectNameNotInWorkspace", projName)));
					handleError(ResourceHandler.getString("Common.projectNameNotInWorkspace", projName));
					return "FAILED";
				}
				if (!workspaceProject.isOpen()) {
					log(ResourceHandler.getString("Common.message",
							ResourceHandler.getString("Common.projectNotOpen", projName)));
					handleError(ResourceHandler.getString("Common.projectNotOpen", projName));
					return "FAILED";
				}
				String dir = workspaceProject.getLocation().toOSString();
				String buildpath = String.valueOf(dir) + File.separator + this.buildFileName;
				File buildFile = new File(buildpath);
				if (!buildFile.exists() || !buildFile.canRead()) {
					log(ResourceHandler.getString("Common.message", ResourceHandler.getString(
							"ProjectSetBuild.missingProject", (Object[]) new String[] { projName, buildpath })));
					handleError(ResourceHandler.getString("ProjectSetBuild.missingProject",
							(Object[]) new String[] { projName, buildpath }));
					return "FAILED";
				}
				Ant task = new Ant();
				File basedir = new File(dir);
				task.setDir(basedir);
				task.setAntfile(this.buildFileName);
				task.setTaskName(String.valueOf(projName) + "-" + this.buildFileName);
				task.setProject(getProject());
				if (this.buildTarget != null && this.buildTarget.length() > 0)
					task.setTarget(this.buildTarget);
				log(ResourceHandler.getString("ProjectSetBuild.invokingBuild", buildpath));
				task.execute();
			} else {
				ProjectBuild task = new ProjectBuild();
				task.setDisableValidators(isDisableValidators());
				task.setProjectName(projName);
				task.setProject(getProject());
				task.setBuildtype(this.buildTypeString);
				task.setDebugcompilation(this.debugCompilation);
				task.setFailOnError(isFailOnError());
				task.setSummary(this.summary);
				task.setShowerrors(this.showErrors);
				task.setCountValidationErrors(this.countValidationErrors);
				task.setQuiet(this.isQuiet);
				task.setSeveritylevel(this.severityString);
				task.setPropertycountname(this.propertyCountName);
				task.setPropertymessagesname(this.propertyMessagesName);
				log(ResourceHandler.getString("ProjectSetBuild.invokingBuild", projName));
				task.execute();
			}
			projectNames = String.valueOf(projectNames) + projName + " ";
		}
		log(ResourceHandler.getString("Common.projectSetFileFinished", this.projectSetFileName));
		return projectNames;
	}

	private List<String> readProjectSet() {
		List<String> projects = new ArrayList<String>();
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			ProjectSetContentHandler handler = new ProjectSetContentHandler();
			InputSource source = new InputSource(this.projectSetFileName);
			parser.parse(source, handler);
			Map<String, List<String>> map = handler.getReferences();
			Iterator<Map.Entry<String, List<String>>> it = map.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, List<String>> entry = it.next();
				List<String> references = entry.getValue();
				Iterator<String> referencesIterator = references.iterator();
				while (referencesIterator.hasNext()) {
					String projectName = processLine(referencesIterator.next());
					if (projectName != null)
						projects.add(projectName);
				}
			}
		} catch (ParserConfigurationException e) {
			handleError(e.getMessage(), e);
		} catch (SAXException e) {
			handleError(e.getMessage(), e);
		} catch (IOException e) {
			handleError(e.getMessage(), e);
		}
		return projects;
	}

	private String processLine(String line) {
		String projectName = null;
		String[] parts = line.split(",");
		if (parts.length < 4) {
			String msg = ResourceHandler.getString("Common.missingProjectNameFromReference", line);
			log(ResourceHandler.getString("Common.message", msg));
			handleError(msg);
		} else {
			projectName = parts[3].trim();
		}
		IProject workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		if (workspaceProject != null && !workspaceProject.exists()) {
			log(ResourceHandler.getString("ProjectSetBuild.skippingMissingProject", projectName));
			this.missingProjects = true;
			projectName = null;
		} else if (workspaceProject != null && !workspaceProject.isOpen()) {
			log(ResourceHandler.getString("ProjectSetBuild.skippingClosedProject", projectName));
			this.missingProjects = true;
			projectName = null;
		}
		return projectName;
	}

	public void setProjectSetFileName(String name) {
		this.projectSetFileName = name;
	}

	public void setPropertyBuiltProjectNames(String name) {
		this.propertyBuiltProjectNames = name;
	}

	public void setBuildtype(String type) {
		this.buildTypeString = type;
	}

	public void setDebugcompilation(boolean b) {
		this.debugCompilation = b;
	}

	public void setSummary(boolean b) {
		this.summary = b;
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

	public void setUseBuildXML(boolean b) {
		this.useBuildXML = b;
	}

	public void setBuildTarget(String name) {
		this.buildTarget = name;
	}

	public void setBuildFileName(String name) {
		this.buildFileName = name;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.projectSetFileName == null)
			handleError(ResourceHandler.getString("Common.missingProjectSetFileName"));
		if (!this.projectSetFileName.substring(this.projectSetFileName.length() - 4, this.projectSetFileName.length())
				.equalsIgnoreCase(".psf"))
			handleError(NLS.bind(NLSMessageConstants.IMPORT_PROJECT_INVALID_PSF_EXTENSION, this.projectSetFileName));
		File file = new File(this.projectSetFileName);
		if (!file.isFile()) {
			log(ResourceHandler.getString("Common.message",
					ResourceHandler.getString("Common.projectSetFileIsNotAFile", this.projectSetFileName)));
			handleError(ResourceHandler.getString("Common.projectSetFileIsNotAFile", this.projectSetFileName));
		}
		if (!file.exists()) {
			log(ResourceHandler.getString("Common.message",
					ResourceHandler.getString("Common.projectSetFileDoesNotExist", this.projectSetFileName)));
			handleError(ResourceHandler.getString("Common.projectSetFileDoesNotExist", this.projectSetFileName));
		}
		if (!file.canRead()) {
			log(ResourceHandler.getString("Common.message",
					ResourceHandler.getString("Common.projectSetFileNonReadable", this.projectSetFileName)));
			handleError(ResourceHandler.getString("Common.projectSetFileNonReadable", this.projectSetFileName));
		}
		if (!this.buildTypeString.equalsIgnoreCase("INCREMENTAL"))
			if (!this.buildTypeString.equalsIgnoreCase("FULL"))
				if (this.buildTypeString.equalsIgnoreCase("AUTO")) {
					log(ResourceHandler.getString("Common.autoDeprecated"));
				} else if (!this.buildTypeString.equalsIgnoreCase("CLEAN")) {
					String msg = ResourceHandler.getString("Common.invalidBuildType", this.buildTypeString);
					log(ResourceHandler.getString("Common.message", msg));
					handleError(msg);
				}
		if (!this.severityString.equalsIgnoreCase("ERROR"))
			if (!this.severityString.equalsIgnoreCase("WARNING") && !this.severityString.equalsIgnoreCase("WARN"))
				if (!this.severityString.equalsIgnoreCase("INFO")
						&& !this.severityString.equalsIgnoreCase("INFORMATION")) {
					String msg = ResourceHandler.getString("Common.invalidSeverityLevel", this.severityString);
					log(ResourceHandler.getString("Common.message", msg));
					handleError(msg);
				}
	}

	public final void setDisableValidators(boolean value) {
		this.disableValidators = value;
	}

	public final boolean isDisableValidators() {
		return this.disableValidators;
	}
}
