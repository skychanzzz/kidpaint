package util;

import GameObject.ISerializableGameObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class JavaNetwork {
    public static ISerializableGameObject readServerGO(DataInputStream tcpIn) throws IOException, ClassNotFoundException {
        int len = 0;
        len = tcpIn.readInt();
        byte[] objByte = new byte[len];
        tcpIn.read(objByte, 0, len);
        return (ISerializableGameObject) ByteArrayParser.byte2Object(objByte);
    }

    public static void writeServerGO(DataOutputStream tcpOut, ISerializableGameObject serializableGO) {
        byte[] GOBytes = new byte[0];
        try {
            GOBytes = ByteArrayParser.object2Byte(serializableGO);
            tcpOut.writeInt(GOBytes.length);
            tcpOut.write(GOBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
