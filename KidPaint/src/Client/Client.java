package Client;

import GameObject.ISerializableGameObject;
import GameObject.Room;
import util.ByteArrayParser;
import util.IObserver;
import util.JavaNetwork;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

public class Client implements IObserver{
    private static Client instance;

    DatagramSocket udpSocket;

    public DataInputStream tcpIn;
    public DataOutputStream tcpOut;

    public List<Room> rooms;
    List<IObserver> observers;
    private String serverAddr;

    //Singleton
    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private Client() {
        try {
            observers = new ArrayList<>();
            udpSocket = new DatagramSocket();
            this.sendMsg("", "255.255.255.255", 12345);
        } catch (SocketException e) {
            System.out.printf("udpSocket error on client init process:\n " + e.getStackTrace());
        } catch (IOException e) {
            e.printStackTrace();
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
        this.serverAddr = ConnectToMaster();
    }

    private String ConnectToMaster() {
        String serverAddr = "";
        while (true) {
            if (serverAddr.equals("")) {
                System.out.println("\nWaiting for rooms information...");
                serverAddr = this.receiveMasterTcp();
            } else {
                String[] serverInfo = serverAddr.split("\\:");
                rooms = new ArrayList<>();
                subscribe(this);
                ConnectToTcpServer(serverInfo[0], Integer.parseInt(serverInfo[1]));
                return serverInfo[0];
            }
        }
    }

    public void joinRoom(String roomName) {
        for(Room room : rooms) {
            if(room.name.equals(roomName)) {
                ConnectToTcpServer(this.serverAddr, room.port);
                break;
            }
        }
    }

    private void ConnectToTcpServer(String serverAddr, int port) {
        Socket tcpSocket = null;
        try {
            tcpSocket = new Socket(serverAddr, port);
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


    private String receiveMasterTcp() {
        try {
            String serverAddr = "";

            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            udpSocket.receive(packet);
            byte[] data = packet.getData();
            String str = new String(data, 0, packet.getLength());       // receive the ACK from Server.Server by UDP

            for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                serverAddr += packet.getAddress().toString().charAt(i);         // to get the IP address of server
            }
            udpSocket.close();         //UDP close
            this.udpSocket = null;
            return serverAddr + ":" + str;

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
        return "";
    }

    private ISerializableGameObject readServerGO() {
        return JavaNetwork.readServerGO(this.tcpIn);
    }

    public void writeServerGO(ISerializableGameObject serializableGO) {
        JavaNetwork.writeServerGO(this.tcpOut, serializableGO);
    }

    //Observe to self
    @Override
    public void updateGameObject(ISerializableGameObject GO) {
        if(GO instanceof Room && rooms !=null) {
            rooms.add((Room) GO);
        }
    }
}
