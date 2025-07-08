package mataf.snifit.tasks;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProjectSetImport extends FailOnErrorTask {
	private boolean autoDeleteExistingProjects = true;

	private String projectSetFileName = null;

	private String rootDirectory = null;

	private String propertyImportedProjectNames = "ImportedProjectNames";

	public ProjectSetImport() {
		setTaskName("projectSetImport");
	}

	@Override
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			validateAttributes(monitor);
			log(ResourceHandler.getString("Common.readingProjectSet", this.projectSetFileName));
			ProjectSetContentHandler handler = new ProjectSetContentHandler();
			Map<String, List<String>> projectSetMap = readProjectSet(handler);
			List<IProject> projectImported = new ArrayList<>();
			String projectNames = "";
			Iterator<Map.Entry<String, List<String>>> it = projectSetMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, List<String>> entry = it.next();
				String providerId = entry.getKey();
				List<String> references = entry.getValue();
				if (references == null || references.isEmpty()) {
					handleError(ResourceHandler.getString("Common.noProjectReferences", this.projectSetFileName));
				} else {
					log(ResourceHandler.getString("ProjectSetImport.projectsToImport", Integer.toString(references.size())));
					projectImported.addAll(teamImportProjects(providerId, references));
				}
			}
			for (int i = 0; i < projectImported.size(); i++) {
				IProject proj = projectImported.get(i);
				projectNames = String.valueOf(projectNames) + proj.getName() + " ";
			}
			getProject().setUserProperty(this.propertyImportedProjectNames, projectNames);
		} finally {
			provider.dispose();
		}
	}

	private List<IProject> teamImportProjects(String providerId, List<String> references) {
		List<IProject> projects = new ArrayList<>();
		if (references != null) {
			for (int i = 0; i < references.size(); i++) {
				String line = references.get(i);
				String projectName = null;
				String projectPath = null;
				String[] parts = line.split(",");
				if (parts.length < 4) {
					handleError(ResourceHandler.getString("Common.missingProjectNameFromReference", line));
				} else {
					projectPath = parts[3].trim();
					parts = projectPath.split("/");
					projectName = parts[parts.length - 1].trim();
				}
				try {
					File sourceProjectDir = Paths.get(rootDirectory, projectPath).toFile();
					if (!sourceProjectDir.isDirectory())
						handleError(ResourceHandler.getString("Common.sourceProjectIsNotADirectory", sourceProjectDir.getAbsolutePath()));
					if (!sourceProjectDir.exists() || !new File(sourceProjectDir, ".project").exists())
						handleError(ResourceHandler.getString("Common.sourceProjectDirectoryIsInvalid", sourceProjectDir.getAbsolutePath()));
					IProjectDescription description = ResourcesPlugin.getWorkspace().loadProjectDescription(new Path(new File(sourceProjectDir, ".project").getAbsolutePath()));
					IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(description.getName());
					if (this.autoDeleteExistingProjects && project.exists()) {
						project.delete(false, null);
					}
					project.create(description, null);
					project.open(null);
					projects.add(project);
					log(ResourceHandler.getString("ProjectSetImport.projectImported", new String[] { Integer.toString(i), project.getName() }));
				} catch (BuildException | CoreException e) {
					handleError(ResourceHandler.getString("Common.ProjectImportFailed", projectName));
				}
			}
		}
		return projects;
	}

	private Map<String, List<String>> readProjectSet(ProjectSetContentHandler handler) {
		Map<String, List<String>> projectSetMap = null;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			InputSource source = new InputSource(this.projectSetFileName);
			parser.parse(source, handler);
			projectSetMap = handler.getReferences();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			handleError(e.getMessage(), e);
		}
		return projectSetMap;
	}

	public void setAutoDeleteExistingProjects(boolean b) {
		this.autoDeleteExistingProjects = b;
	}

	public void setProjectSetFileName(String name) {
		this.projectSetFileName = name;
	}

	public void setPropertyImportedProjectNames(String name) {
		this.propertyImportedProjectNames = name;
	}

	public void setRootDirectory(String dir) {
		this.rootDirectory = dir;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.projectSetFileName == null)
			handleError(ResourceHandler.getString("Common.missingProjectSetFileName"));
		if (!this.projectSetFileName.substring(this.projectSetFileName.length() - 4, this.projectSetFileName.length()).equalsIgnoreCase(".psf"))
			handleError(ResourceHandler.getString("Common.invalidPSFFileExtension", this.projectSetFileName));
		File file = new File(this.projectSetFileName);
		if (!file.isFile())
			handleError(ResourceHandler.getString("Common.projectSetFileIsNotAFile", this.projectSetFileName));
		if (!file.exists())
			handleError(ResourceHandler.getString("Common.projectSetFileDoesNotExist", this.projectSetFileName));
		if (!file.canRead())
			handleError(ResourceHandler.getString("Common.projectSetFileNonReadable", this.projectSetFileName));
		if (this.rootDirectory == null)
			handleError(ResourceHandler.getString("Common.missingRootDirectory"));
		file = new File(this.rootDirectory);
		if (!file.isDirectory())
			handleError(ResourceHandler.getString("Common.rootDirectoryIsNotADir", this.rootDirectory));
		if (!file.exists())
			handleError(ResourceHandler.getString("Common.rootDirectoryDoesNotExist", this.rootDirectory));
		if (!file.canRead())
			handleError(ResourceHandler.getString("Common.rootDirectoryIsNonReadable", this.rootDirectory));
	}
}
