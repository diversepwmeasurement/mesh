package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.io.File;
import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.binary.Binaries;
import com.gentics.mesh.core.data.binary.HibBinary;
import com.gentics.mesh.core.data.binary.HibImageVariant;
import com.gentics.mesh.core.data.binary.ImageVariant;
import com.gentics.mesh.core.data.binary.impl.ImageVariantImpl;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.core.data.storage.BinaryStorage;
import com.gentics.mesh.core.image.ImageManipulator;
import com.gentics.mesh.core.rest.node.field.image.ImageVariantRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.graphdb.spi.GraphDatabase;
import com.gentics.mesh.parameter.image.ImageManipulation;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.traversals.VertexTraversal;

import dagger.Lazy;

/**
 * @See {@link BinaryDaoWrapper}
 */
@Singleton
public class BinaryDaoWrapperImpl extends AbstractDaoWrapper<HibBinary> implements BinaryDaoWrapper {

	private final Binaries binaries;
	private final GraphDatabase database;
	private final ImageManipulator imageManipulator;
	private final BinaryStorage binaryStorage;

	@Inject
	public BinaryDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Binaries binaries, GraphDatabase database, ImageManipulator imageManipulator, BinaryStorage binaryStorage) {
		super(boot);
		this.binaries = binaries;
		this.database = database;
		this.imageManipulator = imageManipulator;
		this.binaryStorage = binaryStorage;
	}

	@Override
	public Result<? extends HibBinaryField> findFields(HibBinary binary) {
		return toGraph(binary).findFields();
	}

	@Override
	public Binaries binaries() {
		return binaries;
	}

	@Override
	public Result<? extends ImageVariant> getVariants(HibBinary binary, InternalActionContext ac) {
		return toGraph(binary).getVariants();
	}

	@Override
	public void deletePersistingVariant(HibBinary binary, HibImageVariant variant) {
		String variantUuid = variant.getUuid();
		ImageVariant imageVariant = toGraph(variant);
		toGraph(binary).unlinkOut(imageVariant, GraphRelationships.HAS_VARIANTS);
		imageVariant.remove();binaryStorage.delete(variantUuid).blockingGet();
	}

	@Override
	public ImageVariant createPersistedVariant(HibBinary binary, ImageVariantRequest request, Consumer<HibImageVariant> inflater) {
		FramedGraph graph = toGraph(binary).getGraph();
		ImageVariantImpl variant = graph.addFramedVertex(ImageVariantImpl.class);
		toGraph(binary).linkOut(variant, GraphRelationships.HAS_VARIANTS);
		inflater.accept(variant);
		
		String variantUuid = variant.getUuid();
		
		long filesize = imageManipulator.handleResize(binary, request)
				.flatMap(cachePath -> {
					long size = new File(cachePath).length();
					return binaryStorage.moveInPlace(variantUuid, cachePath, false).toSingleDefault(size);
				})
				.blockingGet();

		variant.setSize(filesize);
		return variant;
	}

	@Override
	public ImageVariant getVariant(HibBinary binary, ImageManipulation variant, InternalActionContext ac) {
		VertexTraversal<?, ?, ?> edge = toGraph(binary).out(GraphRelationships.HAS_VARIANTS);
		if (variant.getRect() != null) {
			edge = edge.has(ImageVariant.CROP_X_KEY, variant.getRect().getStartX()).has(ImageVariant.CROP_Y_KEY, variant.getRect().getStartY()).has(ImageVariant.WIDTH_KEY, variant.getRect().getWidth()).has(ImageVariant.HEIGHT_KEY, variant.getRect().getHeight());
		} else {
			edge = edge.hasNot(ImageVariant.CROP_X_KEY).hasNot(ImageVariant.CROP_Y_KEY);
			if (variant.getWidth() != null) {
				if ("auto".equals(variant.getWidth())) {
					edge = edge.has(ImageVariant.AUTO_KEY, true);
				} else {
					edge = edge.has(ImageVariant.WIDTH_KEY, Integer.parseInt(variant.getWidth()));
				}
			} else {
				edge = edge.hasNot(ImageVariant.WIDTH_KEY);
			}
			if (variant.getHeight() != null) {
				if ("auto".equals(variant.getHeight())) {
					edge = edge.has(ImageVariant.AUTO_KEY, true);
				} else {
					edge = edge.has(ImageVariant.HEIGHT_KEY, Integer.parseInt(variant.getHeight()));
				}
			} else {
				edge = edge.hasNot(ImageVariant.HEIGHT_KEY);
			}
		}
		if (variant.getCropMode() != null) {
			edge = edge.has(ImageVariant.CROP_MODE_KEY, variant.getCropMode().getKey());
		} else {
			edge = edge.hasNot(ImageVariant.CROP_MODE_KEY);
		}
		if (variant.getFocalPoint() != null) {
			edge = edge.has(ImageVariant.FOCAL_POINT_X_KEY, variant.getFocalPoint().getX()).has(ImageVariant.FOCAL_POINT_Y_KEY, variant.getFocalPoint().getY());
		} else {
			edge = edge.hasNot(ImageVariant.FOCAL_POINT_X_KEY).hasNot(ImageVariant.FOCAL_POINT_Y_KEY);
		}
		if (variant.getFocalPointZoom() != null) {
			edge = edge.has(ImageVariant.FOCAL_POINT_ZOOM_KEY, variant.getFocalPointZoom());
		} else {
			edge = edge.hasNot(ImageVariant.FOCAL_POINT_ZOOM_KEY);
		}
		if (variant.getResizeMode() != null) {
			edge = edge.has(ImageVariant.RESIZE_MODE_KEY, variant.getResizeMode().getKey());
		} else {
			edge = edge.hasNot(ImageVariant.RESIZE_MODE_KEY);
		}
		return edge.nextOrDefaultExplicit(ImageVariantImpl.class, null);
	}
}
