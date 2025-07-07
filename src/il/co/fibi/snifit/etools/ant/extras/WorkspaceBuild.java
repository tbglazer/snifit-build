package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import il.co.fibi.snifit.etools.performance.monitor.core.IDynamicPerformanceMonitor;
import il.co.fibi.snifit.etools.performance.monitor.core.PerformanceMonitorFactory;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.validation.ValidationFramework;

public class WorkspaceBuild extends FailOnErrorTask {
  private String buildTypeString = "INCREMENTAL";
  
  private int buildType = 10;
  
  private boolean debugCompilation;
  
  private boolean debugOptionsSpecified = false;
  
  private boolean isQuiet = false;
  
  private boolean showErrors = true;
  
  private boolean countValidationErrors = true;
  
  private String encoding = null;
  
  private String severityString = "ERROR";
  
  private String propertyCountName = "WorkspaceErrorCount";
  
  private String propertyMessagesName = "WorkspaceErrorMessages";
  
  private boolean summary = false;
  
  private WorkspaceGetErrors workspaceGetErrors = null;
  
  private boolean disableValidators = false;
  
  private String localVariable;
  
  private String lineNumber;
  
  private String sourceFile;
  
  public void execute() throws BuildException {
    IDynamicPerformanceMonitor pm = PerformanceMonitorFactory.getDefault().getDynamicPerformanceMonitor(
        "com.ibm.etools.j2ee.ant/perf/build/workspaceBuild", this);
    pm.startRun();
    MonitorHelper provider = new MonitorHelper(this);
    IProgressMonitor monitor = provider.createProgressGroup();
    try {
      validateAttributes(monitor);
      if (isDisableValidators())
        ValidationFramework.getDefault().suspendAllValidation(true); 
      IWorkspace workspace = ResourcesPlugin.getWorkspace();
      if (workspace == null) {
        handleError(ResourceHandler.getString("WorkspaceBuild.nullWorkspace"));
        return;
      } 
      String originalEncoding = null;
      setDebugOptions(this.debugCompilation);
      try {
        monitor.beginTask(
            ResourceHandler.getString("Common.buildingWorkspace", this.buildTypeString), 0);
        ((AntConsoleProgressMonitor)monitor).setQuiet(this.isQuiet);
        ((AntConsoleProgressMonitor)monitor).setSummary(this.summary);
        if (this.encoding != null) {
          log(
              ResourceHandler.getString("WorkspaceBuild.setFileEncoding", this.encoding));
          originalEncoding = System.getProperty("file.encoding");
          System.setProperty("file.encoding", this.encoding);
        } 
        if (this.buildType == 6) {
          monitor.beginTask("WorkspaceBuild", 0);
          workspace.getRoot().refreshLocal(2, monitor);
          monitor.beginTask("WorkspaceBuild", 0);
        } 
        BuildUtilities.buildWorkspace(workspace, this.buildType, this, monitor);
      } catch (CoreException e) {
        String msg = e.getMessage();
        if (msg == null)
          msg = e.getClass().toString(); 
        handleError(ResourceHandler.getString("Common.coreException", msg), (Exception)e);
      } catch (OperationCanceledException operationCanceledException) {
        monitor.setCanceled(true);
      } catch (InterruptedException interruptedEx) {
        String msg = interruptedEx.getMessage();
        if (msg == null)
          msg = interruptedEx.getClass().toString(); 
        handleError(msg, interruptedEx);
      } finally {
        restoreDebugOptions();
        monitor.done();
        this.workspaceGetErrors = new WorkspaceGetErrors();
        this.workspaceGetErrors.setProject(getProject());
        this.workspaceGetErrors.setshowerrors(this.showErrors);
        this.workspaceGetErrors.setquiet(this.isQuiet);
        this.workspaceGetErrors.setpropertycountname(this.propertyCountName);
        this.workspaceGetErrors.setpropertymessagesname(this.propertyMessagesName);
        this.workspaceGetErrors.setseveritylevel(this.severityString);
        this.workspaceGetErrors.setCountValidationErrors(this.countValidationErrors);
        this.workspaceGetErrors.setFailOnError(isFailOnError());
        this.workspaceGetErrors.loopThroughProjects();
        getProject().setUserProperty(this.propertyCountName, 
            Integer.toString(this.workspaceGetErrors.errorCount));
        getProject().setUserProperty(this.propertyMessagesName, this.workspaceGetErrors.errorMessages);
        log(
            ResourceHandler.getString("WorkspaceBuild.finished", Integer.toString(this.workspaceGetErrors.errorCount)));
        this.workspaceGetErrors = null;
        if (originalEncoding != null) {
          log(
              ResourceHandler.getString("WorkspaceBuild.unsetFileEncoding", originalEncoding));
          System.setProperty("file.encoding", originalEncoding);
        } 
      } 
    } finally {
      monitor.done();
      provider.dispose();
      pm.endRun();
    } 
  }
  
