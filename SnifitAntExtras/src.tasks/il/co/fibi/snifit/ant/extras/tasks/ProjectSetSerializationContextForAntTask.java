package il.co.fibi.snifit.ant.extras.tasks;

import org.eclipse.core.resources.IProject;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.TeamException;

public class ProjectSetSerializationContextForAntTask extends ProjectSetSerializationContext {
	IProject[] overwriteProject;

	public ProjectSetSerializationContextForAntTask(String filename, IProject[] projects) {
		super(filename);
		this.overwriteProject = projects;
	}

	@Override
	public IProject[] confirmOverwrite(IProject[] projects) throws TeamException {
		return this.overwriteProject;
	}
}
