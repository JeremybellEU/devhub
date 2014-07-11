package nl.tudelft.ewi.devhub.server.web.resources;

import javax.inject.Inject;
import javax.persistence.EntityNotFoundException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Locale;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.inject.persist.Transactional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.tudelft.ewi.build.jaxrs.models.BuildRequest;
import nl.tudelft.ewi.build.jaxrs.models.BuildResult.Status;
import nl.tudelft.ewi.build.jaxrs.models.GitSource;
import nl.tudelft.ewi.build.jaxrs.models.MavenBuildInstruction;
import nl.tudelft.ewi.devhub.server.Config;
import nl.tudelft.ewi.devhub.server.backend.BuildResultMailer;
import nl.tudelft.ewi.devhub.server.backend.BuildsBackend;
import nl.tudelft.ewi.devhub.server.database.controllers.BuildResults;
import nl.tudelft.ewi.devhub.server.database.controllers.Groups;
import nl.tudelft.ewi.devhub.server.database.entities.BuildResult;
import nl.tudelft.ewi.devhub.server.database.entities.Group;
import nl.tudelft.ewi.devhub.server.web.filters.RequireAuthenticatedBuildServer;
import nl.tudelft.ewi.git.client.GitServerClient;
import nl.tudelft.ewi.git.client.Repositories;
import nl.tudelft.ewi.git.models.BranchModel;
import nl.tudelft.ewi.git.models.DetailedRepositoryModel;
import org.jboss.resteasy.plugins.guice.RequestScoped;

@Slf4j
@RequestScoped
@Path("hooks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON + Resource.UTF8_CHARSET)
public class HooksResource extends Resource {

	@Data
	private static class GitPush {
		private String repository;
	}

	private final Config config;
	private final BuildsBackend buildBackend;
	private final GitServerClient client;
	private final BuildResults buildResults;
	private final Groups groups;
	private final BuildResultMailer mailer;

	@Inject
	HooksResource(Config config, BuildsBackend buildBackend, GitServerClient client, BuildResults buildResults,
			Groups groups, BuildResultMailer mailer) {

		this.config = config;
		this.buildBackend = buildBackend;
		this.client = client;
		this.buildResults = buildResults;
		this.groups = groups;
		this.mailer = mailer;
	}

	@POST
	@Path("git-push")
	public void onGitPush(@Context HttpServletRequest request, GitPush push) throws UnsupportedEncodingException {
		log.info("Received git-push event: {}", push);

		Repositories repositories = client.repositories();
		DetailedRepositoryModel repository = repositories.retrieve(push.getRepository());

		MavenBuildInstruction instruction = new MavenBuildInstruction();
		instruction.setWithDisplay(true);
		instruction.setPhases(new String[] { "package" });

		Group group = groups.findByRepoName(push.getRepository());
		for (BranchModel branch : repository.getBranches()) {
			if ("HEAD".equals(branch.getSimpleName())) {
				continue;
			}
			if (buildResults.exists(group, branch.getCommit())) {
				continue;
			}
			
			BuildResult buildResult = BuildResult.newBuildResult(group, branch.getCommit());
			buildResults.persist(buildResult);

			log.info("Submitting a build for branch: {} of repository: {}", branch.getName(), repository.getName());

			GitSource source = new GitSource();
			source.setRepositoryUrl(repository.getUrl());
			source.setBranchName(branch.getName());
			source.setCommitId(branch.getCommit());

			StringBuilder callbackBuilder = new StringBuilder();
			callbackBuilder.append(config.getHttpUrl());
			callbackBuilder.append("/hooks/build-result");
			callbackBuilder.append("?buildId=" + buildResult.getId());

			BuildRequest buildRequest = new BuildRequest();
			buildRequest.setCallbackUrl(callbackBuilder.toString());
			buildRequest.setInstruction(instruction);
			buildRequest.setSource(source);
			buildRequest.setTimeout(group.getBuildTimeout());
			
			buildBackend.offerBuild(buildRequest, buildResult.getId());
		}
	}

	@POST
	@Path("build-result")
	@RequireAuthenticatedBuildServer
	@Transactional
	public void onBuildResult(@QueryParam("buildId") long buildId,
			nl.tudelft.ewi.build.jaxrs.models.BuildResult buildResult) throws UnsupportedEncodingException {

		try {
			BuildResult result = buildResults.find(buildId);
			result.setCompleted(new Date());
			result.setSuccess(buildResult.getStatus() == Status.SUCCEEDED);
			result.setLog(Joiner.on('\n').join(buildResult.getLogLines()));
			buildResults.merge(result);

			if (!result.getSuccess()) {
				mailer.sendFailedBuildResult(Lists.newArrayList(Locale.ENGLISH), result);
			}
		}
		catch (EntityNotFoundException e) {
			log.error("Could not find build result: " + buildId + ": " + e.getMessage(), e);
			return;
		}
	}

}
