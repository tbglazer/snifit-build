package il.co.fibi.snifit.etools.ant.extras.importing;

import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.osgi.util.NLS;

public class ArchiveImportOperation {
	private List<Object> selectedFiles;

	private Object source;

	private IPath destinationPath;

	private AntArchivedImportStructureProvider provider;

	private IContainer destinationContainer;

	final IProgressMonitor monitor;

	public ArchiveImportOperation(IPath containerPath, Object source1, AntArchivedImportStructureProvider provider1,
			IProgressMonitor monitor1) {
		this.destinationPath = containerPath;
		this.source = source1;
		this.provider = provider1;
		this.monitor = monitor1;
	}

	public ArchiveImportOperation(IPath containerPath, Object source1, AntArchivedImportStructureProvider provider1,
			List<Object> filesToImport, IProgressMonitor monitor1) {
		this(containerPath, source1, provider1, monitor1);
		this.selectedFiles = filesToImport;
	}

	public ArchiveImportOperation(IPath containerPath, AntArchivedImportStructureProvider provider1,
			List<Object> filesToImport, IProgressMonitor monitor1) {
		this(containerPath, (Object) null, provider1, monitor1);
		this.selectedFiles = filesToImport;
	}

	public final void execute() throws CoreException, IOException {
		int creationCount = this.selectedFiles.size();
		this.monitor.beginTask(NLS.bind(NLSMessageConstants.IMPORT_ARCHIVE_PROJECT_BEGIN, this.destinationPath),
				creationCount + 100);
		AntContainerGenerator generator = new AntContainerGenerator(this.destinationPath);
		validateFiles(this.selectedFiles);
		this.destinationContainer = generator
				.generateContainer((IProgressMonitor) new SubProgressMonitor(this.monitor, 50));
		importFileSystemObjects(this.selectedFiles);
		this.monitor.done();
	}

	private void validateFiles(List<Object> sourceFiles) {
		List<Object> overwriteReadonly = new ArrayList();
		collectExistingReadonlyFiles(this.destinationPath, sourceFiles, overwriteReadonly);
	}

	private void collectExistingReadonlyFiles(IPath sourceStart, List<Object> sources, List<Object> overwriteReadonly) {
		Path path = null;
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		Iterator<Object> sourceIter = sources.iterator();
		IPath sourceRootPath = null;
		if (this.source != null)
			path = new Path(this.provider.getFullPath(this.source));
		while (sourceIter.hasNext()) {
			IPath newDestinationPath;
			Object nextSource = sourceIter.next();
			Path path1 = new Path(this.provider.getFullPath(nextSource));
			if (path == null) {
				newDestinationPath = sourceStart.append(this.provider.getLabel(nextSource));
			} else {
				int prefixLength = path1.matchingFirstSegments((IPath) path);
				IPath relativeSourcePath = path1.removeFirstSegments(prefixLength);
				newDestinationPath = this.destinationPath.append(relativeSourcePath);
			}
			IResource newDestination = workspaceRoot.findMember(newDestinationPath);
			if (newDestination == null)
				continue;
			IFolder folder = getFolder(newDestination);
			if (folder != null) {
				if (this.provider.isFolder(nextSource))
					collectExistingReadonlyFiles(newDestinationPath, this.provider.getChildren(nextSource),
							overwriteReadonly);
				continue;
			}
			IFile file = getFile(newDestination);
			if (file != null && file.isReadOnly())
				overwriteReadonly.add(file);
		}
	}

	private void importRecursivelyFrom(Object fileSystemObject) throws CoreException, IOException {
		if (!this.provider.isFolder(fileSystemObject)) {
			importFile(fileSystemObject);
			return;
		}
		importFolder(fileSystemObject);
		Iterator<Object> children = this.provider.getChildren(fileSystemObject).iterator();
		while (children.hasNext())
			importRecursivelyFrom(children.next());
	}

