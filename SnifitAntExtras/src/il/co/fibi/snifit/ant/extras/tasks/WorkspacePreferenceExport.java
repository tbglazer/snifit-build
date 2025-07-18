package il.co.fibi.snifit.ant.extras.tasks;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;

public class WorkspacePreferenceExport extends FailOnErrorTask {
  private String preferenceFileName;
  
  private boolean overwrite = false;
  
  public final void execute() throws BuildException {
    super.execute();
    MonitorHelper provider = new MonitorHelper(this);
    IProgressMonitor monitor = provider.createProgressGroup();
    validateAttributes(monitor);
    try {
      IPreferencesService service = Platform.getPreferencesService();
      File preferenceFile = new File(getPreferenceFileName());
      service.exportPreferences(service.getRootNode(), new FileOutputStream(preferenceFile), null);
    } catch (FileNotFoundException e) {
      String errorMessage = e.getMessage();
      handleError(errorMessage, e);
    } catch (CoreException e) {
      String errorMessage = e.getStatus().getMessage();
      handleError(errorMessage, (Exception)e);
    } finally {
      monitor.done();
      provider.dispose();
    } 
  }
  
  public String getPreferenceFileName() {
    return this.preferenceFileName;
  }
  
  public void setPreferenceFileName(String preferencesFile) {
    this.preferenceFileName = preferencesFile;
  }
  
  public boolean isOverwrite() {
    return this.overwrite;
  }
  
  public void setOverwrite(boolean value) {
    this.overwrite = value;
  }
  
  protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
    if (this.preferenceFileName == null || this.preferenceFileName.equals(""))
      handleError(ResourceHandler.getString("WorkspacePreferenceExport.missingFileName")); 
    if (this.preferenceFileName.charAt(0) == ' ')
      handleError(
          ResourceHandler.getString("WorkspacePreferenceExport.spaceBeforeFileName")); 
    File preferenceFile = new File(this.preferenceFileName);
    if (preferenceFile.exists() && !this.overwrite)
      handleError(
          ResourceHandler.getString("WorkspacePreferenceExport.fileExists", preferenceFile.getAbsolutePath())); 
    if (preferenceFile.exists() && !preferenceFile.canWrite())
      handleError(
          ResourceHandler.getString("WorkspacePreferenceExport.fileReadOnly", preferenceFile.getAbsolutePath())); 
  }
}
