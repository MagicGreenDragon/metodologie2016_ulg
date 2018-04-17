package gapp.ulg.games;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una OthelloFactory è una fabbrica di {@link GameRuler} per giocare a Othello.
 * I {@link GameRuler} fabbricati dovrebbero essere oggetti {@link Othello}. */
public class OthelloFactory implements GameFactory<GameRuler<PieceModel<Species>>>
{
	/** Array dei nomi dei giocatori */
	private String[] player_names;
	
	/** Possibili valori del parametro "Time" */
	private final List<String> time_values = Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
	/** Possibili valori del parametro "Board" */
	private final List<String> board_values = Arrays.asList("6x6","8x8","10x10","12x12");
	
	/** Lista dei parametri */
	private final List<Param<?>> params;
	
	/** Parametro "Time" */
	private final Param<String> time = new Param<String>() {
		/** Valore del parametro "Time" */
		private String value = "No limit";
		
		@Override
	    public String name(){ return "Time"; }

	    @Override
	    public String prompt() { return "Time limit for a move"; }

	    @Override
	    public List<String> values() { return time_values; }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof String && time_values.contains(v) )
				this.value = (String)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public String get() { return this.value; }
	};
	
	/** Parametro "Board" */
	private final Param<String> board = new Param<String>() {
		/** Valore del parametro "Board" */
		private String value = "8x8";
		
		@Override
	    public String name(){ return "Board"; }

	    @Override
	    public String prompt() { return "Board size"; }

	    @Override
	    public List<String> values() { return board_values; }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof String && board_values.contains(v) )
				this.value = (String)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public String get() { return this.value; }
	};
	
	/** Crea una fabbrica di {@code GameRuler} per giocare a Othello */
	public OthelloFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.time, this.board) );
	}
	
    @Override
    public String name() { return "Othello"; }

    @Override
    public int minPlayers() { return 2; }

    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti due parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo String
     *     - name: "Board"
     *     - prompt: "Board size"
     *     - values: ["6x6","8x8","10x10","12x12"]
     *     - default: "8x8"
     * </pre>
     * @return la lista con i due parametri */
    @Override
    public List<Param<?>> params()
    {
    	return this.params;
    }

    @Override
    public void setPlayerNames(String... names)
    {
    	if( names == null )
			throw new NullPointerException();
    	
    	for(String name : names)
    	{
    		if( name == null )
    			throw new NullPointerException();
    	}
        
        if( names.length != 2 )
        	throw new IllegalArgumentException();
        
        this.player_names = names;
    }

    @Override
    public GameRuler<PieceModel<Species>> newGame()
    {
    	if( this.player_names == null )
    		throw new IllegalStateException();
    	
    	return new Othello(this.getTimeParam(), this.getSizeParam(), this.player_names[0], this.player_names[1]);
    }
    
    /** Legge il parametro "Time" e ne ritorna il valore (in millisecondi)
     * @return il tempo massimo per eseguire una mossa (in millisecondi)
     */
    private long getTimeParam()
    {
    	String value = String.valueOf(this.time.get());
    	
    	if( value.equals("No limit") )
    		return -1;
    	
    	long t = Long.parseLong(value.substring(0, value.length()-1));
    	
    	return (value.substring(value.length()-1).equals("s") ? t : t*60)*1000;
    }
    
    /** Legge il parametro "Board" e ne ritorna il valore
     * @return lato della board
     */
    private int getSizeParam()
    {
    	String value = String.valueOf(this.board.get()).split("x")[0];
    	return Integer.parseInt(value);
    }
}
