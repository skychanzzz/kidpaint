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

public class Client implements IObserver {
    private static Client instance;

    DatagramSocket udpSocket;

    Socket tcpSocket;
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
            this.sendBroadcast();
        } catch (SocketException e) {
            System.out.printf("udpSocket error on client init process:\n " + e.getStackTrace());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(IObserver observer) {
        this.observers.add(observer);
    }

    public void sendBroadcast() throws IOException {
        InetAddress destination = InetAddress.getByName("255.255.255.255");
        DatagramPacket packet = new DatagramPacket("".getBytes(), "".length(), destination, 12345);
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

    public boolean joinRoom(String roomName) {
        for (Room room : rooms) {
            if (room.name.equals(roomName)) {
                ConnectToTcpServer(this.serverAddr, room.port);
                return true;
            }
        }
        return false;
    }

    public void createRoom(String roomName) {
        Room newRoom = new Room(roomName, 0);
        int roomCount = rooms.size();
        JavaNetwork.writeServerGO(tcpOut, newRoom);

        new Thread(() -> {
            while (roomCount == rooms.size());
            joinRoom(roomName);
        }).start();
    }

    private void ConnectToTcpServer(String serverAddr, int port) {
        try {
            tcpSocket = new Socket(serverAddr, port);
            this.tcpIn = new DataInputStream(tcpSocket.getInputStream());
            this.tcpOut = new DataOutputStream(tcpSocket.getOutputStream());

            Thread t = new Thread(() -> {
                while (true) {
                    ISerializableGameObject go = null;
                    try {
                        go = readServerGO();
                    } catch (IOException ioException) {
                        System.out.println("Server io crash");
                        break;
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

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

    private ISerializableGameObject readServerGO() throws IOException, ClassNotFoundException {
        return JavaNetwork.readServerGO(this.tcpIn);
    }

    public void writeServerGO(ISerializableGameObject serializableGO) {
        JavaNetwork.writeServerGO(this.tcpOut, serializableGO);
    }

    //Observe to self
    @Override
    public void updateGameObject(ISerializableGameObject GO) {
        if (GO instanceof Room && rooms != null) {
            boolean hasRoom = false;
            for(Room room: rooms) {
                if(room.name.equals(((Room) GO).name)) hasRoom = true;
            }
            if(!hasRoom) rooms.add((Room) GO);
        }
    }
}
