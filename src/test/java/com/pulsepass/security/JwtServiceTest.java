package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String ISSUER = "pulsepass-test";
    private static final Duration TTL = Duration.ofMinutes(15);

    // 64 bytes: permite firmar tambien con HS512 en la prueba de algoritmo.
    private static final String SECRET = Base64.getEncoder().encodeToString(new byte[64]);

    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private final JwtService service = new JwtService(SECRET, ISSUER, TTL);

    // ---------- Construccion ----------

    @Test
    void constructor_rejectsSecretShorterThan32Bytes() {
        String shortSecret = Base64.getEncoder().encodeToString(new byte[31]);

        assertThatThrownBy(() -> new JwtService(shortSecret, ISSUER, TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void constructor_acceptsSecretOfExactly32Bytes() {
        String minimum = Base64.getEncoder().encodeToString(new byte[32]);

        assertThat(new JwtService(minimum, ISSUER, TTL).getAccessTokenTtl()).isEqualTo(TTL);
    }

    @Test
    void constructor_rejectsBlankIssuer() {
        assertThatThrownBy(() -> new JwtService(SECRET, "  ", TTL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void constructor_rejectsZeroOrNegativeTtl() {
        assertThatThrownBy(() -> new JwtService(SECRET, ISSUER, Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JwtService(SECRET, ISSUER, Duration.ofMinutes(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Generacion ----------

    @Test
    void generateToken_containsRequiredClaims() {
        Claims claims = service.parseAndValidate(service.generateToken("42"));

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo(ISSUER);
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
    }

    @Test
    void generateToken_expiresAfterConfiguredTtl() {
        Claims claims = service.parseAndValidate(service.generateToken("42"));

        long seconds = (claims.getExpiration().getTime() - claims.getIssuedAt().getTime()) / 1000;

        assertThat(seconds).isEqualTo(TTL.toSeconds());
    }

    @Test
    void generateToken_usesDifferentIdEachTime() {
        String first = service.parseAndValidate(service.generateToken("42")).getId();
        String second = service.parseAndValidate(service.generateToken("42")).getId();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void generateToken_rejectsBlankSubject() {
        assertThatThrownBy(() -> service.generateToken(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.generateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- Validacion ----------

    @Test
    void parseAndValidate_rejectsNullBlankAndGarbageTokens() {
        assertThatThrownBy(() -> service.parseAndValidate(null)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.parseAndValidate("  ")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> service.parseAndValidate("not-a-jwt")).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsTamperedPayload() {
        String token = service.generateToken("42");
        String[] parts = token.split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                ("{\"sub\":\"1\",\"iss\":\"" + ISSUER + "\",\"jti\":\"x\","
                        + "\"iat\":" + Instant.now().getEpochSecond() + ","
                        + "\"exp\":" + Instant.now().plusSeconds(600).getEpochSecond() + "}").getBytes());

        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> service.parseAndValidate(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsTokenSignedWithAnotherKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor(new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
                17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32});
        String token = validBuilder().signWith(otherKey, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsDifferentIssuer() {
        String token = validBuilder().issuer("someone-else").signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsExpiredToken() {
        Instant now = Instant.now();
        String token = validBuilder()
                .issuedAt(Date.from(now.minusSeconds(7200)))
                .expiration(Date.from(now.minusSeconds(3600)))
                .signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsTokenIssuedInTheFuture() {
        Instant now = Instant.now();
        String token = validBuilder()
                .issuedAt(Date.from(now.plusSeconds(3600)))
                .expiration(Date.from(now.plusSeconds(7200)))
                .signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("future");
    }

    @Test
    void parseAndValidate_rejectsTokenWithoutId() {
        String token = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("required");
    }

    @Test
    void parseAndValidate_rejectsTokenWithoutSubject() {
        String token = Jwts.builder()
                .issuer(ISSUER)
                .id("abc")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(KEY, Jwts.SIG.HS256).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void parseAndValidate_rejectsAnyAlgorithmOtherThanHs256() {
        String token = validBuilder().signWith(KEY, Jwts.SIG.HS512).compact();

        assertThatThrownBy(() -> service.parseAndValidate(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("algorithm");
    }

    @Test
    void parseAndValidate_rejectsUnsignedToken() {
        String unsigned = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .id("abc")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .compact();

        assertThatThrownBy(() -> service.parseAndValidate(unsigned)).isInstanceOf(JwtException.class);
    }

    // ---------- isValid ----------

    @Test
    void isValid_isTrueOnlyForTokensTheServiceIssued() {
        assertThat(service.isValid(service.generateToken("42"))).isTrue();
        assertThat(service.isValid("garbage")).isFalse();
        assertThat(service.isValid(null)).isFalse();
    }

    private io.jsonwebtoken.JwtBuilder validBuilder() {
        return Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .id("token-id")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(600)));
    }
}
