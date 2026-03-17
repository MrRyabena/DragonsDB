package core;

/**
 * 2D coordinates value object with validation constraints.
 */
public class Coordinates {

    /**
     * Constructs coordinates with validation.
     *
     * @param x x coordinate (must be greater than -359, cannot be null)
     * @param y y coordinate (must be less than or equal to 603, cannot be null)
     * @throws BadDataException if constraints are violated
     * @throws NullPointerException if x or y is null
     */
    public Coordinates(Float x, Float y) throws BadDataException, NullPointerException {
        setXY(x, y);
    }

    /**
     * Returns x coordinate.
     *
     * @return x coordinate
     */
    public Float getX() {
        return x;
    }

    /**
     * Returns y coordinate.
     *
     * @return y coordinate
     */
    public Float getY() {
        return y;
    }

    /**
     * Sets both coordinates with validation.
     *
     * @param x x coordinate (must be greater than -359, cannot be null)
     * @param y y coordinate (must be less than or equal to 603, cannot be null)
     * @throws BadDataException if constraints are violated
     * @throws NullPointerException if x or y is null
     */
    public final void setXY(Float x, Float y) throws BadDataException, NullPointerException {
        if (x == null || y == null) throw new NullPointerException();
        if (x <= -359) throw new BadDataException("Value 'x' must be greater than -359");
        if (y > 603) throw new BadDataException("Value 'y' must be less than 603");
        this.x = x;
        this.y = y;
    }

    /** X coordinate. Must be greater than -359, cannot be null. */
    private Float x;

    /** Y coordinate. Maximum value is 603, cannot be null. */
    private Float y;
}
