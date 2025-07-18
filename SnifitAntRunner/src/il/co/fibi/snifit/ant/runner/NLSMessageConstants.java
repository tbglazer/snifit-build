package il.co.fibi.snifit.ant.runner;

import org.eclipse.osgi.util.NLS;

public class NLSMessageConstants extends NLS {
  public static String COMMON_JOBS_STILL_RUNNING;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_INITIAL;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_TEMP;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_SAVE_FAILED;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_PREVIOUS_FAILED_INIT;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_RESTORED_AUTO_BUILD;
  
  public static String HEADLESS_WORKSPACE_SETTINGS_RESTORE_FAILED;
  
  private static final String BUNDLE_NAME = "il.co.fibi.snifit.ant.runner.NLSMessages";
  
  static {
    NLS.initializeMessages(BUNDLE_NAME, NLSMessageConstants.class);
  }
}
