import java.io.IOException;
import java.util.*;
import java.lang.Math;

public class Main {
    //atributo compAnt serve para armazenar a jogada realizada pelo adversário
    //no caso o bot pyhton, na rodada anterior a que o bot java irá decidir qual
    //jogada que irá realizar
    public static int compAnt = 99;
    //metodo lista serve para retornar o nome da jogada a partir do numero da escolha
    public static String lista(int a){
        Map<Integer, String> mapaJog = new HashMap<Integer, String>();
        mapaJog.put(0, "pedra");
        mapaJog.put(1, "spock");
        mapaJog.put(2, "papel");
        mapaJog.put(3, "lagarto");
        mapaJog.put(4, "tesoura");
        return mapaJog.get(a);
    }
    //metodo jogador retorna a jogada que o bot Java irá realizar
    //ele faz a jogada utilizando por base uma inteligência Artificial
    //baseada em cadeias de markov
    //A a jogada do bot Java levará em conta a jogada da rodada anterior do bot Python
    //de modo que se tome um escolha que vença a proxima Jogada predita pela matriz da
    //cadeia de Markov
    public static Integer jogador(Server servidor,int[][] markovChain) throws IOException {
        int esc = proxJogada(compAnt,markovChain);
        System.out.println("O JAVA jogou: " + lista(esc));
        //Envio da jogada via socket para o bot Python
        servidor.sendEscolha(esc);
        return esc;
    }

    //metodo adversário retorna o valor da jogada que foi recebida do bot Python
    //através do socket
    public static Integer adversario(Server servidor) {
        int n = servidor.receiveEscolha();
        System.out.println("O PYTHON jogou: " + lista(n));
        return n;
    }

    //metodo que retorna a diferença entre o valor das jogadas entre o bot Java e bot Python
    public static Integer diferenca(Server servidor, int[][] markovChain, int rodada) throws IOException {
        ResultsDAO resultsDao = new ResultsDAO();
        int comp = adversario(servidor);
        int jog = jogador(servidor, markovChain);
        //ARMAZENAMENTO DAS INFORMACOES DE JOGADA DA RODADA NO BANCO DE DADOS MYSQL
        resultsDao.save(rodada,jog,comp);
        //Atualização da matriz da cadeia de markov com a jogada da rodada anterior e a jogada da rodada atual
        //linhas - jogada rodada anterior
        //coluna - jogada feita na rodada posterior
        if(compAnt != 99) {
            atlzMarkov(compAnt, comp, markovChain);
        }
        //atualiza a jogada anterior com o valor da jogada atual do bot Java a fim de que se utilize esse
        //valor para atualizar a matriz na proxima jogada
        compAnt=comp;
        int dif =  jog - comp;
        return dif;
    }

    //metodo que retorna para quais jogadas cada escolha feita perderia
    //o metodo recebe a jogada que foi feita e retonar uma array com dois elementos
    //ou seja, duas jogadas para as quais a jogada passada como parâmetro perderia
    public static Integer[] perdePara(int jogada){
        Integer[] r = new Integer[2];
        switch (jogada){
            //0 "pedra" > perde para Papel e Spock
            case 0:
                r = new Integer[]{1,2};
                break;
            //1 "spock" > perde para Papel e Lagarto
            case 1:
                r = new Integer[]{2,3};
                break;
            //2 "papel" > perde para tesoura e lagarto
            case 2:
                r = new Integer[]{3,4};
                break;
            //3 "lagarto" > perde para tesoura e pedra
            case 3:
                r = new Integer[]{0,4};
                break;
            //4  "tesoura" > perde para pedra e spock
            case 4:
                r = new Integer[]{0,1};
                break;
        }
        return r;
    }
    //INTELIGÊNCIA ARTIFICIAL BASEADA EM CADEIAS DE MARKOV
    //Para escolher o próximo movimento a ser executado pela IA do Bot Java, será a matriz da cadeia de markov.
    // Usaremos a jogada anterior feita pelo bot Python. Com isso, escolhemos na matriz a linha correspondente.
    // Iteramos sobre os elementos dessa linha e escolhemos a jogada para a qual a coluna tem o número máximo de eventos.
    // dessa forma essa será a jogada PREDITA para a próxima escolha do bot Python.
    public static Integer proxJogada(int antJogada, int[][] markovChain){
        Random randon = new Random();
        int proxJogada=9;
        int max_value = 0;
        if(antJogada != 99) {
            for (int j = 0; j < 5; j++) {
                if (markovChain[antJogada][j] > max_value) {
                    proxJogada = j;
                    max_value = markovChain[antJogada][j];
                }
            }
        }
        //Caso a IA não consiga estimar a proxima jogada do bot python, a proxima jogada será escolhida
        // de forma randomizada
        if (proxJogada == 9){
            Integer n = randon.nextInt(100);
            double res = Math.PI / n;
            proxJogada = ((int) res) % 5;
        }
        //dado que se tem a proxima jogada PREDITA do bot Python, escolheremos alguma das duas outras jogadas
        //que são capazes de vencê-la
        return perdePara(proxJogada)[randon.nextInt(2)];
    }

