from random import randint
import socket
from pymongo import MongoClient
from fpdf import FPDF, XPos, YPos

# CONEXAO COM O MONGODB
client = MongoClient('127.0.0.1', 27017)
db = client.results

# CRIAR PDF COM RESULTADOS DAS RODAS
pdf = FPDF('P', 'mm', 'Letter')
pdf.add_page()
pdf.image('logo_ime.png', 10, 8, 25)
pdf.set_font('helvetica', 'B', 20)
pdf.cell(80)
pdf.cell(80, 20, 'BOLETIM DE RESULTADOS JAVA vs PYTHON', new_x=XPos.LEFT, new_y=YPos.NEXT, align='C')
pdf.ln(20)
pdf.set_font('helvetica', '', 16)

# Lista com o numero e nome da jogada correspondente
lista = [[0, 'pedra'], [1, 'spock'], [2, 'papel'], [3, 'lagarto'], [4, 'tesoura']]
# variável global para armazenar a jogada anterior do bot python
# utilizado dentro da lógica da cadeia de Markov
jogAnt = 99


# criação do socket Cliente
def clienteSocket():
    ip = '127.0.0.1'
    port = 3322
    addr = ((ip, port))
    client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client_socket.connect(addr)
    return client_socket


# método jogador responsável por retornar a jogada escolhida pelo bot Python
def jogador(client_socket, markov_chain):
    global jogAnt
    # A Heuristica do bot Python é feita com base na jogada que o bot Java faz após
    # a jogada do bot python, ou seja, ele irá pegar a jogada de maior repetição feita
    # após a jogada do bot python e escolhe para jogar a jogada que derrotaria o bot Java
    # caso ele respeitasse esse padrão de repetição
    n = prox_jogada(jogAnt, markov_chain)
    print(f'-> O PYTHON jogou {lista[n][1]}')
    pdf.cell(5, 10, f'-> O PYTHON jogou {lista[n][1]}', new_x=XPos.LEFT, new_y=YPos.NEXT)
    client_socket.send(str(n).encode())
    client_socket.send(b"\n")
    return n


# método adversário responsável por retornar a jogada escolhida pelo bot Java
def adversario(client_socket):
    #recebimento da jogada do bot Java através do Socket
    rcb = int(client_socket.recv(10).decode())
    print(f'-> O JAVA jogou {lista[rcb][1]}')
    pdf.cell(5, 10, f'-> O JAVA jogou {lista[rcb][1]}', new_x=XPos.LEFT, new_y=YPos.NEXT)
    return rcb

# metodo dif para retornar a diferença entre as jogadas dos bots a fim de definir o resultado da rodada
def dif(client_socket, markov_chain, rodada):
    jog = jogador(client_socket, markov_chain)
    comp = adversario(client_socket)
    global jogAnt
    # atualização da matriz da cadeia de markov(5x5 - atributos das linhas e colunas são as jogadas possíveis)
    # Linhas: jogada do bot python na rodada ANTEIOR
    # Coluna: jogada feita pelo bot Java na rodada POSTERIOR
    if jogAnt != 99:
        atlz_markov(jogAnt, comp, markov_chain)
    jogAnt = jog
    # INSERT dos dados da Rodada no BANCO DE DADOS MONGODB
    db.resultados.insert_one({
        "rodada": rodada,
        "esc_python": lista[jog][1],
        "esc_java": lista[comp][1]
    })
    dife = jog - comp
    return dife

# metodo que retorna para quais jogadas uma dada escolha perderia
def perde_para(jogada):
    r = []
    if jogada == 0:
        r = [1, 2]
    elif jogada == 1:
        r = [2, 3]
    elif jogada == 2:
        r = [3, 4]
    elif jogada == 3:
        r = [0, 4]
    elif jogada == 4:
        r = [0, 1]
    return r

# metodo que retorna a PREDICAO da próxima jogada para o BOT PYTHON
# Utilizando para isso um valor randômico para a primeira jogada
# após isso ele utiliza uma Inteligência Artificial baseada em
# Cadeias de Markov
def prox_jogada(ant_jogada, markov_chain):
    prox_jogada = 9
    max_value = 0
    if ant_jogada != 99:
        for j in range(0, 5):
            if markov_chain[ant_jogada][j] > max_value:
                prox_jogada = j
                max_value = markov_chain[ant_jogada][j]
    if prox_jogada == 9:
        prox_jogada = randint(0, 4)
    # Escolha para a jogada do Bot Python uma das duas possibilidades de escolha
    # para a qual o bot Java perderia caso escolhesse a jogada PREDITA pela
    # inteligência artificial baseada em Cadeias de Markov
    return perde_para(prox_jogada)[randint(0, 1)]

# metodo de atualização da matriz da Cadeia de Markov
def atlz_markov(ant_jogada, prox_jogada, markov_chain):
    markov_chain[ant_jogada][prox_jogada] = markov_chain[ant_jogada][prox_jogada] + 1


