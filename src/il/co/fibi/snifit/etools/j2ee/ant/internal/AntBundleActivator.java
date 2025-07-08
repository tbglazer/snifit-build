package il.co.fibi.snifit.etools.j2ee.ant.internal;

import java.util.Hashtable;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.BundleContext;

public class AntBundleActivator extends Plugin {
  private static DebugTrace trace = null;
  
  public static final String PLUGIN_ID = "il.co.fibi.snifit.etools.j2ee.ant";
  
  private static AntBundleActivator plugin = null;
  
  public void start(BundleContext context) throws Exception {
    super.start(context);
    plugin = this;
    AntTrace internalTrace = new AntTrace();
    Hashtable<String, String> props = new Hashtable<>(1);
    props.put("listener.symbolic.name", PLUGIN_ID);
    context.registerService(DebugOptionsListener.class.getName(), internalTrace, props);
    trace = internalTrace.getDebugTrace();
  }
  
  public void stop(BundleContext arg0) throws Exception {}
  
  public static DebugTrace getDebugTrace() {
    return trace;
  }
  
  public static final AntBundleActivator getInstance() {
    return plugin;
  }
}
