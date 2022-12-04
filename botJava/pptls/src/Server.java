import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Scanner;
//CLASSE UTILIZADA PARA CRIACAO DO SOCKET SERVIDOR
//O Programa em JAVA irá atuar como servidor, recebendo o pedido de conexão do cliente Python
public class Server {
    public Socket cliente;
    public Scanner entrada;
    public ServerSocket server;

    public void carregaServer() throws IOException {
        this.server = new ServerSocket(3322);
        System.out.println("Servidor iniciado na porta 3322");
        this.cliente = this.server.accept();
        System.out.println("Cliente conectado do IP "+cliente.getInetAddress().getHostAddress());
    }

    public void carregaScanner() throws IOException {
        this.entrada = new Scanner(this.cliente.getInputStream());
    }

    //metodo utilizado para receber a jogada do botPython
    public int receiveEscolha(){
        return Integer.parseInt(this.entrada.nextLine());
    }
    //metodo utilizado para enviar ao botPython a jogoda feita pelo Bot JAVA
    public void sendEscolha(Integer escolha) throws IOException {
        this.cliente.getOutputStream().write(escolha.toString().getBytes());
    }

    public void close() throws IOException {
        this.entrada.close();
        this.server.close();
    }

}