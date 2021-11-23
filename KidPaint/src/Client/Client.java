package Client;

import GameObject.ISerializableGameObject;
import util.ByteArrayParser;
import util.IObserver;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Client {
    private static Client instance;

    DatagramSocket udpSocket;

    public DataInputStream tcpIn;
    public DataOutputStream tcpOut;

    List<IObserver> observers;

    //Singleton
    public static Client getInstance() {
        if(instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Client() {
        try {
            observers = new ArrayList<>();
            udpSocket = new DatagramSocket();
        } catch (SocketException e) {
            System.out.printf("udpSocket error on client init process:\n " + e.getStackTrace());
        }
    }

    public void subscribe(IObserver observer) {
        this.observers.add(observer);
    }

    public void sendMsg(String str, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
        udpSocket.send(packet);
    }

    public void runServ() {
        String serverAddr = ConnectToMaster();
        ConnectToRoom(serverAddr);
    }

    private String ConnectToMaster() {
        String serverAddr = "";
        while (true) {
            if (serverAddr.equals("")) {
                System.out.println("\nWaiting for rooms information...");
                serverAddr = this.receiveRoomAddress();
            } else {
                System.out.println("\nWaiting for room response...");
                return serverAddr;
            }
        }
    }

    private void ConnectToRoom(String serverAddr) {
        Socket tcpSocket = null;
        try {
            tcpSocket = new Socket(serverAddr, 45678);
            this.tcpIn = new DataInputStream(tcpSocket.getInputStream());
            this.tcpOut = new DataOutputStream(tcpSocket.getOutputStream());

            Thread t = new Thread(() -> {
                while (true) {
                    ISerializableGameObject go = readServerGO();

                    for (IObserver observer : observers) {
                        observer.updateGameObject(go);
                    }
                }
            });
            t.start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String receiveRoomAddress() {
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

    private ISerializableGameObject readServerGO() {
        int len = 0;
        try {
            len = tcpIn.readInt();
            byte[] objByte = new byte[len];
            tcpIn.read(objByte, 0, len);
            return (ISerializableGameObject) ByteArrayParser.byte2Object(objByte);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void writeServerGO(ISerializableGameObject serializableGO) {
        if(this.tcpOut == null) return;
        byte[] GOBytes = new byte[0];
        try {
            GOBytes = ByteArrayParser.object2Byte(serializableGO);
            this.tcpOut.writeInt(GOBytes.length);
            this.tcpOut.write(GOBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
