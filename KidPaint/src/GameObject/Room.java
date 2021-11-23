package GameObject;

public class Room implements ISerializableGameObject{
    public String name;
    public int port;

    public Room(String name, int port) {
        this.name = name;
        this.port = port;
    }
}
