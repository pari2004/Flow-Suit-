package com.flowsuite.api.security;

import com.flowsuite.api.common.error.ApiException;
import com.flowsuite.api.common.error.ErrorCode;
import com.flowsuite.api.config.JwtProperties;
import com.flowsuite.api.domain.entity.User;
import com.flowsuite.api.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-definitely-long-enough-32b";

    private JwtService service(long ttlMinutes) {
        return new JwtService(new JwtProperties(SECRET, ttlMinutes, 30, 30, "flowsuite"));
    }

    @Test
    void generatesAndParsesSubject() {
        JwtService jwt = service(15);
        User user = TestFixtures.user("a@b.com", "A", "hash");

        String token = jwt.generateAccessToken(user);
        UUID parsed = jwt.parseUserId(token);

        assertThat(parsed).isEqualTo(user.getId());
    }

    @Test
    void rejectsTamperedToken() {
        JwtService jwt = service(15);
        User user = TestFixtures.user("a@b.com", "A", "hash");
        String token = jwt.generateAccessToken(user) + "x";

        assertThatThrownBy(() -> jwt.parseUserId(token))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_INVALID));
    }

    @Test
    void rejectsExpiredToken() {
        JwtService jwt = service(-1); // already expired
        User user = TestFixtures.user("a@b.com", "A", "hash");
        String token = jwt.generateAccessToken(user);

        assertThatThrownBy(() -> jwt.parseUserId(token))
                .isInstanceOf(ApiException.class)
                .satisfies(e -> assertThat(((ApiException) e).getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties("short", 15, 30, 30, "flowsuite")))
                .isInstanceOf(IllegalStateException.class);
    }
}
