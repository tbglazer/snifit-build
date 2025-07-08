package il.co.fibi.snifit.etools.ant.extras;


import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.tools.ant.BuildException;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.core.runtime.IBundleGroup;
import org.eclipse.core.runtime.IBundleGroupProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;

public class ProductDebug extends FailOnErrorTask {
  public void execute() throws BuildException {
    super.execute();
    MonitorHelper provider = new MonitorHelper(this);
    IProgressMonitor monitor = provider.createProgressGroup();
    try {
      validateAttributes(monitor);
      if (this.action.equals("pauseApp")) {
        pause();
      } else if (this.action.equals("listFeatures")) {
        IBundleGroup[] allBundleGroups = getFilteredBundleGroups();
        for (int i = 0; i < allBundleGroups.length; i++) {
          String bundleGroupID = allBundleGroups[i].getIdentifier();
          String bundleGroupName = allBundleGroups[i].getName();
          String bundleGroupProvider = allBundleGroups[i].getProviderName();
          String bundleGroupVersion = allBundleGroups[i].getVersion();
          String bundleGroupDesc = allBundleGroups[i].getDescription();
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_ACTION_FEATURE_DETAILS, bundleGroupID));
          if (bundleGroupName != null)
            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_NAME, bundleGroupName)); 
          if (bundleGroupProvider != null)
            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_PROVDER, bundleGroupProvider)); 
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_VERSION, bundleGroupVersion));
          if (bundleGroupDesc != null)
            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_DESCRIPTION, bundleGroupDesc)); 
//          if (allBundleGroups[i] instanceof FeatureEntry) {
//            FeatureEntry feature = (FeatureEntry)allBundleGroups[i];
//            SiteEntry entry = feature.getSite();
//            StringBuffer url = new StringBuffer();
//            if (entry != null)
//              url.append(entry.getResolvedURL().toExternalForm()); 
//            url.append(feature.getURL());
//            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_LOCATION, url.toString()));
//          } 
        } 
      } else if (this.action.equals("listPlugins")) {
        Bundle[] allBundles = getFilteredBundles();
        for (int i = 0; i < allBundles.length; i++) {
          String bundleSymName = allBundles[i].getSymbolicName();
          String bundleName = (String)allBundles[i].getHeaders(null).get("Bundle-Name");
          String bundleProvider = (String)allBundles[i].getHeaders(null).get("Bundle-Vendor");
          String bundleVersion = allBundles[i].getVersion().toString();
          String bundleLocation = allBundles[i].getLocation();
          String bundleState = getBundleState(allBundles[i].getState());
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_ACTION_BUNDLE_DETAILS, bundleSymName));
          if (bundleName != null)
            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_NAME, bundleName)); 
          if (bundleProvider != null)
            log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_PROVDER, bundleProvider)); 
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_VERSION, bundleVersion));
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_LOCATION, bundleLocation));
          log(NLS.bind(NLSMessageConstants.DEBUG_TASK_BUNDLE_STATE, bundleState));
        } 
      } 
    } finally {
      monitor.done();
      provider.dispose();
    } 
  }
  
  protected final void validateAttributes(IProgressMonitor monitor) throws BuildException {
    if (this.action == null) {
      handleError(NLSMessageConstants.DEBUG_TASK_ACTION_MISSING);
    } else if (!this.action.equals("listFeatures") && 
      !this.action.equals("pauseApp") && 
      !this.action.equals("listPlugins")) {
      handleError(
          
          NLS.bind(NLSMessageConstants.DEBUG_TASK_ACTION_INVALID, (Object[])new String[] { this.action, "listFeatures", "pauseApp", "listPlugins" }));
    } 
  }
  
  private final void pause() {
    log(NLSMessageConstants.DEBUG_TASK_PROMPT_FOR_PAUSE);
    while (true) {
      try {
        while (true)
          Thread.sleep(200L); 
      } catch (InterruptedException interruptedException) {}
    } 
  }
  
  private final IBundleGroup[] getFilteredBundleGroups() {
    List<IBundleGroup> allBundleGroups = new ArrayList<IBundleGroup>(400);
    IBundleGroupProvider[] allBundleGroupProviders = Platform.getBundleGroupProviders();
    for (int i = 0; i < allBundleGroupProviders.length; i++) {
      IBundleGroup[] bundleGroups = allBundleGroupProviders[i].getBundleGroups();
      for (int j = 0; j < bundleGroups.length; j++)
        allBundleGroups.add(bundleGroups[j]); 
    } 
    List<IBundleGroup> filteredBundleGroups = null;
    if (this.filters.size() > 0) {
      filteredBundleGroups = new ArrayList<IBundleGroup>(400);
      Iterator<IBundleGroup> bundleGroupIterator = allBundleGroups.iterator();
      while (bundleGroupIterator.hasNext()) {
        IBundleGroup bundleGroup = bundleGroupIterator.next();
        String bundleGroupID = bundleGroup.getIdentifier();
        Iterator<ProductDebugFilter> filterIterator = this.filters.iterator();
        while (filterIterator.hasNext()) {
          String filterName = ((ProductDebugFilter)filterIterator.next()).getName();
          if (filterName.equals(bundleGroupID))
            filteredBundleGroups.add(bundleGroup); 
        } 
      } 
    } else {
      filteredBundleGroups = allBundleGroups;
    } 
    return filteredBundleGroups.<IBundleGroup>toArray(new IBundleGroup[filteredBundleGroups.size()]);
  }
  
  private final Bundle[] getFilteredBundles() {
    Bundle[] allBundles;
    if (this.filters.size() == 0) {
      allBundles = AntCorePlugin.getPlugin().getBundle().getBundleContext().getBundles();
    } else {
      List<Bundle> newBundles = new ArrayList<Bundle>(this.filters.size());
      Iterator<ProductDebugFilter> filterIterator = this.filters.iterator();
      while (filterIterator.hasNext()) {
        String bundleName = ((ProductDebugFilter)filterIterator.next()).getName();
        Bundle b = Platform.getBundle(bundleName);
        if (b != null) {
          newBundles.add(b);
          continue;
        } 
        log(NLS.bind(NLSMessageConstants.DEBUG_TASK_FILTER_NO_BUNDLE_FOUND, bundleName), 
            0);
      } 
      allBundles = newBundles.<Bundle>toArray(new Bundle[newBundles.size()]);
    } 
    return allBundles;
  }
  
  private final String getBundleState(int state) {
    String stateAsString = null;
    switch (state) {
      case 32:
        stateAsString = "ACTIVE";
        return stateAsString;
      case 2:
        stateAsString = "INSTALLED";
        return stateAsString;
      case 1:
        stateAsString = "UNINSTALLED";
        return stateAsString;
      case 4:
        stateAsString = "RESOLVED";
        return stateAsString;
      case 8:
        stateAsString = "STARTING";
        return stateAsString;
      case 16:
        stateAsString = "STOPPING";
        return stateAsString;
    } 
    stateAsString = "UNKNOWN";
    return stateAsString;
  }
  
  public ProductDebugFilter createFilter() {
    ProductDebugFilter filter = new ProductDebugFilter();
    this.filters.add(filter);
    return filter;
  }
  
  public final void setAction(String act) {
    this.action = act;
  }
  
  private List<ProductDebugFilter> filters = new ArrayList<ProductDebugFilter>();
  
  private String action = null;
  
  private static final String ACTION_LIST_PLUGINS = "listPlugins";
  
  private static final String ACTION_LIST_FEATURES = "listFeatures";
  
  private static final String ACTION_PAUSE_APP = "pauseApp";
}
