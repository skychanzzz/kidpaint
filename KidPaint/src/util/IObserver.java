package util;

import GameObject.ISerializableGameObject;

public interface IObserver {
    void updateGameObject(ISerializableGameObject GO);
}
