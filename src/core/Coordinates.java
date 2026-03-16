package core;

public class Coordinates {

    public Coordinates(Float x, Float y) throws BadDataException, NullPointerException {
        setXY(x, y);
    }

    public Float getX() {
        return x;
    }

    public Float getY() {
        return y;
    }

    public final void setXY(Float x, Float y) throws BadDataException, NullPointerException {
        if (x == null || y == null) throw new NullPointerException();
        if (x <= -359) throw new BadDataException("Value 'x' must be greater than -359");
        if (y > 603) throw new BadDataException("Value 'y' must be less than 603");
        this.x = x;
        this.y = y;
    }

    private Float x; // Значение поля должно быть больше -359, Поле не может быть null
    private Float y; // Максимальное значение поля: 603, Поле не может быть null
}
