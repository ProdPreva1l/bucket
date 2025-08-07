package info.preva1l.bucket;

import java.util.concurrent.CompletableFuture;

/**
 * An object that can be saved into a repository. (Not mandatory)
 *
 * @author Preva1l
 * @since 23/07/2025
 */
public interface Savable {
    /**
     * Save the object to its respective {@link Repository}
     *
     * @return the result of {@link Repository#save(Object)}
     */
    CompletableFuture<Boolean> save();
}