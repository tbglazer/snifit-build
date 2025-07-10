package il.co.fibi.snifit.etools.j2ee.ant.internal;

import il.co.fibi.snifit.support.trace.core.InternalTrace;
import org.eclipse.osgi.service.debug.DebugOptions;

public class AntTrace extends InternalTrace {
	public static final String ROOT_TRACE_OPTIONS_STRING = "/debug";

	public static final String SERVER_TRACE_OPTIONS_STRING = "/debug/antserver";

	public static final String J2EE_TRACE_OPTIONS_STRING = "/debug/antj2ee";

	public static final String EXTRAS_TRACE_OPTIONS_STRING = "/debug/antextras";

	public AntTrace() {
		super("com.ibm.etools.j2ee.ant");
	}

	protected void updateOptions(DebugOptions options) {
		ROOT_TRACE_ENABLED = options.getBooleanOption("il.co.fibi.snifit.etools.j2ee.ant/debug", false);
		SERVER_TRACE_ENABLED = options.getBooleanOption("il.co.fibi.snifit.etools.j2ee.ant/debug/antserver", false);
		J2EE_TRACE_ENABLED = options.getBooleanOption("il.co.fibi.snifit.etools.j2ee.ant/debug/antj2ee", false);
		EXTRAS_TRACE_ENABLED = options.getBooleanOption("il.co.fibi.snifit.etools.j2ee.ant/debug/antextras", false);
	}

	public static boolean SERVER_TRACE_ENABLED = false;

	public static boolean J2EE_TRACE_ENABLED = false;

	public static boolean EXTRAS_TRACE_ENABLED = false;

	public static boolean ROOT_TRACE_ENABLED = false;
}
