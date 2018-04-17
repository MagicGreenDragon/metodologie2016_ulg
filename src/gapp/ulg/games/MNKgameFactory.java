package gapp.ulg.games;

import gapp.ulg.game.Param;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.GameFactory;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una {@code MNKgameFactory} è una fabbrica di {@link GameRuler} per giocare a
 * (m,n,k)-game. I {@link GameRuler} fabbricati dovrebbero essere oggetti
 * {@link MNKgame}. */
public class MNKgameFactory implements GameFactory<GameRuler<PieceModel<Species>>>
{
	/** Array dei nomi dei giocatori */
	private String[] player_names;
	
	/** Massimo valore possibile del parametro M (non modificabile) */
	private int maxM = 20;
	/** Massimo valore possibile del parametro N (non modificabile) */
	private int maxN = 20;
	/** Minimo valore possibile del parametro K (non modificabile) */
	private int minK = 1;
	
	/** Possibili valori del parametro "Time" */
	private final List<String> time_values = Arrays.asList("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
	/** Possibili valori del parametro "M" */
	private List<Integer> m_values = Arrays.asList( 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 );
	/** Possibili valori del parametro "N" */
	private List<Integer> n_values = Arrays.asList( 1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20 );
	/** Possibili valori del parametro "K" */
	private List<Integer> k_values = Arrays.asList( 1,2,3 );
	
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
	
	/** Parametro "M" */
	private final Param<Integer> m = new Param<Integer>() {
		/** Valore del parametro "M" */
		private Integer value = 3;
		
		@Override
	    public String name(){ return "M"; }

	    @Override
	    public String prompt() { return "Board width"; }

	    @Override
	    public List<Integer> values() { return Collections.unmodifiableList(m_values); }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && m_values.contains(v) )
			{
				this.value = (Integer)v;
				
				updateParamsValues();
			}
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Parametro "N" */
	private final Param<Integer> n = new Param<Integer>() {
		/** Valore del parametro "N" */
		private Integer value = 3;
		
		@Override
	    public String name(){ return "N"; }

	    @Override
	    public String prompt() { return "Board height"; }

	    @Override
	    public List<Integer> values() { return Collections.unmodifiableList(n_values); }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && n_values.contains(v) )
			{
	    		this.value = (Integer)v;
				
	    		updateParamsValues();
			}
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Parametro "K" */
	private final Param<Integer> k = new Param<Integer>() {
		/** Valore del parametro "K" */
		private Integer value = 3;
		
		@Override
	    public String name(){ return "K"; }

	    @Override
	    public String prompt() { return "Length of line"; }

	    @Override
	    public List<Integer> values() { return Collections.unmodifiableList(k_values); }

	    @Override
	    public void set(Object v)
	    {
	    	if( v instanceof Integer && k_values.contains(v) )
			{
	    		this.value = (Integer)v;
				
	    		updateParamsValues();
			}
			else
				throw new IllegalArgumentException();
	    }
	    
	    @Override
	    public Integer get() { return this.value; }
	};
	
	/** Crea una fabbrica di {@code GameRuler} per giocare a (m,n,k)-game */
	public MNKgameFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.time, this.m, this.n, this.k) );
	}
	
	@Override
    public String name() { return "m,n,k-game"; }

    @Override
    public int minPlayers() { return 2; }

    @Override
    public int maxPlayers() { return 2; }

    /** Ritorna una lista con i seguenti quattro parametri:
     * <pre>
     * Primo parametro, valori di tipo String
     *     - name: "Time"
     *     - prompt: "Time limit for a move"
     *     - values: ["No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m"]
     *     - default: "No limit"
     * Secondo parametro, valori di tipo Integer
     *     - name: "M"
     *     - prompt: "Board width"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Terzo parametro, valori di tipo Integer
     *     - name: "N"
     *     - prompt: "Board height"
     *     - values: [1,2,3,...,20]
     *     - default: 3
     * Quarto parametro, valori di tipo Integer
     *     - name: "K"
     *     - prompt: "Length of line"
     *     - values: [1,2,3]
     *     - default: 3
     * </pre>
     * Per i parametri "M","N" e "K" i valori ammissibili possono cambiare a seconda
     * dei valori impostati. Più precisamente occorre che i valori ammissibili
     * garantiscano sempre le seguenti condizioni
     * <pre>
     *     1 <= K <= max{M,N} <= 20   AND   1 <= min{M,N}
     * </pre>
     * dove M,N,K sono i valori impostati. Indicando con minX, maxX il minimo e il
     * massimo valore per il parametro X le condizioni da rispettare sono:
     * <pre>
     *     minM <= M <= maxM
     *     minN <= N <= maxN
     *     minK <= K <= maxK
     *     minK = 1  AND  maxK = max{M,N}  AND  maxN = 20  AND  maxN = 20
     *     N >= K  IMPLICA  minM = 1
     *     N < K   IMPLICA  minM = K
     *     M >= K  IMPLICA  minN = 1
     *     M < K   IMPLICA  minN = K
     * </pre>
     * @return la lista con i quattro parametri */
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
    	
    	int M = Integer.parseInt(String.valueOf(this.m.get()));
    	int N = Integer.parseInt(String.valueOf(this.n.get()));
    	int K = Integer.parseInt(String.valueOf(this.k.get()));
    	
    	return new MNKgame(getTimeParam(), M, N, K, this.player_names[0], this.player_names[1]);
    }
    
    /** Aggiorna i valori ammessi dei parametri M, N, K in base ai loro nuovi valori */
    private void updateParamsValues()
    {
    	// Recupera i valori
    	int M = this.m.get().intValue();
		int N = this.n.get().intValue();
		int K = this.k.get().intValue();
		
		// Aggiorna i limiti
		int maxK = Math.max(M, N);
		int minM = N >= K ? 1 : K;
		int minN = M >= K ? 1 : K;
		
		// Ripulisce le liste e le riempie con i nuovi range
		this.m_values = new ArrayList<>();
		this.n_values = new ArrayList<>();
		this.k_values = new ArrayList<>();
		
		int i;
		
        for (i = minM ; i <= this.maxM ; i++) this.m_values.add(i);
        for (i = minN ; i <= this.maxN ; i++) this.n_values.add(i);
        for (i = this.minK ; i <= maxK ; i++) this.k_values.add(i);
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
