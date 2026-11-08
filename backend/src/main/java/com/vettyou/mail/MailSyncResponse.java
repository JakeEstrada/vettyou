package com.vettyou.mail;

import java.util.List;

public record MailSyncResponse(int fetched, int pending, List<MailMessageResponse> messages) {
}
