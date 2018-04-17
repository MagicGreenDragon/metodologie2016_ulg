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
 * Factory di Breakthrough.
 * 
 * @author Daniele Giudice
 *
 */
public class BreakthroughFactory implements GameFactory<GameRuler<PieceModel<Species>>>
{
	/** Array dei nomi dei giocatori */
	private String[] player_names;
	
	/** Possibili valori del parametro "Time" */
	private final List<String> time_values = Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
	/** Possibili valori del parametro "Width" */
	private List<Integer> m_values = Arrays.asList( 2,3,4,5,6,7,8,9,10,11,12 );
	/** Possibili valori del parametro "Height" */
	private List<Integer> n_values = Arrays.asList( 2,3,4,5,6,7,8,9,10,11,12 );
	
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
	
	/** Parametro "Width" */
	private final Param<Integer> width = new Param<Integer>() {
		/** Valore del parametro "Width" */
		private Integer value = 8;
		
		@Override
	    public String name(){ return "Width"; }

	    @Override
	    public String prompt() { return "Board width"; }

	    @Override
	    public List<Integer> values() { return Collections.unmodifiableList(m_values); }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && m_values.contains(v) )
				this.value = (Integer)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Parametro "Height" */
	private final Param<Integer> height = new Param<Integer>() {
		/** Valore del parametro "Height" */
		private Integer value = 8;
		
		@Override
	    public String name(){ return "Height"; }

	    @Override
	    public String prompt() { return "Board height"; }

	    @Override
	    public List<Integer> values() { return Collections.unmodifiableList(n_values); }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && n_values.contains(v) )
	    		this.value = (Integer)v;
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Crea una fabbrica di {@code GameRuler} per giocare a Breakthrough */
	public BreakthroughFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.time, this.width, this.height) );
	}
	
    @Override
    public String name() { return "Breakthrough"; }

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
    	
    	return new Breakthrough(this.getTimeParam(), this.width.get().intValue(), this.height.get().intValue(), this.player_names[0], this.player_names[1]);
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