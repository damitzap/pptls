import java.net.Socket;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;
public class Client {
    private static Socket cliente;

    private static void initCliente(){
        try {
            cliente = new Socket("127.0.0.1",3322);
        } catch (IOException ex) {
            Logger.getLogger("teste").log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        initCliente();
    }
}
