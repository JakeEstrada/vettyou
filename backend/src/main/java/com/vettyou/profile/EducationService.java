package com.vettyou.profile;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.common.InvalidFieldsException;
import com.vettyou.common.NotFoundException;

@Service
public class EducationService {

	private final EducationRepository educations;

	public EducationService(EducationRepository educations) {
		this.educations = educations;
	}

	@Transactional(readOnly = true)
	public List<EducationResponse> list(UUID userId) {
		return educations.findByUserIdOrderByCreatedAtDesc(userId).stream().map(EducationResponse::from).toList();
	}

	@Transactional
	public EducationResponse create(UUID userId, EducationRequest request) {
		validate(request);
		return EducationResponse.from(educations.save(Education.create(userId, request)));
	}

	@Transactional
	public EducationResponse update(UUID userId, UUID id, EducationRequest request) {
		validate(request);
		Education education = educations.findByIdAndUserId(id, userId).orElseThrow(NotFoundException::new);
		education.apply(request);
		return EducationResponse.from(educations.save(education));
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		educations.delete(educations.findByIdAndUserId(id, userId).orElseThrow(NotFoundException::new));
	}

	private static void validate(EducationRequest request) {
		if (request.startDate() != null && request.endDate() != null && request.endDate().isBefore(request.startDate())) {
			throw new InvalidFieldsException("endDate", "End date must be on or after the start date.");
		}
	}

}
