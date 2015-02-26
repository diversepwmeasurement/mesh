package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.rest.response.RestProject;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return projectVerticle;
	}

	@Test
	public void testReadProjectByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";

		Project project = getDataProvider().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestProject.class);
	}

	@Test
	public void testReadProjectByName() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";

		Project project = getDataProvider().getProject();
		assertNotNull("The name of the project must not be null.", project.getName());

		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/projects/" + project.getName(), 200, "OK");
		TestUtil.assertEqualsSanitizedJson(json, response, RestProject.class);
	}

	@Test
	public void testReadProjectInvalidName() throws Exception {
		String json = "{\"message\":\"Project not found {bogusName}\"}";

		Project project = getDataProvider().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/projects/" + "bogusName", 404, "Not Found");
		assertEquals(json, response);
	}

	@Test
	public void testCreateProject() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteProjectByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteProjectByName() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateProject() {
		fail("Not yet implemented");
	}

}
