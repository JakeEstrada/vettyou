package com.vettyou.profile;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.vettyou.auth.AuthService;
import com.vettyou.auth.UserAccount;
import com.vettyou.auth.UserRepository;
import com.vettyou.common.InvalidFieldsException;

@Service
public class ExperienceService {

	private final UserRepository users;

	private final WorkExperienceRepository experiences;

	private final SourceVersionRepository versions;

	public ExperienceService(UserRepository users, WorkExperienceRepository experiences, SourceVersionRepository versions) {
		this.users = users;
		this.experiences = experiences;
		this.versions = versions;
	}

	@Transactional(readOnly = true)
	public List<ExperienceSummary> list(String email) {
		UserAccount account = account(email);
		return experiences.findByUserIdOrderByCreatedAtDesc(account.getId()).stream().map(ExperienceSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public List<ExperienceText> texts(UUID userId) {
		return experiences.findByUserIdOrderByCreatedAtDesc(userId).stream()
			.map(experience -> new ExperienceText(experience.getId(), experience.getEmployer(), experience.getTitle(),
					experience.getLocation(), experience.getStartDate(), experience.getEndDate(),
					experience.isCurrentRole(),
					versions.findFirstByExperienceIdOrderByVersionNumberDesc(experience.getId())
						.map(SourceVersion::getBody)
						.orElse("")))
			.toList();
	}

	@Transactional(readOnly = true)
	public ExperienceResponse get(String email, UUID id) {
		UserAccount account = account(email);
		return response(own(account.getId(), id));
	}

	@Transactional
	public ExperienceResponse create(String email, ExperienceRequest request) {
		validate(request);
		UserAccount account = account(email);
		WorkExperience experience = experiences.saveAndFlush(WorkExperience.create(account.getId(), request));
		versions.save(SourceVersion.create(account.getId(), experience.getId(), 1, request.body().trim()));
		return response(experience);
	}

	@Transactional
	public ExperienceResponse update(String email, UUID id, ExperienceRequest request) {
		validate(request);
		UserAccount account = account(email);
		WorkExperience experience = own(account.getId(), id);
		experience.apply(request);
		String body = request.body().trim();
		SourceVersion latest = versions.findFirstByExperienceIdOrderByVersionNumberDesc(experience.getId())
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource was not found"));
		if (!latest.getBody().equals(body)) {
			versions.save(SourceVersion.create(account.getId(), experience.getId(), latest.getVersionNumber() + 1, body));
		}
		return response(experiences.save(experience));
	}

	@Transactional
	public void delete(String email, UUID id) {
		UserAccount account = account(email);
		experiences.delete(own(account.getId(), id));
	}

	private ExperienceResponse response(WorkExperience experience) {
		return ExperienceResponse.from(experience, versions.findByExperienceIdOrderByVersionNumberAsc(experience.getId()));
	}

	private WorkExperience own(UUID userId, UUID id) {
		return experiences.findByIdAndUserId(id, userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource was not found"));
	}

	private UserAccount account(String email) {
		return users.findByEmail(AuthService.normalize(email))
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
	}

	private static void validate(ExperienceRequest request) {
		if (request.current() && request.endDate() != null) {
			throw new InvalidFieldsException("endDate", "Leave the end date empty if you still work here.");
		}
		if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
			throw new InvalidFieldsException("endDate", "End date must be on or after the start date.");
		}
	}

}
