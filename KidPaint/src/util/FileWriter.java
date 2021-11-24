package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileWriter {
    public static void writeFile(String filename, byte[] objByte) throws IOException {
        File file = new File(filename);
        FileOutputStream out = new FileOutputStream(file);
        out.write(objByte);
        out.close();
    }

    public static byte[] readFile(String filename) throws IOException {
        byte[] buffer = new byte[1024];
        File file = new File(filename);
        FileInputStream in = new FileInputStream(file);
        long size = file.length();
        byte[] objByte = new byte[(int) size];
        in.read(objByte);
        return objByte;
    }
}
