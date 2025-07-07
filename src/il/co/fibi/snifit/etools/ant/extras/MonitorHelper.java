package il.co.fibi.snifit.etools.ant.extras;

import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.ProgressProvider;

public class MonitorHelper extends ProgressProvider {
	private final Task runningTask;

	public MonitorHelper(Task task) {
		this.runningTask = task;
		Job.getJobManager().setProgressProvider(this);
	}

	public void dispose() {
		Job.getJobManager().setProgressProvider(null);
	}

	public IProgressMonitor createProgressGroup() {
		return new AntConsoleProgressMonitor(this.runningTask);
	}

	public IProgressMonitor createMonitor(Job job) {
		IProgressMonitor monitor = null;
		if (!job.isSystem())
			monitor = new AntConsoleProgressMonitor(this.runningTask, job);
		return monitor;
	}

	@Deprecated
	public static IProgressMonitor getProgressMonitor(Task task) {
		return getProgressMonitor(task, false);
	}

	@Deprecated
	public static IProgressMonitor getProgressMonitor(Task task, boolean isQuiet) {
		AntConsoleProgressMonitor monitor = new AntConsoleProgressMonitor(task);
		monitor.setQuiet(isQuiet);
		return monitor;
	}

	@Deprecated
	public static void displayMsg(Task task, String msg) {
		displayMsg(task, msg, false);
	}

	@Deprecated
	public static void displayMsg(Task task, String msg, boolean quiet) {
		if (!quiet)
			task.log(msg);
	}

	@Deprecated
	public static void displayMsg(Task task, String msg, int level) {
		displayMsg(task, msg, level, false);
	}

	@Deprecated
	public static void displayMsg(Task task, String msg, int level, boolean quiet) {
		if (level == 0 || level == 1) {
			task.log(msg, level);
		} else if (!quiet) {
			task.log(msg, level);
		}
	}
}
