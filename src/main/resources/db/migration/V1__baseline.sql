--
-- Baseline schema for social-broker-service.
--
-- GENERATED from the JPA entities by Hibernate's schema exporter, not hand-written. The service
-- runs with ddl-auto: validate, so any drift between this file and the entities fails startup —
-- generating it is what guarantees the two agree.
--
-- Regenerate after an entity change (then add a NEW V<n>__ migration; never edit an applied one):
--   mvn -o verify -Dit.test=<AnIT> -DfailIfNoSpecifiedTests=false \
--     -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create \
--     -Dspring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=target/generated-schema.sql
--
-- Existing (pre-Flyway) databases are handled by flyway.baseline-on-migrate=true: they are marked
-- at the baseline version and this migration is skipped, since their tables already exist.
--
create table identity_provider (enabled boolean not null, created_at timestamp(6) with time zone not null, protocol varchar(8) not null check ((protocol in ('OIDC','OAUTH2','SAML'))), updated_at timestamp(6) with time zone not null, id uuid not null, provider_key varchar(32) not null, alias varchar(64) not null, tenant_id varchar(64) not null, user_name_attribute varchar(64), display_name varchar(128) not null, provider_directory varchar(128), client_id varchar(320), authorization_uri varchar(512), issuer_uri varchar(512), jwk_set_uri varchar(512), saml_entity_id varchar(512), saml_metadata_url varchar(512), scopes varchar(512), token_uri varchar(512), user_info_uri varchar(512), client_secret varchar(2048), primary key (id), constraint uq_idp_tenant_alias unique (tenant_id, alias));

