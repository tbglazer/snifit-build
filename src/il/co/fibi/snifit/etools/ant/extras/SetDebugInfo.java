package il.co.fibi.snifit.etools.ant.extras;

import il.co.fibi.snifit.etools.ant.extras.common.ResourceHandler;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntBundleActivator;
import il.co.fibi.snifit.etools.j2ee.ant.internal.AntTrace;
import java.util.Hashtable;
import org.apache.tools.ant.BuildException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.JavaCore;

public class SetDebugInfo extends FailOnErrorTask {
	private String debugInfo = null;

	private String localVariable = null;

	private String lineNumber = null;

	private String sourceFile = null;

	private String propertyName = "DebugInfo";

	public void execute() throws BuildException {
		super.execute();
		MonitorHelper provider = new MonitorHelper(this);
		IProgressMonitor monitor = provider.createProgressGroup();
		validateAttributes(monitor);
		try {
			setDebugOptions();
		} finally {
			monitor.done();
			provider.dispose();
		}
	}

	public void setDebugOptions() {
		Hashtable<String, String> options = JavaCore.getOptions();
		if (this.debugInfo != null)
			if (this.debugInfo.equalsIgnoreCase("true")) {
				options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
				options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
				options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
			} else if (this.debugInfo.equalsIgnoreCase("false")) {
				options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "do not generate");
				options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "do not generate");
				options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "do not generate");
			}
		if (this.localVariable != null)
			if (this.localVariable.equalsIgnoreCase("true")) {
				options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "generate");
			} else if (this.localVariable.equalsIgnoreCase("false")) {
				options.put("org.eclipse.jdt.core.compiler.debug.localVariable", "do not generate");
			}
		if (this.lineNumber != null)
			if (this.lineNumber.equalsIgnoreCase("true")) {
				options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "generate");
			} else if (this.lineNumber.equalsIgnoreCase("false")) {
				options.put("org.eclipse.jdt.core.compiler.debug.lineNumber", "do not generate");
			}
		if (this.sourceFile != null)
			if (this.sourceFile.equalsIgnoreCase("true")) {
				options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "generate");
			} else if (this.sourceFile.equalsIgnoreCase("false")) {
				options.put("org.eclipse.jdt.core.compiler.debug.sourceFile", "do not generate");
			}
		JavaCore.setOptions(options);
		options = JavaCore.getOptions();
		String variables = options.get("org.eclipse.jdt.core.compiler.debug.localVariable");
		String lines = options.get("org.eclipse.jdt.core.compiler.debug.lineNumber");
		String source = options.get("org.eclipse.jdt.core.compiler.debug.sourceFile");
		String DebugInfoMsg = ResourceHandler.getString("SetDebugInfo.debugInfo",
				(Object[]) new String[] { variables, lines, source });
		System.out.println(DebugInfoMsg);
		getProject().setUserProperty(this.propertyName, DebugInfoMsg);
	}

	public void setDebugInfo(String str) {
		this.debugInfo = str;
	}

	public void setLocalVariable(String str) {
		this.localVariable = str;
	}

	public void setLineNumber(String str) {
		this.lineNumber = str;
	}

	public void setSourceFile(String str) {
		this.sourceFile = str;
	}

	public void setPropertyName(String name) {
		this.propertyName = name;
	}

	protected void validateAttributes(IProgressMonitor monitor) throws BuildException {
		if (AntTrace.EXTRAS_TRACE_ENABLED) {
			AntBundleActivator.getDebugTrace().traceEntry("/debug/antextras",
					"Printing out value of all " + getTaskName() + " passed attributes");
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"debugInfo\":" + this.debugInfo);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"lineNumber\":" + this.lineNumber);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"localVariable\":" + this.localVariable);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"sourceFile\":" + this.sourceFile);
			AntBundleActivator.getDebugTrace().trace("/debug/antextras",
					"Value of attribute \"propertyName\":" + this.propertyName);
			AntBundleActivator.getDebugTrace().traceExit("/debug/antextras",
					"Value of attribute \"failOnError\":" + this.failOnError);
		}
		if (this.debugInfo != null && !this.debugInfo.equalsIgnoreCase("true")
				&& !this.debugInfo.equalsIgnoreCase("false")) {
			handleError(ResourceHandler.getString("SetDebugInfo.invalidDebugInfo", this.debugInfo));
			return;
		}
		if (this.localVariable != null && !this.localVariable.equalsIgnoreCase("true")
				&& !this.localVariable.equalsIgnoreCase("false")) {
			handleError(ResourceHandler.getString("SetDebugInfo.invalidVariableSymbol", this.localVariable));
			return;
		}
		if (this.lineNumber != null && !this.lineNumber.equalsIgnoreCase("true")
				&& !this.lineNumber.equalsIgnoreCase("false")) {
			handleError(ResourceHandler.getString("SetDebugInfo.invalidLineNumber", this.lineNumber));
			return;
		}
		if (this.sourceFile != null && !this.sourceFile.equalsIgnoreCase("true")
				&& !this.sourceFile.equalsIgnoreCase("false")) {
			handleError(ResourceHandler.getString("SetDebugInfo.invalidSourceFile", this.sourceFile));
			return;
		}
	}
}
