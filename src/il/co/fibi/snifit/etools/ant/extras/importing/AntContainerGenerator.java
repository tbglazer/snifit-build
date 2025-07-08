package il.co.fibi.snifit.etools.ant.extras.importing;

import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

public class AntContainerGenerator {
	IPath containerFullPath;

	IContainer container;

	public AntContainerGenerator(IPath containerPath) {
		this.containerFullPath = containerPath;
	}

	protected IFolder createFolder(IFolder folderHandle, IProgressMonitor monitor) throws CoreException {
		folderHandle.create(false, true, monitor);
		if (monitor.isCanceled())
			throw new OperationCanceledException();
		return folderHandle;
	}

	protected IFolder createFolderHandle(IContainer container1, String folderName) {
		return container1.getFolder((IPath) new Path(folderName));
	}

	protected IProject createProject(IProject projectHandle, IProgressMonitor monitor) throws CoreException {
		try {
			monitor.beginTask("", 2000);
			projectHandle.create((IProgressMonitor) new SubProgressMonitor(monitor, 1000));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
			projectHandle.open((IProgressMonitor) new SubProgressMonitor(monitor, 1000));
			if (monitor.isCanceled())
				throw new OperationCanceledException();
		} finally {
			monitor.done();
		}
		return projectHandle;
	}

	protected IProject createProjectHandle(IWorkspaceRoot root, String projectName) {
		return root.getProject(projectName);
	}

	public IContainer generateContainer(IProgressMonitor monitor) throws CoreException {
		IWorkspaceRunnable workspaceRunnable = new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor1) throws CoreException {
				monitor1.beginTask(
						NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_PROJECT_CONTAINER_CREATING,
								AntContainerGenerator.this.containerFullPath),
						1000 * AntContainerGenerator.this.containerFullPath.segmentCount());
				if (AntContainerGenerator.this.container != null)
					return;
				IWorkspaceRoot root = AntContainerGenerator.this.getWorkspaceRoot();
				AntContainerGenerator.this.container = (IContainer) root
						.findMember(AntContainerGenerator.this.containerFullPath);
				if (AntContainerGenerator.this.container != null)
					return;
				AntContainerGenerator.this.container = (IContainer) root;
				for (int i = 0; i < AntContainerGenerator.this.containerFullPath.segmentCount(); i++) {
					String currentSegment = AntContainerGenerator.this.containerFullPath.segment(i);
					IResource resource = AntContainerGenerator.this.container.findMember(currentSegment);
					if (resource != null) {
						AntContainerGenerator.this.container = (IContainer) resource;
					} else if (i == 0) {
						IProject projectHandle = AntContainerGenerator.this.createProjectHandle(root, currentSegment);
						AntContainerGenerator.this.container = (IContainer) AntContainerGenerator.this.createProject(
								projectHandle, (IProgressMonitor) new SubProgressMonitor(monitor1, 1000));
					} else {
						IFolder folderHandle = AntContainerGenerator.this
								.createFolderHandle(AntContainerGenerator.this.container, currentSegment);
						AntContainerGenerator.this.container = (IContainer) AntContainerGenerator.this
								.createFolder(folderHandle, (IProgressMonitor) new SubProgressMonitor(monitor1, 1000));
					}
				}
			}
		};
		ResourcesPlugin.getWorkspace().run(workspaceRunnable, monitor);
		return this.container;
	}

	IWorkspaceRoot getWorkspaceRoot() {
		return ResourcesPlugin.getWorkspace().getRoot();
	}
}
