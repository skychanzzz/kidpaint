package Server;

import GameObject.Sketchpad;
import util.ByteArrayParser;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class RoomServer {
    protected ServerSocket srvSocket;
    protected int[][] sketchData;
    protected ArrayList<Socket> clients;

    protected RoomServer() throws IOException {
        this.srvSocket = new ServerSocket(45678);
        clients = new ArrayList<>();
        sketchData = new int[50][50];
        sketchData[0][0] = -543230;
        sketchData[1][1] = -543230;
    }

    protected void start() {
        try {
            while (true) {
                System.out.println("Listening at port 45678...");
                Socket cSocket = srvSocket.accept();
                synchronized (clients) {
                    clients.add(cSocket);
                    System.out.printf("Total %d clients are connected.\n", clients.size());
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(cSocket);
                    } catch (IOException e) {
                        System.err.println("connection dropped.");
                    }
                    synchronized (clients) {
                        clients.remove(cSocket);
                    }
                });
                t.start();
            }
        } catch (IOException e) {
            System.err.println("connection dropped.");
        }
    }


    protected void serve(Socket cSocket) throws IOException {
        System.out.printf("Established a connection to host %s:%d\n\n",
                cSocket.getInetAddress(), cSocket.getPort());
        DataInputStream clientIn = new DataInputStream(cSocket.getInputStream());
        DataOutputStream clientOut = new DataOutputStream(cSocket.getOutputStream());

        Sketchpad pad = new Sketchpad(sketchData);
        byte[] padByte = ByteArrayParser.object2Byte(pad);
        forwardObj2Self(clientOut, padByte, padByte.length);

        while (true) {
            int objSize = clientIn.readInt();
            byte[] objByte = new byte[objSize];
            clientIn.read(objByte, 0, objSize);
            forwardObj2All(cSocket, objByte, objSize);
        }
    }

    protected void forwardObj2All(Socket cSocket, byte[] data, int len) {
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                try {
                    Socket socket = clients.get(i);
                    if (cSocket.equals(socket)) continue;
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(len);
                    out.write(data, 0, len);
                } catch (IOException e) {
                    // the connection is dropped but the socket is not yet removed.
                }
            }
        }
    }

    protected void forwardObj2Self(DataOutputStream out, byte[] data, int len) {
        try {
            out.writeInt(len);
            out.write(data, 0, len);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Getter
    protected int getPort() {
        return this.srvSocket.getLocalPort();
    }
}
