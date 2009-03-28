package org.twdata.maven.yamlpom;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.provider.ScmProviderRepository;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.maven.scm.repository.ScmRepositoryException;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.manager.NoSuchScmProviderException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.ScmException;

import java.util.List;
import java.util.Iterator;
import java.util.Arrays;
import java.io.File;

/**
 * Adds YAML pom files to the commit
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
            scmManager.add(repository, new ScmFileSet(basedir, Arrays.asList(new File(yamlPomName), new File(syncFileName))));

        }
        catch (ScmRepositoryException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (NoSuchScmProviderException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        catch (ScmException e)
        {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
