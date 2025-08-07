package info.preva1l.bucket;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.codecs.pojo.annotations.BsonId;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.StreamSupport;

/**
 * Represents a singular {@link Dao}, providing utility methods for CRUD operations.
 *
 * @author Preva1l
 * @since 9/07/2025
 * @param <T> the type of entity to get stored.
 * @param <ID> the type of ID the entity is stored with.
 */
@ApiStatus.Internal
class GenericMongoDao<T, ID> implements Dao<T, ID> {
    private final MongoCollection<T> collection;
    private final Executor executor;

    GenericMongoDao(MongoCollection<T> collection, Executor executor) {
        this.collection = collection;
        this.executor = executor;
    }

    public CompletableFuture<Boolean> save(@NotNull T entity) {
        return CompletableFuture.supplyAsync(() -> {
            ID id = getId(entity);
            if (id == null)
                throw new DatabaseException("no field annotated with @Identifier found on " + entity.getClass().getName());

            UpdateResult result = collection.replaceOne(
                    Filters.eq("_id", id.toString()),
                    entity,
                    new ReplaceOptions().upsert(true)
            );

            return result.getModifiedCount() > 0 || result.getUpsertedId() != null;
        }, executor);
    }

    public CompletableFuture<@Nullable T> findById(@NotNull ID id) {
        return CompletableFuture.supplyAsync(() -> collection.find(Filters.eq("_id", id.toString())).first(), executor);
    }

    public CompletableFuture<Boolean> edit(@NotNull ID id, Function<@Nullable T, @NotNull T> block) {
        return findById(id).thenComposeAsync(it -> save(block.apply(it)), executor);
    }

    public CompletableFuture<List<T>> findAll() {
        return CompletableFuture.supplyAsync(() -> StreamSupport.stream(collection.find().spliterator(), false).toList(), executor);
    }

    public CompletableFuture<Boolean> deleteById(@NotNull ID id) {
        return CompletableFuture.supplyAsync(() -> {
            DeleteResult result = collection.deleteOne(Filters.eq("_id", id.toString()));
            return result.getDeletedCount() > 0;
        }, executor);
    }

    public CompletableFuture<Boolean> delete(@NotNull T entity) {
        ID id = getId(entity);
        if (id == null)
            throw new DatabaseException("no field annotated with @Identifier found on " + entity.getClass().getName());
        return deleteById(id);
    }

    public CompletableFuture<Boolean> exists(@NotNull ID id) {
        return CompletableFuture.supplyAsync(() -> collection.countDocuments(Filters.eq("_id", id.toString())) > 0, executor);
    }

    public CompletableFuture<Long> count() {
        return CompletableFuture.supplyAsync(collection::countDocuments, executor);
    }

    public <R> CompletableFuture<@Nullable R> withCollection(Function<MongoCollection<T>, @Nullable R> operation) {
        return CompletableFuture.supplyAsync(() -> operation.apply(collection), executor);
    }

    public <R> CompletableFuture<List<R>> findWith(Function<MongoCollection<T>, FindIterable<R>> operation) {
        return CompletableFuture.supplyAsync(() -> StreamSupport.stream(operation.apply(collection).spliterator(), false).toList());
    }

    private <I> I getId(T instance) {
        if (instance == null) return null;

        Field targetField = null;
        for (Field field : instance.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(BsonId.class)) {
                targetField = field;
                break;
            }
        }

        if (targetField == null) return null;

        boolean wasAccessible = targetField.canAccess(instance);
        try {
            targetField.setAccessible(true);
            // noinspection unchecked
            return (I) targetField.get(instance);
        } catch (Exception e) {
            return null;
        } finally {
            targetField.setAccessible(wasAccessible);
        }
    }
}