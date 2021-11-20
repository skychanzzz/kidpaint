import util.ByteArrayParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.LinkedList;


public class KidPaint extends JFrame {
    DatagramSocket udpSocket;
    Socket tcpSocket;
    private String name;
    UI ui;          // get the instance of UI


    public KidPaint(int port) {
        try {
            udpSocket = new DatagramSocket(port);
            showNamePanel();

            String serverAddr = "";
            while (true) {
                if(serverAddr.equals("")) {
                    System.out.println("\nWaiting for rooms information...");
                    serverAddr = this.receiveRoomAddress();
                }else {
                    System.out.println("\nWaiting for room response...");
                    tcpTransmission(serverAddr);      // tcp operation
                }
            }
        } catch (IOException e) {
            System.out.printf("the %d is being used by another", port);
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


    private void tcpTransmission(String serverAddr) {
        try {
            tcpSocket = new Socket(serverAddr, 45678);
            DataInputStream in = new DataInputStream(tcpSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream());

            int[][] sketchData = fetchSketchData(in);
            redrawUI(sketchData);

            Thread t = new Thread(() -> {
                byte[] buffer = new byte[1024];
                try {
                    while (true) {
                       int mode= in.readInt();
                       if(mode==2) {            //send diagram data
                           ui.selectColor(in.readInt());
                           int len = in.readInt();
                           in.read(buffer, 0, len);
                           byte[] object = new byte[len];
                           for (int i = 0; i < len; i++) {
                               object[i] = buffer[i];
                           }
                           LinkedList<Point> point = (LinkedList<Point>) ByteArrayParser.byte2Object(object);
                           for (int i = 0; i < point.size(); i++) {
                               ui.paintPixel(point.get(i).x, point.get(i).y);
                           }
                       }else{               //receive message
                           int len = in.readInt();
                           in.read(buffer, 0, len);
                           String content=new String(buffer, 0, len)+": ";
                           len = in.readInt();
                           in.read(buffer, 0, len);
                           content +=new String(buffer, 0, len);
                           ui.msgShow(content);
                       }
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    System.out.println(ex.getMessage());
                    System.err.println("Connection dropped!");
                    System.exit(-1);
                }
            });
            t.start();

            while (true) {
                Object[] change = ui.getChange();
                if(change.length==2) {
                    out.writeInt(2);
                    out.writeInt((int) change[1]);
                    LinkedList<Point> points = (LinkedList<Point>) change[0];
                    out.writeInt(points.size());
                    LinkedList<Point> point = new LinkedList<>();
                    for (int i = 0; i < points.size(); i++) {
                        point.add(points.get(i));
                        out.writeInt(ByteArrayParser.object2Byte(point).length);
                        out.write(ByteArrayParser.object2Byte(point));
                        point.pop();
                    }
                }else{
                    out.writeInt(1);
                    String str =(String)change[0];
                    out.writeInt(name.length());
                    out.write(name.getBytes(),0,name.length());
                    out.writeInt(str.length());
                    out.write(str.getBytes(), 0, str.length());
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void redrawUI(int[][] sketchData) {
        ui = UI.getInstance();
        ui.setData(sketchData, 20);    // set the data array and block size. comment this statement to use the default data array and block size.
        ui.setVisible(true);                // set the ui
    }

    private int[][] fetchSketchData(DataInputStream in) throws IOException {
        int[][] sketchData = new int[50][50];
        for (int j = 0; j < sketchData.length; j++) {
            for (int i = 0; i < sketchData[j].length; i++) {
                sketchData[j][i] = in.readInt();
            }
        }
        return sketchData;
    }

    public void showNamePanel() {
        this.setSize(new Dimension(320, 240));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel namePanel = new JPanel();
        this.getContentPane().add(namePanel, BorderLayout.CENTER);
        namePanel.setLayout(null);
        JLabel label = new JLabel("Please input your name:");
        label.setBounds(80, 20, 150, 20);
        namePanel.add(label);
        JTextField nameField = new JTextField();
        nameField.setBounds(80, 60, 150, 20);
        namePanel.add(nameField);
        nameField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == 10) {        // if the user press ENTER
                    try {
                        name=nameField.getText();
                        sendMsg("", "255.255.255.255", 12345);
                    } catch (IOException exception) {
                        System.out.println(exception.getMessage());
                    }
                    System.out.println(nameField.getText());
                    dispose();
                }
            }

        });
        this.setVisible(true);
    }

    public static void main(String[] args) {
        KidPaint client = new KidPaint(23451);
    }
}
