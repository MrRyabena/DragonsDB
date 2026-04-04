package dragon;

import core.Coordinates;

/**
 * Represents a dragon entity with various attributes such as name, coordinates, age, etc.
 * Implements Comparable for sorting by ID and Serializable for persistence.
 */
public class Dragon implements Comparable<Dragon>, java.io.Serializable {

    /**
     * Constructs a new Dragon with all attributes specified.
     *
     * @param name the name of the dragon (cannot be null or empty)
     * @param coordinates the coordinates of the dragon (cannot be null)
     * @param age the age of the dragon (must be greater than 0)
     * @param weight the weight of the dragon (must be greater than 0)
     * @param type the type of the dragon (can be null)
     * @param head the head of the dragon (cannot be null)
     */
    public Dragon(
            String name,
            Coordinates coordinates,
            int age,
            long weight,
            DragonType type,
            DragonHead head) {

        // Creatable members:

        var uuid = java.util.UUID.randomUUID();
        id = uuid.getMostSignificantBits() ^ uuid.getLeastSignificantBits();
        creationDate = java.time.LocalDateTime.now();

        // Settable and checkable members:

        if (name == null) {
            throw new NullPointerException("Name cannot be null");
        }
        this.name = name;

        if (coordinates == null) {
            throw new NullPointerException("Coordinates cannot be null");
        }
        this.coordinates = coordinates;

        if (age <= 0) {
            throw new IllegalArgumentException("Age must be greater than 0");
        }
        this.age = age;

        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be greater than 0");
        }
        this.weight = weight;

        if (head == null) {
            throw new NullPointerException("Head cannot be null");
        }
        this.head = head;

        // Settable and not checkable members:

        this.type = type;
    }

    /**
     * Constructs a new Dragon with basic attributes, type and head set to null.
     *
     * @param name the name of the dragon (cannot be null or empty)
     * @param coordinates the coordinates of the dragon (cannot be null)
     * @param age the age of the dragon (must be greater than 0)
     * @param weight the weight of the dragon (must be greater than 0)
     * @throws IllegalArgumentException if data validation fails
     * @throws NullPointerException if required parameters are null
     */
    public Dragon(String name, Coordinates coordinates, int age, long weight)
            throws IllegalArgumentException, NullPointerException {
        this(name, coordinates, age, weight, null, null);
    }

    /**
     * Returns the unique identifier of the dragon.
     *
     * @return the ID
     */
    public long getId() {
        return id;
    }

    /**
     * Returns the name of the dragon.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the coordinates of the dragon.
     *
     * @return the coordinates
     */
    public Coordinates getCoordinates() {
        return coordinates;
    }

    /**
     * Returns the creation date of the dragon.
     *
     * @return the creation date
     */
    public java.time.LocalDateTime getCreationDate() {
        return creationDate;
    }

    /**
     * Returns the age of the dragon.
     *
     * @return the age
     */
    public int getAge() {
        return age;
    }

    /**
     * Returns the weight of the dragon.
     *
     * @return the weight
     */
    public long getWeight() {
        return weight;
    }

    /**
     * Returns whether the dragon is speaking.
     *
     * @return true if the dragon is speaking, false otherwise
     */
    public boolean isSpeaking() {
        return speaking;
    }

    /**
     * Returns the type of the dragon.
     *
     * @return the dragon type
     */
    public DragonType getType() {
        return type;
    }

    /**
     * Returns the head of the dragon.
     *
     * @return the dragon head
     */
    public DragonHead getHead() {
        return head;
    }

    /**
     * Returns the hash code based on the dragon's ID.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return id.hashCode();
    }

    /**
     * Returns true if equals object's classes and compareTo() returns 0.
     *
     * @param obj the other obj
     * @return true if objects equals
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;
        return compareTo((Dragon) obj) == 0;
    }

    /**
     * Compares this dragon to another based on their IDs.
     *
     * @param other the other dragon to compare to
     * @return a negative integer, zero, or a positive integer as this dragon's ID is less than,
     *     equal to, or greater than the specified dragon's ID
     */
    @Override
    public int compareTo(Dragon other) {
        return id.compareTo(other.id);
    }

    /**
     * The unique identifier of the dragon. Cannot be null, must be greater than 0, must be unique,
     * and is generated automatically.
     */
    private final Long id;

    /** The name of the dragon. Cannot be null, string cannot be empty. */
    private final String name;

    /** The coordinates of the dragon. Cannot be null. */
    private Coordinates coordinates;

    /** The creation date of the dragon. Cannot be null, generated automatically. */
    private final java.time.LocalDateTime creationDate;

    /** The age of the dragon. Must be greater than 0. */
    private int age;

    /** The weight of the dragon. Must be greater than 0. */
    private long weight;

    /** Indicates whether the dragon is speaking. */
    private boolean speaking;

    /** The type of the dragon. Can be null. */
    private final DragonType type;

    /** The head of the dragon. */
    private final DragonHead head;
}
