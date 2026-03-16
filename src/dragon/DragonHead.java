package dragon;

/** Represents the head of a dragon, including it's size and tooth count. */
public class DragonHead implements java.io.Serializable {
    /**
     * Constructs a new DragonHead with the specified size and tooth count.
     *
     * @param size the size of the dragon's head
     * @param toothCount the number of teeth in the dragon's head
     */
    public DragonHead(float size, float toothCount) {
        this.size = size;
        this.toothCount = toothCount;
    }

    /**
     * Returns the size of the dragon's head.
     *
     * @return the size
     */
    public float getSize() {
        return size;
    }

    /**
     * Returns the number of teeth in the dragon's head.
     *
     * @return the tooth count
     */
    public float getToothCount() {
        return toothCount;
    }

    private float size;
    private float toothCount;
}
