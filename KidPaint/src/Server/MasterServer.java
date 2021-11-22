package Server;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class MasterServer {
    DatagramSocket socket;

    //Add
    RoomServer room;
    //

    ServerSocket srvSocket;
    ArrayList<Socket> list = new ArrayList<>();

    public MasterServer(int port) {
        try {
            socket = new DatagramSocket(port);

            room = new RoomServer();

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
            sendMsg("ACK", srcAddr, packet.getPort());
            room.start();
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public void sendMsg(String str, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
        socket.send(packet);
    }

    public static void main(String[] args) {
        MasterServer server = new MasterServer(12345);
    }
}
