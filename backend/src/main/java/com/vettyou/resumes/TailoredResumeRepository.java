package com.vettyou.resumes;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TailoredResumeRepository extends JpaRepository<TailoredResume, UUID> {

	@Query("select new com.vettyou.resumes.TailoredResumeSummary(r.id, r.version, r.createdAt, r.total) "
			+ "from TailoredResume r where r.postingId = :postingId and r.userId = :userId order by r.version desc")
	List<TailoredResumeSummary> summaries(@Param("postingId") UUID postingId, @Param("userId") UUID userId);

	Optional<TailoredResume> findByIdAndPostingIdAndUserId(UUID id, UUID postingId, UUID userId);

	@Query("select coalesce(max(r.version), 0) from TailoredResume r where r.postingId = :postingId")
	int lastVersion(@Param("postingId") UUID postingId);

}
