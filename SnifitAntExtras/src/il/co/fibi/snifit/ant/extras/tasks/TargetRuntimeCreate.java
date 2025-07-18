package il.co.fibi.snifit.ant.extras.tasks;

import com.ibm.ws.ast.st.core.internal.WebSphereCorePlugin;

import il.co.fibi.snifit.ant.extras.common.ResourceHandler;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.wst.server.core.IRuntime;
import org.eclipse.wst.server.core.IRuntimeType;
import org.eclipse.wst.server.core.IRuntimeWorkingCopy;
import org.eclipse.wst.server.core.ServerCore;
import org.osgi.framework.Bundle;

@Deprecated
public class TargetRuntimeCreate extends FailOnErrorTask {
	public static String WASv85Type = "com.ibm.ws.ast.st.runtime.v85";

	String targetId = null;

	String runtimeTypeId = null;

	String runtimeName = null;

	String runtimeLocation = null;

	private boolean overwrite = true;

	public TargetRuntimeCreate() {
		setTaskName("targetRuntimeCreate");
	}

	@SuppressWarnings("restriction")
	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		validateAttributes(monitor);
		try {
			IRuntimeType runtimeType = getServerRuntimeType(this.runtimeTypeId);
			if (runtimeType == null) {
				handleError(ResourceHandler.getString("TargetRuntimeCreate.missingRuntimeTypeId", this.runtimeTypeId));
			} else {
				if (runtimeNameInUse(this.runtimeName)) {
					if (runtimeAlreadyExists(this.runtimeName, this.runtimeLocation)) {
						log(ResourceHandler.getString("TargetRuntimeCreate.runtimeExists",
								(Object[]) new String[] { this.runtimeName, this.runtimeLocation }));
						return;
					}
					handleError(ResourceHandler.getString("TargetRuntimeCreate.invalidName", this.runtimeName));
				}
				IRuntimeWorkingCopy workingCopy = null;
				try {
					workingCopy = runtimeType.createRuntime(this.targetId,
							(IProgressMonitor) new NullProgressMonitor());
				} catch (CoreException e) {
					handleError(ResourceHandler.getString("TargetRuntimeCreate.createRuntimeException", e.getMessage()),
							(Exception) e);
				}
				if (workingCopy == null) {
					handleError(ResourceHandler.getString("TargetRuntimeCreate.createdWorkingCopy", this.targetId));
				} else {
					Path path = new Path(this.runtimeLocation);
					workingCopy.setLocation((IPath) path);
					workingCopy.setName(this.runtimeName);
					try {
						monitor.setTaskName("Create Target Runtime : " + this.runtimeName);
						String jobName = WebSphereCorePlugin.getResourceStr("L-LoadingConfiguration");
						Bundle bundle = Platform.getBundle("com.ibm.ws.ast.st.core");
						int timelimit = 0;
						while (bundle.getState() != 32 && timelimit < 150) {
							try {
								Thread.sleep(200L);
							} catch (InterruptedException e) {
								log(e.getMessage());
								break;
							}
							timelimit++;
						}
						IJobManager manager = Job.getJobManager();
						Job[] allJobs = manager.find(null);
						for (int i = 0; i < allJobs.length; i++) {
							Job theJob = allJobs[i];
							if (theJob.getName().equals(jobName))
								try {
									theJob.join();
								} catch (InterruptedException ie) {
									log(ie.getMessage());
									break;
								}
						}
						workingCopy.save(true, monitor);
						log(ResourceHandler.getString("TargetRuntimeCreate.workingCopySaved"));
					} catch (CoreException e) {
						String msg = e.getStatus().toString();
						log(ResourceHandler.getString("Common.coreException", msg));
						String badMsg = "java.lang.NullPointerException encountered while running com.ibm.etools.siteedit.builder.SiteNavBuilder";
						if (msg.indexOf(badMsg) >= 0) {
							log(ResourceHandler.getString("TargetRuntimeCreate.badMessage", badMsg));
						} else {
							handleError(msg, (Exception) e);
						}
					}
				}
			}
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected IRuntimeType getServerRuntimeType(String typeId) {
		IRuntimeType runtimeType = ServerCore.findRuntimeType(typeId);
		if (runtimeType == null) {
			handleError(ResourceHandler.getString("TargetRuntimeCreate.getRuntimeTypeId", typeId));
		} else {
			String name = runtimeType.getName();
			String desc = runtimeType.getDescription();
			String vendor = runtimeType.getVendor();
			String version = runtimeType.getVersion();
			log(ResourceHandler.getString("TargetRuntimeCreate.retrievedRuntime1", typeId));
			log(ResourceHandler.getString("TargetRuntimeCreate.retrievedRuntime2", name));
			log(ResourceHandler.getString("TargetRuntimeCreate.retrievedRuntime3", desc));
			log(ResourceHandler.getString("TargetRuntimeCreate.retrievedRuntime4", vendor));
			log(ResourceHandler.getString("TargetRuntimeCreate.retrievedRuntime5", version));
		}
		return runtimeType;
	}

	public void setTargetLocation(String targetLocation) {
		this.runtimeLocation = targetLocation;
	}

	public void setTargetId(String id) {
		this.targetId = id;
	}

	public void setTargetName(String name) {
		this.runtimeName = name;
	}

	public void setRuntimeTypeId(String typeId) {
		this.runtimeTypeId = typeId;
	}

	public boolean isOverwrite() {
		return this.overwrite;
	}

	public void setOverwrite(boolean value) {
		this.overwrite = value;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (this.runtimeLocation == null)
			handleError(ResourceHandler.getString("TargetRuntimeCreate.missingRuntimeLocation"));
		if (this.targetId == null)
			handleError(ResourceHandler.getString("TargetRuntimeCreate.missingTargetId"));
		if (this.runtimeTypeId == null) {
			String msg = ResourceHandler.getString("TargetRuntimeCreate.usingDefaultRuntimeTypeId", WASv85Type);
			log(msg);
			this.runtimeTypeId = WASv85Type;
		}
		if (this.runtimeName == null) {
			String msg = ResourceHandler.getString("TargetRuntimeCreate.usingDefaultTargetId", this.targetId);
			log(msg);
			this.runtimeName = this.targetId;
		}
	}

	private boolean runtimeNameInUse(String name) {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		for (int i = 0; i < runtimes.length; i++) {
			if (runtimes[i].getName().equals(name))
				return true;
		}
		return false;
	}

	private boolean runtimeAlreadyExists(String name, String runtimePath) {
		IRuntime[] runtimes = ServerCore.getRuntimes();
		for (int i = 0; i < runtimes.length; i++) {
			if (runtimes[i].getName().equals(name) && runtimes[i].getLocation() != null
					&& runtimes[i].getLocation().toFile().equals(new File(runtimePath)))
				return true;
		}
		return false;
	}
}
