package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Set;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HasPermissions;
import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

// TODO move the contents of this to UserDao once migration is done
public interface UserDaoWrapper extends UserDao, DaoWrapper<HibUser>, DaoTransformable<HibUser, UserResponse> {

	String getSubETag(HibUser user, InternalActionContext ac);

	TraversalResult<? extends HibUser> findAll();

	/**
	 * Check whether the user has the given permission on the given element.
	 *
	 * @param user
	 * @param element
	 * @param permission
	 * @return
	 * @deprecated Use {@link #hasPermission(HibUser, HibElement, GraphPermission)} instead.
	 */
	@Deprecated
	boolean hasPermission(HibUser user, MeshVertex element, GraphPermission permission);

	boolean hasPermission(HibUser user, HibElement element, GraphPermission permission);

	/**
	 * Check whether the user has the given permission on the element with the given id.
	 *
	 * @param user
	 * @param elementId
	 * @param permission
	 * @return
	 */
	boolean hasPermissionForId(HibUser user, Object elementId, GraphPermission permission);

	/**
	 * Check the read permission on the given container and return false if the needed permission to read the container is not set. This method will not return
	 * false if the user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param user
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	boolean hasReadPermission(HibUser user, NodeGraphFieldContainer container, String branchUuid, String requestedVersion);

	/**
	 * Check the read permission on the given container and fail if the needed permission to read the container is not set. This method will not fail if the
	 * user has READ permission or READ_PUBLISH permission on a published node.
	 *
	 * @param container
	 * @param branchUuid
	 * @param requestedVersion
	 */
	default void failOnNoReadPermission(HibUser user, NodeGraphFieldContainer container, String branchUuid, String requestedVersion) {
		Node node = container.getParentNode();
		if (!hasReadPermission(user, container, branchUuid, requestedVersion)) {
			throw error(FORBIDDEN, "error_missing_perm", node.getUuid(),
				"published".equals(requestedVersion)
					? READ_PUBLISHED_PERM.getRestPerm().getName()
					: READ_PERM.getRestPerm().getName());
		}
	}

	/**
	 * Check whether the user is allowed to read the given node. Internally this check the currently configured version scope and check for
	 * {@link GraphPermission#READ_PERM} or {@link GraphPermission#READ_PUBLISHED_PERM}.
	 *
	 * @param user
	 * @param ac
	 * @param node
	 * @return
	 */
	boolean canReadNode(HibUser user, InternalActionContext ac, Node node);

	HibUser loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	TransformablePage<? extends HibUser> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	HibUser loadObjectByUuid(InternalActionContext ac, String userUuid, GraphPermission perm);

	/**
	 * Return the permission info object for the given vertex.
	 *
	 * @param user
	 * @param element
	 * @return
	 */
	PermissionInfo getPermissionInfo(HibUser user, HibElement element);

	/**
	 * Return a set of permissions which the user got for the given vertex.
	 *
	 * @param element
	 * @return
	 */
	Set<GraphPermission> getPermissions(HibUser user, HibElement element);

