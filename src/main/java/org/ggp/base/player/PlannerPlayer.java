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
import java.util.HashSet;
import java.util.List;

/**
 * Created by mariana on 6/13/16.
 */
public class PlannerPlayer extends StateMachineGamer{
    StateMachine stateMachine;
    List<Move> plan;
    long tempoComeço, tempoFim;
    int turn = 0;
    int escoreEsperado = 0;

    HashSet<MachineState> fechada, aberta;

    FileWriter writer;

    @Override
    public StateMachine getInitialStateMachine() {
        return new CachedStateMachine(new ProverStateMachine());
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        try{
            writer = new FileWriter("outputPlanner.txt", true);

            fechada = new HashSet<MachineState>();
            aberta = new HashSet<MachineState>();

            stateMachine = getStateMachine();
            tempoComeço = System.currentTimeMillis();
            plan = new ArrayList<Move>();
            escoreEsperado = findBestPlan(stateMachine.getInitialState()); // core of the code
            tempoFim = System.currentTimeMillis();

            int escoreReal = stateMachine.getGoal(this.getCurrentState(), this.getRole());
            long tempoDeDeliberacao = tempoFim - tempoComeço;
            int tamanhoPlano = plan.size();

            writer.append("======Estatisticas Planner========\n");
            writer.append("Tempo de deliberação = "+tempoDeDeliberacao+"\n");
            writer.append("Tamanho do plan = "+tamanhoPlano+"\n");
            writer.append("Quantidade de rodadas = "+ turn +"\n");
            writer.append("Escore estimado = "+escoreEsperado+"\n");
            writer.append("Escore obtido = "+escoreReal+"\n");

            // Após o cálculo das estatísticas, limpa as estruturas de dados
            tempoComeço = tempoFim = 0;
            plan = new ArrayList<Move>();
            escoreEsperado = turn = 0;

        } catch (GoalDefinitionException e) {
            e.printStackTrace();
            System.out.println("ERRO");
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERRO");
        }
    }

    private int findBestPlan(MachineState state) {
        List<Move> nextMove; // this list only exists to pass as a parameter
        int result = 0;
        try {
            if(stateMachine.isTerminal(state)) {
                int score = stateMachine.getGoal(state, this.getRole());
                writer.append(score+" // "+ state.toString()+"\n");
                return stateMachine.getGoal(state, this.getRole());
            }
            List<Move> moves = stateMachine.getLegalMoves(state, this.getRole());

            for(Move move:moves){
                nextMove= new ArrayList<Move>();
                nextMove.add(move);
                MachineState next = stateMachine.getNextState(state, nextMove);
                if(!fechada.contains(next)) {
                    fechada.add(next);
                    result = findBestPlan(next);
                    if (result == 100) {
                        plan.add(move);
                        break;
                    }
                }
            }
            return result;
        } catch (GoalDefinitionException e) {
            e.printStackTrace();
        } catch (MoveDefinitionException e) {
            e.printStackTrace();
        } catch (TransitionDefinitionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        if(plan.size() > 0)
            return plan.get(turn++); // Retorna o próximo movimento do plan, de acordo com a turn atual
        else{
            turn++;
            // Caso algo tenha dado errado com o plan, retorna um movimento aleatório e continua contando rodadas
            System.out.println("random");
            return stateMachine.getRandomMove(this.getCurrentState(), this.getRole());
        }
    }

    @Override
    public void stateMachineStop() {


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
