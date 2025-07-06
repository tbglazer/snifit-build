package il.co.fibi.snifit.ant.extras;

import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.ant.core.AntCorePlugin;
import org.eclipse.ant.core.AntCorePreferences;
import org.eclipse.ant.core.AntRunner;
import org.eclipse.ant.core.IAntClasspathEntry;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class RunAnt extends AntRunner {
  private static final int BACKGROUND_JOB_SLEEP_TIME = 250;
  
  private static final int BACKGROUND_JOB_DISPLAY_JOBS = 120000;
  
  public Object run(Object argArray) throws Exception {
    IJobManager jobMan;
    int waitTime;
    boolean displayMessage;
    URL[] classloaderURLs = getClasspathURLEntries();
    if (classloaderURLs != null && classloaderURLs.length > 0)
      setCustomClasspath(classloaderURLs); 
    System.setProperty("ant.headless.environment", "true");
    HeadlessWorkspaceSettingsHelper workspaceSettings = null;
    Object returnObj = null;
    try {
      workspaceSettings = new HeadlessWorkspaceSettingsHelper();
      JavaCore.initializeAfterLoad(null);
      returnObj = super.run(argArray);
    } finally {
      IJobManager iJobManager = Job.getJobManager();
      ResourcesPlugin.getWorkspace().save(true, null);
      int i = 0;
      boolean bool = true;
      while (!iJobManager.isIdle()) {
        Thread.sleep(BACKGROUND_JOB_SLEEP_TIME);
        if (i >= BACKGROUND_JOB_DISPLAY_JOBS) {
          displayAllJobsState(bool);
          bool = false;
          i = 0;
          continue;
        } 
        i += 250;
      } 
      iJobManager.suspend();
      if (workspaceSettings != null)
        workspaceSettings.restore(); 
    } 
    return returnObj;
  }
  
  private URL[] getClasspathURLEntries() {
    List<URL> bundleURL = new LinkedList<URL>();
    AntCorePreferences corePreferences = AntCorePlugin.getPlugin().getPreferences();
    IAntClasspathEntry[] antHomeCPEntries = corePreferences.getAntHomeClasspathEntries();
    if (antHomeCPEntries != null)
      for (int i = 0; i < antHomeCPEntries.length; i++)
        bundleURL.add(antHomeCPEntries[i].getEntryURL());  
    IAntClasspathEntry[] antAdditionalCPEntries = corePreferences.getAdditionalClasspathEntries();
    if (antAdditionalCPEntries != null)
      for (int i = 0; i < antAdditionalCPEntries.length; i++)
        bundleURL.add(antAdditionalCPEntries[i].getEntryURL());  
    IExtensionRegistry extRegistry = Platform.getExtensionRegistry();
    if (extRegistry != null) {
      IConfigurationElement[] bundlesCPEntries = extRegistry.getConfigurationElementsFor(
          "il.co.fibi.snifit.etools.j2ee.ant", "bundleClasspathEntries");
      for (int bundleIndex = 0; bundleIndex < bundlesCPEntries.length; bundleIndex++) {
        String bundleName = bundlesCPEntries[bundleIndex].getAttribute("name");
        if (bundleName != null) {
          Bundle bundle = getBundle(bundleName);
          if (bundle != null) {
            URL root = bundle.getEntry("/");
            bundleURL.add(root);
            IConfigurationElement[] libraries = bundlesCPEntries[bundleIndex].getChildren("library");
            for (int libIndex = 0; libIndex < libraries.length; libIndex++) {
              String libPath = libraries[libIndex].getAttribute("path");
              if (libPath != null) {
                URL libraryURL = bundle.getEntry(libPath);
                bundleURL.add(libraryURL);
              } 
            } 
          } 
        } 
      } 
    } 
    return bundleURL.<URL>toArray(new URL[bundleURL.size()]);
  }
  
  private final Bundle getBundle(String name) {
    BundleContext context = AntBundleActivator.getInstance().getBundle().getBundleContext();
    Bundle[] allBundles = context.getBundles();
    Bundle result = null;
    for (int i = 0; i < allBundles.length; i++) {
      String bundleSymName = allBundles[i].getSymbolicName();
      if (name.equals(bundleSymName)) {
        result = allBundles[i];
        break;
      } 
    } 
    return result;
  }
  
  private final void displayAllJobsState(boolean displayConsoleMessage) {
    IJobManager jobMan = Job.getJobManager();
    Job[] allJobs = jobMan.find(null);
    if (allJobs.length > 0) {
      boolean append = setTaskTraceFile();
      if (displayConsoleMessage)
        System.out.println(
            NLS.bind(NLSMessageConstants.COMMON_JOBS_STILL_RUNNING, taskTraceFile.getAbsolutePath())); 
      BufferedWriter bufferedWriter = null;
      try {
        bufferedWriter = new BufferedWriter(new FileWriter(taskTraceFile, append));
        bufferedWriter.append("# timestamp: " + 
            TRACE_FILE_DATE_FORMATTER.format(new Date(System.currentTimeMillis())));
        bufferedWriter.append(LINE_SEPARATOR);
        for (int i = 0; i < allJobs.length; i++) {
          bufferedWriter.write(getJobInfo(allJobs[i], i));
          bufferedWriter.append(LINE_SEPARATOR);
        } 
      } catch (IOException ioEx) {
        Status status = new Status(4, "com.ibm.etools.j2ee.ant", 0, ioEx.getMessage(), ioEx);
        AntCorePlugin.getPlugin().getLog().log((IStatus)status);
      } finally {
        if (bufferedWriter != null)
          try {
            bufferedWriter.close();
          } catch (IOException ioEx) {
            Status status = new Status(1, 
                "com.ibm.etools.j2ee.ant", 0, ioEx.getMessage(), ioEx);
            AntCorePlugin.getPlugin().getLog().log((IStatus)status);
          }  
      } 
    } 
  }
  
  private final boolean setTaskTraceFile() {
    boolean append = true;
    if (taskTraceFile == null) {
      File traceFileDirectory = Platform.getLogFileLocation().toFile().getParentFile();
      File[] existingTraceFiles = traceFileDirectory.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
              if (name.startsWith("taskTrace_") && name.endsWith("txt"))
                return true; 
              return false;
            }
          });
      StringBuffer fileName = new StringBuffer("taskTrace_");
      fileName.append(getNextTraceFileIndex(existingTraceFiles));
      fileName.append(".txt");
      taskTraceFile = new File(traceFileDirectory, fileName.toString());
      append = !taskTraceFile.exists();
    } 
    return append;
  }
  
  private final int getNextTraceFileIndex(File[] existingTraceFiles) {
    int newIndex = 0;
    if (existingTraceFiles.length > 0)
      if (existingTraceFiles.length == 10) {
        File newestFile = null;
        long modTime = 0L;
        for (int i = 0; i < existingTraceFiles.length; i++) {
          if (existingTraceFiles[i].lastModified() > modTime) {
            modTime = existingTraceFiles[i].lastModified();
            newestFile = existingTraceFiles[i];
          } 
        } 
        if (newestFile != null) {
          String traceFileName = newestFile.getName();
          String indexString = traceFileName.substring(10, traceFileName.length() - 4);
          newIndex = Integer.parseInt(indexString);
          if (newIndex == 9) {
            newIndex = 0;
          } else {
            newIndex++;
          } 
        } else {
          newIndex = 0;
        } 
      } else {
        boolean[] indexUsed = new boolean[10];
        int i;
        for (i = 0; i < existingTraceFiles.length; i++) {
          String traceFileName = existingTraceFiles[i].getName();
          String indexString = traceFileName.substring(10, traceFileName.length() - 4);
          int usedIndex = Integer.parseInt(indexString);
          indexUsed[usedIndex] = true;
        } 
        for (i = 0; i < indexUsed.length; i++) {
          if (!indexUsed[i]) {
            newIndex = i;
            break;
          } 
        } 
      }  
    return newIndex;
  }
  
  private final String getJobInfo(Job job, int jobIndex) {
    StringBuffer jobInfo = new StringBuffer("[");
    jobInfo.append(jobIndex);
    jobInfo.append("] name: ");
    jobInfo.append(job.getName());
    if (job.getThread() != null) {
      jobInfo.append(", thread: (");
      jobInfo.append(job.getThread().getId());
      jobInfo.append(") ");
      jobInfo.append(job.getThread().getName());
    } 
    jobInfo.append(", blocking: " + job.isBlocking());
    jobInfo.append(", system: " + job.isSystem());
    jobInfo.append(", user: " + job.isUser());
    jobInfo.append(", priority: ");
    switch (job.getPriority()) {
      case 10:
        jobInfo.append("Job.INTERACTIVE");
        break;
      case 20:
        jobInfo.append("Job.SHORT");
        break;
      case 30:
        jobInfo.append("Job.LONG");
        break;
      case 40:
        jobInfo.append("Job.BUILD");
        break;
      case 50:
        jobInfo.append("Job.DECORATE");
        break;
      default:
        jobInfo.append("Unknown");
        break;
    } 
    jobInfo.append(", state: ");
    switch (job.getState()) {
      case 4:
        jobInfo.append("Job.RUNNING");
        return jobInfo.toString();
      case 2:
        jobInfo.append("Job.WAITING");
        return jobInfo.toString();
      case 1:
        jobInfo.append("Job.SLEEPING");
        return jobInfo.toString();
      case 0:
        jobInfo.append("Job.NONE");
        return jobInfo.toString();
    } 
    jobInfo.append("Unknown");
    return jobInfo.toString();
  }
  
  private static final SimpleDateFormat TRACE_FILE_DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  
  private static final String LINE_SEPARATOR;
  
  static {
    String s = System.getProperty("line.separator");
    LINE_SEPARATOR = (s == null) ? "\n" : s;
  }
  
  private static File taskTraceFile = null;
}
