package com.gentics.mesh.core.schema.field;

import org.junit.Ignore;

import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;

@MicroschemaTest
@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = false)
public class HtmlMicroFieldMigrationTest extends HtmlFieldMigrationTest {

	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToBinary() throws Exception {
	}

	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToMicronode() throws Exception {
	}

	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToMicronodeList() throws Exception {
	}
}
