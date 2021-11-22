package GameObject;

public class Sketchpad implements ISerializableGameObject{
    public int[][] sketchData;

    public Sketchpad(int[][] data) {
        this.sketchData = data;
    }
}
