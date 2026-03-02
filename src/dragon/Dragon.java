package dragon;

import core.BadDataException;
import core.Coordinates;

public class Dragon implements Comparable<Dragon> {

    public Dragon(
            String name,
            Coordinates coordinates,
            int age,
            long weight,
            DragonType type,
            DragonHead head) {

        var uuid = java.util.UUID.randomUUID();
        id = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();

        this.name = name;
        this.coordinates = coordinates;
        creationDate = java.time.LocalDateTime.now();
        this.age = age;
        this.weight = weight;
        this.type = type;
        this.head = head;
    }

    public Dragon(String name, Coordinates coordinates, int age, long weight)
            throws BadDataException, NullPointerException {
        this(name, coordinates, age, weight, null, null);
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Coordinates getCoordinates() {
        return coordinates;
    }

    public java.time.LocalDateTime getCreationDate() {
        return creationDate;
    }

    public int getAge() {
        return age;
    }

    public long getWeight() {
        return weight;
    }

    public boolean isSpeaking() {
        return speaking;
    }

    public DragonType getType() {
        return type;
    }

    public DragonHead getHead() {
        return head;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(Dragon other) {
        return id.compareTo(other.id);
    }

    /**
     * Поле не может быть null, Значение поля должно быть больше 0, Значение этого
     * поля должно быть
     * уникальным, Значение этого поля должно генерироваться автоматически
     */
    private final Long id;

    private final String name; // Поле не может быть null, Строка не может быть пустой
    private Coordinates coordinates; // Поле не может быть null
    private final java.time.LocalDateTime creationDate; // Поле не может быть null, Значение этого поля должно
    // генерироваться
    // автоматически
    private int age; // Значение поля должно быть больше 0
    private long weight; // Значение поля должно быть больше 0
    private boolean speaking;
    private final DragonType type; // Поле может быть null
    private final DragonHead head;
}
