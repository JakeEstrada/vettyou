package com.vettyou.mail;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MailMessageRepository extends JpaRepository<MailMessage, UUID> {

	List<MailMessage> findByUserIdAndReviewStateOrderBySentAtDesc(UUID userId, String reviewState);

	Optional<MailMessage> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndFolderAndUidvalidityAndUid(UUID userId, String folder, long uidvalidity, long uid);

	void deleteByUserId(UUID userId);

}