  public WorkspaceBuild() {
    this.localVariable = "";
    this.lineNumber = "";
    this.sourceFile = "";
    setTaskName("workspaceBuild");
  }
  
  public void setDebugOptions(boolean b) {
    if (!this.debugOptionsSpecified)
      return; 
    Hashtable<String, String> options = JavaCore.getOptions();
    this.localVariable = options.get("org.eclipse.jdt.core.compiler.debug.localVariable");
    this.lineNumber = options.get("org.eclipse.jdt.core.compiler.debug.lineNumber");
    this.sourceFile = options.get("org.eclipse.jdt.core.compiler.debug.sourceFile");
    if (b) {
      options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
      options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
      options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
    } else {
      options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "do not generate");
      options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "do not generate");
      options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "do not generate");
    } 
    JavaCore.setOptions(options);
  }
  
  public void restoreDebugOptions() {
    if (!this.debugOptionsSpecified)
      return; 
    Hashtable<String, String> options = JavaCore.getOptions();
    if (this.localVariable != null) {
      options.put("org.eclipse.jdt.core.compiler.debug.localVariable", this.localVariable);
    } else {
      options.remove("org.eclipse.jdt.core.compiler.debug.localVariable");
    } 
    if (this.lineNumber != null) {
      options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", this.lineNumber);
    } else {
      options.remove("org.eclipse.jdt.core.compiler.debug.lineNumber");
    } 
    if (this.sourceFile != null) {
      options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", this.sourceFile);
    } else {
      options.remove("org.eclipse.jdt.core.compiler.debug.sourceFile");
    } 
    JavaCore.setOptions(options);
  }
  
  public void setbuildtype(String type) {
    this.buildTypeString = type;
  }
  
  public void setdebugcompilation(boolean b) {
    this.debugCompilation = b;
    this.debugOptionsSpecified = true;
  }
  
  public void setsummary(boolean b) {
    this.summary = b;
  }
  
  public void setshowerrors(boolean b) {
    this.showErrors = b;
  }
  
  public void setCountValidationErrors(boolean b) {
    this.countValidationErrors = b;
  }
  
  public void setquiet(boolean b) {
    this.isQuiet = b;
  }
  
  public void setpropertycountname(String name) {
    this.propertyCountName = name;
  }
  
  public void setpropertymessagesname(String name) {
    this.propertyMessagesName = name;
  }
  
  public void setseveritylevel(String s) {
    this.severityString = s;
  }
  
  protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
    if (AntTrace.EXTRAS_TRACE_ENABLED) {
      AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras", 
          "Printing out value of all " + getTaskName() + " passed attributes");
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"buildType\":" + this.buildTypeString);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"propertyCountName\":" + this.propertyCountName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"propertyMessagesName\":" + this.propertyMessagesName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"disableValidators\":" + this.disableValidators);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"countValidationErrors\":" + this.countValidationErrors);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"severityLevel\":" + this.severityString);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"showErrors\":" + this.showErrors);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"debugCompilation\":" + this.debugCompilation);
      AntBundleActivator.getDebugTrace().traceExit("/debug/antextras", 
          "Value of attribute \"failOnError\":" + this.failOnError);
    } 
    if (this.buildTypeString.equalsIgnoreCase("INCREMENTAL")) {
      this.buildType = 10;
    } else if (this.buildTypeString.equalsIgnoreCase("FULL")) {
      this.buildType = 6;
    } else if (this.buildTypeString.equalsIgnoreCase("AUTO")) {
      this.buildType = 9;
      log(ResourceHandler.getString("Common.autoDeprecated"));
    } else {
      handleError(
          ResourceHandler.getString("Common.invalidBuildType", this.buildTypeString));
    } 
    if (!this.severityString.equalsIgnoreCase("ERROR"))
      if (!this.severityString.equalsIgnoreCase("WARNING") && !this.severityString.equalsIgnoreCase("WARN"))
        if (!this.severityString.equalsIgnoreCase("INFO") && !this.severityString.equalsIgnoreCase("INFORMATION"))
          handleError(
              ResourceHandler.getString("Common.invalidSeverityLevel", this.severityString));   
  }
  
  public final void setDisableValidators(boolean value) {
    this.disableValidators = value;
  }
  
  public final boolean isDisableValidators() {
    return this.disableValidators;
  }
}
