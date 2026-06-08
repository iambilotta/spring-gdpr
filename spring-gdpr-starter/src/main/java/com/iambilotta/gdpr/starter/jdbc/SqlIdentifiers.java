package com.iambilotta.gdpr.starter.jdbc;

/**
 * Whitelist validation for SQL identifiers (table and column names) that the ready-made JDBC SPI
 * base classes interpolate into statements.
 *
 * <p>Bind <em>values</em> with {@code ?} placeholders; identifiers cannot be parameterized in JDBC,
 * so they are validated against {@code [A-Za-z_][A-Za-z0-9_]*} instead. A constructor argument that
 * is not a bare identifier is rejected at construction time, before any SQL is ever built, so a
 * mis-typed or hostile table/column name fails fast and can never reach the database.
 */
public final class SqlIdentifiers {

    private SqlIdentifiers() {
    }

    /**
     * Returns {@code identifier} unchanged if it is a bare SQL identifier; throws otherwise.
     *
     * @param role human-readable name of the argument, used in the error message
     */
    public static String requireIdentifier(String identifier, String role) {
        if (identifier == null || !identifier.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException(
                    role + " must match [A-Za-z_][A-Za-z0-9_]* (got: " + identifier + ")");
        }
        return identifier;
    }
}
