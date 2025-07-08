package il.co.fibi.snifit.etools.ant.extras.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class AntZipLeveledStructureProvider implements AntArchivedImportStructureProvider {
	private ZipFile zipFile;

	private static final ZipEntry root = new ZipEntry("/");

	private Map<ZipEntry, List<Object>> children;

	private Map<IPath, ZipEntry> directoryEntryCache = new HashMap<>();

	private int stripLevel;

	public AntZipLeveledStructureProvider(ZipFile sourceFile) {
		this.zipFile = sourceFile;
		this.stripLevel = 0;
	}

	protected ZipEntry createContainer(IPath pathname) {
		ZipEntry parent, existingEntry = this.directoryEntryCache.get(pathname);
		if (existingEntry != null)
			return existingEntry;
		if (pathname.segmentCount() == 1) {
			parent = root;
		} else {
			parent = createContainer(pathname.removeLastSegments(1));
		}
		ZipEntry newEntry = new ZipEntry(pathname.toString());
		this.directoryEntryCache.put(pathname, newEntry);
		List<Object> childList = new ArrayList<>();
		this.children.put(newEntry, childList);
		List<Object> parentChildList = this.children.get(parent);
		parentChildList.add(newEntry);
		return newEntry;
	}

	protected void createFile(ZipEntry entry) {
		ZipEntry parent;
		Path path = new Path(entry.getName());
		if (path.segmentCount() == 1) {
			parent = root;
		} else {
			parent = this.directoryEntryCache.get(path.removeLastSegments(1));
		}
		List<Object> childList = this.children.get(parent);
		childList.add(entry);
	}

	public List<Object> getChildren(Object element) {
		if (this.children == null)
			initialize();
		return this.children.get(element);
	}

	public InputStream getContents(Object element) {
		try {
			return this.zipFile.getInputStream((ZipEntry) element);
		} catch (IOException iOException) {
			return null;
		}
	}

	private String stripPath(String path) {
		String pathOrig = path;
		String temp = path;
		for (int i = 0; i < this.stripLevel; i++) {
			int firstSep = temp.indexOf('/');
			if (firstSep == 0) {
				temp = temp.substring(1);
				firstSep = temp.indexOf('/');
			}
			if (firstSep == -1)
				return pathOrig;
			temp = temp.substring(firstSep);
		}
		return temp;
	}

	public String getFullPath(Object element) {
		return stripPath(((ZipEntry) element).getName());
	}

	public String getLabel(Object element) {
		if (element.equals(root))
			return ((ZipEntry) element).getName();
		return stripPath((new Path(((ZipEntry) element).getName())).lastSegment());
	}

	public Object getRoot() {
		return root;
	}

	public ZipFile getZipFile() {
		return this.zipFile;
	}

	protected void initialize() {
		this.children = HashMap.newHashMap(1000);
		this.children.put(root, new ArrayList<>());
		Enumeration<? extends ZipEntry> entries = this.zipFile.entries();
		while (entries.hasMoreElements()) {
			ZipEntry entry = entries.nextElement();
			IPath path = (new Path(entry.getName())).addTrailingSeparator();
			if (entry.isDirectory()) {
				createContainer(path);
				continue;
			}
			int pathSegmentCount = path.segmentCount();
			if (pathSegmentCount > 1)
				createContainer(path.uptoSegment(pathSegmentCount - 1));
			createFile(entry);
		}
	}

	public boolean isFolder(Object element) {
		return ((ZipEntry) element).isDirectory();
	}

	public void setStrip(int level) {
		this.stripLevel = level;
	}

	public int getStrip() {
		return this.stripLevel;
	}
}
