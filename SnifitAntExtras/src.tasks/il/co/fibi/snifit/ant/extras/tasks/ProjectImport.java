package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.NLSMessageConstants;
import il.co.fibi.snifit.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.ant.extras.importing.AntArchivedImportStructureProvider;
import il.co.fibi.snifit.ant.extras.importing.AntProjectRecord;
import il.co.fibi.snifit.ant.extras.importing.AntZipLeveledStructureProvider;
import il.co.fibi.snifit.ant.extras.importing.ArchiveImportOperation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;

public class ProjectImport extends FailOnErrorTask {
	IProject workspaceProject = null;

	private final IWorkspace workspace = ResourcesPlugin.getWorkspace();

	IProjectDescription workspaceProjectDescription = null;

	private String archive = null;

	private AntProjectRecord[] selectedProjects = new AntProjectRecord[0];

	private AntArchivedImportStructureProvider structureProvider;

	List<IProject> createdProjects;

	private String projectName = null;

	private String projectLocation = null;

	public ProjectImport() {
		setTaskName("projectImport");
	}

	public void execute() throws BuildException {
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			validateAttributes(monitor);
			runOperation(monitor);
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected void runOperation(final IProgressMonitor monitor) throws BuildException {
		if (this.archive == null) {
			try {
				IEclipsePreferences antPrefs = InstanceScope.INSTANCE.getNode("il.co.fibi.snifit.etools.j2ee.ant");
				boolean createUnknownProjects = antPrefs.getBoolean("createUnknownProjects", true);
				boolean alreadyExists = this.workspaceProject.exists();
				if (alreadyExists) {
					if (!this.workspaceProject.isOpen()) {
						log(ResourceHandler.getString("project.open"));
						this.workspaceProject.open(monitor);
					} else {
						log(ResourceHandler.getString("project.refresh"));
						this.workspaceProject.refreshLocal(2, monitor);
					}
				} else {
					IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
						public void run(IProgressMonitor runnableMonitor) throws CoreException {
							ProjectImport.this.log(ResourceHandler.getString("project.create"));
							ProjectImport.this.workspaceProject.create(ProjectImport.this.workspaceProjectDescription,
									runnableMonitor);
							ProjectImport.this.log(ResourceHandler.getString("project.open"));
							ProjectImport.this.workspaceProject.open(runnableMonitor);
							if (ProjectImport.this.workspaceProject.exists() && ProjectImport.this.workspaceProject
									.hasNature("org.eclipse.jdt.core.javanature")) {
								IJavaProject javaProject = JavaCore
										.create(ProjectImport.this.workspaceProject.getProject());
								if (!javaProject.isOpen())
									javaProject.open(runnableMonitor);
							}
						}
					};
					if (this.projectLocation == null)
						this.projectLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation()
								.append(this.projectName).toOSString();
					File sf = new File(String.valueOf(this.projectLocation) + File.separator + ".project");
					boolean projectExists = sf.isFile();
					if (projectExists || (!projectExists && createUnknownProjects)) {
						ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
					} else {
						handleError("[WARNING] The project " + this.projectName
								+ " being imported does not exist; Skipping creation \n");
					}
					boolean wasInterrupted = false;
					do {
						try {
							Job.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
							wasInterrupted = false;
						} catch (OperationCanceledException e) {
							e.printStackTrace();
						} catch (InterruptedException interruptedException) {
							wasInterrupted = true;
						}
					} while (wasInterrupted);
				}
			} catch (CoreException e) {
				String msg = e.getMessage();
				if (msg == null)
					msg = e.getClass().toString();
				handleError(msg, (Exception) e);
			}
		} else {
			final AntProjectRecord[] selected = this.selectedProjects;
			this.createdProjects = new ArrayList<IProject>();
			IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
				public void run(IProgressMonitor runnableMonitor) throws CoreException {
					monitor.beginTask(
							NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_GENERIC_BEGIN, ProjectImport.this.getArchive()),
							selected.length);
					if (monitor.isCanceled())
						throw new OperationCanceledException();
					for (int i1 = 0; i1 < selected.length; i1++)
						ProjectImport.this.importArchivedProject(selected[i1], SubMonitor.convert(monitor));
					monitor.done();
				}
			};
			try {
				ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
			} catch (CoreException e) {
				handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_GENERIC_FAILED, getArchive()), (Exception) e);
			} finally {
				closeStructureProvider(getStructureProvider());
			}
		}
	}

	protected void importArchivedProject(AntProjectRecord record, IProgressMonitor monitor) {
		String projectName1 = record.getProjectName();
		IWorkspace workspace1 = ResourcesPlugin.getWorkspace();
		IProject project1 = workspace1.getRoot().getProject(projectName1);
		this.createdProjects.add(project1);
		record.getDescription().setName(projectName1);
		if (record.getProjectArchiveFile() != null) {
			List<Object> fileSystemObjects = getStructureProvider().getChildren(record.getParent());
			getStructureProvider().setStrip(record.getLevel());
			if (project1.exists()) {
				log(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_EXISTING_PROJECT_SKIPPED, project1.getName()), 1);
				return;
			}
			ArchiveImportOperation operation = new ArchiveImportOperation(project1.getFullPath(),
					getStructureProvider().getRoot(), getStructureProvider(), fileSystemObjects,
					(IProgressMonitor) SubMonitor.convert(monitor));
			try {
				operation.execute();
			} catch (CoreException e) {
				handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_GENERIC_FAILED, getArchive()), (Exception) e);
			} catch (IOException e) {
				handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_GENERIC_IO_ISSUES, getArchive()), e);
			}
		}
	}

	private void collectProjectFilesFromProvider(Collection<AntProjectRecord> files, Object entry, int level,
			IProgressMonitor monitor) {
		List<Object> children = getStructureProvider().getChildren(entry);
		if (children == null)
			children = new ArrayList<>(1);
		Iterator<Object> childrenEnum = children.iterator();
		while (childrenEnum.hasNext() && !monitor.isCanceled()) {
			Object child = childrenEnum.next();
			if (getStructureProvider().isFolder(child))
				collectProjectFilesFromProvider(files, child, level + 1, monitor);
			String elementLabel = getStructureProvider().getLabel(child);
			if (elementLabel.equals(".project"))
				files.add(new AntProjectRecord(child, entry, level, this));
		}
	}

	public void setArchive(String arch) {
		this.archive = arch;
	}

	public void setProjectName(String name) {
		this.projectName = name;
	}

	public void setProjectLocation(String prjLocation) {
		this.projectLocation = prjLocation;
	}

	private void setStructureProvider(AntArchivedImportStructureProvider structureProvider1) {
		this.structureProvider = structureProvider1;
	}

	public AntArchivedImportStructureProvider getStructureProvider() {
		return this.structureProvider;
	}

	private boolean isValidArchiveExtension(String archive1) {
		return archive1.substring(archive1.length() - 4, archive1.length()).equalsIgnoreCase(".zip");
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.archive != null) {
			if (isValidArchiveExtension(this.archive)) {
				Collection<AntProjectRecord> files = new ArrayList<AntProjectRecord>();
				if (isZipFile(this.archive)) {
					ZipFile sourceZipFile = null;
					try {
						if (this.archive.length() != 0)
							sourceZipFile = new ZipFile(this.archive);
					} catch (ZipException e) {
						handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_ZIP_FILE_CORRUPT, getArchive(),
								Platform.getLogFileLocation().append(".log").toOSString()), e);
					} catch (IOException e) {
						handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_ZIP_FILE_NOT_READABLE, getArchive(),
								Platform.getLogFileLocation().append(".log").toOSString()), e);
					}
					this.selectedProjects = assignProjectRecordsFromZipArchive(files, sourceZipFile, monitor);
				}
			} else {
				handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_INVALID_FILE, getArchive()));
			}
		} else if (this.projectName != null) {
			if (this.projectLocation != null && this.projectLocation.equalsIgnoreCase("/" + this.projectName))
				this.projectLocation = null;
			if (this.projectLocation != null && this.projectLocation.equalsIgnoreCase("\\" + this.projectName))
				this.projectLocation = null;
			log(ResourceHandler.getString("ProjectImport.projectInfo",
					(Object[]) new String[] { this.projectName, this.projectLocation }));
			this.workspaceProject = this.workspace.getRoot().getProject(this.projectName);
			this.workspaceProjectDescription = this.workspace.newProjectDescription(this.projectName);
			this.workspaceProjectDescription.setName(this.projectName);
			this.workspaceProjectDescription.setLocation(null);
			if (this.projectLocation != null
					&& (this.projectLocation.equalsIgnoreCase("") || this.projectLocation.equalsIgnoreCase("null")))
				this.projectLocation = null;
			if (this.projectLocation != null) {
				int dotdot = this.projectLocation.indexOf("..");
				if (dotdot > 0) {
					String projectLocationValue = this.projectLocation.substring(dotdot + 3);
					String path = this.projectLocation.substring(0, dotdot - 1);
					int slash = path.lastIndexOf("/", dotdot - 2);
					int i = path.lastIndexOf("\\", dotdot - 2);
					if (i > slash)
						slash = i;
					path = path.substring(0, slash + 1);
					this.projectLocation = String.valueOf(path) + projectLocationValue;
				}
				int bkslash = this.projectLocation.indexOf("\\");
				while (bkslash > 0) {
					this.projectLocation = String.valueOf(this.projectLocation.substring(0, bkslash)) + "/"
							+ this.projectLocation.substring(bkslash + 1);
					bkslash = this.projectLocation.indexOf("\\");
				}
				String wkspc = this.workspace.getRoot().getLocation().toOSString();
				String workspaceProjectName = String.valueOf(wkspc) + File.separatorChar + this.projectName;
				bkslash = workspaceProjectName.indexOf("\\");
				while (bkslash > 0) {
					workspaceProjectName = String.valueOf(workspaceProjectName.substring(0, bkslash)) + "/"
							+ workspaceProjectName.substring(bkslash + 1);
					bkslash = workspaceProjectName.indexOf("\\");
				}
				int slashes = this.projectLocation.indexOf("//");
				if (slashes > 0) {
					this.projectLocation = String.valueOf(this.projectLocation.substring(0, slashes))
							+ this.projectLocation.substring(slashes + 1);
					slashes = this.projectLocation.indexOf("//");
				}
				if (!this.projectLocation.equalsIgnoreCase(workspaceProjectName)) {
					Path path = new Path(this.projectLocation);
					log(ResourceHandler.getString("ProjectImport.importProjectLocation", path.toOSString()));
					this.workspaceProjectDescription.setLocation((IPath) path);
				} else {
					log(ResourceHandler.getString("ProjectImport.importProjectLocation", this.projectName));
				}
			}
		} else if ((((this.archive == null) ? 1 : 0) & ((this.projectName == null) ? 1 : 0)) != 0) {
			handleError(ResourceHandler.getString("Common.missingProjectName"));
		}
	}

	private AntProjectRecord[] assignProjectRecordsFromZipArchive(Collection<AntProjectRecord> files,
			ZipFile sourceFile, IProgressMonitor monitor) {
		AntProjectRecord[] projs = new AntProjectRecord[0];
		if (sourceFile != null) {
			setStructureProvider(new AntZipLeveledStructureProvider(sourceFile));
			Object child = getStructureProvider().getRoot();
			collectProjectFilesFromProvider(files, child, 0, monitor);
			Iterator<AntProjectRecord> filesIterator = files.iterator();
			projs = new AntProjectRecord[files.size()];
			int index = 0;
			while (filesIterator.hasNext())
				projs[index++] = filesIterator.next();
		}
		return projs;
	}

	private void closeStructureProvider(AntArchivedImportStructureProvider structureProvider1) {
		if (structureProvider1 instanceof AntZipLeveledStructureProvider antzipleveledstructureprovider)
			try {
				antzipleveledstructureprovider.getZipFile().close();
			} catch (IOException e) {
				handleError(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_ZIP_FILE_CORRUPT, getArchive(),
						Platform.getLogFileLocation().append(".log").toOSString()), e);
			}
	}

	private boolean isZipFile(String fileName) {
		boolean isZip = false;
		if (!fileName.isEmpty()) {
			File f = new File(fileName);
			if (f.exists()) {
				ZipFile zipFile = null;
				try {
					zipFile = new ZipFile(f);
					isZip = true;
				} catch (IOException iOException) {
					isZip = false;
				} finally {
					if (zipFile != null)
						try {
							zipFile.close();
						} catch (IOException iOException) {
						}
				}
			}
		}
		return isZip;
	}

	public String getArchive() {
		return this.archive;
	}
}
