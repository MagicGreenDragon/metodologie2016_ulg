package gapp.ulg.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.PieceModel.Species;

/**
 * Factory di Add Game.
 * 
 * @author Daniele Giudice
 *
 */
public class AddGameFactory implements GameFactory<GameRuler<PieceModel<Species>>>
{
	/** Array dei nomi dei giocatori */
	private String[] player_names;
	
	/** Possibili valori del parametro "Time" */
	private final List<String> time_values = Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
	
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
	
	/** Crea una fabbrica di {@code GameRuler} per giocare a Add Game */
	public AddGameFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.time) );
	}
	
    @Override
    public String name() { return "Add Game"; }

    @Override
    public int minPlayers() { return 2; }

    @Override
    public int maxPlayers() { return 2; }

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
    	
    	return new AddGame(this.getTimeParam(), this.player_names[0], this.player_names[1]);
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
}