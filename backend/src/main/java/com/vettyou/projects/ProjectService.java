package com.vettyou.projects;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vettyou.common.InvalidFieldsException;
import com.vettyou.common.NotFoundException;
import com.vettyou.evidence.EvidenceService;
import com.vettyou.evidence.ReadmeParser;

@Service
public class ProjectService {

	private final ProjectRepository projects;

	private final ProjectSourceVersionRepository sources;

	private final EvidenceService evidence;

	private final ReadmeParser parser;

	public ProjectService(ProjectRepository projects, ProjectSourceVersionRepository sources, EvidenceService evidence,
			ReadmeParser parser) {
		this.projects = projects;
		this.sources = sources;
		this.evidence = evidence;
		this.parser = parser;
	}

	@Transactional(readOnly = true)
	public List<ProjectSummary> list(UUID userId) {
		return projects.findByUserIdOrderByCreatedAtDesc(userId).stream().map(ProjectSummary::from).toList();
	}

	@Transactional(readOnly = true)
	public ProjectResponse get(UUID userId, UUID id) {
		return toResponse(owned(userId, id));
	}

	@Transactional
	public ProjectResponse update(UUID userId, UUID id, ProjectDetailsRequest request) {
		Project project = owned(userId, id);
		project.apply(request);
		return toResponse(projects.save(project));
	}

	@Transactional
	public void delete(UUID userId, UUID id) {
		projects.delete(owned(userId, id));
	}

	@Transactional
	public ProjectResponse addReadme(UUID userId, UUID id, ReadmeUploadRequest request) {
		validate(request);
		Project project = owned(userId, id);
		parser.heading(request.body()).ifPresent(project::rename);
		saveReadme(userId, project, request);
		return toResponse(projects.save(project));
	}

	@Transactional
	public ProjectResponse reread(UUID userId, UUID id) {
		Project project = owned(userId, id);
		for (ProjectSourceVersion version : sources.findByProjectIdOrderByVersionNumberAsc(project.getId())) {
			evidence.extractFromReadme(userId, project.getId(), version.getId(), version.getBody());
		}
		return toResponse(project);
	}

	@Transactional
	public ProjectResponse importReadme(UUID userId, ReadmeUploadRequest request) {
		validate(request);
		String fileName = request.fileName().trim();
		Project project = projects.save(Project.fromReadme(userId, parser.title(request.body(), fileName),
				parser.summary(request.body())));
		saveReadme(userId, project, request);
		return toResponse(project);
	}

	private void saveReadme(UUID userId, Project project, ReadmeUploadRequest request) {
		int next = sources.findFirstByProjectIdOrderByVersionNumberDesc(project.getId())
			.map(version -> version.getVersionNumber() + 1)
			.orElse(1);
		ProjectSourceVersion version = sources
			.save(ProjectSourceVersion.of(userId, project.getId(), next, request.fileName().trim(), request.body()));
		evidence.extractFromReadme(userId, project.getId(), version.getId(), request.body());
	}

	private static void validate(ReadmeUploadRequest request) {
		String lower = request.fileName().trim().toLowerCase(Locale.ROOT);
		if (!(lower.endsWith(".md") || lower.endsWith(".markdown") || lower.endsWith(".txt"))) {
			throw new InvalidFieldsException("fileName", "Upload a .md, .markdown or .txt file.");
		}
		if (request.body().indexOf('\0') >= 0) {
			throw new InvalidFieldsException("body", "That file is not plain text.");
		}
	}

	private Project owned(UUID userId, UUID id) {
		return projects.findByIdAndUserId(id, userId).orElseThrow(NotFoundException::new);
	}

	private ProjectResponse toResponse(Project project) {
		List<ProjectResponse.Source> versions = sources.findByProjectIdOrderByVersionNumberAsc(project.getId())
			.stream()
			.map(ProjectResponse.Source::from)
			.toList();
		return new ProjectResponse(project.getId(), project.getName(), project.getSummary(), project.getRepoUrl(),
				project.getLiveUrl(), versions, evidence.forProject(project.getId()));
	}

}
