import Client.Client;
import GameObject.ISerializableGameObject;
import GameObject.Message;
import GameObject.Pen;
import GameObject.Sketchpad;
import util.ByteArrayParser;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;
import java.util.LinkedList;


public class KidPaint extends JFrame {
    Client client;
    private String name;
    UI ui;          // get the instance of UI


    public KidPaint() {
            client = Client.getInstance();
            showNamePanel();

            String serverAddr = "";
            while (true) {
                if(serverAddr.equals("")) {
                    System.out.println("\nWaiting for rooms information...");
                    serverAddr = this.client.receiveRoomAddress();
                }else {
                    System.out.println("\nWaiting for room response...");
                    tcpTransmission(serverAddr);      // tcp operation
                }
            }
    }

    private void tcpTransmission(String serverAddr){
        try {
            Socket tcpSocket = new Socket(serverAddr, 45678);
            DataInputStream in = new DataInputStream(tcpSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream());

            int id = in.readInt();
            Sketchpad pad = (Sketchpad) readServerGO(in);
            int[][] sketchData = pad.sketchData;
            redrawUI(sketchData);

            Thread t = new Thread(() -> {
                try {
                    while (true) {
                       int mode= in.readInt();
                       if(mode==2) {
                           Pen pen = (Pen) readServerGO(in);
                           ui.selectColor(pen.color);
                           LinkedList<Point> padPoints = pen.points;
                           for (int i = 0; i < padPoints.size(); i++) {
                               ui.paintPixel(padPoints.get(i).x, padPoints.get(i).y);
                           }
                       }else if(mode == 1){
                           Message msg = (Message) readServerGO(in);
                           String content = msg.name + ": " + msg.message;
                           ui.msgShow(content);
                       }
                    }
                } catch (IOException  ex) {
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
                    Pen pen = new Pen((int) change[1], (LinkedList<Point>) change[0]);
                    writeServerGO(out, pen);
                }else{
                    out.writeInt(1);
                    Message msg = new Message(name, (String) change[0]);
                    writeServerGO(out, msg);
                }
            }

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private ISerializableGameObject readServerGO(DataInputStream in) {
        int len = 0;
        try {
            len = in.readInt();
            System.out.println(len);
            byte[] objByte = new byte[len];
            in.read(objByte, 0, len);
            return (ISerializableGameObject) ByteArrayParser.byte2Object(objByte);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void writeServerGO(DataOutputStream out, ISerializableGameObject serializableGO) {
        byte[] GOBytes = new byte[0];
        try {
            GOBytes = ByteArrayParser.object2Byte(serializableGO);
            out.writeInt(GOBytes.length);
            out.write(GOBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void redrawUI(int[][] sketchData) {
        ui = UI.getInstance();
        ui.setData(sketchData, 20);    // set the data array and block size. comment this statement to use the default data array and block size.
        ui.setVisible(true);                // set the ui
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
                        client.sendMsg("", "255.255.255.255", 12345);
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
        KidPaint client = new KidPaint();
    }
}
