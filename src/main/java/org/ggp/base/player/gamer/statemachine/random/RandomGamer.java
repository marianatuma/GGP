package org.ggp.base.player.gamer.statemachine.random;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.ggp.base.apps.player.detail.DetailPanel;
import org.ggp.base.apps.player.detail.SimpleDetailPanel;
import org.ggp.base.player.gamer.event.GamerSelectedMoveEvent;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

/**
 * RandomGamer is a very simple state-machine-based Gamer that will always
 * pick randomly from the legal moves it finds at any state in the game.
 */
public final class RandomGamer extends StateMachineGamer
{

	List<Long> temposDeDeliberacao = new ArrayList<Long>();
	long tempoComecoDePartida;
	long tempoFimDePartida;
    StateMachine maquina;

	@Override
	public String getName() {
		return "Random";
	}

	@Override
	public Move stateMachineSelectMove(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
		long start = System.currentTimeMillis();

		List<Move> moves = getStateMachine().getLegalMoves(getCurrentState(), getRole());
		Move selection = (moves.get(new Random().nextInt(moves.size())));

		long stop = System.currentTimeMillis();

		notifyObservers(new GamerSelectedMoveEvent(moves, selection, stop - start));
        temposDeDeliberacao.add(stop - start);

		return selection;

    }

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// Random gamer does no game previewing.
	}

	@Override
	public void stateMachineMetaGame(long timeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException
	{
        maquina = getStateMachine();
        tempoComecoDePartida = System.currentTimeMillis();
		// Random gamer does no metagaming at the beginning of the match.
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
            FileWriter writer = new FileWriter("outputRandomPlayer.txt", true);

            writer.append("======Estatisticas Random Player========\n");

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
        temposDeDeliberacao = new ArrayList<Long>();

        // Random gamer does no special cleanup when the match ends normally.
	}

	@Override
	public void stateMachineAbort() {
		// Random gamer does no special cleanup when the match ends abruptly.
	}

	@Override
	public DetailPanel getDetailPanel() {
		return new SimpleDetailPanel();
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
}