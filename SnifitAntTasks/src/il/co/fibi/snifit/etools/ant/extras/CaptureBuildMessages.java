package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.BuildListener;
import org.eclipse.core.runtime.IProgressMonitor;

public class CaptureBuildMessages extends FailOnErrorTask {
	private String action = null;

	private String searchString = null;

	private String searchContext = null;

	private String messageLevel = "information";

	private String errorPrefixMessage = null;

	private String propertyMessagesName = "BuildMessages";

	private int loglevel = 2;

	private String DEFAULTsearchContext = "contains";

	private static CapturedMessages capturedMessages;

	private static boolean capturing = false;

	private void stop(CapturedMessages capturer) {
		capturer.stop();
		if (capturing)
			getProject().removeBuildListener((BuildListener) capturedMessages);
		capturing = false;
	}

	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		validateAttributes(monitor);
		try {
			getProject().log(ResourceHandler.getString("CaptureBuildMessages.startupMessage",
					(Object[]) new String[] { this.action, this.searchString }), 4);
			CapturedMessages capturer = getCapturer();
			capturer.setMessageOutputLevel(this.loglevel);
			if (this.action == null || this.action.equalsIgnoreCase("start")) {
				if (!capturing)
					getProject().addBuildListener((BuildListener) capturedMessages);
				capturer.start();
				capturing = true;
			} else if (this.action.equalsIgnoreCase("stop")) {
				stop(capturer);
			} else if (this.action.equalsIgnoreCase("getAllMessages")) {
				stop(capturer);
				String msgs = capturer.getLongMessage();
				if (msgs != null) {
					getProject().setUserProperty(this.propertyMessagesName, msgs);
				} else if (getProject().getUserProperty(this.propertyMessagesName) != null) {
					getProject().setUserProperty(this.propertyMessagesName, "");
				}
			} else if (this.action.equalsIgnoreCase("getWsadminMessages")) {
				stop(capturer);
				String msgs = capturer.getWsadminMessages();
				if (msgs != null) {
					getProject().setUserProperty(this.propertyMessagesName, msgs);
				} else if (getProject().getUserProperty(this.propertyMessagesName) != null) {
					getProject().setUserProperty(this.propertyMessagesName, "");
				}
			} else if (this.action.equalsIgnoreCase("findMessage")) {
				stop(capturer);
				String msg = capturer.findMessage(this.searchString, this.searchContext);
				if (msg != null) {
					getProject().setUserProperty(this.propertyMessagesName, msg);
				} else if (getProject().getUserProperty(this.propertyMessagesName) != null) {
					getProject().setUserProperty(this.propertyMessagesName, "");
				}
			} else if (this.action.equalsIgnoreCase("failOnErrorMessagePresent")) {
				stop(capturer);
				String msg = capturer.findMessage(this.searchString, this.searchContext);
				if (msg != null) {
					if (this.errorPrefixMessage != null)
						msg = String.valueOf(this.errorPrefixMessage) + msg;
					msg = ResourceHandler.getString("CaptureBuildMessages.failOnErrorMessageFound", msg);
					handleError(msg);
				}
			} else if (this.action.equalsIgnoreCase("failOnErrorMessageMissing")) {
				stop(capturer);
				String msg = capturer.findMessage(this.searchString, this.searchContext);
				if (msg == null) {
					msg = this.searchString;
					if (this.errorPrefixMessage != null)
						msg = String.valueOf(this.errorPrefixMessage) + msg;
					msg = ResourceHandler.getString("CaptureBuildMessages.failOnErrorMessageMissing", msg);
					handleError(msg);
				}
				getProject().setUserProperty(this.propertyMessagesName, msg);
			}
			this.searchString = null;
			this.searchContext = null;
			this.action = null;
			this.errorPrefixMessage = null;
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	protected CapturedMessages getCapturer() {
		if (capturedMessages == null)
			capturedMessages = new CapturedMessages();
		return capturedMessages;
	}

	public void setPropertyMessagesName(String name) {
		this.propertyMessagesName = name;
	}

	public void setSearchContext(String context) {
		this.searchContext = context;
	}

	public void setSearchString(String value) {
		this.searchString = value;
	}

	public void setErrorPrefixMessage(String msg) {
		this.errorPrefixMessage = msg;
	}

	public void setAction(String value) {
		this.action = value;
	}

	public void setMessageLevel(String level) {
		this.messageLevel = level;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"action\":" + this.action);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"messageLevel\":" + this.messageLevel);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"searchString\":" + this.searchString);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"propertyMessagesName\":" + this.propertyMessagesName);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"errorPrefixMessage\":" + this.errorPrefixMessage);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.action == null) {
			this.action = null;
		} else if (this.action.equalsIgnoreCase("start")) {
			this.action = "start";
		} else if (this.action.equalsIgnoreCase("stop")) {
			this.action = "stop";
		} else if (this.action.equalsIgnoreCase("getAllMessages")) {
			this.action = "getAllMessages";
		} else if (this.action.equalsIgnoreCase("getWsadminMessages")) {
			this.action = "getWsadminMessages";
		} else if (this.action.equalsIgnoreCase("findMessage")) {
			this.action = "findMessage";
			if (this.searchString == null)
				handleError(ResourceHandler.getString("CaptureBuildMessages.invalidSearchString", "findMessages"));
			if (this.searchContext == null)
				this.searchContext = this.DEFAULTsearchContext;
		} else if (this.action.equalsIgnoreCase("failOnErrorMessagePresent")) {
			this.action = "failOnErrorMessagePresent";
			if (this.searchString == null)
				handleError(ResourceHandler.getString("CaptureBuildMessages.invalidSearchString",
						"failonerrormessagepresent"));
			if (this.searchContext == null)
				this.searchContext = this.DEFAULTsearchContext;
		} else if (this.action.equalsIgnoreCase("failOnErrorMessageMissing")) {
			this.action = "failOnErrorMessageMissing";
			if (this.searchString == null)
				handleError(ResourceHandler.getString("CaptureBuildMessages.invalidSearchString",
						"failonerrormessagemissing"));
			if (this.searchContext == null)
				this.searchContext = this.DEFAULTsearchContext;
		} else {
			handleError(ResourceHandler.getString("CaptureBuildMessages.invalidAction", this.action));
		}
		if (this.messageLevel.equalsIgnoreCase("error")) {
			this.loglevel = 0;
		} else if (this.messageLevel.equalsIgnoreCase("warn") || this.messageLevel.equalsIgnoreCase("warning")) {
			this.loglevel = 1;
		} else if (this.messageLevel.equalsIgnoreCase("info") || this.messageLevel.equalsIgnoreCase("information")) {
			this.loglevel = 2;
		} else if (this.messageLevel.equalsIgnoreCase("verbose")) {
			this.loglevel = 3;
		} else if (this.messageLevel.equalsIgnoreCase("debug")) {
			this.loglevel = 4;
		} else {
			handleError(ResourceHandler.getString("CaptureBuildMessages.invalidMessageLevel", this.messageLevel));
		}
	}
}
