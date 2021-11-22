package Client;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class Client {
    private static Client instance;

    DatagramSocket udpSocket;

    //Singleton
    public static Client getInstance() {
        if(instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Client() {
        try {
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.printf("udpSocket error on client init process:\n " + e.getStackTrace());
        }
    }

    public void sendMsg(String str, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
        udpSocket.send(packet);
    }

    public String receiveRoomAddress() {
        try {
            String serverAddr = "";

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            udpSocket.receive(packet);
            byte[] data = packet.getData();
            String str = new String(data, 0, packet.getLength());       // receive the ACK from Server.Server by UDP
            if (str.equals("ACK")) {
                for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                    serverAddr += packet.getAddress().toString().charAt(i);         // to get the IP address of server
                }
                udpSocket.close();         //UDP close
                return serverAddr;
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }
}
