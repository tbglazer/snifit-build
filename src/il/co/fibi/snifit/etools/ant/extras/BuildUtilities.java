package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.NLSMessageConstants;
import com.ibm.ws.ast.st.core.internal.WebSphereCorePlugin;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.osgi.util.NLS;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.ServerCore;
import org.eclipse.wst.validation.ValidationFramework;

public class BuildUtilities {
  private static final void waitForStubRuntimes(Task task) throws BuildException {
    Job[] allJobs = Job.getJobManager().find(null);
    String wasRuntimeCreationJobName = WebSphereCorePlugin.getResourceStr("L-UpdatingWTERuntime");
    if (wasRuntimeCreationJobName != null) {
      Job wteCreationJob = null;
      for (int i = 0; i < allJobs.length; i++) {
        if (wasRuntimeCreationJobName.equals(allJobs[i].getName())) {
          wteCreationJob = allJobs[i];
          break;
        } 
      } 
      if (wteCreationJob != null)
        try {
          task.log(NLSMessageConstants.BUILD_WAIT_WAS_STUB_RUNTIMES_INITIALIZING);
          wteCreationJob.join();
        } catch (InterruptedException interruptedException) {} 
    } else {
      throw new BuildException(NLSMessageConstants.BUILD_WAIT_WAS_STUB_RUNTIMES_FAIL_LOCATE_NAME);
    } 
  }
  
  private static final boolean validateContainers(IWorkspace workspace, Task task) throws CoreException {
    IProject[] allProjects = workspace.getRoot().getProjects();
    boolean valid = true;
    for (int i = 0; i < allProjects.length; i++) {
      boolean projectValid = validateContainers(allProjects[i], task);
      if (!projectValid)
        valid = false; 
    } 
    return valid;
  }
  
  private static final boolean validateContainers(IProject project, Task task) throws CoreException {
    boolean valid = true;
    if (project.hasNature("org.eclipse.jdt.core.javanature")) {
      IJavaProject javaProject = JavaCore.create(project);
      IClasspathEntry[] classpath = javaProject.getRawClasspath();
      for (int i = 0; i < classpath.length; i++) {
        if (classpath[i].getEntryKind() == 5) {
          IPath path = classpath[i].getPath();
          String firstSegment = path.segment(0);
          IClasspathContainer container = JavaCore.getClasspathContainer(path, javaProject);
          if (container == null || (container.getKind() == 0 && path.equals(container.getPath()))) {
            task.log(
                NLS.bind(NLSMessageConstants.BUILD_PATH_VALIDATION_FAILURE_RESOLVING_CONTAINER, new Object[] { project.getName(), path.toString() }), 1);
            valid = false;
          } else if (firstSegment != null) {
            if (firstSegment.equals("org.eclipse.jdt.launching.JRE_CONTAINER")) {
              String jreType = path.segment(1);
              String jreID = path.segment(2);
              if (jreType != null && jreID != null) {
                IVMInstall jre = JavaRuntime.getVMInstall(path);
                if (jre == null) {
                  valid = false;
                  task.log(NLS.bind(
                        NLSMessageConstants.BUILD_PATH_VALIDATION_JRE_CONTAINER_MISSING_JRE, 
                        new Object[] { project.getName(), jreType, jreID }), 1);
                } 
              } 
            } else if (firstSegment.equals("org.eclipse.jst.server.core.container")) {
              String runtimeType = path.segment(1);
              String runtimeID = path.segment(2);
              if (runtimeType != null && runtimeID != null) {
                IRuntime runtime = ServerCore.findRuntime(runtimeID);
                if (runtime == null) {
                  valid = false;
                  task.log(
                      NLS.bind(
                        NLSMessageConstants.BUILD_PATH_VALIDATION_SERVER_CONTAINER_MISSING_RUNTIME, 
                        new Object[] { project.getName(), runtimeType, runtimeID }), 1);
                } 
              } 
            } 
          } 
        } 
      } 
    } 
    return valid;
  }
  
  public static void buildWorkspace(IWorkspace workspace, int kind, Task task, IProgressMonitor monitor) throws CoreException, OperationCanceledException, InterruptedException {
    waitForStubRuntimes(task);
    validateContainers(workspace, task);
    workspace.build(kind, monitor);
    ValidationFramework.getDefault().joinValidationOnly(monitor);
  }
  
  public static void buildProject(IProject project, int kind, Task task, IProgressMonitor monitor) throws CoreException, OperationCanceledException, InterruptedException {
    waitForStubRuntimes(task);
    validateContainers(project, task);
    project.build(kind, monitor);
    ValidationFramework.getDefault().joinValidationOnly(monitor);
  }
}
