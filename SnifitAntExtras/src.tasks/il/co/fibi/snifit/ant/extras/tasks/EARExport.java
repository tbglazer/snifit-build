package il.co.fibi.snifit.ant.extras.tasks;

import java.io.File;

import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jst.j2ee.application.internal.operations.EARComponentExportDataModelProvider;
import org.eclipse.jst.j2ee.internal.archive.operations.EARComponentExportOperation;
import org.eclipse.jst.j2ee.project.EarUtilities;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.wst.common.componentcore.ComponentCore;
import org.eclipse.wst.common.componentcore.resources.IVirtualComponent;
import org.eclipse.wst.common.componentcore.resources.IVirtualReference;
import org.eclipse.wst.common.frameworks.datamodel.DataModelFactory;
import org.eclipse.wst.common.frameworks.datamodel.IDataModel;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

public class EARExport extends FailOnErrorTask {

	private String earFilePath;

	private IPath myEARFilePath;

	private IProject myProject;

	private String earProjectName;

	private boolean exportSource;

	private boolean overwrite;

	private boolean includeProjectMetaFiles;

	private boolean refresh;

	private boolean shared = false;

	public EARExport() {
		this.refresh = this.overwrite = this.exportSource = this.includeProjectMetaFiles = false;
	}

	@SuppressWarnings({ "restriction", "deprecation" })
	@Override
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			validateAttributes(monitor);
			try {
				monitor.beginTask(ResourceHandler.getString("Common.exportingProject", this.earProjectName), 0);
				if (this.refresh) {
					monitor.subTask(ResourceHandler.getString("Common.refreshingProject", this.earProjectName));
					this.myProject.refreshLocal(2, monitor);
					IVirtualComponent earComponent = ComponentCore.createComponent(this.myProject);
					IVirtualReference[] moduleReferences = EarUtilities.getJ2EEModuleReferences(earComponent);
					for (int i = 0; i < moduleReferences.length; i++) {
						IVirtualComponent moduleComponent = moduleReferences[i].getReferencedComponent();
						IProject modProject = moduleComponent.getProject();
						modProject.refreshLocal(2, monitor);
					}
				}
				IDataModel model = DataModelFactory.createDataModel(new EARComponentExportDataModelProvider());
				model.setProperty(EARComponentExportDataModelProvider.PROJECT_NAME, earProjectName);
				model.setProperty(EARComponentExportDataModelProvider.ARCHIVE_DESTINATION, earFilePath);
				model.setProperty(EARComponentExportDataModelProvider.OVERWRITE_EXISTING, overwrite);
				model.setProperty(EARComponentExportDataModelProvider.EXPORT_SOURCE_FILES, exportSource);
				model.setProperty(EARComponentExportDataModelProvider.RUN_BUILD, false);
				EARComponentExportOperation op = new EARComponentExportOperation(model);
				IStatus status = op.execute(monitor, null);
				if (!status.isOK()) {
					throw new CoreException(status);
				}
	            log(ResourceHandler.getString("EARExport.finished", this.earFilePath));	            
			} catch (CoreException e) {
				handleError(ResourceHandler.getString("EARExport.coreException", e.getStatus().toString()),	e);
			} catch (Exception e) {
				handleError(ResourceHandler.getString("EARExport.executionException", e.getMessage()), e);
			}
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	private boolean isEARProject() {
		boolean returnCode = false;
		if (JavaEEProjectUtilities.isEARProject(this.myProject))
			returnCode = true;
		return returnCode;
	}

	public void setEarExportFile(String filePath) {
		this.earFilePath = filePath;
	}

	public void setEarProjectName(String projectName) {
		this.earProjectName = projectName;
	}

	public void setExportSource(boolean exportSrc) {
		this.exportSource = exportSrc;
	}

	public void setOverwrite(boolean over) {
		this.overwrite = over;
	}

	public void setRefresh(boolean ref) {
		this.refresh = ref;
	}

	public void setShared(boolean value) {
		this.shared = value;
	}

	public boolean isShared() {
		return this.shared;
	}

	@Deprecated
	public void setIncludeProjectMetaFiles(boolean value) {
		this.includeProjectMetaFiles = value;
	}

	public boolean isIncludeProjectMetaFiles() {
		return this.includeProjectMetaFiles;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.earProjectName == null)
			handleError(ResourceHandler.getString("EARExport.missingProjectName"));
		this.myProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.earProjectName);
		if (!this.myProject.exists())
			handleError(ResourceHandler.getString("EARExport.missingProject", this.earProjectName));
		if (!isEARProject())
			handleError(ResourceHandler.getString("EARExport.notAnEnterpriseProject", this.earProjectName));
		if (this.earFilePath == null)
			handleError(ResourceHandler.getString("EARExport.missingExportfile"));
		if (!this.earFilePath.substring(this.earFilePath.length() - 4, this.earFilePath.length())
				.equalsIgnoreCase(".ear"))
			handleError(ResourceHandler.getString("EARExport.invalidExportFile"));
		this.myEARFilePath = new Path(this.earFilePath);
		File f = this.myEARFilePath.toFile();
		if (f.exists() && !this.overwrite)
			handleError(ResourceHandler.getString("EARExport.fileExists", this.earFilePath));
	}
}
