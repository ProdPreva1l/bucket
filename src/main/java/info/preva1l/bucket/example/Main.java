package info.preva1l.bucket.example;

import info.preva1l.bucket.DataRepositories;
import org.bson.codecs.configuration.CodecRegistries;

import java.util.UUID;

/**
 * This is a class.
 *
 * @author Preva1l
 * @since 7/08/2025
 */
public class Main {
    public static void main(String[] args) {
        DataRepositories.connect(
                "mongo://127.0.0.1:27017",
                "myUsername",
                "myPassword".toCharArray(),
                "prison-core",
                CodecRegistries.fromCodecs()
        );

        UUID uuid = UUID.randomUUID();

        DataRepositories.getOrCreate(OfflinePlayerData.class, UUID.class)
                .edit(uuid, offlinePlayerData -> {
                    if (offlinePlayerData == null) offlinePlayerData = new OfflinePlayerData(uuid);
                    offlinePlayerData.setLastName("OhWowEditingAValue");
                    return offlinePlayerData;
                });
    }
}
