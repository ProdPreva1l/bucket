package info.preva1l.bucket.example;

import info.preva1l.bucket.annotations.Entity;
import org.bson.codecs.pojo.annotations.BsonCreator;
import org.bson.codecs.pojo.annotations.BsonId;

import java.util.UUID;

@Entity("offline_player")
public class OfflinePlayerData {
    @BsonId
    private final UUID identifier;

    private String lastName;

    @BsonCreator
    public OfflinePlayerData(UUID identifier) {
        this.identifier = identifier;
        this.lastName = "Unknown";
    }

    public UUID getIdentifier() {
        return identifier;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }
}
