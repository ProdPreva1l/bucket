package info.preva1l.bucket.repository;

import com.mongodb.Function;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * A simple interface representing a Mongo repository.
 * Provides useful functions (CRUD)
 * <p>
 * You can create an instance of {@link Repository } by using the {@link info.preva1l.bucket.DataRepositories#getOrCreate(Class, Class) } function.
 *
 * @author Preva1l
 * @since 9/07/2025
 */
public interface Repository<T, ID> {
    /**
     * Inserts the given entity into the database, performing an update if the entity already exists,
     * or inserting it as a new document otherwise. The field annotated with {@link org.bson.codecs.pojo.annotations.BsonId} is used as
     * the identifier for the operation.
     *
     * @param entity The entity to be saved in the database. Must have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @return `true` if the operation results in an update or an insertion, otherwise `false`.
     * @throws info.preva1l.bucket.exception.DatabaseException If the provided entity does not have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     */
    CompletableFuture<Boolean> save(@NotNull T entity);

    /**
     * Finds and returns an entity of type {@link T} by its identifier.
     *
     * @param id The UUID identifier of the entity to retrieve.
     * @return The entity of type {@link T} if found, or `null` if no matching entity exists.
     */
    CompletableFuture<@Nullable T> findById(@NotNull ID id);

    CompletableFuture<Boolean> edit(@NotNull ID id, Function<@Nullable T, @NotNull T> block);

    /**
     * Retrieves all entities of type {@link T} from the database.
     *
     * @return A list containing all entities of type {@link T} stored in the database.
     */
    CompletableFuture<List<T>> findAll();

    /**
     * Deletes a document from the database given its unique identifier.
     *
     * @param id The unique {@link ID} identifier of the document to be deleted.
     * @return `true` if a document was successfully deleted, `false` otherwise.
     */
    CompletableFuture<Boolean> deleteById(@NotNull ID id);

    /**
     * Deletes the given entity from the database.
     * The entity must have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId},
     * and the value of that field is used to identify the document to be deleted.
     *
     * @param entity The entity to be deleted. Must include a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @return `true` if the entity was successfully deleted, `false` otherwise.
     * @throws info.preva1l.bucket.exception.DatabaseException If the provided entity does not have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     */
    CompletableFuture<Boolean> delete(@NotNull T entity);

    /**
     * Checks if a document with the given identifier exists in the database.
     *
     * @param id The unique {@link ID} identifier of the document to check for existence.
     * @return `true` if a document with the specified identifier exists, `false` otherwise.
     */
    CompletableFuture<Boolean> exists(@NotNull ID id);

    /**
     * Counts the total number of documents present in the associated database collection.
     *
     * @return The total count of documents as a long.
     */
    CompletableFuture<Long> count();

    <R> CompletableFuture<@Nullable R> withCollection(Function<MongoCollection<T>, @Nullable R> operation);
    <R> CompletableFuture<List<R>> findWith(Function<MongoCollection<T>, FindIterable<R>> operation);
}
