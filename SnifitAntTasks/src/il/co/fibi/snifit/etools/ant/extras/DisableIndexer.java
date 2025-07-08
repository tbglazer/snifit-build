package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jst.jsp.core.internal.java.search.JSPIndexManager;
import org.eclipse.osgi.util.NLS;

public class DisableIndexer extends FailOnErrorTask {
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		try {
			monitor.beginTask(NLS.bind(NLSMessageConstants.DISABLE_INDEXER_BEGIN, this.name), -1);
			validateAttributes(monitor);
			if (this.name != null)
				try {
					if (this.name.equals(JSP_INDEXER_IDENTIFIER)) {
						disableJSPIndexer();
					} else if (this.name.equals(JS_INDEXER_IDENTIFIER)) {
						disableJavaScriptIndexer();
					} else if (this.name.equals(JAVA_INDEXER_IDENTIFIER)) {
						disableJavaIndexer();
					} else if (this.name.equals(LINKS_INDEXER_IDENTIFIER)) {
						disableLinksIndexer();
					} else if (this.name.equals(ALL_INDEXER_IDENTIFIER)) {
						disableAllIndexers();
					}
				} catch (Exception ex) {
					handleError(ex.getMessage(), ex);
				}
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected final void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras", "Value of attribute \"name\":" + this.name);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.name == null)
			handleError(NLSMessageConstants.DISABLE_INDEXER_MISSING_NAME);
		boolean foundValidIndexer = false;
		if (this.name.equals("jsp") || this.name.equals("javascript") || this.name.equals("java")
				|| this.name.equals("links") || this.name.equals("all"))
			foundValidIndexer = true;
		if (!foundValidIndexer)
			handleError(NLS.bind(NLSMessageConstants.DISABLE_INDEXER_INVALID_NAME, this.name));
	}

	private final void disableAllIndexers() throws Exception {
		disableJSPIndexer();
		disableJavaScriptIndexer();
		disableJavaIndexer();
		disableLinksIndexer();
	}

	@SuppressWarnings("restriction")
	private final void disableJSPIndexer() throws Exception {
		JSPIndexManager.getDefault().stop();
		log(NLSMessageConstants.DISABLE_INDEXER_JSP_INDEXER);
	}

	@SuppressWarnings("restriction")
	private final void disableJavaScriptIndexer() {
		JavaModelManager.getJavaModelManager().shutdown();
		log(NLSMessageConstants.DISABLE_INDEXER_JS_INDEXER);
	}

	@SuppressWarnings("restriction")
	private final void disableJavaIndexer() {
		JavaModelManager.getIndexManager().shutdown();
		log(NLSMessageConstants.DISABLE_INDEXER_JAVA_INDEXER);
	}

	private final void disableLinksIndexer() throws Exception {
//		ServiceTracker serviceTracker = new ServiceTracker(AntCorePlugin.getPlugin().getBundle().getBundleContext(),
//				PackageAdmin.class.getName(), null);
//		serviceTracker.open();
//		PackageAdmin packageAdmin = (PackageAdmin) serviceTracker.getService();
//		if (packageAdmin != null) {
//			Bundle referencesBundle = packageAdmin.getBundle(ReferenceManager.class);
//			if (referencesBundle != null)
//				referencesBundle.stop();
//		}
		log(NLSMessageConstants.DISABLE_INDEXER_LINKS_INDEXER);
	}

	public final void setName(String indexerName) {
		this.name = indexerName;
	}

	private String name = null;

	private static final String ALL_INDEXER_IDENTIFIER = "all";

	private static final String JSP_INDEXER_IDENTIFIER = "jsp";

	private static final String JAVA_INDEXER_IDENTIFIER = "java";

	private static final String JS_INDEXER_IDENTIFIER = "javascript";

	private static final String LINKS_INDEXER_IDENTIFIER = "links";
}
