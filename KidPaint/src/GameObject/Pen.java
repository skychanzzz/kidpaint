package GameObject;

import java.awt.*;
import java.io.IOException;
import java.util.LinkedList;

public class Pen implements ISerializableGameObject {
    public int color;
    public LinkedList<Point> points;

    public Pen(int color, LinkedList<Point> points) throws IOException {
        this.color = color;
        this.points = points;
    }
}
