package pw.stego.network.proxy.tunnel;

import pw.stego.network.container.Sign;
import pw.stego.network.container.steganography.Steganography;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Tunnel through socket
 * Created by lina on 21.12.16.
 */
public class SocketTunnel<S extends Steganography> extends Tunnel<Socket, S> {
    private final DataInputStream is;
    private final DataOutputStream os;

    public SocketTunnel(Sign sign, S algo, String ipOut, int portOut) throws IOException {
        super(sign, new Socket(ipOut, portOut), algo);

        is = new DataInputStream(connection.getInputStream());
        os = new DataOutputStream(connection.getOutputStream());
    }

    public SocketTunnel(Sign sign, S algo, Socket socket) throws IOException {
        super(sign, socket, algo);

        is = new DataInputStream(connection.getInputStream());
        os = new DataOutputStream(connection.getOutputStream());
    }

    @Override
    public void send(byte[] data) throws IOException {
        File container = algo.insert(sign, data, getNextContainer());
        byte[] stegoBytes = Files.readAllBytes(container.toPath());

        os.writeInt(stegoBytes.length);
        os.write(stegoBytes);

        os.flush();
    }

    @Override
    public byte[] receive() throws IOException {
        byte[] data = new byte[is.readInt()];

//        int i = 0, count;
//        byte[] buf = new byte[1024];
//        while ((count = is.read(buf)) != -1) try {
//            System.arraycopy(buf, 0, data, i, count);
//            if ((i += count) == data.length)
//                break;
//        } catch (ArrayIndexOutOfBoundsException e) {
//            System.out.println(count);
//            System.out.println(i);
//            System.out.println(data.length);
//        }

        int i = 0, count, len = data.length;
        while ((count = is.read(data, i, len)) != -1) {
            i += count;
            if ((len -= count) == 0)
                break;
        }

        File container =  File.createTempFile(UUID.randomUUID().toString(), "stt");
        Files.write(container.toPath(), data);

        byte[] extracted = new byte[0];
        if (algo.isAcceptableContainer(container))
            if (algo.isSignedWith(sign, container))
                extracted = algo.extract(sign, container);

        //noinspection ResultOfMethodCallIgnored
        container.delete();
        return extracted;
    }

    @Override
    public void close() throws IOException {
        connection.close();
    }
}