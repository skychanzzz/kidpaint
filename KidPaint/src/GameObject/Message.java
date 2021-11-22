package GameObject;

public class Message implements ISerializableGameObject{
    public String name;
    public String message;

    public Message(String name, String message) {
        this.name = name;
        this.message = message;
    }
}
