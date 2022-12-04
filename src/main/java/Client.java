import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Client {
    public static void main(String[] args) {
        String serverResponse = "";
        try (Socket socket = new Socket("localhost", 8989);
             OutputStream outputStream = socket.getOutputStream();
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));) {
            socket.setSoTimeout(1000);
            out.println("бизнес");
            System.out.print("Server response is: ");
            try {
                serverResponse = in.readLine();
            } catch (SocketTimeoutException e) {
                serverResponse = "NO RESPONSE";
            } finally {
                System.out.println(serverResponse);
            }
        } catch (IOException e) {
            System.out.println("Can't connect to server");
        }
    }
}
