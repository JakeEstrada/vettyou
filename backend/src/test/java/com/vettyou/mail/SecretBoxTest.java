package com.vettyou.mail;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SecretBoxTest {

	@Test
	void sealsWithoutLeavingThePlainSecretInTheStoredValue() {
		SecretBox box = new SecretBox("test-mail-encryption-key-not-for-prod");
		String packed = box.seal("mailbox-secret");
		assertThat(packed).doesNotContain("mailbox-secret");
		assertThat(box.open(packed)).isEqualTo("mailbox-secret");
	}

}
