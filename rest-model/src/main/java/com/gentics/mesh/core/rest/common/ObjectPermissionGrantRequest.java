package com.gentics.mesh.core.rest.common;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * Request to grant permissions to multiple roles
 */
public class ObjectPermissionGrantRequest extends ObjectPermissionResponse {
	@JsonProperty(required = false, defaultValue = "false")
	@JsonPropertyDescription("Flag which indicates whether the permissions granted to only the given roles (will be revoked from all other roles).")
	private boolean exclusive = false;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles which are ignored when the exclusive flag is set.")
	private Set<RoleReference> ignore;

	/**
	 * Flag that indicated that the request should be executed exclusively.
	 *
	 * @return Flag value
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Set the flag which indicated whether the permission changes should be applied exclusively.
	 *
	 * @param exclusive
	 *            Flag value
	 * @return Fluent API
	 */
	public ObjectPermissionGrantRequest setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
		return this;
	}

	public Set<RoleReference> getIgnore() {
		return ignore;
	}

	public ObjectPermissionGrantRequest setIgnore(Set<RoleReference> ignore) {
		this.ignore = ignore;
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setCreate(Set<RoleReference> create) {
		super.setCreate(create);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setRead(Set<RoleReference> read) {
		super.setRead(read);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setUpdate(Set<RoleReference> update) {
		super.setUpdate(update);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setDelete(Set<RoleReference> delete) {
		super.setDelete(delete);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setPublish(Set<RoleReference> publish) {
		super.setPublish(publish);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setReadPublished(Set<RoleReference> readPublished) {
		super.setReadPublished(readPublished);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest add(RoleReference role, Permission permission) {
		super.add(role, permission);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest set(RoleReference role, Permission perm, boolean flag) {
		super.set(role, perm, flag);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setOthers(boolean includePublishPermissions) {
		super.setOthers(includePublishPermissions);
		return this;
	}
}