    //metodo utilizado para atualizar a matriz da cadeia de markov a cada jogada
    public static void atlzMarkov(int antJogada, int proxJogada, int[][] markovChain){
        markovChain[antJogada][proxJogada]++;
    }

    //Metodo principal do jogo
    public static void main(String[] args) throws IOException {
        //total de jogadas do bot Java
        int totJog = 0;
        //total  de jogadas do bot Python
        int totComp = 0;
        int totRodadas = 15;
        //{JAVA,EMPATE,PYTHON}
        int[] status = new int[]{0, 0, 0};

        //inicializar a matriz de jogadas [jogada da rodada anterior][jogada da rodadad posterior]
        //inicializa a matriz com valor zero para todas as combinações de jogada
        int[][] markovChain = new int[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                markovChain[i][j] = 0;
            }
        }

        System.out.println("\t\tPLACAR\nJAVA\t\t\tPYTHON\n " + totJog + "\t\t\t\t" + totComp);
        //INICIALIZAÇÃO DO SOCKET SERVIDOR QUE VAI AGUARDAR O RECEBIMENTO DAS JOGADAS DO BOT PYTHON
        Server servidor = new Server();
        servidor.carregaServer();
        servidor.carregaScanner();

        //Laço de repetição utilização para fazer as 15 rodadas do jogo
        for (int i = 0; i < totRodadas; i++) {
            System.out.println("\nRodada atual: " + (i + 1));

            //Calculo do bot vitorioso
            int dif = diferenca(servidor, markovChain,i+1);
            if ((dif) == 2 || (dif) == 1 || (dif) == -3 || (dif) == -4) {
                totJog += 1;
                System.out.println("JAVA ganhou");
                System.out.println("\t\tPLACAR\nJAVA\t\t\tPYTHON\n " + totJog + "\t\t\t\t" + totComp);
                status[0]++;
            } else if ((dif) == 0) {
                System.out.println("EMPATE!");
                System.out.println("\t\tPLACAR\nJAVA\t\t\tPYTHON\n " + totJog + "\t\t\t\t" + totComp);
                status[1]++;
            } else {
                totComp += 1;
                System.out.println("JAVA Perdeu");
                System.out.println("\t\tPLACAR\nJAVA\t\t\tPYTHON\n " + totJog + "\t\t\t\t" + totComp);
                status[2]++;
            }
        }
        //Resutados estatísticos ao final das 15 rodadas do Jogo
        System.out.println("\nEstatísticas Finais do Jogo:");
        int total = status[0] + status[1] + status[2];
        System.out.println("Java : " + status[0] + " - " +
                (status[0] / (float) total * 100f) + "%");
        System.out.println("Empates : " + status[1] + " - " +
                (status[1] / (float) total * 100f) + "%");
        System.out.println("Python : " + status[2] + " - " +
                (status[2] / (float) total * 100f) + "%");


    }
}
