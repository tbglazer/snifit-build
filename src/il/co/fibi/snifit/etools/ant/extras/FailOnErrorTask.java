package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public abstract class FailOnErrorTask extends Task {
	protected abstract void validateAttributes(IProgressMonitor paramIProgressMonitor) throws BuildException;

	protected void handleError(String errorMessage) throws BuildException {
		handleError(errorMessage, null);
	}

	protected void handleError(String errorMessage, Exception ex) throws BuildException {
		log(errorMessage, 0);
		if (ex != null) {
			int severity = this.failOnError ? 4 : 2;
			Status status = new Status(severity, "com.ibm.etools.j2ee.ant", errorMessage, ex);
			AntBundleActivator.getInstance().getLog().log((IStatus) status);
		}
		if (this.failOnError) {
			BuildException buildEx = null;
			if (ex != null) {
				buildEx = new BuildException(errorMessage, ex);
			} else {
				buildEx = new BuildException(errorMessage);
			}
			throw buildEx;
		}
	}

	public final void setFailOnError(boolean value) {
		this.failOnError = value;
	}

	public final boolean isFailOnError() {
		return this.failOnError;
	}

	protected boolean failOnError = true;
}
