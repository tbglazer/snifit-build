package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;

public class GetProjectData extends FailOnErrorTask {
  private IProject workspaceProject = null;
  
  private String projectName = null;
  
  private String projectProperty = "projectName";
  
  private String workspaceProperty = "workspaceName";
  
  private String locationProperty = "locationName";
  
  private String natureProperty = "natureName";
  
  private String hasSpecifiedNature = "";
  
  private String hasSpecifiedNatureProperty = "";
  
  public GetProjectData() {
    setTaskName("getProjectData");
  }
  
  public void execute() throws BuildException {
    super.execute();
    MonitorHelper provider = new MonitorHelper(this);
    IProgressMonitor monitor = provider.createProgressGroup();
    validateAttributes(monitor);
    String workspaceName = "?workspace?";
    String natureName = "?nature?";
    String locationName = "?location?";
    try {
      workspaceName = ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString();
      locationName = this.workspaceProject.getLocation().toOSString();
      if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, "jst.appclient")) {
        natureName = "AppClient";
      } else if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, 
          "jst.connector")) {
        natureName = "RarConnector";
      } else if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, "jst.ear")) {
        natureName = "EAR";
      } else if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, "jst.ejb")) {
        natureName = "EJB";
      } else if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, "jst.utility")) {
        natureName = "Utility";
      } else if (JavaEEProjectUtilities.isProjectOfType(this.workspaceProject, "jst.web")) {
        natureName = "WAR";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.EJB2_0Nature")) {
        natureName = "EJB";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.WebNature")) {
        natureName = "WAR";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.StaticWebNature")) {
        natureName = "StaticWeb";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.web.StaticWebNature")) {
        natureName = "StaticWeb";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.EAR13Nature")) {
        natureName = "EAR";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.ApplicationClient_J2EE13_Nature")) {
        natureName = "AppClient";
      } else if (this.workspaceProject.hasNature("org.eclipse.jst.j2ee.internal.ConnectorNature")) {
        natureName = "RarConnector";
      } else if (this.workspaceProject.hasNature("org.eclipse.jdt.core.javanature")) {
        natureName = "Java";
      } else if (this.workspaceProject.hasNature("org.eclipse.jdt.core.javanature")) {
        natureName = "Java";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.ctc.javaprojectnature")) {
        natureName = "Java";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.server.core.nature")) {
        natureName = "Server";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.portal.tools.PortletProjectNature")) {
        natureName = "Portlet";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.struts.StrutsNature")) {
        natureName = "Struts";
      } else if (this.workspaceProject.hasNature("com.ibm.etools.jsf.JSFNature")) {
        natureName = "JSF";
      } else {
        natureName = "UNKNOWN";
        String[] natures = this.workspaceProject.getDescription().getNatureIds();
        if (natures != null) {
          for (int i = 0; i < natures.length; i++)
            natureName = String.valueOf(natureName) + "," + natures[i] + ","; 
        } else {
          natureName = "???nature??? ";
        } 
      } 
      log(ResourceHandler.getString("GetProjectData.retrievedProjectData"));
      log(
          ResourceHandler.getString("Common.indentedKeyValuePair", (Object[])new String[] { this.workspaceProperty, workspaceName }));
      log(
          ResourceHandler.getString("Common.indentedKeyValuePair", (Object[])new String[] { this.natureProperty, natureName }));
      getProject().setUserProperty(this.projectProperty, this.projectName);
      getProject().setUserProperty(this.workspaceProperty, workspaceName);
      getProject().setUserProperty(this.locationProperty, locationName);
      getProject().setUserProperty(this.natureProperty, natureName);
      if (this.hasSpecifiedNature != null && this.hasSpecifiedNature.length() > 0) {
        String result = "false";
        if (this.workspaceProject.hasNature(this.hasSpecifiedNature))
          result = "true"; 
        if (result == "true") {
          getProject().setUserProperty("hasSpecifiedNature", this.hasSpecifiedNature);
        } else {
          getProject().setUserProperty("hasSpecifiedNature", "");
        } 
        if (this.hasSpecifiedNatureProperty != null && this.hasSpecifiedNatureProperty.length() > 0)
          if (result == "true") {
            getProject().setUserProperty(this.hasSpecifiedNatureProperty, "true");
          } else if (getProject().getUserProperty(this.hasSpecifiedNatureProperty) != null) {
            handleError(ResourceHandler.getString(
                  "GetProjectData.cannotUnsetNature", this.hasSpecifiedNatureProperty));
            getProject().setUserProperty(this.hasSpecifiedNatureProperty, "FALSE");
          }  
      } 
    } catch (CoreException e1) {
      handleError(e1.getMessage(), (Exception)e1);
    } catch (Exception e) {
      handleError(e.getMessage(), e);
    } finally {
      monitor.done();
      provider.dispose();
    } 
  }
  
  public void setBasedir(String basedir) {
    int slash = basedir.lastIndexOf("\\");
    int slash2 = basedir.lastIndexOf("/");
    if (slash2 > slash)
      slash = slash2; 
    if (slash >= 0) {
      this.projectName = basedir.substring(slash + 1);
    } else {
      this.projectName = basedir;
    } 
    log(ResourceHandler.getString("GetProjectData.setProjectName", this.projectName));
  }
  
  public void setProjectName(String name) {
    this.projectName = name;
  }
  
  public void setHasSpecifiedNature(String name) {
    this.hasSpecifiedNature = name;
  }
  
  public void setHasSpecifiedNatureProperty(String name) {
    this.hasSpecifiedNatureProperty = name;
  }
  
  public void setWorkspaceProperty(String name) {
    this.workspaceProperty = name;
  }
  
  public void setLocationProperty(String name) {
    this.locationProperty = name;
  }
  
  public void setNatureProperty(String name) {
    this.natureProperty = name;
  }
  
  protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
    if (AntTrace.EXTRAS_TRACE_ENABLED) {
      AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras", 
          "Printing out value of all " + getTaskName() + " passed attributes");
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"projectName\":" + this.projectName);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"workspaceProperty\":" + this.workspaceProperty);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"locationProperty\":" + this.locationProperty);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"natureProperty\":" + this.natureProperty);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"hasSpecifiedNature\":" + this.hasSpecifiedNature);
      AntBundleActivator.getDebugTrace().trace("/debug/antextras", 
          "Value of attribute \"hasSpecifiedNatureProperty\":" + this.hasSpecifiedNatureProperty);
      AntBundleActivator.getDebugTrace().traceExit("/debug/antextras", 
          "Value of attribute \"failOnError\":" + this.failOnError);
    } 
    if (this.projectName == null)
      handleError(ResourceHandler.getString("Common.missingProjectName")); 
    this.workspaceProject = ResourcesPlugin.getWorkspace().getRoot().getProject(this.projectName);
    if (this.workspaceProject == null)
      handleError(ResourceHandler.getString("Common.projectNameNull", this.projectName)); 
    if (!this.workspaceProject.exists())
      handleError(
          ResourceHandler.getString("Common.projectNameNotInWorkspace", this.projectName)); 
    if (!this.workspaceProject.isOpen())
      handleError(ResourceHandler.getString("Common.projectNotOpen", this.projectName)); 
  }
}
