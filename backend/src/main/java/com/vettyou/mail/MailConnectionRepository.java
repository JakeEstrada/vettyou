package com.vettyou.mail;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface MailConnectionRepository extends JpaRepository<MailConnection, UUID> {

	Optional<MailConnection> findByUserId(UUID userId);

}
