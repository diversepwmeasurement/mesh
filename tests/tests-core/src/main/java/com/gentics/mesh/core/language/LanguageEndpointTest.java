package com.gentics.mesh.core.language;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Random;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.lang.LanguageListResponse;
import com.gentics.mesh.core.rest.lang.LanguageResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.parameter.impl.ProjectLoadParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class LanguageEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Before
	public void setup() {
		tx(tx -> {
			tx.languageDao().assign(tx.languageDao().findByLanguageTag(english()), project(), null, false);
			tx.success();
		});
	}

	@Override
	@Ignore
	@Test
	public void testCreate() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateReadDelete() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithNoPerm() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithUuid() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateWithDuplicateUuid() throws Exception {
	}

	@Override
	@Test
	public void testReadByUUID() throws Exception {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByUuid(englishUuid));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
	}

	@Override
	@Ignore
	@Test
	public void testReadByUuidWithRolePerms() {
	}

	@Override
	@Ignore
	@Test
	public void testReadByUUIDWithMissingPermission() throws Exception {
	}

	@Override
	@Test
	public void testReadMultiple() throws Exception {
		LanguageListResponse languages = call(() -> client().findLanguages());
		assertEquals(11, languages.getData().size());
	}

	@Override
	@Ignore
	@Test
	public void testPermissionResponse() {
	}

	@Override
	@Ignore
	@Test
	public void testUpdate() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateByUUIDWithoutPerm() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
	}

	@Override
	@Ignore
	@Test
	public void testDeleteByUUID() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testUpdateMultithreaded() throws Exception {
	}

	@Override
	@Test
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		LanguageListResponse languages = call(() -> client().findLanguages());
		Random rnd = new Random();

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findLanguageByUuid(languages.getData().get(rnd.nextInt(languages.getData().size())).getUuid()).toCompletable())
			.blockingAwait();
	}

	@Override
	@Ignore
	@Test
	public void testDeleteByUUIDMultithreaded() throws Exception {
	}

	@Override
	@Ignore
	@Test
	public void testCreateMultithreaded() throws Exception {
	}

	@Override
	@Test
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		LanguageListResponse languages = call(() -> client().findLanguages());
		Random rnd = new Random();

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findLanguageByUuid(languages.getData().get(rnd.nextInt(languages.getData().size())).getUuid()).toCompletable())
			.blockingAwait();
	}

	@Test
	public void testReadByTag() {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByTag(english()));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
	}

	@Test
	public void testReadFromProjectByTag() {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByTag(PROJECT_NAME, english()));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
		assertTrue("The project should contain the language", tx(tx -> { 
			return project().getLanguages().stream().anyMatch(lang -> lang.getUuid().equals(englishUuid) && lang.getLanguageTag().equals(english())); 
		}));
	}

	@Test
	public void testReadFromProjectByUUID() throws Exception {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		LanguageResponse english = call(() -> client().findLanguageByUuid(PROJECT_NAME, englishUuid));
		assertEquals(englishUuid, english.getUuid());
		assertEquals(english(), english.getLanguageTag());
		assertTrue("The project should contain the language", tx(tx -> { 
			return project().getLanguages().stream().anyMatch(lang -> lang.getUuid().equals(englishUuid) && lang.getLanguageTag().equals(english())); 
		}));
	}

	@Test
	public void testAssignByUuid() {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		ProjectResponse projectResponse = call(() -> client().assignLanguageToProjectByUuid(PROJECT_NAME, englishUuid, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).contains(englishUuid);
	}

	@Test
	public void testAssignByTag() {
		ProjectResponse projectResponse = call(() -> client().assignLanguageToProjectByTag(PROJECT_NAME, english(), new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getLanguageTag)).contains(english());
	}

	@Test
	public void testUnassignByUuid() {
		String englishUuid = tx(tx -> { 
			return tx.languageDao().findByLanguageTag(english()).getUuid();
		});
		ProjectResponse projectResponse = call(() -> client().unassignLanguageFromProjectByUuid(PROJECT_NAME, englishUuid, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain(englishUuid);
	}

	@Test
	public void testUnassignByTag() {
		ProjectResponse projectResponse = call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, english(), new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain(english());
	}

	@Test
	public void testReadFromProjectByFalseTag() {
		call(() -> client().findLanguageByTag(PROJECT_NAME, "bogus"), HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");

		assertTrue("The project should not contain the false language", tx(tx -> { 
			return project().getLanguages().stream().noneMatch(lang -> lang.getUuid().equals("bogus") && lang.getLanguageTag().equals(english())); 
		}));
	}

	@Test
	public void testAssignByFalseTag() {
		call(() -> client().assignLanguageToProjectByTag(PROJECT_NAME, "bogus", new ProjectLoadParametersImpl().setLangs(true)), 
				HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getLanguageTag)).doesNotContain("bogus");
	}

	@Test
	public void testUnassignByFalseTag() {
		call(() -> client().unassignLanguageFromProjectByTag(PROJECT_NAME, "bogus", new ProjectLoadParametersImpl().setLangs(true)), 
				HttpResponseStatus.NOT_FOUND, "error_language_not_found", "bogus");
		ProjectResponse projectResponse = call(() -> client().findProjectByName(PROJECT_NAME, new ProjectLoadParametersImpl().setLangs(true)));
		assertNotNull(projectResponse.getLanguages());
		assertThat(projectResponse.getLanguages().stream().map(LanguageResponse::getUuid)).doesNotContain("bogus");
	}
}
