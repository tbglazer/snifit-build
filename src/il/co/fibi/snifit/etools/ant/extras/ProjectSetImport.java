package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.IProjectSetSerializer;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.TeamPlugin;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ProjectSetImport extends FailOnErrorTask {
	private boolean autoDeleteExistingProjects = true;

	private boolean isQuiet = false;

	private boolean summary = false;

	private String projectSetFileName = null;

	private String propertyImportedProjectNames = "ImportedProjectNames";

	private String USERID = null;

	private String PASSWORD = null;

	private boolean existingProjects = false;

	private static final String TARGET_USERID_PASSWORD = "USERID:PASSWORD@";

	public ProjectSetImport() {
		setTaskName("projectSetImport");
	}

	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			validateAttributes(monitor);
			log(ResourceHandler.getString("Common.readingProjectSet", this.projectSetFileName));
			ProjectSetContentHandler handler = new ProjectSetContentHandler();
			Map<String, List<String>> projectSetMap = readProjectSet(handler);
			List<IProject> projectImported = new ArrayList<IProject>();
			String projectNames = "";
			Iterator<Map.Entry<String, List<String>>> it = projectSetMap.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry<String, List<String>> entry = it.next();
				String providerId = entry.getKey();
				List<String> references = entry.getValue();
				if (references == null || references.size() == 0) {
					if (this.existingProjects) {
						String msg = ResourceHandler.getString("ProjectSetImport.onlyExistingProjects",
								this.projectSetFileName);
						log(msg);
						continue;
					}
					handleError(ResourceHandler.getString("Common.noProjectReferences", this.projectSetFileName));
					continue;
				}
				log(ResourceHandler.getString("ProjectSetImport.providerId", providerId));
				log(ResourceHandler.getString("ProjectSetImport.projectsToImport",
						Integer.toString(references.size())));
				projectImported.addAll(teamImportProjects(providerId, references, handler));
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

	private IProject[] getIProjectsFromReferences(List<String> references) {
		List<IProject> projects = new ArrayList<IProject>();
		for (int i = 0; i < references.size(); i++) {
			String line = references.get(i);
			String projectName = null;
			String[] parts = line.split(",");
			if (parts.length < 4) {
				handleError(ResourceHandler.getString("Common.missingProjectNameFromReference", line));
			} else {
				projectName = parts[3].trim();
			}
			IProject workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
			if (workspaceProject != null && workspaceProject.exists())
				projects.add(workspaceProject);
		}
		return projects.<IProject>toArray(new IProject[projects.size()]);
	}

	private List<IProject> teamImportProjects(String providerId, List<String> references,
			ProjectSetContentHandler handler) {
		IProject[] overwriteProject = new IProject[0];
		if (this.autoDeleteExistingProjects)
			overwriteProject = getIProjectsFromReferences(references);
		List<String> newReferences = references;
		AntConsoleProgressMonitor monitor = new AntConsoleProgressMonitor(this);
		ProjectSetSerializationContext pssc = new ProjectSetSerializationContextForAntTask(this.projectSetFileName,
				overwriteProject);
		IProject[] projects = (IProject[]) null;
		if ((newReferences == null || newReferences.size() == 0) && handler.isVersionOne()) {
			IProjectSetSerializer serializer = Team.getProjectSetSerializer("versionOneSerializer");
			if (serializer != null)
				try {
					projects = serializer.addToWorkspace(new String[0], this.projectSetFileName, pssc, monitor);
				} catch (TeamException teamEx) {
					handleError(ResourceHandler.getString("Common.exception", teamEx.getMessage()));
				}
		} else {
			RepositoryProviderType providerType = RepositoryProviderType.getProviderType(providerId);
			if (providerType == null)
				providerType = TeamPlugin.getAliasType(providerId);
			if (providerType == null) {
				handleError(ResourceHandler.getString("ProjectSetImport.missingProviderType",
						(Object[]) new String[] { providerId, this.projectSetFileName }));
			} else {
				ProjectSetCapability serializer = providerType.getProjectSetCapability();
				ProjectSetCapability.ensureBackwardsCompatible(providerType, serializer);
				if (serializer != null && newReferences != null)
					try {
						projects = serializer.addToWorkspace(
								newReferences.<String>toArray(new String[newReferences.size()]), pssc, monitor);
					} catch (TeamException teamEx) {
						if (teamEx.getMessage().equals("Authentication error: No CVS authenticator is registered")
								&& this.USERID == null && this.PASSWORD == null)
							handleError(

									ResourceHandler.getString("Common.exception",

											ResourceHandler.getString("ProjectSetImport.noUserIdAndPasswordSpecified")),
									(Exception) teamEx);
						handleError(teamEx.getMessage(), (Exception) teamEx);
					} catch (IllegalStateException isex) {
						handleError(
								ResourceHandler.getString("Common.exception",
										ResourceHandler.getString("ProjectSetImport.CVSAuthenticationIncorrect")),
								isex);
					} catch (Exception ex) {
						handleError(ex.getMessage(), ex);
					}
			}
		}
		List<IProject> returnList = null;
		if (projects != null) {
			returnList = new ArrayList<IProject>(projects.length);
			for (int i = 0; i < projects.length; i++) {
				log(ResourceHandler.getString("ProjectSetImport.projectImported",
						(Object[]) new String[] { Integer.toString(i), projects[i].getName() }));
				returnList.add(projects[i]);
			}
		} else {
			returnList = Collections.emptyList();
		}
		return returnList;
	}

	private Map<String, List<String>> readProjectSet(ProjectSetContentHandler handler) {
		Map<String, List<String>> projectSetMap = null;
		try {
			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();
			InputSource source = new InputSource(this.projectSetFileName);
			parser.parse(source, handler);
			projectSetMap = handler.getReferences();
		} catch (ParserConfigurationException e) {
			handleError(e.getMessage(), e);
		} catch (SAXException e) {
			handleError(e.getMessage(), e);
		} catch (IOException e) {
			handleError(e.getMessage(), e);
		}
		return projectSetMap;
	}

	public void setUSERID(String s) {
		if (s != null && s.length() == 0) {
			this.USERID = null;
		} else {
			this.USERID = s;
		}
	}

	public void setPASSWORD(String s) {
		this.PASSWORD = s;
	}

	public void setAutoDeleteExistingProjects(boolean b) {
		this.autoDeleteExistingProjects = b;
	}

	public void setsummary(boolean b) {
		this.summary = b;
	}

	public void setquiet(boolean b) {
		this.isQuiet = b;
	}

	public void setProjectSetFileName(String name) {
		this.projectSetFileName = name;
	}

	public void setPropertyImportedProjectNames(String name) {
		this.propertyImportedProjectNames = name;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"propertyImportedProjectNames\":" + this.propertyImportedProjectNames);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"autoDeleteExistingProjects\":" + this.autoDeleteExistingProjects);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"USERID\":" + this.USERID);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"PASSWORD\":" + this.PASSWORD);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.projectSetFileName == null)
			handleError(ResourceHandler.getString("Common.missingProjectSetFileName"));
		if (!this.projectSetFileName.substring(this.projectSetFileName.length() - 4, this.projectSetFileName.length())
				.equalsIgnoreCase(".psf"))
			handleError(NLS.bind(NLSMessageConstants.IMPORT_PROJECT_INVALID_PSF_EXTENSION, this.projectSetFileName));
		File file = new File(this.projectSetFileName);
		if (!file.isFile())
			handleError(ResourceHandler.getString("Common.projectSetFileIsNotAFile", this.projectSetFileName));
		if (!file.exists())
			handleError(ResourceHandler.getString("Common.projectSetFileDoesNotExist", this.projectSetFileName));
		if (!file.canRead())
			handleError(ResourceHandler.getString("Common.projectSetFileNonReadable", this.projectSetFileName));
		if ((this.USERID != null && this.PASSWORD == null) || (this.USERID == null && this.PASSWORD != null))
			handleError(ResourceHandler.getString("ProjectSetImport.invalidUserIdPasswordPair"));
	}
}
