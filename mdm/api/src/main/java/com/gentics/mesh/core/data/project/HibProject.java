package com.gentics.mesh.core.data.project;

import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;

import java.util.Objects;
import java.util.Set;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.user.HibUserTracking;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for project.
 */
public interface HibProject extends HibCoreElement<ProjectResponse>, HibReferenceableElement<ProjectReference>, HibUserTracking, HibBucketableElement, HibNamedElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.PROJECT, PROJECT_CREATED, PROJECT_UPDATED, PROJECT_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Return the currently set latest branch of the project.
	 *
	 * @return
	 */
	HibBranch getLatestBranch();

	/**
	 * Return the initial branch of the project.
	 *
	 * @return
	 */
	HibBranch getInitialBranch();

	/**
	 * Return the base node of the project.
	 *
	 * @return
	 */
	HibNode getBaseNode();

	/**
	 * Set the base node of the project.
	 *
	 * @param baseNode
	 */
	void setBaseNode(HibNode baseNode);

	/**
	 * Return the schema hib base element which is used to track permissions.
	 *
	 * @return
	 */
	HibBaseElement getSchemaPermissionRoot();

	/**
	 * Return the branch hib base element which is used to track permissions.
	 * 
	 * @return
	 */
	HibBaseElement getBranchPermissionRoot();

	/**
	 * Return the tag family hib base element which tracks tag family permissions.
	 *
	 * @return
	 */
	HibBaseElement getTagFamilyPermissionRoot();

	/**
	 * Return the node hib base element which tracks node permissions.
	 *
	 * @return
	 */
	HibBaseElement getNodePermissionRoot();

	/**
	 * Return a traversal result of languages that were assigned to the project.
	 *
	 * @return
	 */
	Result<? extends HibLanguage> getLanguages();

	/**
	 * Find an assigned language by its tag.
	 * @param languageTag 
	 * 
	 * @return
	 */
	HibLanguage findLanguageByTag(String languageTag);

	/**
	 * Unassign the language from the project.
	 *
	 * @param language
	 */
	void removeLanguage(HibLanguage language);

	/**
	 * Assign the given language to the project.
	 *
	 * @param language
	 */
	void addLanguage(HibLanguage language);

	/**
	 * Return a traversal result of the schemas that were assigned to the project.
	 *
	 * @return
	 */
	Result<? extends HibSchema> getSchemas();

	/**
	 * Return a traversal result of the microschemas that were assigned to the project.
	 *
	 * @return
	 */
	Result<? extends HibMicroschema> getMicroschemas();

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/projects/" + getUuid();
	}

	@Override
	default ProjectReference transformToReference() {
		return new ProjectReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	default boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant, Set<InternalPermission> permissionsToRevoke) {
		boolean permissionChanged = false;
		if (recursive) {
			ProjectDao projectDao = Tx.get().projectDao();
			permissionChanged = projectDao.getTagFamilyPermissionRoot(this).applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			permissionChanged = projectDao.getBranchPermissionRoot(this).applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			permissionChanged = getBaseNode().applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		}
		permissionChanged = HibCoreElement.super.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;

	}

	/**
	 * Compose the document id for project index documents.
	 *
	 * @param projectUuid
	 * @return
	 */
	static String composeDocumentId(String projectUuid) {
		Objects.requireNonNull(projectUuid, "A projectUuid must be provided.");
		return projectUuid;
	}

	/**
	 * Compose the index name for the project index.
	 *
	 * @return
	 */
	static String composeIndexName() {
		return "project";
	}
}
