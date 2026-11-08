package com.vettyou.profile;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "profiles")
public class Profile {

	@Id
	private UUID id;

	@Column(name = "user_id", nullable = false, unique = true)
	private UUID userId;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(nullable = false)
	private String email;

	@Column(nullable = false)
	private String phone;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false)
	private String website;

	@Column(nullable = false)
	private String linkedin;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Profile() {
	}

	public static Profile create(UUID userId, ProfileRequest request) {
		Profile profile = new Profile();
		profile.userId = userId;
		profile.apply(request);
		return profile;
	}

	public void apply(ProfileRequest request) {
		displayName = request.displayName().trim();
		email = blank(request.email());
		phone = blank(request.phone());
		location = blank(request.location());
		website = blank(request.website());
		linkedin = blank(request.linkedin());
	}

	private static String blank(String value) {
		return value == null ? "" : value.trim();
	}

	@PrePersist
	void onCreate() {
		Instant now = Instant.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		createdAt = now;
		updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		updatedAt = Instant.now();
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public String getLocation() {
		return location;
	}

	public String getWebsite() {
		return website;
	}

	public String getLinkedin() {
		return linkedin;
	}

}
