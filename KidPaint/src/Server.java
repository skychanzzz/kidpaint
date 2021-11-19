import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class Server {
    DatagramSocket socket;
    ServerSocket srvSocket;
    ArrayList<Socket> list = new ArrayList<Socket>();
    int[][] data = new int[50][50];

    public Server(int port) {
        try {
            socket = new DatagramSocket(port);
            srvSocket = new ServerSocket(45678);
            data[0][0] = -543230;
            data[1][1] = -543230;
            while (true) {
                System.out.println("wait");
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                socket.receive(packet);
                Thread t = new Thread(()->receive(packet));
                t.start();
            }
        } catch (IOException e) {
            e.getMessage();
        }
    }

    public void sendMsg(String str, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
        socket.send(packet);
    }

    public void receive(DatagramPacket packet) {     //for UDP
        try {
                byte[] data = packet.getData();
                String name = new String(data, 0, packet.getLength());      //receive the name of the client

                String srcAddr = "";
                for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                    srcAddr += packet.getAddress().toString().charAt(i);
                }

                sendMsg("ACK", srcAddr, packet.getPort());

                tcpTransmission(name);
        } catch (IOException e) {
            e.getMessage();
        }
    }


    private void tcpTransmission(String name) {
        try {
            while (true) {
                System.out.println("Listening at port 45678...");
                Socket clientSocket = srvSocket.accept();
                synchronized (list) {
                    list.add(clientSocket);
                    System.out.printf("Total %d clients are connected.\n", list.size());
                }

                Thread t = new Thread(() -> {
                    try {
                        serve(clientSocket, name);
                    } catch (IOException e) {
                        System.err.println("connection dropped.");
                    }
                    synchronized (list) {
                        list.remove(clientSocket);
                    }
                });
                t.start();
            }
        }catch (IOException e){
            System.err.println("connection dropped.");
        }
    }


    private void serve(Socket clientSocket, String name) throws IOException {
            System.out.printf("Established a connection to host %s:%d\n\n",
                    clientSocket.getInetAddress(), clientSocket.getPort());
            DataInputStream in = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream());
            for (int j = 0; j < data.length; j++) {
                for (int i = 0; i < data[j].length; i++) {
                    out.writeInt(data[j][i]);               //send sketchpad data
                }
            }

          byte[] buffer = new byte[1024];

            while(true){
             int len = in.readInt();
             in.read(buffer, 0, len);
             int color=in.readInt();
             forward(clientSocket,name,buffer,len,color);
            }

    }

    private void forward(Socket clientSocket, String name,byte[] data, int len, int color) {
        synchronized (list) {
            for (int i = 0; i < list.size(); i++) {
                try {
                    Socket socket = list.get(i);
                    if(clientSocket.equals(socket)) continue;
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//                    out.writeInt(name.length());
//                    out.write(name.getBytes(), 0, name.length());
                    out.writeInt(len);
                    out.write(data, 0, len);
                    out.writeInt(color);
                } catch (IOException e) {
                    // the connection is dropped but the socket is not yet removed.
                }
            }
        }
    }


    public static void main(String[] args) {
        Server server = new Server(12345);

    }
}
