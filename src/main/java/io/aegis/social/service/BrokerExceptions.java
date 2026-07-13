package io.aegis.social.service;

/** Domain exceptions for the identity-provider registry. */
public final class BrokerExceptions {

    private BrokerExceptions() {
    }

    /** An alias already exists within the tenant. */
    public static class DuplicateProviderException extends RuntimeException {
        public DuplicateProviderException(String message) {
            super(message);
        }
    }

    /** No such provider in the tenant. */
    public static class ProviderNotFoundException extends RuntimeException {
        public ProviderNotFoundException(String message) {
            super(message);
        }
    }

    /** A required field for the chosen provider preset is missing or invalid. */
    public static class ProviderValidationException extends RuntimeException {
        public ProviderValidationException(String message) {
            super(message);
        }
    }
}
