package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.wst.validation.ValidationFramework;

public class ProjectBuild extends FailOnErrorTask {
  private String projectName;
  
  private String propertyCountName = "ProjectErrorCount";
  
  private String propertyMessagesName = "ProjectErrorMessages";
  
  private String buildTypeString = "INCREMENTAL";
  
  private boolean isQuiet = false;
  
  private boolean summary = false;
  
  private boolean showErrors = true;
  
  private boolean countValidationErrors = true;
  
  private String severityString = "ERROR";
  
  private boolean debugCompilation;
  
  private boolean debugOptionsSpecified = false;
  
  private int buildTypeInt = 10;
  
  private String EMPTY_STRING = "";
  
  private boolean disableValidators = false;
  
  private IProject workspaceProject = null;
  
  private String localVariable;
  
  private String lineNumber;
  
  private String sourceFile;
  
  public void execute() throws BuildException {
    MonitorHelper provider = new MonitorHelper(this);
    IProgressMonitor monitor = provider.createProgressGroup();
    try {
      validateAttributes(monitor);
      if (isDisableValidators())
        ValidationFramework.getDefault().suspendValidation(this.workspaceProject, true); 
      setDebugOptions(this.debugCompilation);
      monitor.beginTask(ResourceHandler.getString("Common.buildingProject", this.projectName), 0);
      ((AntConsoleProgressMonitor)monitor).setSummary(this.summary);
      ((AntConsoleProgressMonitor)monitor).setQuiet(this.isQuiet);
      try {
        this.workspaceProject.refreshLocal(1, monitor);
        BuildUtilities.buildProject(this.workspaceProject, this.buildTypeInt, this, monitor);
      } catch (CoreException e) {
        String msg = e.getMessage();
        if (msg == null)
          msg = e.getClass().toString(); 
        handleError(ResourceHandler.getString("Common.coreException", msg), (Exception)e);
      } catch (InterruptedException interruptedEx) {
        String msg = interruptedEx.getMessage();
        if (msg == null)
          msg = interruptedEx.getClass().toString(); 
        handleError(msg, interruptedEx);
      } finally {
        ProjectGetErrors projectGetErrors1 = new ProjectGetErrors();
        projectGetErrors1.setProject(getProject());
        projectGetErrors1.setShowerrors(this.showErrors);
        projectGetErrors1.setSeveritylevel(this.severityString);
        projectGetErrors1.setCountValidationErrors(this.countValidationErrors);
        projectGetErrors1.setFailOnError(isFailOnError());
        projectGetErrors1.setProjectName(this.projectName);
        monitor.done();
        restoreDebugOptions();
        int i;
        if ((i = projectGetErrors1.getErrorCount(this.workspaceProject)) == 0) {
          getProject().setUserProperty(this.propertyMessagesName, this.EMPTY_STRING);
          getProject().setUserProperty(this.propertyCountName, Integer.toString(i));
        } else {
          getProject().setUserProperty(this.propertyMessagesName, 
              projectGetErrors1.messages.split("\n")[0]);
          getProject().setUserProperty(this.propertyCountName, Integer.toString(i));
          handleError(ResourceHandler.getString(
                "ProjectGetErrors.displayFailOnError", 
                (Object[])new String[] { this.projectName, Integer.toString(i), 
                  projectGetErrors1.getProject().getUserProperty(this.propertyMessagesName) }));
        } 
        projectGetErrors1 = null;
        log(ResourceHandler.getString("ProjectBuild.finished"));
      } 
    } finally {
      monitor.done();
      provider.dispose();
    } 
  }
  
  public ProjectBuild() {
    this.localVariable = "";
    this.lineNumber = "";
    this.sourceFile = "";
    setTaskName("projectBuild");
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
    options.put("org.eclipse.jdt.core.compiler.debug.localVariable", this.localVariable);
    options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", this.lineNumber);
    options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", this.sourceFile);
    JavaCore.setOptions(options);
  }
  
  public void setProjectName(String name) {
    this.projectName = name;
  }
  
  public void setBuildtype(String type) {
    this.buildTypeString = type;
  }
  
  public void setDebugcompilation(boolean b) {
    this.debugCompilation = b;
    this.debugOptionsSpecified = true;
  }
  
  public void setSummary(boolean b) {
    this.summary = b;
  }
  
  public void setShowerrors(boolean b) {
    this.showErrors = b;
  }
  
  public void setCountValidationErrors(boolean b) {
    this.countValidationErrors = b;
  }
  
  public void setQuiet(boolean b) {
    this.isQuiet = b;
  }
  
  public void setPropertycountname(String name) {
    this.propertyCountName = name;
  }
  
  public void setPropertymessagesname(String name) {
    this.propertyMessagesName = name;
  }
  
  public void setSeveritylevel(String s) {
    this.severityString = s;
  }
  
  protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
    if (AntTrace.EXTRAS_TRACE_ENABLED) {
      AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras", 
          "Printing out value of all " + getTaskName() + " passed attributes");
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"projectName\":" + this.projectName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"buildType\":" + this.buildTypeString);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"propertyCountName\":" + this.propertyCountName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"propertyMessagesName\":" + this.propertyMessagesName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"disableValidators\":" + this.disableValidators);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"quiet\":" + this.isQuiet);
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
    if (this.projectName == null)
      handleError(ResourceHandler.getString("Common.missingProjectName")); 
    this.workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
    if (this.workspaceProject == null) {
      handleError(ResourceHandler.getString("Common.projectNameNull", this.projectName));
      return;
    } 
    if (!this.workspaceProject.exists()) {
      handleError(
          ResourceHandler.getString("Common.projectNameNotInWorkspace", this.projectName));
      return;
    } 
    if (!this.workspaceProject.isOpen()) {
      handleError(ResourceHandler.getString("Common.projectNotOpen", this.projectName));
      return;
    } 
    if (this.buildTypeString.equalsIgnoreCase("INCREMENTAL")) {
      this.buildTypeInt = 10;
    } else if (this.buildTypeString.equalsIgnoreCase("FULL")) {
      this.buildTypeInt = 6;
    } else if (this.buildTypeString.equalsIgnoreCase("AUTO")) {
      this.buildTypeInt = 9;
      log(ResourceHandler.getString("Common.autoDeprecated"));
    } else if (this.buildTypeString.equalsIgnoreCase("CLEAN")) {
      this.buildTypeInt = 15;
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
