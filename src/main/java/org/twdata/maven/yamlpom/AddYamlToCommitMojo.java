package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;

import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Adds YAML pom files to the commit.  Meant to be used with the release plugin. 
 *
 * @goal release-prepare
 * @requiresProject true
 * @requiresDependencyResolution test
 */
public class AddYamlToCommitMojo extends AbstractMojo
{
    /**
     * Yaml pom file
     *
     * @parameter expression="${pom.yaml}"
     */
    private String yamlPomName = "pom.yml";

    /**
     * Sync file name
     *
     * @parameter expression="${yamlpom.syncfile}"
     */
    private String syncFileName = ".pom.yml";

    /**
     * The SCM manager.
     *
     * @component
     */
    private ScmManager scmManager;

    /**
     * @parameter expression="${basedir}"
     * @required
     * @readonly
     */
    protected File basedir;


    /**
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    protected List<MavenProject> reactorProjects;


    public void execute() throws MojoExecutionException, MojoFailureException
    {
        String sourceUrl = null;
        MavenProject rootProject = getRootProject(reactorProjects);
        if (rootProject != null && rootProject.getScm() != null)
        {
            if (rootProject.getScm().getDeveloperConnection() != null)
            {
                sourceUrl = rootProject.getScm().getDeveloperConnection();
            }
            else
            {
                if (rootProject.getScm().getConnection() != null)
                {
                    sourceUrl = rootProject.getScm().getConnection();
                }
            }
        }
        if (sourceUrl == null)
        {
            throw new MojoExecutionException("Missing required setting: scm connection or developerConnection must be specified.");
        }

        ScmRepository repository = null;
        try
        {
            repository = scmManager.makeScmRepository(sourceUrl);
            getLog().info("Adding yaml pom files to commit");
            getLog().info("Adding " + new File(basedir, yamlPomName));
            getLog().info("Adding " + new File(basedir, syncFileName));
            scmManager.add(repository, new ScmFileSet(basedir, Arrays.asList(new File(basedir, yamlPomName), new File(basedir, syncFileName))));

        }
        catch (ScmRepositoryException e)
        {
            throw new MojoExecutionException("Unable to add YAML files to repository", e);
        }
        catch (NoSuchScmProviderException e)
        {
            throw new MojoExecutionException("No such scm provider", e);
        }
        catch (ScmException e)
        {
            throw new MojoExecutionException("General SCM failure", e);
        }


    }

    public static MavenProject getRootProject(List<MavenProject> reactorProjects)
    {
        MavenProject project = reactorProjects.get(0);
        for (MavenProject currentProject : reactorProjects)
        {
            if (currentProject.isExecutionRoot())
            {
                project = currentProject;
                break;
            }
        }

        return project;
    }
}
