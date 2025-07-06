package il.co.fibi.snifit.support.trace.core;

import il.co.fibi.snifit.support.trace.internal.Activator;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.Bundle;
import org.osgi.service.packageadmin.PackageAdmin;

public abstract class InternalTrace implements DebugOptionsListener {
  private final String symbolicName;
  
  private InternalDebugTrace traceObject;
  
  private static final String NULL_OBJECT_STRING_VALUE = "<null>";
  
  public InternalTrace(String symbolicBundleName) {
    this.traceObject = null;
    this.symbolicName = symbolicBundleName;
  }
  
  public final void optionsChanged(DebugOptions options) {
    if (this.traceObject == null) {
      DebugTrace osgiTraceObject = options.newDebugTrace(this.symbolicName, getClass());
      this.traceObject = new InternalDebugTrace(osgiTraceObject);
    } 
    updateOptions(options);
  }
  
  protected abstract void updateOptions(DebugOptions paramDebugOptions);
  
  public final DebugTrace getDebugTrace() {
    return this.traceObject;
  }
  
  public static String convertToString(String label, Object[] array) {
    StringBuilder result = new StringBuilder(label);
    result.append(": ");
    if (array != null) {
      result.append("[type: " + array.getClass().getName());
      result.append(", size: " + array.length);
      result.append(", contents: (");
      for (int i = 0; i < array.length; i++) {
        if (array[i] != null) {
          result.append(array[i].toString());
        } else {
          result.append("<null>");
        } 
        if (i + 1 < array.length)
          result.append(", "); 
      } 
      result.append(")]");
    } else {
      result.append("<null>");
    } 
    return result.toString();
  }
  
  public static String convertToString(String label, Collection<?> collection) {
    StringBuilder result = new StringBuilder(label);
    result.append(": ");
    if (collection != null) {
      result.append("[type: " + collection.getClass().getName());
      result.append(", size: " + collection.size());
      result.append(", contents: (");
      Iterator<?> collectionIterator = collection.iterator();
      while (collectionIterator.hasNext()) {
        Object entry = collectionIterator.next();
        if (entry != null) {
          result.append(entry.toString());
        } else {
          result.append("<null>");
        } 
        if (collectionIterator.hasNext())
          result.append(", "); 
      } 
      result.append(")]");
    } else {
      result.append("<null>");
    } 
    return result.toString();
  }
  
  public static String convertToString(String label, Map<?, ?> map) {
    StringBuilder result = new StringBuilder(label);
    result.append(": ");
    if (map != null) {
      result.append("[type: " + map.getClass().getName());
      result.append(", size: " + map.size());
      result.append(", contents: (");
      Iterator<?> mapIterator = map.entrySet().iterator();
      while (mapIterator.hasNext()) {
        Map.Entry<?, ?> entry = (Map.Entry<?, ?>)mapIterator.next();
        result.append(entry.getKey());
        result.append("=");
        Object value = entry.getValue();
        if (value != null) {
          result.append(value.toString());
        } else {
          result.append("<null>");
        } 
        if (mapIterator.hasNext())
          result.append(", "); 
      } 
      result.append(")]");
    } else {
      result.append("<null>");
    } 
    return result.toString();
  }
  
  public static String convertToString(String label, Object obj) {
    StringBuilder result = new StringBuilder(label);
    result.append(": ");
    if (obj != null) {
      result.append("[type: " + obj.getClass().getName());
      result.append(", value: ");
      result.append(obj.toString());
      result.append("]");
    } else {
      result.append("<null>");
    } 
    return result.toString();
  }
  
  public static final DebugOptions getDebugOptions() {
    return Activator.getInstance().getDebugOptions();
  }
  
  public final void enableDevelopmentMode(boolean state) {
    DebugOptions dbgOptions = getDebugOptions();
    if (dbgOptions != null)
      dbgOptions.setDebugEnabled(!state); 
  }
  
  public static final void setUpJUnit(String bundleId) {
    PackageAdmin pkgAdmin = Activator.getInstance().getPackageAdmin();
    Bundle theBundle = null;
    if (pkgAdmin != null) {
      Bundle[] bundles = pkgAdmin.getBundles(bundleId, null);
      if (bundles != null)
        theBundle = bundles[0]; 
    } 
    if (theBundle != null) {
      URL optionFileEntry = theBundle.getEntry("/.options");
      if (optionFileEntry != null) {
        Properties entries = new Properties();
        try {
          entries.load(optionFileEntry.openStream());
        } catch (IOException ex) {
          ex.printStackTrace();
        } 
        Iterator<Map.Entry<Object, Object>> entriesIterator = entries.entrySet().iterator();
        Map<String, String> debugEntries = new HashMap<String, String>();
        while (entriesIterator.hasNext()) {
          Map.Entry<Object, Object> entry = entriesIterator.next();
          String value = (String)entry.getValue();
          if (Boolean.FALSE.toString().equals(value))
            debugEntries.put((String)entry.getKey(), Boolean.TRUE.toString()); 
        } 
        DebugOptions debugOptions = getDebugOptions();
        if (debugOptions != null) {
          debugOptions.setDebugEnabled(true);
          debugOptions.setOptions(debugEntries);
        } 
      } 
    } 
  }
}
