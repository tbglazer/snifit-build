package il.co.fibi.snifit.support.trace.internal;

import org.eclipse.osgi.service.debug.DebugOptions;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.util.tracker.ServiceTracker;

public class Activator implements BundleActivator {
	private static BundleContext context;

	public static BundleContext getContext() {
		return context;
	}

	@SuppressWarnings("deprecation")
	public void start(BundleContext bundleContext) throws Exception {
		instance = this;
		context = bundleContext;
		this.bundleTracker = new ServiceTracker<>(context, PackageAdmin.class.getName(), null);
		this.bundleTracker.open();
		this.debugTracker = new ServiceTracker<>(bundleContext, DebugOptions.class.getName(), null);
		this.debugTracker.open();
	}

	public void stop(BundleContext bundleContext) throws Exception {
		if (this.bundleTracker != null) {
			this.bundleTracker.close();
			this.bundleTracker = null;
		}
		if (this.debugTracker != null) {
			this.debugTracker.close();
			this.debugTracker = null;
		}
		context = null;
	}

	public static final Activator getInstance() {
		return instance;
	}

	@SuppressWarnings("deprecation")
	public final PackageAdmin getPackageAdmin() {
		return (PackageAdmin) this.bundleTracker.getService();
	}

	public final DebugOptions getDebugOptions() {
		return (DebugOptions) this.debugTracker.getService();
	}

	private ServiceTracker<Object, Object> bundleTracker = null;

	private ServiceTracker<Object, Object> debugTracker = null;

	private static Activator instance = null;
}
