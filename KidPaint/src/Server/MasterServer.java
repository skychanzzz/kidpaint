package Server;

import GameObject.ISerializableGameObject;
import GameObject.Room;
import util.ByteArrayParser;
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
            RoomServer room1 = new RoomServer("Room1", 50, 50);
            rooms.add(room1);
            Thread t1 = new Thread(() -> room1.start());
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
                DataInputStream tcpIn = new DataInputStream(cSocket.getInputStream());
                DataOutputStream tcpOut = new DataOutputStream(cSocket.getOutputStream());

                SendRooms(tcpOut);

                new Thread(() -> {
                    Room newRoom = getCreateRoomData(tcpIn);
                    if (newRoom != null) {
                        RoomServer room = new RoomServer(newRoom.name, newRoom.sizeX, newRoom.sizeY);
                        rooms.add(room);
                        new Thread(() -> room.start()).start();
                        SendRooms(tcpOut);
                    }
                }).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Room getCreateRoomData(DataInputStream tcpIn) {
        try {
            int objSize = tcpIn.readInt();
            System.out.println("Object size is " + objSize);
            if (objSize == -1) return null;
            byte[] objByte = new byte[objSize];
            tcpIn.read(objByte, 0, objSize);
            Object GO = ByteArrayParser.byte2Object(objByte);
            return (GO instanceof Room) ? (Room) GO : null;
        } catch (IOException | ClassNotFoundException ioException) {
            System.out.println("IO exception on create room");
        }
        return null;
    }

    private void SendRooms(DataOutputStream tcpOut) {
        for (RoomServer room : rooms) {
            Room roomGO = new Room(room.name, room.getPort(), room.sketchData.length, room.sketchData[0].length);
            JavaNetwork.writeServerGO(tcpOut, roomGO);
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
