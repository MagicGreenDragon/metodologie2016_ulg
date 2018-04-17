package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Player;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una MCTSPlayerFactory è una fabbrica di {@link MCTSPlayer}.
 * @param <P>  tipo del modello dei pezzi */
public class MCTSPlayerFactory<P> implements PlayerFactory<Player<P>,GameRuler<P>>
{
	/** Possibili valori del parametro "Rollouts" */
	private final List<Integer> rollout_values = Arrays.asList(1,10,50,100,200,500,1000);
	/** Possibili valori del parametro "Time" */
	private final List<String> execution_values = Arrays.asList("Sequential","Parallel");
	
	/** Lista dei parametri */
	private final List<Param<?>> params;
	
	/** Parametro "Rollouts" */
	private final Param<Integer> rollouts = new Param<Integer>() {
		/** Valore del parametro "Rollouts" */
		private Integer value = 50;
		
		@Override
	    public String name(){ return "Rollouts"; }

	    @Override
	    public String prompt() { return "Number of rollouts per move"; }

	    @Override
	    public List<Integer> values() { return rollout_values; }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && rollout_values.contains(v) )
				this.value = (Integer)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Parametro "Execution" */
	private final Param<String> execution = new Param<String>() {
		/** Valore del parametro "Execution" */
		private String value = "Sequential";
		
		@Override
	    public String name(){ return "Execution"; }

	    @Override
	    public String prompt() { return "Threaded execution"; }

	    @Override
	    public List<String> values() { return execution_values; }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof String && execution_values.contains(v) )
				this.value = (String)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public String get() { return this.value; }
	};
	
	/** Crea una fabbrica di {@code Player} per creare giocatori del tipo MCTSPlayer */
	public MCTSPlayerFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.rollouts, this.execution) );
	}
	
	@Override
    public String name() { return "Monte-Carlo Tree Search Player"; }

    @Override
    public void setDir(Path dir) { }

    /** Ritorna una lista con i seguenti due parametri:
     * <pre>
     * Primo parametro
     *     - name: "Rollouts"
     *     - prompt: "Number of rollouts per move"
     *     - values: [1,10,50,100,200,500,1000]
     *     - default: 50
     * Secondo parametro
     *     - name: "Execution"
     *     - prompt: "Threaded execution"
     *     - values: ["Sequential","Parallel"]
     *     - default: "Sequential"
     * </pre>
     * @return la lista con i due parametri */
    @Override
    public List<Param<?>> params()
    {
        return this.params;
    }

    @Override
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF)
    {
    	Objects.requireNonNull(gF);
        return Play.YES;
    }

    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel,
                             Supplier<Boolean> interrupt)
    {
    	Objects.requireNonNull(gF);
        return null;
    }

    /** Ritorna un {@link MCTSPlayer} che rispetta i parametri impostati
     * {@link MCTSPlayerFactory#params()} e il nome specificato. */
    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name)
    {
    	if (this.canPlay(gF) != Play.YES)
    		throw new IllegalStateException();
        
    	Objects.requireNonNull(name);
    	
        return new MCTSPlayer<>(name, Integer.parseInt(String.valueOf(this.rollouts.get())), String.valueOf(this.execution.get()).equals("Parallel") );
    }
}
