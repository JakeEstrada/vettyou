package com.vettyou.mail;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MailLinkRepository extends JpaRepository<MailLink, UUID> {

	void deleteByUserId(UUID userId);

}
