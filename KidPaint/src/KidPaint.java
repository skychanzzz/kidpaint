import Client.Client;
import GameObject.ISerializableGameObject;
import GameObject.Message;
import GameObject.Pen;
import GameObject.Sketchpad;
import util.ByteArrayParser;
import util.IObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;
import java.util.LinkedList;


public class KidPaint extends JFrame implements IObserver {
    Client client;
    private String name;
    UI ui;          // get the instance of UI


    public KidPaint() {
        client = Client.getInstance();
        ui = UI.getInstance();

        client.subscribe(this);

        showNamePanel();

        client.runServ();
        try {
            serveUIChange();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void updateGameObject(ISerializableGameObject GO) {
        if (GO instanceof Sketchpad) {
            redrawUI((Sketchpad) GO);
        } else if (GO instanceof Pen) {
            SetPen((Pen) GO);
        } else if (GO instanceof Message) {
            SetMessage((Message) GO);
        }
    }

    private void serveUIChange() throws IOException {
        while (true) {
            Object[] change = ui.getChange();
            if (change.length == 2) {
                Pen pen = new Pen((int) change[1], (LinkedList<Point>) change[0]);
                client.writeServerGO(pen);
            } else {
                Message msg = new Message(name, (String) change[0]);
                client.writeServerGO(msg);
            }
        }
    }

    private void SetMessage(Message go) {
        Message msg = go;
        String content = msg.name + ": " + msg.message;
        ui.msgShow(content);
    }

    private void SetPen(Pen go) {
        Pen pen = go;
        ui.selectColor(pen.color);
        LinkedList<Point> padPoints = pen.points;
        for (int i = 0; i < padPoints.size(); i++) {
            ui.paintPixel(padPoints.get(i).x, padPoints.get(i).y);
        }
    }

    private void redrawUI(Sketchpad pad) {
        int[][] sketchData = pad.sketchData;
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
                        name = nameField.getText();
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
