package com.vettyou.resumes;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeUploadRepository extends JpaRepository<ResumeUpload, UUID> {
}
