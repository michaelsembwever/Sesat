/*
 * Copyright (2007) Schibsted Søk AS
 * This file is part of SESAT.
 *
 *   SESAT is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   SESAT is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with SESAT.  If not, see <http://www.gnu.org/licenses/>.
 */

package no.sesat.mojo;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.artifact.resolver.ArtifactCollector;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.dependency.fromConfiguration.CopyMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.tag.TagScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/** Handles deployment of sesat and skins builds.
 * Deployment differs greatly between the development and the other profiles.
 * In development profile the behaviour just uses the superclass CopyMojo,
 *  or the dependency:copy goal, with a precondition that the outputDirectory exists.
 *  (avoids a "null" directory created).
 * In the other profiles, the profile's classifier is used, the artifact downloaded from a remote serverDeployLocation
 *  is neccessary, and then uploaded to the configured 'profile'DeployRepository which corresponds to the
 *  the environments webapp directory.
 *  Skins are expected to override these deployRepository settings.
 *
 * @goal deploy
 * @author mick
 * @version $Id$
 */
public class DeploySesatWarfilesMojo extends CopyMojo implements Contextualizable{

    // Constants -----------------------------------------------------

    // Attributes ----------------------------------------------------

    private PlexusContainer container;

    private String profile = null;

    /**
     */
    private String serverDeployLocation;

    // All of these attributes are just explicit overrides to get them into the mojo.
    //  read http://www.mail-archive.com/dev@maven.apache.org/msg60770.html

    /**
     * Strip artifact version during copy
     *
     * @parameter expression="${mdep.stripVersion}" default-value="false"
     * @parameter
     */
    private boolean stripVersion = false;

    /**
     * Default location used for mojo unless overridden in ArtifactItem
     *
     * @parameter expression="${outputDirectory}"
     *            default-value="${project.build.directory}/dependency"
     * @optional
     * @since 1.0
     */
    private File outputDirectory;

    /**
     * Overwrite release artifacts
     *
     * @optional
     * @since 1.0
     * @parameter expression="${mdep.overWriteReleases}" default-value="false"
     */
    private boolean overWriteReleases;

    /**
     * Overwrite snapshot artifacts
     *
     * @optional
     * @since 1.0
     * @parameter expression="${mdep.overWriteSnapshots}" default-value="false"
     */
    private boolean overWriteSnapshots;

    /**
     * Overwrite if newer
     *
     * @optional
     * @since 2.0
     * @parameter expression="${mdep.overIfNewer}" default-value="true"
     */
    private boolean overWriteIfNewer;

    /**
     * Collection of ArtifactItems to work on. (ArtifactItem contains groupId,
     * artifactId, version, type, classifier, location, destFile, markerFile and overwrite.)
     * See "Usage" and "Javadoc" for details.
     *
     * @parameter
     * @required
     * @since 1.0
     */
    private ArrayList artifactItems;

    /**
     * Used to look up Artifacts in the remote serverDeployLocation.
     *
     * @parameter expression="${component.org.apache.maven.artifact.factory.ArtifactFactory}"
     * @required
     * @readonly
     */
    private org.apache.maven.artifact.factory.ArtifactFactory factory;

    /**
     * Used to look up Artifacts in the remote serverDeployLocation.
     *
     * @parameter expression="${component.org.apache.maven.artifact.resolver.ArtifactResolver}"
     * @required
     * @readonly
     */
    private org.apache.maven.artifact.resolver.ArtifactResolver resolver;

    /**
     * Artifact collector, needed to resolve dependencies.
     *
     * @component role="org.apache.maven.artifact.resolver.ArtifactCollector"
     * @required
     * @readonly
     */
    private ArtifactCollector artifactCollector;

    /**
     * @component role="org.apache.maven.artifact.metadata.ArtifactMetadataSource"
     *            hint="maven"
     * @required
     * @readonly
     */
    private ArtifactMetadataSource artifactMetadataSource;

    /**
     * Location of the local serverDeployLocation.
     *
     * @parameter expression="${localRepository}"
     * @readonly
     * @required
     */
    private org.apache.maven.artifact.repository.ArtifactRepository local;

    /**
     * List of Remote Repositories used by the resolver
     *
     * @parameter expression="${project.remoteArtifactRepositories}"
     * @readonly
     * @required
     */
    private java.util.List remoteRepos;

    /**
     * To look up Archiver/UnArchiver implementations
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.manager.ArchiverManager}"
     * @required
     * @readonly
     */
    private ArchiverManager archiverManager;

    /**
     * POM
     *
     * @parameter expression="${project}"
     * @readonly
     * @required
     */
    private MavenProject project;

    /**
     * Contains the full list of projects in the reactor.
     *
     * @parameter expression="${reactorProjects}"
     * @required
     * @readonly
     */
    private List reactorProjects;

    /**
     * If the plugin should be silent.
     *
     * @optional
     * @since 2.0
     * @parameter expression="${silent}" default-value="false"
     */
    private boolean silent;

    /**
     * Output absolute filename for resolved artifacts
     *
     * @optional
     * @since 2.0
     * @parameter expression="${outputAbsoluteArtifactFilename}"
     *            default-value="false"
     */
    private boolean outputAbsoluteArtifactFilename;

    // Static --------------------------------------------------------

    // Constructors --------------------------------------------------

    public DeploySesatWarfilesMojo() {
    }

    // Public --------------------------------------------------------

    // Contextualizable implementation ----------------------------------------------

