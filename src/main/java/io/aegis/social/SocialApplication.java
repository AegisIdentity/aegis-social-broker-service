package io.aegis.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Inbound federation: social login + corporate SAML/OIDC brokering.
 *
 * <p>Maturity: scaffold. This is a buildable, secured resource-server skeleton (health + a
 * protected info endpoint + the shared hardening baseline) ready for feature work. See
 * aegis-platform-docs/architecture/SERVICE-CATALOG.md for the intended contract. */
@SpringBootApplication
public class SocialApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
