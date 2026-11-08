# ADR 0006: IMAP over TLS for Namecheap Private Email

- **Status:** Accepted
- **Date:** 2026-11-07

## Context

Job replies go to `contact@jakeestrada.org`. DNS MX for `jakeestrada.org` is Namecheap Private Email (`mx1.privateemail.com`). That host has no Gmail-style REST API. IMAP over TLS is the supported way for a third-party app to read the inbox.

## Decision

- Connect with Jakarta Mail (Angus Mail) over IMAPS to `mail.privateemail.com:993`.
- The person types the mailbox address and an app password in the signed-in UI. The password is sent to the Java API, tested with a login, then stored encrypted. It is never returned, never logged, and never kept in the Angular build or localStorage.
- Encryption key is `MAIL_ENCRYPTION_KEY` on the server only.
- Reading uses IMAP UID sync of INBOX and Sent. Classification is Java phrase rules (`rules-1`), not the LLM. Status changes only after the person confirms a message.

## Alternatives considered

- Gmail API or Microsoft Graph: the mailbox is not on those hosts.
- Storing the password in `.env`: that would be a shared server secret, not the person's mailbox login.

## Consequences

- Vettyou can prove the mailbox login works before saving it.
- Status updates from mail still wait on a later sync and review step.
