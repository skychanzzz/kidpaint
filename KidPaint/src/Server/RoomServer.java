package Server;

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
        for (int j = 0; j < sketchData.length; j++) {
            for (int i = 0; i < sketchData[j].length; i++) {
                clientOut.writeInt(sketchData[j][i]);               //send sketchpad data
            }
        }

        byte[] buffer = new byte[1024];

        while (true) {
            int mode=clientIn.readInt();

            if(mode==2) {
                int color = clientIn.readInt();
                int size = clientIn.readInt();
                for (int i = 0; i < size; i++) {
                    int len = clientIn.readInt(); //total length of linedList
                    clientIn.read(buffer, 0, len);
                    forward(cSocket, buffer, len, color);
                }
            }else{
                int len = clientIn.readInt();
                clientIn.read(buffer, 0, len);
                String cName = new String(buffer,0,len);
                len = clientIn.readInt();
                clientIn.read(buffer, 0, len);
                forward(cSocket,cName ,buffer, len);
            }
        }

    }


    protected void forward(Socket cSocket,  byte[] data, int len, int color) {
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                try {
                    Socket socket = clients.get(i);
                    if (cSocket.equals(socket)) continue;
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(2);
                    out.writeInt(color);
                    out.writeInt(len);
                    out.write(data, 0, len);
                } catch (IOException e) {
                    // the connection is dropped but the socket is not yet removed.
                }
            }
        }
    }

    protected void forward(Socket cSocket, String cName, byte[] data, int len) {
        synchronized (clients) {
            for (int i = 0; i < clients.size(); i++) {
                try {
                    Socket socket = clients.get(i);
                    if(cSocket.equals(socket)) continue;
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(1);
                    out.writeInt(cName.length());
                    out.write(cName.getBytes(), 0, cName.length());
                    out.writeInt(len);
                    out.write(data, 0, len);
                } catch (IOException e) {
                    // the connection is dropped but the socket is not yet removed.
                }
            }
        }
    }

    //Getter
    protected int getPort() { return this.srvSocket.getLocalPort(); }
}
