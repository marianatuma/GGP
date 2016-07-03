package org.ggp.base.player;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

import java.util.*;

/**
 * Created by mariana on 7/3/16.
 */
public class AStarPlayer extends StateMachineGamer {
    Set<MachineState> aberta, fechada; // Set evita que estados já visitados sejam adicionados novamente
    Map<MachineState, MachineState> proveniente; // Para cada estado, guarda o melhor estado que leva a ele
    HashMap<MachineState, Integer> distanciaComeco; // guarda a distancia de um estado do começo
    HashMap<MachineState, Integer> distanciaEstimadaFim; // guarda as distancias estimadas até um estado terminal
    StateMachine maquina;
    List<Move> plano;

    @Override
    public StateMachine getInitialStateMachine() {
        return null;
    }

    @Override
    public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        aberta = new HashSet<MachineState>();
        fechada = new HashSet<MachineState>();
        proveniente = new HashMap<MachineState,MachineState>();
        distanciaComeco = new HashMap<MachineState, Integer>();
        distanciaEstimadaFim = new HashMap<MachineState, Integer>();
        plano = new ArrayList<Move>();

        maquina = getStateMachine();

        MachineState inicial = maquina.getInitialState();
        aberta.add(inicial);
        int estimativaInicial = estimaDistancia(inicial); // gera uma estimativa do estado inicial até um estado terminal usando depth charge


        MachineState selecionado = this.getCurrentState();
        while(aberta.size() != 0) { // enquanto houverem estados na lista aberta
            int menorDistancia = Integer.MAX_VALUE;
            for(MachineState estado : aberta) {
                int estimativa = distanciaEstimadaFim.get(estado);
                if(estimativa < menorDistancia)
                    selecionado = estado; // ao final, este vai ser o estado com menor distancia ate o final
            }
            if(maquina.isTerminal(selecionado)){
                montaPlano(selecionado);
            } else {
                aberta.remove(selecionado);
                fechada.add(selecionado);
                List<Move> movimentosLegais = maquina.getLegalMoves(selecionado, this.getRole());
                for(Move movimento : movimentosLegais){
                    List<Move> movimentos = new ArrayList<Move>();
                    movimentos.add(movimento);
                    MachineState proximo = maquina.getNextState(selecionado, movimentos);
                    if(!fechada.contains(proximo)){ // se o estado estiver na lista fechada, ignora
                        int distanciaEstimada = distanciaComeco.get(selecionado)+1;
                        distanciaComeco.put(proximo, distanciaEstimada);
                        aberta.add(proximo); // o conjunto se encarrega de garantir que não haja duplicatas

                        // este é o melhor caminho até agora
                        proveniente.put(proximo, selecionado);
                        distanciaComeco.put(proximo, distanciaEstimada);
                        estimaDistancia(proximo);

                    }
                }
            }
        }
    }

    private void montaPlano(MachineState selecionado) {
    }

    private int estimaDistancia(MachineState estado) {
        int[] profundidade = new int[1]; // guarda a profundidade encontrada no depth charge
        try {
            Move meuMovimento = maquina.getRandomMove(estado, this.getRole());
            maquina.performDepthCharge( // executa depth charge para encontrar a profundidade
                    maquina.getRandomNextState(estado, getRole(),
                            meuMovimento), profundidade);

            return profundidade[0];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.MAX_VALUE;
    }

    @Override
    public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
        return null;
    }

    @Override
    public void stateMachineStop() {

    }

    @Override
    public void stateMachineAbort() {

    }

    @Override
    public void preview(Game g, long timeout) throws GamePreviewException {

    }

    @Override
    public String getName() {return getClass().getSimpleName();}
}
