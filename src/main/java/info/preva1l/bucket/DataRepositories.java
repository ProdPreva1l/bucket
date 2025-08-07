package info.preva1l.bucket;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import info.preva1l.bucket.annotations.Entity;
import info.preva1l.bucket.repository.MongoRepository;
import info.preva1l.bucket.repository.Repository;
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
 * @author Preva1l
 * @since 9/07/2025
 */
@SuppressWarnings("unchecked")
public class DataRepositories {
    private static final Logger logger = Logger.getLogger("Bucket");
    private static final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private static final AtomicReference<MongoClient> connectionRef = new AtomicReference<>();
    private static final AtomicReference<MongoDatabase> databaseRef = new AtomicReference<>();
    public static final ConcurrentHashMap<Class<?>, Repository<?, ?>> repositoryCache = new ConcurrentHashMap<>();

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

    public static <T, ID> Repository<T, ID> getOrCreate(Class<T> clazz, Class<ID> idClazz) {
        return (Repository<T, ID>) repositoryCache.computeIfAbsent(clazz, key -> {
            String collectionName = clazz.getAnnotation(Entity.class).value();
            MongoCollection<T> collection = database().getCollection(collectionName, clazz);
            return new MongoRepository<T, ID>(collection, executor);
        });
    }

    public static MongoDatabase database() {
        MongoDatabase db = databaseRef.get();
        if (db == null) throw new IllegalStateException("MongoDB connection is not established.");
        return db;
    }

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
