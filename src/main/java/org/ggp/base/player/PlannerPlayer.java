package org.ggp.base.player;

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

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by mariana on 6/13/16.
 */
public class PlannerPlayer extends StateMachineGamer{
    StateMachine maquina;
    List<Move> plano;
    long tempoComeço, tempoFim;
    int rodada = 0;
    int escoreEsperado = 0;

    FileWriter writer;

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        try{
            writer = new FileWriter("outputPlanner.txt", true);

            maquina = getStateMachine();
            tempoComeço = System.currentTimeMillis();
            plano = new ArrayList<Move>();
            escoreEsperado = encontraMelhorPlano(maquina.getInitialState());
            tempoFim = System.currentTimeMillis();

            long tempoDeDeliberacao = tempoFim - tempoComeço;
            int tamanhoPlano = plano.size();
            Collections.reverse(plano); // inverte o plano, pois foi gerado à partir do último movimento

            System.out.print("======Estatisticas Planner========\n");
            System.out.print("Tempo de deliberação = "+tempoDeDeliberacao+"\n");
            System.out.print("Tamanho do plano = "+tamanhoPlano+"\n");
            System.out.print("Quantidade de rodadas = "+rodada+"\n");
            System.out.print("Escore estimado = "+escoreEsperado+"\n");

            // Após o cálculo das estatísticas, limpa as estruturas de dados
            tempoComeço = tempoFim = 0;
            escoreEsperado = rodada = 0;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int encontraMelhorPlano(MachineState estado) {
        List<Move> proximoMovimento;
        int resultado = 0;
        try {
            if(maquina.isTerminal(estado)) {
                int escore = maquina.getGoal(estado, this.getRole());
                return maquina.getGoal(estado, this.getRole());
            }
            List<Move> movimentos = maquina.getLegalMoves(estado, this.getRole());

            for(Move movimento:movimentos){
                proximoMovimento= new ArrayList<Move>();
                proximoMovimento.add(movimento);
                MachineState proximo = maquina.getNextState(estado, proximoMovimento);
                resultado = encontraMelhorPlano(proximo);
                if (resultado == 100) {
                    plano.add(movimento);
                    break;
                }
            }
            return resultado;
        } catch (GoalDefinitionException e) {
            e.printStackTrace();
        } catch (MoveDefinitionException e) {
            e.printStackTrace();
        } catch (TransitionDefinitionException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        if(plano.size() > 0)
            return plano.get(rodada++); // Retorna o próximo movimento do plano, de acordo com a rodada atual
        else{
            rodada++;
            // Caso algo tenha dado errado com o plano, retorna um movimento aleatório e continua contando rodadas
            return maquina.getRandomMove(this.getCurrentState(), this.getRole());
        }
    }

    @Override
    public void stateMachineStop() {
        try {
            rodada = 0;
            plano = new ArrayList<Move>(); // descarta o plano
            int escoreReal = maquina.getGoal(this.getCurrentState(), this.getRole());
            System.out.print("Escore obtido = "+escoreReal+"\n");
        } catch (GoalDefinitionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stateMachineAbort() {
        // Nenhuma ação é necessária
    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {
        // Nenhuma ação é necessária
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
