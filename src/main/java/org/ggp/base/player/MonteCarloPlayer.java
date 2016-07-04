package org.ggp.base.player;

import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by mariana on 6/03/16.
 */
public class MonteCarloPlayer extends StateMachineGamer {

    List<Long> temposDeDeliberacao = new ArrayList<Long>();
    long tempoComecoDePartida;
    long tempoFimDePartida;
    int qtdRodadas = 0;
    StateMachine maquina;

    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
        maquina = getStateMachine();
        long tempoComeço = System.currentTimeMillis();
        long prazo = timeout - 1000; // estimativa de prazo para evitar estouro do tempo

        List<Move> movimentos = maquina.getLegalMoves(getCurrentState(), getRole());
        Move selecionado = movimentos.get(0);
        if (movimentos.size() > 1) {
            int[] escoreDoMovimento = new int[movimentos.size()];
            int[] sondasDoMovimento = new int[movimentos.size()];

            // Realiza um aprofundamento para cada movimento candidato e mede
            // o escore total e quantidade de sondas por movimento
            for (int i = 0; true; i = (i+1) % movimentos.size()) {
                if (System.currentTimeMillis() > prazo)
                    break;

                int escore = depthCharge(getCurrentState(), movimentos.get(i));
                escoreDoMovimento[i] += escore;
                sondasDoMovimento[i] += 1;
            }

            // Computa a estimativa de cada movimento
            double[] escoreEstimado = new double[movimentos.size()];
            for (int i = 0; i < movimentos.size(); i++) {
                escoreEstimado[i] = (double)escoreDoMovimento[i] / sondasDoMovimento[i];
            }

            // Encontra melhor escore estimado
            int melhor = 0;
            double escoreDoMelhor = escoreEstimado[0];
            for (int i = 1; i < movimentos.size(); i++) {
                if (escoreEstimado[i] > escoreDoMelhor) {
                    escoreDoMelhor = escoreEstimado[i];
                    melhor = i;
                }
            }
            selecionado = movimentos.get(melhor);
        }

        long tempoFim = System.currentTimeMillis();
        long tempoRodada = tempoFim-tempoComeço;
        if(tempoRodada > 1) { // quando o jogador não tem opção a não ser
            // noop (turn do adversário) o tempo de deliberação é zero. Estes tempos são
            // descontados aqui
            temposDeDeliberacao.add(tempoFim - tempoComeço);
        }

        qtdRodadas++;
        notifyObservers(new GamerSelectedMoveEvent(movimentos, selecionado, tempoFim - tempoComeço));
        return selecionado;
    }

    private int[] profundidade = new int[1]; // tem tamanho 1
    // por causa da maneira como stateMachine.performDepthCharge é implementado
    int depthCharge(MachineState estado, Move meuMovimento) {
        try {
            MachineState estadoFinal = maquina.performDepthCharge(
                    maquina.getRandomNextState(estado, getRole(),
                            meuMovimento), profundidade);
            return maquina.getGoal(estadoFinal, getRole());
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public void stateMachineAbort() {
        // Nenhuma ação especial é tomada caso a partida seja abortada
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Nenhuma ação especial é tomada para realizar um preview do jogo
    }

    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
    {
        maquina = getStateMachine();
        tempoComecoDePartida = System.currentTimeMillis();
        // Nenhum metagame é realizado para Monte Carlo
    }

    @Override
    public void stateMachineStop() {
        tempoFimDePartida = System.currentTimeMillis();

        double medio = 0;
        long minimo;
        long maximo;

        Collections.sort(temposDeDeliberacao);

        minimo = temposDeDeliberacao.get(0);
        maximo = temposDeDeliberacao.get(temposDeDeliberacao.size()-1);

        try {
            FileWriter writer = new FileWriter("outputMonteCarlo.txt", true);

            writer.append("======Estatisticas Monte Carlo========\n");

            writer.append("Tempos de deliberação:\n");

            int i = 1;
            for (Long tempo : temposDeDeliberacao){
                writer.append("("+(i++)+","+tempo+")\n");

                medio += tempo;
            }

            medio = medio/temposDeDeliberacao.size();

            int escoreObtido = maquina.getGoal(this.getCurrentState(), this.getRole());

            writer.append("Tempo médio = "+medio+"\n");
            writer.append("Tempo minimo = "+minimo+"\n");
            writer.append("Tempo maximo = "+maximo+"\n");
            writer.append("Mediana = "+calculaMediana()+"\n");
            writer.append("Desvio padrão = "+calculaDesvioPadrao(medio)+"\n");
            writer.append("Duração de partida = "+(tempoFimDePartida-tempoComecoDePartida)+"\n");
            writer.append("Quantidade de rodadas = "+qtdRodadas+"\n");
            writer.append("Escore obtido = "+escoreObtido+"\n");


            writer.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GoalDefinitionException e) {
            e.printStackTrace();
        }


        // Depois do cálculo de estatísticas, as estruturas de dados são reiniciadas
        qtdRodadas = 0;
        temposDeDeliberacao = new ArrayList<Long>();

    }

    private double calculaDesvioPadrao(double medio){
        double desvioPadrao = 0.0;
        for (Long tempo : temposDeDeliberacao){
            desvioPadrao += (medio-tempo)*(medio-tempo);
        }

        return desvioPadrao/temposDeDeliberacao.size();
    }

    private long calculaMediana() {
        long mediana = 0;

        if (temposDeDeliberacao.size()% 2 == 0)
            mediana = (temposDeDeliberacao.get(temposDeDeliberacao.size()/2) +
                    temposDeDeliberacao.get(temposDeDeliberacao.size()/2 - 1))/2;
        else
            mediana = temposDeDeliberacao.get(temposDeDeliberacao.size()/2);
        return mediana;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

}
