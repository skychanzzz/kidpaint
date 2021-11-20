package util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ByteArrayParser {
    public static byte[] object2Byte(Object object) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.flush();
        return baos.toByteArray();
    }
    public static Object byte2Object(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        ObjectInputStream ois = new ObjectInputStream(bais);
        return ois.readObject();
    }
    public static byte[] list2Byte(List list) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(0);
        DataOutputStream baosOut = new DataOutputStream(baos);

        for(Object el: list) {
            byte[] data = ByteArrayParser.object2Byte(el);
            baosOut.writeInt(data.length);
            baosOut.write(data, 0, data.length);
        }
        baosOut.flush();
        byte[] data = baos.toByteArray();
        return data;
    }

    public static List byte2List(byte[] data) throws IOException {
        List<Object> objectList = new ArrayList<>();

        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream in = new DataInputStream(bais);
        while (in.available() > 0) {
            int len = in.readInt();
            byte[] buffer = new byte[len];
            in.read(buffer, 0, len);
            Object obj = null;
            try {
                obj = byte2Object(buffer);
            } catch (ClassNotFoundException e) {
                System.out.println("Parser cannot parse to object");
            }
            objectList.add(obj);
        }
        return objectList;
    }
}
