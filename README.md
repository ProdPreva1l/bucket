# Bucket is an elegant MongoDB POJO wrapper (ORM)

## Usage
```java
public class Main {
    public static void main(String[] args) {
        DataRepositories.connect(
                "mongo://127.0.0.1:27017",
                "myUsername",
                "myPassword".toCharArray(),
                "my-application-name",
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
```

```java
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
```