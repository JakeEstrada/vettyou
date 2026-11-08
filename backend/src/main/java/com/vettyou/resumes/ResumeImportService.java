package com.vettyou.resumes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.profile.EducationRequest;
import com.vettyou.profile.EducationResponse;
import com.vettyou.profile.EducationService;
import com.vettyou.profile.ExperienceRequest;
import com.vettyou.profile.ExperienceService;
import com.vettyou.profile.ExperienceSummary;
import com.vettyou.profile.ProfileRequest;
import com.vettyou.profile.ProfileResponse;
import com.vettyou.profile.ProfileService;
import com.vettyou.projects.ProjectDetailsRequest;
import com.vettyou.projects.ProjectResponse;
import com.vettyou.projects.ProjectService;
import com.vettyou.projects.ProjectSummary;
import com.vettyou.projects.ReadmeUploadRequest;
import com.vettyou.resumes.ResumeImportResponse.Added;
import com.vettyou.resumes.ResumeParser.Contact;
import com.vettyou.resumes.ResumeParser.Job;
import com.vettyou.resumes.ResumeParser.Parsed;
import com.vettyou.resumes.ResumeParser.ProjectBlock;
import com.vettyou.resumes.ResumeParser.School;

@Service
public class ResumeImportService {

	private final ResumeFileReader files;

	private final ResumeParser parser;

	private final ResumeUploadRepository uploads;

	private final ProfileService profiles;

	private final ExperienceService experiences;

	private final EducationService educations;

	private final ProjectService projects;

	public ResumeImportService(ResumeFileReader files, ResumeParser parser, ResumeUploadRepository uploads,
			ProfileService profiles, ExperienceService experiences, EducationService educations, ProjectService projects) {
		this.files = files;
		this.parser = parser;
		this.uploads = uploads;
		this.profiles = profiles;
		this.experiences = experiences;
		this.educations = educations;
		this.projects = projects;
	}

	@Transactional
	public ResumeImportResponse importResume(UUID userId, String email, ResumeUploadRequest request) {
		String text = files.text(request);
		uploads.save(ResumeUpload.create(userId, request.fileName().trim(), text));
		Parsed parsed = parser.parse(text);
		List<String> skipped = new ArrayList<>();
		return new ResumeImportResponse(mergeProfile(email, parsed.contact(), skipped), jobs(email, parsed.jobs(), skipped),
				projects(userId, parsed.projects(), skipped), schools(userId, parsed.schools(), skipped), skipped);
	}

	private ProfileResponse mergeProfile(String email, Contact contact, List<String> skipped) {
		ProfileResponse current = profiles.get(email);
		String name = prefer(contact.name(), current.displayName());
		if (name.isBlank()) {
			skipped.add("No name found, so the profile was left as it was.");
			return current;
		}
		return profiles.save(email, new ProfileRequest(name, prefer(contact.email(), current.email()),
				prefer(contact.phone(), current.phone()),
				prefer(contact.location(), current.location()), prefer(contact.website(), current.website()),
				prefer(contact.linkedin(), current.linkedin())));
	}

	private List<Added> jobs(String email, List<Job> found, List<String> skipped) {
		Set<String> seen = new HashSet<>();
		for (ExperienceSummary existing : experiences.list(email)) {
			seen.add(key(existing.employer(), existing.title()));
		}
		List<Added> added = new ArrayList<>();
		for (int index = found.size() - 1; index >= 0; index--) {
			Job job = found.get(index);
			String label = job.title() + " at " + job.employer();
			if (!seen.add(key(job.employer(), job.title()))) {
				skipped.add(label + " was already saved.");
				continue;
			}
			var saved = experiences.create(email, new ExperienceRequest(job.employer(), job.title(), job.location(),
					job.start(), job.end(), job.current(), job.body()));
			added.add(new Added(saved.id(), label));
		}
		return added.reversed();
	}

	private List<Added> projects(UUID userId, List<ProjectBlock> found, List<String> skipped) {
		Set<String> seen = new HashSet<>();
		for (ProjectSummary existing : projects.list(userId)) {
			seen.add(key(existing.name()));
		}
		List<Added> added = new ArrayList<>();
		for (int index = found.size() - 1; index >= 0; index--) {
			ProjectBlock block = found.get(index);
			if (!seen.add(key(block.name()))) {
				skipped.add(block.name() + " was already saved.");
				continue;
			}
			ProjectResponse saved = projects.importReadme(userId,
					new ReadmeUploadRequest(block.name() + ".txt", block.markdown()));
			if (!block.url().isBlank()) {
				boolean github = block.url().toLowerCase(Locale.ROOT).contains("github.com");
				saved = projects.update(userId, saved.id(), new ProjectDetailsRequest(saved.summary(),
						github ? block.url() : saved.repoUrl(), github ? saved.liveUrl() : block.url()));
			}
			added.add(new Added(saved.id(), saved.name()));
		}
		return added.reversed();
	}

	private List<Added> schools(UUID userId, List<School> found, List<String> skipped) {
		Set<String> seen = new HashSet<>();
		for (EducationResponse existing : educations.list(userId)) {
			seen.add(key(existing.institution(), existing.credential()));
		}
		List<Added> added = new ArrayList<>();
		for (int index = found.size() - 1; index >= 0; index--) {
			School school = found.get(index);
			String label = school.credential().isBlank() ? school.institution()
					: school.institution() + ", " + school.credential();
			if (!seen.add(key(school.institution(), school.credential()))) {
				skipped.add(label + " was already saved.");
				continue;
			}
			EducationResponse saved = educations.create(userId, new EducationRequest(school.institution(),
					school.credential(), school.field(), school.start(), school.end()));
			added.add(new Added(saved.id(), label));
		}
		return added.reversed();
	}

	private static String prefer(String incoming, String current) {
		return incoming == null || incoming.isBlank() ? current : incoming;
	}

	private static String key(String... parts) {
		return String.join("\n", parts).trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
	}

}
