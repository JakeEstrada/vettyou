package com.vettyou.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailFolderTest {

	@Test
	void usesTheMailboxFoldersThePersonAlreadyFilesInto() {
		assertThat(MailFolder.section("INBOX.Applications Recieved")).isEqualTo("received");
		assertThat(MailFolder.section("INBOX.Rejections")).isEqualTo("rejections");
		assertThat(MailFolder.section("INBOX.WINS")).isEqualTo("wins");
		assertThat(MailFolder.section("INBOX.Job Listings.Indeed")).isEqualTo("listings");
		assertThat(MailFolder.shouldSync("INBOX.Job Listings")).isFalse();
		assertThat(MailFolder.shouldSync("INBOX.Rejections")).isTrue();
		assertThat(MailFolder.label("INBOX.Rejections", "Honeywell jobs", "We have new jobs that might interest you.")
			.category()).isEqualTo("rejected");
		assertThat(MailFolder.label("INBOX.Applications Recieved", "Thanks", "We received your application.").category())
			.isEqualTo("applied");
	}

}
