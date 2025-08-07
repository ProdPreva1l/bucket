package info.preva1l.bucket;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * The main class for Bucket.
 * This allows you to control the MongoDB database, including managing connectivity and creating repositories.
 *
 * <p>This class provides a centralized way to manage MongoDB connections and repository instances.
 * It uses a singleton pattern to ensure only one database connection exists at a time and
 * caches repository instances for reuse across the application.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Connect to MongoDB
 * DataRepositories.connect(
 *     "mongodb://localhost:27017",
 *     "username",
 *     "password".toCharArray(),
 *     "MyApp",
 *     CodecRegistries.fromCodecs()
 * );
 *
 * // Get a repository for a User entity
 * Repository<User, String> userRepo = DataRepositories.getOrCreate(User.class, String.class);
 *
 * // Use the repository
 * User user = userRepo.findById("123");
 *
 * // Close connection when done
 * DataRepositories.close();
 * }</pre>
 *
 * @author Preva1l
 * @since 9/07/2025
 * @apiNote This class is thread-safe and designed for use in multithreaded environments.
 * @implNote Uses virtual threads for async operations and atomic references for thread-safe state management.
 */
@SuppressWarnings("unchecked")
public class DataRepositories {
    private static final Logger logger = Logger.getLogger("Bucket");
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private static final AtomicReference<MongoClient> connectionRef = new AtomicReference<>();
    private static final AtomicReference<MongoDatabase> databaseRef = new AtomicReference<>();

    private static final ConcurrentHashMap<Class<?>, Repository<?, ?>> repositoryCache = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     * All methods in this class are static and should be accessed directly.
     *
     * @throws UnsupportedOperationException always, as this class should not be instantiated
     */
    private DataRepositories() {
        throw new UnsupportedOperationException("DataRepositories is a utility class and should not be instantiated");
    }

    /**
     * Establishes a connection to MongoDB with the specified configuration.
     *
     * <p>This method creates a MongoDB client with the provided connection parameters,
     * sets up codec registries for POJO mapping, and performs a ping test to verify connectivity.</p>
     *
     * @param connectionUri the MongoDB connection URI (e.g., "mongodb://localhost:27017")
     * @param username the username for authentication
     * @param password the password for authentication as a char array for security
     * @param connectionName the application name to identify this connection
     * @param codecs custom codec registry for additional type mappings
     *
     * @throws IllegalStateException if already connected to prevent multiple connections
     * @throws com.mongodb.MongoException if connection fails or ping test fails
     *
     * @apiNote This method should only be called once during application initialization.
     * @implNote Uses the "admin" database for authentication and "bucket" database for operations.
     * The method performs a ping command to verify the connection is working.
     */
    public static void connect(String connectionUri, String username, char[] password, String connectionName, CodecRegistry codecs) {
        if (connectionRef.get() != null) {
            logger.info("Already connected to MongoDB.");
            return;
        }

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionUri))
                .credential(MongoCredential.createCredential(username, "admin", password))
                .uuidRepresentation(UuidRepresentation.STANDARD)
                .applicationName(connectionName)
                .codecRegistry(CodecRegistries.fromRegistries(
                        codecs,
                        MongoClientSettings.getDefaultCodecRegistry(),
                        CodecRegistries.fromProviders(
                                PojoCodecProvider.builder()
                                        .conventions(List.of(Conventions.ANNOTATION_CONVENTION))
                                        .automatic(true)
                                        .build())
                ))
                .build();

        MongoClient client = MongoClients.create(settings);
        MongoDatabase database = client.getDatabase("bucket");

        database.runCommand(new Document("ping", 1));

        connectionRef.set(client);
        databaseRef.set(database);
        logger.info("Successfully connected to MongoDB");
    }

    /**
     * Retrieves or creates a repository instance for the specified entity and ID types.
     *
     * <p>This method uses a cache to ensure only one repository instance exists per entity type.
     * If a repository for the given class doesn't exist, it creates a new {@link MongoRepository}
     * instance using the entity's {@link Entity} annotation to determine the collection name.</p>
     *
     * @param <T> the entity type
     * @param <ID> the ID type for the entity
     * @param clazz the entity class, must be annotated with {@link Entity}
     * @param idClazz the ID class (e.g., String.class, Long.class)
     * @return a repository instance for the specified entity type
     *
     * @throws IllegalStateException if MongoDB connection is not established
     * @throws NullPointerException if the entity class is not annotated with {@link Entity}
     *
     * @apiNote The entity class must have an {@link Entity} annotation specifying the collection name.
     * @implNote Repository instances are cached and reused across multiple calls for the same entity type.
     */
    public static <T, ID> Repository<T, ID> getOrCreate(Class<T> clazz, Class<ID> idClazz) {
        return (Repository<T, ID>) repositoryCache.computeIfAbsent(clazz, key -> {
            String collectionName = clazz.getAnnotation(Entity.class).value();
            MongoCollection<T> collection = database().getCollection(collectionName, clazz);
            return new MongoRepository<T, ID>(collection, executor);
        });
    }

    /**
     * Returns the current MongoDB database instance.
     *
     * @return the MongoDB database instance for the "bucket" database
     * @throws IllegalStateException if MongoDB connection is not established
     *
     * @apiNote This method provides direct access to the database for advanced operations.
     * Most users should use repositories instead of direct database access.
     */
    public static MongoDatabase database() {
        MongoDatabase db = databaseRef.get();
        if (db == null) throw new IllegalStateException("MongoDB connection is not established.");
        return db;
    }

    /**
     * Closes the MongoDB connection and cleans up resources.
     *
     * <p>This method should be called during application shutdown to ensure proper
     * resource cleanup. It closes the MongoDB client connection, clears the repository
     * cache, and resets all internal state.</p>
     *
     * @apiNote This method is idempotent - it can be safely called multiple times.
     * @implNote After calling this method, {@link #connect} must be called again before using repositories.
     */
    public static void close() {
        databaseRef.set(null);
        MongoClient client = connectionRef.getAndSet(null);
        if (client != null) {
            logger.info("MongoDB connection closed");
            client.close();
        }
        repositoryCache.clear();
    }
}