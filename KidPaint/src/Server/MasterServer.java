package Server;

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;

public class MasterServer {
    DatagramSocket socket;
    ArrayList<RoomServer> rooms;
    int tcpPort = 11111;

    public MasterServer(int port) {
        try {
            socket = new DatagramSocket(port);
            rooms = new ArrayList<>();
            RoomServer room1 = new RoomServer("Room1");
            RoomServer room2 = new RoomServer("Room2");
            RoomServer room3 = new RoomServer("Room3");
            rooms.add(room1);
            rooms.add(room2);
            rooms.add(room3);

            while (true) {
                System.out.println("wait");
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                Thread t = new Thread(() -> receive(packet));
                t.start();
            }
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public void receive(DatagramPacket packet) {     //for UDP
        try {
            String srcAddr = "";
            for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                srcAddr += packet.getAddress().toString().charAt(i);
            }

            ServerSocket srvSocket = new ServerSocket(tcpPort);

            Thread t = new Thread(() -> {
                try {
                    System.out.println("Wait client to connect to master tcp...");
                    Socket cSocket = srvSocket.accept();
                    System.out.println("oisc" + cSocket.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();

            sendMsg(String.valueOf(srvSocket.getLocalPort()).getBytes(), srcAddr, packet.getPort());
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public void sendMsg(byte[] msg, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(msg, msg.length, destination, port);
        socket.send(packet);
    }

    public static void main(String[] args) {
        MasterServer server = new MasterServer(12345);
    }
}
