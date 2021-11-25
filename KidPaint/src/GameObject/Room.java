package GameObject;

public class Room implements ISerializableGameObject{
    public String name;
    public int port;
    public int sizeX;
    public int sizeY;

    public Room(String name, int port, int sizeX, int sizeY) {
        this.name = name;
        this.port = port;
        this.sizeX = sizeX;
        this.sizeY = sizeY;
    }
}
