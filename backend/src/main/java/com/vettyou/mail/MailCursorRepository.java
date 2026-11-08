package com.vettyou.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MailCursorRepository extends JpaRepository<MailCursor, UUID> {

	Optional<MailCursor> findByUserIdAndFolder(UUID userId, String folder);

	void deleteByUserId(UUID userId);

}
