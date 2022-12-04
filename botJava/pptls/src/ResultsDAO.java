import java.sql.*;
import java.util.HashMap;
import java.util.Map;
//CLASSE UTILIZADA PARA POPULAR O BANCO DE DADOS MYSQL
//COM OS DADOS DOS RESULTADOS DE CADA RODADA DO JOGO
public class ResultsDAO {
    public static String lista(int a){
        Map<Integer, String> mapaJog = new HashMap<Integer, String>();
        mapaJog.put(0, "pedra");
        mapaJog.put(1, "spock");
        mapaJog.put(2, "papel");
        mapaJog.put(3, "lagarto");
        mapaJog.put(4, "tesoura");
        return mapaJog.get(a);
    }
    //metodo que salva a rodada, a escolha do bot java e escolha do bot python no BANCO DE DADOS MySQL
    public void save(int rodada, int esc_java, int esc_python){
        String sql = "INSERT INTO results(rodada, esc_java, esc_python) VALUES (?, ?, ?)";
        Connection connection = null;
        PreparedStatement pstm = null;
        try{
            //Criar conexao com o Banco de Dados
            connection = ConnectionModule.connector();
            //Criar um PreparedStatemente para executar uma query;
            pstm = (PreparedStatement) connection.prepareStatement(sql);
            //Adicao de valores que sao esperados pela query obtidos a partir do objeto airport passado com parâmetro
            pstm.setInt(1,rodada);
            pstm.setString(2,lista(esc_java));
            pstm.setString(3,lista(esc_python));
            //Execucao da query
            pstm.execute();
            System.out.println("Aeroporto Salvo com Sucesso");
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            //fechar conexoes
            try{
                if(pstm!=null){
                    pstm.close();
                }
                if(connection!=null){
                    connection.close();
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
