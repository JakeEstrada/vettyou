package com.vettyou.mail;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vettyou.applications.ApplicationResponse;
import com.vettyou.auth.CurrentUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/mail")
public class MailConnectionController {

	private final CurrentUser currentUser;

	private final MailConnectionService connections;

	private final MailInboxService inbox;

	public MailConnectionController(CurrentUser currentUser, MailConnectionService connections, MailInboxService inbox) {
		this.currentUser = currentUser;
		this.connections = connections;
		this.inbox = inbox;
	}

	@GetMapping("/connection")
	public MailConnectionResponse get(@AuthenticationPrincipal UserDetails principal) {
		return connections.get(currentUser.id(principal));
	}

	@PostMapping("/connections")
	public ResponseEntity<MailConnectionResponse> connect(@AuthenticationPrincipal UserDetails principal,
			@Valid @RequestBody MailConnectionRequest request) {
		return ResponseEntity.status(201).body(connections.connect(currentUser.id(principal), request));
	}

	@DeleteMapping("/connection")
	public ResponseEntity<Void> disconnect(@AuthenticationPrincipal UserDetails principal) {
		connections.disconnect(currentUser.id(principal));
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/sync")
	public MailSyncResponse sync(@AuthenticationPrincipal UserDetails principal) {
		return inbox.sync(currentUser.id(principal));
	}

	@GetMapping("/messages")
	public List<MailMessageResponse> messages(@AuthenticationPrincipal UserDetails principal) {
		return inbox.pending(currentUser.id(principal));
	}

	@PostMapping("/messages/{id}/accept")
	public ApplicationResponse accept(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id,
			@RequestBody(required = false) MailAcceptRequest request) {
		return inbox.accept(currentUser.id(principal), id, request);
	}

	@PostMapping("/messages/{id}/ignore")
	public ResponseEntity<Void> ignore(@AuthenticationPrincipal UserDetails principal, @PathVariable UUID id) {
		inbox.ignore(currentUser.id(principal), id);
		return ResponseEntity.noContent().build();
	}

}
