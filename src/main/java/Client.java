import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {

    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;


    public Client() throws HeadlessException, IOException {
        socket = new Socket("localhost", 8189);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());
        setSize(300, 150);
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JButton send = new JButton("send");
        JTextField text = new JTextField();
        send.addActionListener(a -> {
            String[] cmd = text.getText().split(" ");
            if (cmd[0].equals("upload")) {
                giveFile(cmd[1]);
            }
            if (cmd[0].equals("download")) {
                getFile(cmd[1]);
            }
            for (String s : cmd) {
                sendMessage(s);
            }
            System.out.println();
// TODO: 27.10.2020 сделал закрытие клиента
            if (cmd[0].equals("exit")) {
                setVisible(false);
                this.dispose();
            }
        });
        panel.add(text);
        panel.add(send);
        add(panel);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void sendMessage(String fileName) {
        try {
            out.writeUTF(fileName);
            System.out.print(fileName + " ");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // TODO: 27.10.2020 выгрузка с облака
    private void giveFile(String fileName) {
        try {
            out.writeUTF("upload");
            out.writeUTF(fileName);
            File file = new File("client/" + fileName);
            long length = file.length();
            out.writeLong(length);
            FileInputStream fileBytes = new FileInputStream(file);
            int read = 0;
            byte[] buffer = new byte[256];
            while ((read = fileBytes.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    // TODO: 27.10.2020 загрузка в облако
    private void getFile(String fileName) {
        try {
            out.writeUTF("download");
            out.writeUTF(fileName);
            File file = new File("client/" + fileName);
            long size = in.readLong();
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buffer = new byte[256];
            for (int i = 0; i < (size + 255) / 256; i++) {
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
            }
            String status = in.readUTF();
            System.out.println(status);
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Client();
    }

}
