package info.preva1l.bucket.exception;

/**
 * Thrown when a database-related error is detected.
 * Currently only thrown when invalid parameters are supplied to
 * {@link info.preva1l.bucket.DataRepositories}'s save/delete functions.
 *
 * @author Preva1l
 * @since 9/07/2025
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }
}