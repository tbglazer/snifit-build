package il.co.fibi.snifit.ant.tasks;

package il.co.fibi.snifit.etools.ant.extras;

import com.ibm.etools.ant.extras.common.ResourceHandler;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;

public class AntConsoleProgressMonitor implements IProgressMonitor {
  private Task task = null;
  
  private Job runningJob = null;
  
  private String taskname = "unknown";
  
  public static final int ERROR = 0;
  
  public static final int WARNING = 1;
  
  public static final int MESSAGE = 2;
  
  private boolean quiet = false;
  
  private boolean summary = false;
  
  public AntConsoleProgressMonitor(Task t) {
    this(t, null);
  }
  
  public AntConsoleProgressMonitor(Task t, Job job) {
    this.task = t;
    if (this.task == null) {
      System.out.println(ResourceHandler.getString("AntConsoleProgressMonitor.nullTask"));
      this.task = new nullTask();
    } 
    this.taskname = this.task.getTaskName();
    this.runningJob = job;
  }
  
  public void beginTask(String name, int totalTime) {
    if (name != null) {
      this.taskname = name;
      if (!this.quiet)
        if (this.runningJob != null) {
          if (name.equals("")) {
            this.taskname = this.runningJob.getName();
          } else {
            this.taskname = String.valueOf(this.runningJob.getName()) + ": " + name;
          } 
          this.task
            .log(ResourceHandler.getString("AntConsoleProgressMonitor.jobStart", this.taskname));
        } else if (!name.equals("")) {
          this.task
            .log(ResourceHandler.getString("AntConsoleProgressMonitor.taskStart", this.taskname));
        }  
    } 
  }
  
  public void done() {
    if (!this.quiet && this.taskname != null && !this.taskname.equalsIgnoreCase("unknown") && 
      !this.taskname.equals(""))
      if (this.runningJob != null) {
        this.task.log(ResourceHandler.getString("AntConsoleProgressMonitor.jobEnd", this.taskname));
      } else {
        this.task.log(ResourceHandler.getString("AntConsoleProgressMonitor.taskEnd", this.taskname));
      }  
    this.taskname = "unknown";
  }
  
  public void internalWorked(double arg0) {}
  
  public boolean isCanceled() {
    return false;
  }
  
  public void setCanceled(boolean arg0) {}
  
  public void setTaskName(String name) {
    if (!this.quiet)
      this.task.log(ResourceHandler.getString("AntConsoleProgressMonitor.taskSetName", name)); 
    this.taskname = name;
  }
  
  public void subTask(String name) {
    if (!this.quiet && !this.summary && name != null && name.length() > 0 && 
      !name.equalsIgnoreCase("Updating.") && 
      !name.equalsIgnoreCase("Validating Messages removed.") && !name.startsWith("Validating Removing "))
      if (this.runningJob != null) {
        this.task.log(ResourceHandler.getString("AntConsoleProgressMonitor.jobSubTask", name));
      } else {
        this.task.log(ResourceHandler.getString("AntConsoleProgressMonitor.taskSubTask", name));
      }  
  }
  
  public void worked(int timework) {}
  
  public void displayMsg(String msg) {
    if (!this.quiet)
      this.task.log(
          ResourceHandler.getString("AntConsoleProgressMonitor.taskMessage", (Object[])new String[] { this.taskname, msg })); 
  }
  
  public void displayMsg(String msg, int level) {
    if (level == 0 || level == 1) {
      this.task.log(
          ResourceHandler.getString("AntConsoleProgressMonitor.taskMessage", (Object[])new String[] { this.taskname, 
              msg }), level);
    } else if (!this.quiet) {
      this.task.log(
          ResourceHandler.getString("AntConsoleProgressMonitor.taskMessage", (Object[])new String[] { this.taskname, 
              msg }), level);
    } 
  }
  
  public void setQuiet(boolean q) {
    this.quiet = q;
  }
  
  public void setSummary(boolean b) {
    this.summary = b;
  }
  
  protected class nullTask extends Task {
    public void log(String msg) {}
    
    public void log(String msg, int level) {
      if (level == 0 || level == 1)
        System.out.println(ResourceHandler.getString("AntConsoleProgressMonitor.taskNullTaskMessage", msg)); 
    }
  }
}
