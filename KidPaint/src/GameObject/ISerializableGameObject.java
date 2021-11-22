package GameObject;

import java.io.Serializable;
import java.util.UUID;

public interface ISerializableGameObject extends Serializable {
    String uuid = UUID.randomUUID().toString();
}
