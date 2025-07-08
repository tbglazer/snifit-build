package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalog;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogElement;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.catalog.provisional.INextCatalog;

public class XMLCatalogTask extends FailOnErrorTask {
	@SuppressWarnings("restriction")
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			validateAttributes(monitor);
			if (this.catalogLocation != null) {
				ICatalog defaultCatalog = XMLCorePlugin.getDefault().getDefaultXMLCatalog();
				INextCatalog newCatalog = (INextCatalog) defaultCatalog.createCatalogElement(10);
				newCatalog.setCatalogLocation(this.catalogLocation);
				File catalogFile = new File(this.catalogLocation);
				String id = catalogFile.getName().replaceAll(" ", "_");
				int extensionIndex = id.lastIndexOf('.');
				if (extensionIndex != -1)
					id = id.substring(0, extensionIndex);
				newCatalog.setId(id);
				defaultCatalog.addCatalogElement((ICatalogElement) newCatalog);
				saveCatalog(defaultCatalog, monitor);
				if (this.verbose)
					log(ResourceHandler.getString("XMLCatalog.addNextCatalogSuccess",
							new Object[] { newCatalog.getCatalogLocation(), defaultCatalog.getLocation() }));
			}
			if (this.key != null) {
				INextCatalog[] xmlCatalogs = XMLCorePlugin.getDefault().getDefaultXMLCatalog().getNextCatalogs();
				for (int i = 0; i < xmlCatalogs.length; i++) {
					if ("user_catalog".equals(xmlCatalogs[i].getId())) {
						ICatalog userCatalog = xmlCatalogs[i].getReferencedCatalog();
						int type = 2;
						if (this.keyType.equals("system")) {
							type = 3;
						} else if (this.keyType.equals("uri")) {
							type = 4;
						}
						ICatalogEntry newEntry = (ICatalogEntry) userCatalog.createCatalogElement(type);
						newEntry.setKey(this.key);
						newEntry.setURI(this.uri);
						if (this.keyType.equalsIgnoreCase("public") && this.webURL != null)
							newEntry.setAttributeValue("webURL", this.webURL);
						userCatalog.addCatalogElement(newEntry);
						saveCatalog(userCatalog, monitor);
						if (this.verbose)
							log(ResourceHandler.getString("XMLCatalog.addUserCatalogSuccess",
									new Object[] { userCatalog.getLocation() }));
						break;
					}
				}
			}
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	@SuppressWarnings("restriction")
	private void saveCatalog(ICatalog catalog, IProgressMonitor monitor) throws BuildException {
		if (catalog != null)
			try {
				monitor.subTask(ResourceHandler.getString("XMLCatalog.saveCatalogFile", catalog.getLocation()));
				catalog.save();
			} catch (IOException ioEx) {
				handleError(ResourceHandler.getString("XMLCatalog.saveFailure", catalog.getLocation()), ioEx);
			}
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"catalogLocation\":" + this.catalogLocation);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"keyType\":" + this.keyType);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras", "Value of attribute \"key\":" + this.key);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras", "Value of attribute \"uri\":" + this.uri);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"webURL\":" + this.webURL);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.key == null && this.catalogLocation == null)
			handleError(ResourceHandler.getString("XMLCatalog.missingNextCatalogOrKey"));
		if (this.key != null) {
			if (this.uri == null)
				handleError(ResourceHandler.getString("XMLCatalog.missingUserCatalogUri"));
			try {
				URI uriObject = new URI(this.uri);
				uriObject.toURL();
			} catch (MalformedURLException | URISyntaxException malformedURLException) {
				handleError(ResourceHandler.getString("XMLCatalog.malformedURI", this.uri));
			}
			if (this.keyType == null)
				handleError(ResourceHandler.getString("XMLCatalog.missingUserCatalogKeyType"));
			if (!this.keyType.equalsIgnoreCase("public") && !this.keyType.equalsIgnoreCase("system")
					&& !this.keyType.equalsIgnoreCase("uri"))
				handleError(ResourceHandler.getString("XMLCatalog.invalidKeyType", this.keyType));
			if (!this.keyType.equalsIgnoreCase("public") && this.webURL != null)
				log(ResourceHandler.getString("XMLCatalog.invalidWebUrlUsage"));
		}
		if (this.catalogLocation != null) {
			File f = new File(this.catalogLocation);
			if (!f.exists())
				handleError(ResourceHandler.getString("XMLCatalog.invalidNextCatalogLocation", this.catalogLocation));
		}
	}

	public void setUri(String value) {
		this.uri = value;
	}

	public void setKey(String value) {
		this.key = value;
	}

	public void setKeyType(String value) {
		this.keyType = value;
	}

	public void setWebURL(String value) {
		this.webURL = value;
	}

	public void setCatalogLocation(String value) {
		this.catalogLocation = value;
	}

	public void setVerbose(boolean value) {
		this.verbose = value;
	}

	private String catalogLocation = null;

	private String uri = null;

	private String key = null;

	private String keyType = null;

	private String webURL = null;

	private boolean verbose = false;
}
