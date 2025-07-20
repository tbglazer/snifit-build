package il.co.fibi.snifit.ant.extras.runner;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

public class AntBundleActivator extends Plugin {
  
  public static final String PLUGIN_ID = "il.co.fibi.snifit.ant.extras.runner";
  
  private static AntBundleActivator plugin = null;
  
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
  }
  
  public void stop(BundleContext arg0) throws Exception {}
  
  public static final AntBundleActivator getInstance() {
    return plugin;
  }
}
