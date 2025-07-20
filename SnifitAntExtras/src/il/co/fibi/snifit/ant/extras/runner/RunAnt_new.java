package il.co.fibi.snifit.ant.extras.runner;

import java.io.File;
import java.net.URL;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.ProjectHelper;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.osgi.framework.Bundle;

public class RunAnt_new implements IApplication {

	@Override
	public Object start(IApplicationContext context) throws Exception {
		String[] args = (String[]) context.getArguments().get("application.args");

		if (args.length == 0) {
			System.out.println("Usage: -buildfile <path-to-build.xml>");
			return IApplication.EXIT_OK;
		}

		File buildFile = null;
		for (int i = 0; i < args.length; i++) {
			if ("-buildfile".equals(args[i]) && i + 1 < args.length) {
				buildFile = new File(args[i + 1]);
				break;
			}
		}

		if (buildFile == null || !buildFile.exists()) {
			System.out.println("Build file not found.");
			return IApplication.EXIT_OK;
		}

		// Prepare Ant project
		Project project = new Project();
		project.setUserProperty("ant.file", buildFile.getAbsolutePath());
		project.init();

		Bundle bundle = Platform.getBundle("il.co.fibi.snifit.ant.extras");
		URL bundleRootURL = FileLocator.resolve(bundle.getEntry("/"));
		File bundleFile = new File(bundleRootURL.toURI());
		System.err.println("Bundle file/folder: " + bundleFile.getAbsolutePath());

		// Now you can add it to the Ant classpath
		Path path = new Path(bundleFile.getAbsolutePath());
		project.createClassLoader(null).addPathElement(path.toOSString());

		project.addTaskDefinition("snifit.workspacePreferenceFile", il.co.fibi.snifit.ant.extras.tasks.WorkspacePreferenceFile.class);
		project.addTaskDefinition("snifit.projectSetImport", il.co.fibi.snifit.ant.extras.tasks.ProjectSetImport.class);
		project.addTaskDefinition("snifit.workspaceBuild", il.co.fibi.snifit.ant.extras.tasks.WorkspaceBuild.class);

		// Parse and run
		ProjectHelper helper = ProjectHelper.getProjectHelper();
		project.addReference("ant.projectHelper", helper);
		helper.parse(project, buildFile);

		project.executeTarget(project.getDefaultTarget());

		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// no-op
	}
}