	private void importFile(Object fileObject) throws CoreException, IOException {
		IContainer containerResource = getDestinationContainerFor(fileObject);
		String fileObjectPath = this.provider.getFullPath(fileObject);
		this.monitor.subTask(fileObjectPath);
		IFile targetResource = containerResource.getFile((IPath) new Path(this.provider.getLabel(fileObject)));
		IPath targetPath = targetResource.getLocation();
		if (targetPath != null && targetPath.toFile().equals(new File(fileObjectPath)))
			return;
		InputStream contentStream = this.provider.getContents(fileObject);
		if (contentStream == null)
			return;
		if (!targetResource.exists()) {
			targetResource.create(contentStream, false, null);
		} else {
			targetResource.setContents(contentStream, 2, null);
			return;
		}
		setResourceAttributes(targetResource, fileObject);
		contentStream.close();
	}

	private void importFolder(Object folderObject) throws CoreException {
		IContainer containerResource = null;
		containerResource = getDestinationContainerFor(folderObject);
		this.monitor.subTask(this.provider.getFullPath(folderObject));
		IWorkspace workspace = this.destinationContainer.getWorkspace();
		IPath containerPath = containerResource.getFullPath();
		IPath resourcePath = containerPath.append(this.provider.getLabel(folderObject));
		if (!workspace.getRoot().exists(resourcePath))
			workspace.getRoot().getFolder(resourcePath).create(false, true, null);
	}

	private void importFileSystemObjects(List<Object> filesToImport) throws CoreException, IOException {
		Iterator<Object> filesEnum = filesToImport.iterator();
		while (filesEnum.hasNext()) {
			Object fileSystemObject = filesEnum.next();
			if (this.source == null) {
				IPath sourcePath = (new Path(this.provider.getFullPath(fileSystemObject))).removeLastSegments(1);
				if (this.provider.isFolder(fileSystemObject) && sourcePath.isEmpty())
					continue;
				this.source = sourcePath.toFile();
			}
			importRecursivelyFrom(fileSystemObject);
		}
	}

	private IContainer getDestinationContainerFor(Object fileSystemObject) throws CoreException {
		Path path = new Path(this.provider.getFullPath(fileSystemObject));
		return createContainersFor(path.removeLastSegments(1));
	}

	private IContainer createContainersFor(IPath path) throws CoreException {
		IFolder iFolder = null;
		IContainer currentFolder = this.destinationContainer;
		int segmentCount = path.segmentCount();
		if (segmentCount == 0)
			return currentFolder;
		if (currentFolder.getType() == 8)
			return createFromRoot(path);
		for (int i = 0; i < segmentCount; i++) {
			iFolder = currentFolder.getFolder((IPath) new Path(path.segment(i)));
			if (!iFolder.exists())
				iFolder.create(false, true, null);
		}
		return (IContainer) iFolder;
	}

	private void setResourceAttributes(IFile targetResource, Object fileObject) throws CoreException {
		long timeStamp = 0L;
		if (fileObject instanceof ZipEntry) {
			long zipTimeStamp = ((ZipEntry) fileObject).getTime();
			if (zipTimeStamp != -1L)
				timeStamp = zipTimeStamp;
		}
		if (timeStamp != 0L)
			targetResource.setLocalTimeStamp(timeStamp);
	}

	private IContainer createFromRoot(IPath path) throws CoreException {
		IFolder iFolder = null;
		int segmentCount = path.segmentCount();
		IProject iProject = ((IWorkspaceRoot) this.destinationContainer).getProject(path.segment(0));
		for (int i = 1; i < segmentCount; i++) {
			iFolder = iProject.getFolder((IPath) new Path(path.segment(i)));
			if (!iFolder.exists())
				iFolder.create(false, true, null);
		}
		return (IContainer) iFolder;
	}

	private IFolder getFolder(IResource resource) {
		if (resource instanceof IFolder)
			return (IFolder) resource;
		Object adapted = resource.getAdapter(IFolder.class);
		if (adapted == null)
			return null;
		return (IFolder) adapted;
	}

	private IFile getFile(IResource resource) {
		if (resource instanceof IFile)
			return (IFile) resource;
		Object adapted = resource.getAdapter(IFile.class);
		if (adapted == null)
			return null;
		return (IFile) adapted;
	}
}
