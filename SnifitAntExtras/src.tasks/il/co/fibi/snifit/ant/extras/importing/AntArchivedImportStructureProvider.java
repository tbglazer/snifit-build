package il.co.fibi.snifit.ant.extras.importing;

import java.io.InputStream;
import java.util.List;

public interface AntArchivedImportStructureProvider {
	Object getRoot();

	List<Object> getChildren(Object paramObject);

	void setStrip(int paramInt);

	String getFullPath(Object paramObject);

	String getLabel(Object paramObject);

	boolean isFolder(Object paramObject);

	InputStream getContents(Object paramObject);
}
