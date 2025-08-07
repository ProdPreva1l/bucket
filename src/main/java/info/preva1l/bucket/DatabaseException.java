package info.preva1l.bucket;

/**
 * Thrown when a database-related error is detected.
 * Currently only thrown when invalid parameters are supplied to
 * {@link info.preva1l.bucket.DataRepositories}'s save/delete functions.
 *
 * @author Preva1l
 * @since 9/07/2025
 */
class DatabaseException extends RuntimeException {
    /**
     * Create a new DatabaseException.
     *
     * @param message the reason for the error.
     */
    DatabaseException(String message) {
        super(message);
    }
}