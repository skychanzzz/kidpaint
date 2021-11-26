import Client.Client;
import GameObject.*;
import util.ByteArrayParser;
import util.IObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;


public class KidPaint extends JFrame implements IObserver {
    Client client;
    private String name;
    UI ui;          // get the instance of UI


    public KidPaint() {
        this.ui = UI.getInstance();
        client = Client.getInstance();

        client.subscribe(this);

        showNamePanel();

        client.runServ();
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
            }else if(change[0] instanceof Sketchpad) {
                Sketchpad pad = (Sketchpad) change[0];
                client.writeServerGO(pad);
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
        ui.setColor(pen.color);
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
                    name = nameField.getText();
                    System.out.println(nameField.getText());

                    dispose();
                    setUI(client.rooms);
                }
            }

        });
        this.setVisible(true);
    }

    private void setUI(List<Room> rooms) {
        this.setTitle("KidsPaint");
        this.setSize(400, 430);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Container container = this.getContentPane();
        container.setLayout(new FlowLayout());

        JPanel roomListPanel = new JPanel(new GridLayout(0, 1));

        for(Room room: rooms) {
            JButton roomBtn = new JButton(room.name);
            roomBtn.setPreferredSize(new Dimension(300, 30));
            roomBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                    client.joinRoom(roomBtn.getText());

                    Thread t = new Thread(() -> {
                        try {
                            serveUIChange();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    });
                    t.start();
                }
            });
            roomListPanel.add(roomBtn);
        }

        JScrollPane sp = new JScrollPane(roomListPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        sp.setPreferredSize(new Dimension(350, 150));

        container.add(sp);

        JTextField roomName = new JTextField();
        roomName.setPreferredSize(new Dimension(350, 30));
        container.add(roomName);


        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 1));
        container.add(panel);
        JTextField sizeX = new JTextField();
        JTextField sizeY = new JTextField();
        panel.add(sizeX);
        panel.add(sizeY);
        JButton createRoomBtn = new JButton("Create Room");
        createRoomBtn.setFont(new Font("Serif", Font.BOLD, 20));
        createRoomBtn.setBackground(new Color(255, 178, 102));
        panel.add(createRoomBtn);

        JButton joinRoomBtn = new JButton("Join Room");
        joinRoomBtn.setFont(new Font("Serif", Font.BOLD, 20));
        joinRoomBtn.setBackground(new Color(255, 178, 102));
        panel.add(joinRoomBtn);


        createRoomBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
               client.createRoom(roomName.getText(), Integer.parseInt(sizeX.getText()), Integer.parseInt(sizeY.getText()));
               dispose();

                Thread t = new Thread(() -> {
                    try {
                        serveUIChange();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                });
                t.start();
            }
        });

        joinRoomBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {


                if(client.joinRoom(roomName.getText())) dispose();

                Thread t = new Thread(() -> {
                    try {
                        serveUIChange();
                    } catch (IOException ioException) {
                        ioException.printStackTrace();
                    }
                });
                t.start();
            }
        });
        this.setVisible(true);
    }


    public static void main(String[] args) {
        KidPaint client = new KidPaint();
    }
}
