package com.flowsuite.api.common.mail;

import com.flowsuite.api.config.MailProperties;
import com.flowsuite.api.domain.enums.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Transactional email for account flows (password reset, invitations).
 *
 * <p>Delivery is best-effort and degrades gracefully: when an SMTP {@link JavaMailSender} is
 * configured (i.e. {@code spring.mail.host} is set) and {@code flowsuite.mail.enabled} is true,
 * a real message is sent. Otherwise — and in local/dev where no SMTP is wired — the action link
 * is logged so the flow remains testable. Transport failures are logged, never propagated, so a
 * mail outage cannot break registration/invite/reset request handling.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final MailProperties mailProperties;

    public EmailService(ObjectProvider<JavaMailSender> mailSenderProvider, MailProperties mailProperties) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailProperties = mailProperties;
    }

    /** FR-05: send the password-reset link. */
    public void sendPasswordResetEmail(String toEmail, String rawToken) {
        String link = mailProperties.normalizedBaseUrl() + "/reset-password?token=" + encode(rawToken);
        String body = """
                You (or someone using this address) requested a password reset for your FlowSuite account.

                Reset your password using the link below. It expires shortly and can be used once:

                %s

                If you didn't request this, you can safely ignore this email — your password will not change.
                """.formatted(link);
        send(toEmail, "Reset your FlowSuite password", body, "password-reset", link);
    }

    /** FR-10: send an invitation link to join an organization. */
    public void sendInvitationEmail(String toEmail, String organizationName, Role role, String rawToken) {
        String link = mailProperties.normalizedBaseUrl() + "/accept-invite?token=" + encode(rawToken);
        String body = """
                You've been invited to join %s on FlowSuite as a %s.

                Accept the invitation and set up your account using the link below. It expires in 7 days:

                %s

                If you weren't expecting this invitation, you can ignore this email.
                """.formatted(organizationName, role.name().toLowerCase(), link);
        send(toEmail, "You're invited to join " + organizationName + " on FlowSuite", body, "invite", link);
    }

    // ---------------------------------------------------------------------

    private void send(String toEmail, String subject, String body, String kind, String link) {
        JavaMailSender sender = mailProperties.enabled() ? mailSenderProvider.getIfAvailable() : null;
        if (sender == null) {
            // No SMTP configured (or disabled): log the actionable link for local/dev testing.
            log.info("[email:{}] (delivery disabled) to={} link={}", kind, toEmail, link);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(String.format("%s <%s>", mailProperties.fromName(), mailProperties.fromAddress()));
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("[email:{}] sent to={}", kind, toEmail);
        } catch (Exception e) {
            // Best-effort: never let a mail outage break the caller's flow.
            log.warn("[email:{}] delivery failed to={}: {}", kind, toEmail, e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