    public void contextualize(final Context context) throws ContextException {

        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    // CopyMojo overrides ---------------------------------------------------

    public void execute() throws MojoExecutionException{

        // only ever interested in war projects. silently ignore other projects.
        if("war".equals(project.getPackaging())){

            serverDeployLocation = project.getProperties().getProperty("serverDeployLocation");
            pushFields();

            final Wagon wagon = getWagon();
            if(null != wagon){

                // alpha|nuclei|beta|electron|gamma|production deployment goes through scpexe
                try{

                    @SuppressWarnings("unchecked")
                    final List<ArtifactItem> theArtifactItems = getProcessedArtifactItems(stripVersion);

                    for(ArtifactItem item : theArtifactItems){

                        final Artifact artifact = factory.createArtifactWithClassifier(
                                item.getGroupId(),
                                item.getArtifactId(),
                                item.getVersion(),
                                item.getType(),
                                profile);

                        resolver.resolve(artifact, getRemoteRepos(), getLocal());

                        final String sesamSite = project.getProperties().getProperty("sesam.site");
                        final String destName = null != sesamSite
                                ? sesamSite
                                : project.getBuild().getFinalName();

                        // we are ready to go. but first tag the code
                        if(Boolean.parseBoolean(project.getProperties().getProperty("tag.on.deploy"))){

                            final ScmManager scmManager = (ScmManager) container.lookup(ScmManager.ROLE);
                            final String date = new SimpleDateFormat("yyyyMMddHHmm")
                                    .format(Calendar.getInstance().getTime());

                            final TagScmResult result = scmManager.tag(
                                    scmManager.makeScmRepository(project.getScm().getDeveloperConnection()),
                                    new ScmFileSet(project.getBasedir()) ,
                                    profile + "-deployments/" + date + "-" + project.getArtifactId(),
                                    "sesat " + profile + " deployment");

                            getLog().info(result.getCommandOutput());
                        }

                        // now do the upload
                        getLog().info("Uploading " + artifact.getFile().getAbsolutePath()
                                + " to " + wagon.getRepository().getUrl() + '/' + destName + ".war");

                        wagon.put(artifact.getFile(), destName + ".war");
                    }

                    wagon.disconnect();

                }catch(ConnectionException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("repository wagon not disconnected", ex);
                }catch(TransferFailedException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("transfer failed", ex);
                }catch(ResourceDoesNotExistException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("resource does not exist", ex);
                }catch(AuthorizationException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("authorisation exception", ex);
                }catch(ArtifactNotFoundException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("artifact not found", ex);
                }catch(ArtifactResolutionException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("artifact resolution exception", ex);
                }catch(ScmException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("scm exception", ex);
                }catch(ComponentLookupException ex){
                    getLog().error(ex);
                    throw new MojoExecutionException("failed to lookup ScmManager", ex);
                }

            }else{
                // development behaviour comes from super implementation
                // some pre-condition checks first

                // 1. the output directory must exist
                if(getOutputDirectory().exists()){

                    // 2. output directory is writable
                    if(getOutputDirectory().canWrite()){

                        super.execute();

                    }else{
                        // 2.failure output directory isn't writable
                        getLog().error(getOutputDirectory().getAbsolutePath() + " can not be written to.");
                    }
                }else{
                    // 1.failure: the output directory doesn't exist
                    getLog().error(getOutputDirectory().getAbsolutePath() + " does not exist.");
                    final String catalinaBase = System.getProperty("env.CATALINA_BASE");
                    if(null == catalinaBase || 0 == catalinaBase.length()){
                        getLog().info("Define system variable CATALINA_BASE to enable automatic deployment.");
                    }
                }
            }
        }

    }

    // Package protected ---------------------------------------------

    // Protected -----------------------------------------------------

    // Private -------------------------------------------------------

    private void pushFields(){
        setArchiverManager(archiverManager);
        setArtifactCollector(artifactCollector);
        setArtifactMetadataSource(artifactMetadataSource);
        setFactory(factory);
        setResolver(resolver);
    }

    /**
     *
     * @return the wagon (already connected) to use against the profile's serverDeployLocation
     *              or null if in development profile
     * @throws org.apache.maven.plugin.MojoExecutionException
     */
    private Wagon getWagon() throws MojoExecutionException {

        try {

            @SuppressWarnings("unchecked")
            final List<Profile> profiles = project.getActiveProfiles();

            final Object[] profileMap = new Object[]{"alpha","nuclei","beta","electron","gamma","production"};

            for(Object entry : profileMap){
                for(Profile p : profiles){
                    if(p.getId().equals(entry)){
                        profile = p.getId();
                        break;
                    }
                }
                if(null != profile){ break; }
            }

            Wagon wagon = null;

            if(null != profile){

                final String protocol = serverDeployLocation.substring(0, serverDeployLocation.indexOf(':'));

                final Repository wagonRepository = new Repository();

                wagonRepository.setUrl(serverDeployLocation);

                wagon = (Wagon) container.lookup(Wagon.ROLE, protocol);
                wagon.connect(wagonRepository);

            }
            return wagon;

        }catch (ConnectionException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("repository wagon not connected", ex);
        }catch (AuthenticationException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("repository wagon not authenticated", ex);
        }catch (ComponentLookupException ex) {
            getLog().error(ex);
            throw new MojoExecutionException("repository wagon not found", ex);
        }

    }

    // Inner classes -------------------------------------------------
}