# A ideia da jogada do bot python é pegar a sua jogada da rodada anterior
# e armazenar na matriz da cadeia de markov com a jogada do bot Java (adversário) na próxima rodada
# a partir disso pegar essa relação para predizar qual seria a próxima jogada do adversário a fim
# de que se faça escolha correta para derrotá-lo
def jogada():
    cliente_socket = clienteSocket()
    pyt = 0
    jav = 0
    emp = 0
    totRodadas = 15

    print(f'''\t\tPLACAR\nPYTHON\t\t\tJAVA\n{pyt}\t\t\t\t{jav}''')
    #o metodo pdf.cell serve para adicionar a saída ao PDF que será gerado ao final da execução do jogo
    pdf.cell(5, 10, f'PLACAR >> PYTHON = {pyt} || JAVA = {jav}''', new_x=XPos.LEFT, new_y=YPos.NEXT)
    markov_chain = [[0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0], [0, 0, 0, 0, 0]]
    for i in range(0, totRodadas):
        print(f'\nRodada atual: {i + 1}')
        pdf.cell(5, 10, f'\nRodada atual: {i + 1}', new_x=XPos.LEFT, new_y=YPos.NEXT)

        # definicao do resultado da rodada, ou seja, vitoria, derrota ou empate
        difer = dif(cliente_socket, markov_chain, i + 1)
        if difer == 2 or difer == 1 or difer == -3 or difer == -4:
            pyt += 1
            print('PYTHON ganhou')
            print(f'''\t\tPLACAR\nPYTHON\t\t\tJAVA\n{pyt}\t\t\t\t{jav}''')
            pdf.cell(5, 10, 'PYTHON ganhou', new_x=XPos.LEFT, new_y=YPos.NEXT)
            pdf.cell(5, 10, f'PLACAR >> PYTHON = {pyt} || JAVA = {jav}''', new_x=XPos.LEFT, new_y=YPos.NEXT)
            if i != (totRodadas - 1):
                pdf.cell(80, 20, '------------------------------------------------------------------',
                         new_x=XPos.LEFT, new_y=YPos.NEXT, align='C')
        elif difer == 0:
            emp += 1
            print('Empate')
            print(f'''\t\tPLACAR\nPYTHON\t\t\tJAVA\n{pyt}\t\t\t\t{jav}''')
            pdf.cell(5, 10, 'Empate!', new_x=XPos.LEFT, new_y=YPos.NEXT)
            pdf.cell(5, 10, f'PLACAR >> PYTHON = {pyt} || JAVA = {jav}''', new_x=XPos.LEFT, new_y=YPos.NEXT)
            if i != (totRodadas - 1):
                pdf.cell(80, 20, '------------------------------------------------------------------',
                         new_x=XPos.LEFT, new_y=YPos.NEXT, align='C')
        else:
            jav += 1
            print('PYTHON perdeu')
            print(f'''\t\tPLACAR\nPYTHON\t\t\tJAVA\n{pyt}\t\t\t\t{jav}''')
            pdf.cell(5, 10, 'JAVA ganhou', new_x=XPos.LEFT, new_y=YPos.NEXT)
            pdf.cell(5, 10, f'PLACAR >> PYTHON = {pyt} || JAVA = {jav}''', new_x=XPos.LEFT, new_y=YPos.NEXT)
            if i != (totRodadas - 1):
                pdf.cell(80, 20, '------------------------------------------------------------------',
                         new_x=XPos.LEFT, new_y=YPos.NEXT, align='C')

    # ESTATISTICAS DO JOGO
    pdf.cell(80, 20, '==================================================================',
             new_x=XPos.LEFT, new_y=YPos.NEXT, align='C')
    tjav = (jav / 15) * 100
    tpyt = (pyt / 15) * 100
    tempt = (emp / 15) * 100
    print('\nEstatísticas Finais do Jogo:\n')
    print(f'Java :{jav} - {tjav: .2f} %')
    print(f'Empates :{emp} - {tempt: .2f} %');
    print(f'Python : {pyt} - {tpyt: .2f} %');
    pdf.cell(5, 10, '\nEstatísticas Finais do Jogo:\n', new_x=XPos.LEFT, new_y=YPos.NEXT)
    pdf.cell(5, 10, f'Java : {jav} - {tjav: .2f} %', new_x=XPos.LEFT, new_y=YPos.NEXT)
    pdf.cell(5, 10, f'Empates : {emp} - {tempt: .2f} %', new_x=XPos.LEFT, new_y=YPos.NEXT)
    pdf.cell(5, 10, f'Python : {pyt} - {tpyt: .2f} %', new_x=XPos.LEFT, new_y=YPos.NEXT)


jogada()
pdf.cell(50, 10, '\n', new_x=XPos.END, new_y=YPos.NEXT)
pdf.set_font('helvetica', 'B', 14)
pdf.cell(50, 50, 'Criado por: 1º Ten DAMITZ', align='C')
pdf.output("Resultados_PY_VS_JAVA.pdf")
