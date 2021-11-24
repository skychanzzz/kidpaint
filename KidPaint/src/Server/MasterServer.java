package Server;

import GameObject.Room;
import util.JavaNetwork;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class MasterServer {
    DatagramSocket udpSocket;
    ServerSocket srvSocket;
    ArrayList<RoomServer> rooms;
    int tcpPort = 11111;

    public MasterServer(int port) {
        try {
            udpSocket = new DatagramSocket(port);
            rooms = new ArrayList<>();
            RoomServer room1 = new RoomServer("Room1");
            rooms.add(room1);
            Thread t1 = new Thread(() -> room1.start());
            System.out.println(room1.getPort());
            t1.start();

            Thread udpThread = new Thread(() -> receiveBroadcast());
            udpThread.start();

            Thread tcpThread = new Thread(() -> connectTcp());
            tcpThread.start();


        } catch (IOException e) {
            e.getMessage();
        }
    }

    private void connectTcp() {
        try {
            this.srvSocket = new ServerSocket(tcpPort);
            while (true) {
                System.out.println("Wait client to connect to master tcp...");
                Socket cSocket = this.srvSocket.accept();
                
                DataOutputStream tcpOut = new DataOutputStream(cSocket.getOutputStream());
                for (RoomServer room : rooms) {
                    Room roomGO = new Room(room.name, room.getPort());
                    JavaNetwork.writeServerGO(tcpOut, roomGO);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void receiveBroadcast() {
        while (true) {
            System.out.println("Waiting for udp");
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            try {
                udpSocket.receive(packet);
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
            sendPortToSrc(packet);
        }
    }

    private void sendPortToSrc(DatagramPacket packet) {     //for UDP
        try {
            String srcAddr = "";
            for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                srcAddr += packet.getAddress().toString().charAt(i);
            }

            sendMsg(String.valueOf(srvSocket.getLocalPort()).getBytes(), srcAddr, packet.getPort());
        } catch (IOException e) {
            e.getMessage();
        }
    }

    private void sendMsg(byte[] msg, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(msg, msg.length, destination, port);
        udpSocket.send(packet);
    }

    public static void main(String[] args) {
        MasterServer server = new MasterServer(12345);
    }
}
