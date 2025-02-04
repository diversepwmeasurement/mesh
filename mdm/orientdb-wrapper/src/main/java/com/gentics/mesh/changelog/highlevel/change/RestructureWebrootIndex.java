package com.gentics.mesh.changelog.highlevel.change;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.impl.GraphFieldContainerEdgeImpl;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.syncleus.ferma.FramedTransactionalGraph;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Change which will get rid of the old {@link NodeGraphFieldContainer} webroot properties and instead add those props to the HAS_FIELD_CONTAINER edge.
 */
@Singleton
public class RestructureWebrootIndex extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(RestructureWebrootIndex.class);

	private final GraphDatabase db;

	@Inject
	public RestructureWebrootIndex(GraphDatabase db) {
		this.db = db;
	}

	@Override
	public String getUuid() {
		return "7E94C51E763C46D394C51E763C86D3F5";
	}

	@Override
	public String getName() {
		return "Restructure Webroot Index";
	}

	@Override
	public String getDescription() {
		return "Restructures the webroot index by iterating over all publish and draft edges.";
	}

	@Override
	public void apply() {
		NodeDao nodeDao = Tx.get().nodeDao();
		ContentDao contentDao = Tx.get().contentDao();

		log.info("Applying change: " + getName());
		FramedTransactionalGraph graph = GraphDBTx.getGraphTx().getGraph();
		Iterable<? extends GraphFieldContainerEdgeImpl> edges = graph.getFramedEdgesExplicit("@class", HAS_FIELD_CONTAINER,
			GraphFieldContainerEdgeImpl.class);
		long count = 0;
		long total = 0;
		for (GraphFieldContainerEdgeImpl edge : edges) {
			ContainerType type = edge.getType();
			if (DRAFT.equals(type) || PUBLISHED.equals(type)) {
				String branchUuid = edge.getBranchUuid();
				NodeGraphFieldContainer container = edge.getNodeContainer();
				// Skip graph inconsistencies
				if (container == null) {
					continue;
				}
				edge.setUrlFieldInfo(contentDao.getUrlFieldValues(container).collect(Collectors.toSet()));
				String segment = contentDao.getSegmentFieldValue(container);
				if (segment != null && !segment.trim().isEmpty()) {
					HibNode node = contentDao.getNode(container);
					if (node != null) {
						node = nodeDao.getParentNode(node, branchUuid);
					}
					String newInfo = contentDao.composeSegmentInfo(node, segment);
					edge.setSegmentInfo(newInfo);
				} else {
					edge.setSegmentInfo(null);
				}
				if (count % 100 == 0) {
					log.info("Updated {" + count + "} content edges. Processed {" + total + "} edges in total");
				}
				count++;
			}

			String segment = edge.getSegmentInfo();
			if (segment == null || segment.trim().isEmpty()) {
				edge.setSegmentInfo(null);
			}
			if (total % 1000 == 0) {
				graph.commit();
			}
			total++;
		}
		log.info("Done updating all content edges. Updated: {" + count + "} of {" + total + "}");

		Iterable<? extends NodeGraphFieldContainerImpl> containers = graph.getFramedVertices("@class",
			NodeGraphFieldContainerImpl.class.getSimpleName(), NodeGraphFieldContainerImpl.class);
		for (NodeGraphFieldContainer container : containers) {
			container.getElement().removeProperty("publishedWebrootUrlInfo");
			container.getElement().removeProperty("webrootUrlInfo");
			container.getElement().removeProperty("publishedWebrootPathInfo");
			container.getElement().removeProperty("webrootPathInfo");
		}

	}

	@Override
	public void applyNoTx() {
		db.index().removeVertexIndex("webrootPathInfoIndex", NodeGraphFieldContainerImpl.class);
		db.index().removeVertexIndex("publishedWebrootPathInfoIndex", NodeGraphFieldContainerImpl.class);
		db.index().removeVertexIndex("webrootUrlInfoIndex", NodeGraphFieldContainerImpl.class);
		db.index().removeVertexIndex("publishedWebrootInfoIndex", NodeGraphFieldContainerImpl.class);
	}

	@Override
	public boolean isAllowedInCluster(MeshOptions options) {
		return false;
	}
}
