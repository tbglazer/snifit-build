package il.co.fibi.snifit.ant.extras.tasks;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.eclipse.core.runtime.IProgressMonitor;

public abstract class FailOnErrorTask extends Task {
	protected abstract void validateAttributes(IProgressMonitor paramIProgressMonitor) throws BuildException;

	protected void handleError(String errorMessage) throws BuildException {
		handleError(errorMessage, null);
	}

	protected void handleError(String errorMessage, Exception ex) throws BuildException {
		log(errorMessage, 0);
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
