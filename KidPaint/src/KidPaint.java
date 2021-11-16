import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;


public class KidPaint extends JFrame {
    DatagramSocket socket;
    Socket tcpSocket;
    private String serverAddr = "";
    private int[][] data = new int[50][50];

    public KidPaint(int port) {
        try {
            socket = new DatagramSocket(port);
            showNamePanel();
        } catch (IOException e) {
            System.out.printf("the %d is being used by another", port);
        }
    }

    public void sendMsg(String str, String destIP, int port) throws IOException {
        InetAddress destination = InetAddress.getByName(destIP);
        DatagramPacket packet = new DatagramPacket(str.getBytes(), str.length(), destination, port);
        socket.send(packet);
    }

    public void receive() {
        try {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
            socket.receive(packet);
            byte[] data = packet.getData();
            String str = new String(data, 0, packet.getLength());       // receive the ACK from Server by UDP

            if (str.equals("ACK")) {
                for (int i = 1; i < packet.getAddress().toString().length(); i++) {
                    serverAddr += packet.getAddress().toString().charAt(i);         // to get the IP address of server
                }
                socket.close();         //UDP close
                tcpTransmission();      // tcp operation
            }

        } catch (IOException e) {
            e.getMessage();
        }

    }


    private void tcpTransmission() {
        try {
            tcpSocket = new Socket(serverAddr, 45678);
            DataInputStream in = new DataInputStream(tcpSocket.getInputStream());
            DataOutputStream out = new DataOutputStream(tcpSocket.getOutputStream());
            Thread t = new Thread(() -> {

                try {
                    for (int j = 0; j < data.length; j++) {
                        for (int i = 0; i < data[j].length; i++) {
                            data[j][i] = in.readInt();
                        }
                    }
                    while (true) {
                        int col=in.readInt();
                        int row=in.readInt();
                        data[col][row]=in.readInt();
                    }
                } catch (IOException ex) {
                    System.err.println("Connection dropped!");
                    System.exit(-1);
                }
            });
            t.start();


            while(true){

            }

        } catch (IOException e) {
            e.getMessage();
        }
    }


    public int[][] getData() {
        return data;
    }

    public String getServerAddr() {
        return serverAddr;
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
                        sendMsg(nameField.getText(), "255.255.255.255", 12345);
                    } catch (IOException exception) {
                        exception.getMessage();
                    }
                    System.out.print(nameField.getText());
                    dispose();
                }
            }

        });
        this.setVisible(true);
    }

    public static void main(String[] args) {
        KidPaint client = new KidPaint(23456);
        while (client.getServerAddr().equals("")) {
            System.out.println("\nWaiting for data...");
            client.receive();
        }

        UI ui = UI.getInstance();            // get the instance of UI
        ui.setData(client.getData(), 20);    // set the data array and block size. comment this statement to use the default data array and block size.
        ui.setVisible(true);                // set the ui



    }
}