	/**
	 * Update the vertex using the action context information.
	 *
	 * @param user
	 * @param ac
	 * @param batch
	 *            Batch to which entries will be added in order to update the search index.
	 * @return true if the element was updated. Otherwise false
	 */
	boolean update(HibUser user, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Same as {@link User#update(InternalActionContext, EventQueueBatch)}, but does not actually perform any changes.
	 *
	 * Useful to check if any changes have to be made.
	 *
	 * @param user
	 * @param ac
	 * @return true if the user would have been modified
	 */
	boolean updateDry(HibUser user, InternalActionContext ac);

	HibUser create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @return
	 */
	default HibUser create(String username, HibUser creator) {
		return create(username, creator, null);
	}

	/**
	 * Create a new user with the given username and assign it to this aggregation node.
	 * 
	 * @param username
	 *            Username for the newly created user
	 * @param creator
	 *            User that is used to create creator and editor references
	 * @param uuid
	 *            Optional uuid
	 * @return
	 */
	HibUser create(String username, HibUser creator, String uuid);

	/**
	 * This method will set CRUD permissions to the target node for all roles that would grant the given permission on the node. The method is most often used
	 * to assign CRUD permissions on newly created elements. Example for adding CRUD permissions on a newly created project: The method will first determine the
	 * list of roles that would initially enable you to create a new project. It will do so by examining the projectRoot node. After this step the CRUD
	 * permissions will be added to the newly created project and the found roles. In this case the call would look like this:
	 * addCRUDPermissionOnRole(projectRoot, Permission.CREATE_PERM, newlyCreatedProject); This method will ensure that all users/roles that would be able to
	 * create an element will also be able to CRUD it even when the creator of the element was only assigned to one of the enabling roles. Additionally the
	 * permissions of the source node are inherited by the target node. All permissions between the source node and roles are copied to the target node.
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked against to find all roles that would grant the given permission.
	 * @param permission
	 *            Permission that is used in conjunction with the node to determine the list of affected roles.
	 * @param targetNode
	 *            Node to which the CRUD permissions will be assigned.
	 * @return Fluent API
	 */
	HibUser addCRUDPermissionOnRole(HibUser user, HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode);

	/**
	 * This method adds additional permissions to the target node. The roles are selected like in method
	 * {@link #addCRUDPermissionOnRole(User, HasPermissions, GraphPermission, MeshVertex)} .
	 *
	 * @param user
	 * @param sourceNode
	 *            Node that will be checked
	 * @param permission
	 *            checked permission
	 * @param targetNode
	 *            target node
	 * @param toGrant
	 *            permissions to grant
	 * @return Fluent API
	 */
	HibUser addPermissionsOnRole(HibUser user, HasPermissions sourceNode, GraphPermission permission, MeshVertex targetNode,
		GraphPermission... toGrant);

	/**
	 * Inherit permissions edges from the source node and assign those permissions to the target node.
	 *
	 * @param user
	 * @param sourceNode
	 * @param targetNode
	 * @return Fluent API
	 * @deprecated Use {@link #inheritRolePermissions(HibUser, HibElement, HibElement)} instead.
	 */
	@Deprecated
	HibUser inheritRolePermissions(HibUser user, MeshVertex sourceNode, MeshVertex targetNode);

	HibUser inheritRolePermissions(HibUser user, HibElement source, HibElement target);

	/**
	 * Set the plaintext password. Internally the password string will be hashed and the password hash will be set. This will also set
	 * {@link User#setForcedPasswordChange(boolean)} to false.
	 *
	 * @param user
	 * @param password
	 * @return Fluent API
	 */
	// TODO change this to an async call since hashing of the password is
	// blocking
	HibUser setPassword(HibUser user, String password);

	HibUser findByUuid(String uuid);

	HibUser findByName(String name);

	/**
	 * Find the user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	HibUser findByUsername(String username);

	void delete(HibUser user, BulkActionContext bac);

	/**
	 * Find the mesh auth user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUsername(String username);

	/**
	 * Find the mesh auth user with the given UUID.
	 * 
	 * @param userUuid
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUuid(String userUuid);

	/**
	 * Check whether the given token code is valid. This method will also clear the token code if the token has expired.
	 *
	 * @param token
	 *            Token Code
	 * @param maxTokenAgeMins
	 *            maximum allowed token age in minutes
	 * @return
	 */
	default boolean isResetTokenValid(HibUser user, String token, int maxTokenAgeMins) {
		Long resetTokenIssueTimestamp = user.getResetTokenIssueTimestamp();
		if (token == null || resetTokenIssueTimestamp == null) {
			return false;
		}
		long maxTokenAge = 1000 * 60 * maxTokenAgeMins;
		long tokenAge = System.currentTimeMillis() - resetTokenIssueTimestamp;
		boolean isExpired = tokenAge > maxTokenAge;
		boolean isTokenMatch = token.equals(user.getResetToken());

		if (isTokenMatch && isExpired) {
			user.invalidateResetToken();
			return false;
		}
		return isTokenMatch && !isExpired;
	}

}
