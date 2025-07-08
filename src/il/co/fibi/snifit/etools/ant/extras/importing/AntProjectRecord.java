package il.co.fibi.snifit.etools.ant.extras.importing;

import il.co.fibi.snifit.etools.ant.extras.ProjectImport;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

public class AntProjectRecord {
	File projectSystemFile;

	private Object projectArchiveFile;

	String projectName;

	private Object parent;

	private int level;

	boolean hasConflicts;

	ProjectImport importTask;

	private IProjectDescription description;

	public AntProjectRecord(File file, ProjectImport importTask1) {
		this.projectSystemFile = file;
		this.importTask = importTask1;
		setProjectName();
	}

	public AntProjectRecord(Object file, Object parent1, int level1, ProjectImport importTask1) {
		setProjectArchiveFile(file);
		setParent(parent1);
		setLevel(level1);
		this.importTask = importTask1;
		setProjectName();
	}

	private void setProjectName() {
		try {
			if (getProjectArchiveFile() != null) {
				InputStream stream = this.importTask.getStructureProvider().getContents(getProjectArchiveFile());
				if (stream == null) {
					if (getProjectArchiveFile() instanceof ZipEntry) {
						Path path = new Path(((ZipEntry) getProjectArchiveFile()).getName());
						this.projectName = path.segment(path.segmentCount() - 2);
					}
				} else {
					setDescription(ResourcesPlugin.getWorkspace().loadProjectDescription(stream));
					stream.close();
					this.projectName = getDescription().getName();
				}
			}
			if (this.projectName == null) {
				Path path = new Path(this.projectSystemFile.getPath());
				if (isDefaultLocation((IPath) path)) {
					this.projectName = path.segment(path.segmentCount() - 2);
					setDescription(ResourcesPlugin.getWorkspace().newProjectDescription(this.projectName));
				} else {
					setDescription(ResourcesPlugin.getWorkspace().loadProjectDescription((IPath) path));
					this.projectName = getDescription().getName();
				}
			}
		} catch (CoreException coreException) {

		} catch (IOException iOException) {
		}
	}

	private boolean isDefaultLocation(IPath path) {
		if (path.segmentCount() < 2)
			return false;
		return path.removeLastSegments(2).toFile().equals(Platform.getLocation().toFile());
	}

	public String getProjectName() {
		return this.projectName;
	}

	public boolean hasConflicts() {
		return this.hasConflicts;
	}

	public void setDescription(IProjectDescription description1) {
		this.description = description1;
	}

	public IProjectDescription getDescription() {
		return this.description;
	}

	public void setProjectArchiveFile(Object projectArchiveFile1) {
		this.projectArchiveFile = projectArchiveFile1;
	}

	public Object getProjectArchiveFile() {
		return this.projectArchiveFile;
	}

	public void setParent(Object parent1) {
		this.parent = parent1;
	}

	public Object getParent() {
		return this.parent;
	}

	public void setLevel(int level1) {
		this.level = level1;
	}

	public int getLevel() {
		return this.level;
	}
}
