package info.preva1l.bucket;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * A simple interface representing a MongoDB repository.
 * Provides useful functions for CRUD operations on MongoDB collections.
 *
 * <p>This interface provides a high-level abstraction over MongoDB operations,
 * offering asynchronous methods for common database operations like saving,
 * finding, updating, and deleting entities. All operations return {@link CompletableFuture}
 * instances for non-blocking execution.</p>
 *
 * <p>Entities used with this repository must have a field annotated with
 * {@link org.bson.codecs.pojo.annotations.BsonId} to serve as the document identifier.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * Repository<User, String> userRepo = DataRepositories.getOrCreate(User.class, String.class);
 *
 * // Save a user
 * User user = new User("123", "John Doe");
 * userRepo.save(user).thenAccept(success -> {
 *     if (success) {
 *         System.out.println("User saved successfully");
 *     }
 * });
 *
 * // Find a user
 * userRepo.findById("123").thenAccept(foundUser -> {
 *     if (foundUser != null) {
 *         System.out.println("Found: " + foundUser.getName());
 *     }
 * });
 * }</pre>
 *
 * <p>You can create an instance of {@link Repository} by using the
 * {@link info.preva1l.bucket.DataRepositories#getOrCreate(Class, Class)} function.</p>
 *
 * @param <T> the entity type this repository manages
 * @param <ID> the type of the entity's identifier
 * @author Preva1l
 * @since 9/07/2025
 * @apiNote All operations are asynchronous and return CompletableFuture instances.
 * @implNote Implementations should ensure thread-safety for concurrent operations.
 */
@ApiStatus.NonExtendable
public interface Repository<T, ID> {
    /**
     * Inserts the given entity into the database, performing an update if the entity already exists,
     * or inserting it as a new document otherwise. The field annotated with {@link org.bson.codecs.pojo.annotations.BsonId} is used as
     * the identifier for the operation.
     *
     * <p>This is an upsert operation - if a document with the same ID exists, it will be updated;
     * otherwise, a new document will be inserted.</p>
     *
     * @param entity The entity to be saved in the database. Must have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @return a {@link CompletableFuture} that completes with {@code true} if the operation results in an update or an insertion, otherwise {@code false}.
     * @throws DatabaseException If the provided entity does not have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @apiNote This method performs an upsert operation for convenience.
     * @implNote The BsonId field value determines whether this is an insert or update operation.
     */
    CompletableFuture<Boolean> save(@NotNull T entity);

    /**
     * Finds and returns an entity of type {@link T} by its identifier.
     *
     * <p>This method performs a lookup in the database collection using the provided ID
     * as the search criterion for the document's {@code _id} field.</p>
     *
     * @param id The identifier of the entity to retrieve. Must not be null.
     * @return a {@link CompletableFuture} that completes with the entity of type {@link T} if found, or {@code null} if no matching entity exists.
     * @apiNote Returns null when no document is found rather than throwing an exception.
     */
    CompletableFuture<@Nullable T> findById(@NotNull ID id);

    /**
     * Edits an existing entity by applying a transformation function to it.
     *
     * <p>This method retrieves the entity with the specified ID, applies the provided
     * transformation function to it, and saves the result back to the database.
     * The function receives the current entity (or null if not found) and must return
     * a non-null entity to be saved.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * repository.edit(userId, user -> {
     *     if (user != null) {
     *         user.setLastLoginTime(Instant.now());
     *         return user;
     *     }
     *     return new User(userId, "New User"); // Create if not found
     * });
     * }</pre>
     *
     * @param id The identifier of the entity to edit. Must not be null.
     * @param block A function that takes the current entity (possibly null) and returns the updated entity. Must not return null.
     * @return a {@link CompletableFuture} that completes with {@code true} if the edit operation was successful, {@code false} otherwise.
     * @apiNote The transformation function must handle the case where the entity doesn't exist (null input).
     * @implNote This operation is atomic - the find and update are performed as a single operation where possible.
     */
    CompletableFuture<Boolean> edit(@NotNull ID id, Function<@Nullable T, @NotNull T> block);

    /**
     * Retrieves all entities of type {@link T} from the database.
     *
     * <p>This method returns all documents in the collection as a list of entities.
     * Use with caution on large collections as it loads all documents into memory.</p>
     *
     * @return a {@link CompletableFuture} that completes with a list containing all entities of type {@link T} stored in the database.
     * @apiNote This method loads all documents into memory - consider using pagination for large datasets.
     * @implNote The returned list is mutable and safe to modify without affecting the database.
     */
    CompletableFuture<List<T>> findAll();

    /**
     * Deletes a document from the database given its unique identifier.
     *
     * <p>This method removes the document with the specified ID from the collection.
     * If no document with the given ID exists, the operation completes successfully
     * but returns {@code false}.</p>
     *
     * @param id The unique identifier of the document to be deleted. Must not be null.
     * @return a {@link CompletableFuture} that completes with {@code true} if a document was successfully deleted, {@code false} otherwise.
     * @apiNote Returns false if no document with the specified ID exists, rather than throwing an exception.
     */
    CompletableFuture<Boolean> deleteById(@NotNull ID id);

    /**
     * Deletes the given entity from the database.
     * The entity must have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId},
     * and the value of that field is used to identify the document to be deleted.
     *
     * <p>This method extracts the ID from the entity's BsonId-annotated field
     * and uses it to delete the corresponding document from the collection.</p>
     *
     * @param entity The entity to be deleted. Must include a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @return a {@link CompletableFuture} that completes with {@code true} if the entity was successfully deleted, {@code false} otherwise.
     * @throws DatabaseException If the provided entity does not have a field annotated with {@link org.bson.codecs.pojo.annotations.BsonId}.
     * @apiNote This method is equivalent to calling {@link #deleteById(Object)} with the entity's ID.
     */
    CompletableFuture<Boolean> delete(@NotNull T entity);

    /**
     * Checks if a document with the given identifier exists in the database.
     *
     * <p>This method performs an existence check without retrieving the actual document,
     * making it more efficient than {@link #findById(Object)} when you only need to
     * verify existence.</p>
     *
     * @param id The unique identifier of the document to check for existence. Must not be null.
     * @return a {@link CompletableFuture} that completes with {@code true} if a document with the specified identifier exists, {@code false} otherwise.
     * @apiNote This method is more efficient than findById when you only need to check existence.
     * @implNote Implementations should use efficient existence checks rather than full document retrieval.
     */
    CompletableFuture<Boolean> exists(@NotNull ID id);

    /**
     * Counts the total number of documents present in the associated database collection.
     *
     * <p>This method returns the total count of all documents in the collection,
     * regardless of their content or state.</p>
     *
     * @return a {@link CompletableFuture} that completes with the total count of documents as a long.
     * @apiNote The count reflects the total number of documents at the time the operation executes.
     * @implNote Uses MongoDB's efficient count operation rather than retrieving and counting documents.
     */
    CompletableFuture<Long> count();

    /**
     * Executes a custom operation on the underlying MongoDB collection.
     *
     * <p>This method provides direct access to the MongoDB collection for advanced
     * operations that are not covered by the standard repository methods. The operation
     * function receives the raw {@link MongoCollection} and can perform any valid
     * MongoDB operation on it.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * repository.withCollection(collection -> {
     *     return collection.aggregate(Arrays.asList(
     *         Aggregates.match(Filters.eq("status", "active")),
     *         Aggregates.group("$department", Accumulators.sum("count", 1))
     *     )).first();
     * });
     * }</pre>
     *
     * @param <R> the return type of the operation
     * @param operation A function that takes the MongoDB collection and returns a result. May return null.
     * @return a {@link CompletableFuture} that completes with the result of the operation, or {@code null} if the operation returns null.
     * @apiNote Use this method for advanced MongoDB operations not covered by standard repository methods.
     * @implNote The operation is executed asynchronously but the function itself runs synchronously with the collection.
     */
    <R> CompletableFuture<@Nullable R> withCollection(Function<MongoCollection<T>, @Nullable R> operation);

    /**
     * Executes a custom find operation on the underlying MongoDB collection and returns a list of results.
     *
     * <p>This method allows for complex queries and projections by providing direct access
     * to MongoDB's {@link FindIterable}. The operation function should return a configured
     * FindIterable that will be converted to a list.</p>
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * // Find users with custom projection
     * repository.findWith(collection ->
     *     collection.find(Filters.eq("status", "active"))
     *              .projection(Projections.include("name", "email"))
     *              .limit(10)
     * );
     *
     * // Complex aggregation returning custom types
     * repository.findWith(collection ->
     *     collection.aggregate(pipeline, CustomResult.class)
     * );
     * }</pre>
     *
     * @param <R> the type of objects in the result list
     * @param operation A function that takes the MongoDB collection and returns a FindIterable of type R.
     * @return a {@link CompletableFuture} that completes with a list containing all results from the FindIterable.
     * @apiNote This method is ideal for complex queries, aggregations, and custom projections.
     * @implNote The FindIterable is fully consumed and converted to a list before the CompletableFuture completes.
     */
    <R> CompletableFuture<List<R>> findWith(Function<MongoCollection<T>, FindIterable<R>> operation);
}