package gapp.ulg.play;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Supplier;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.GameRuler.Mechanics;
import gapp.ulg.game.board.GameRuler.Next;
import gapp.ulg.game.board.GameRuler.Situation;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Player;
import gapp.ulg.play.OptimalStrategy.SitEnc;
import gapp.ulg.play.OptimalStrategy.Winner;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Una OptimalPlayerFactory è una fabbrica di {@link OptimalPlayer}
 * @param <P>  tipo del modello dei pezzi */
public class OptimalPlayerFactory<P> implements PlayerFactory<Player<P>,GameRuler<P>>
{
	/** Una {@code Strategy} rappresenta una strategia ottimale per uno specifico
     * gioco.
     * @param <P>  tipo del modello dei pezzi */
    interface Strategy<P>
    {
        /** @return il nome del gioco di cui questa è una strategia ottimale */
        String gName();

        /** Ritorna la mossa (ottimale) nella situazione di gioco specificata. Se
         * {@code s} o {@code next} non sono compatibili con il gioco di questa
         * strategia, il comportamento del metodo è indefinito.
         * @param s  una situazione di gioco
         * @param next  la funzione delle mosse valide e prossime situazioni del
         *              gioco, cioè quella di {@link GameRuler.Mechanics#next}.
         * @return la mossa (ottimale) nella situazione di gioco specificata */
        Move<P> move(Situation<P> s, Next<P> next);
    }
    
    /** Non segue i link dei file e delle directory */
	private final LinkOption NOL = LinkOption.NOFOLLOW_LINKS;
	/** Directory delle strategie */
	private Path strategies_dir;
	
	/** Lista delle strategie */
	private List<OptimalStrategy<P>> strategies;
    
	/** Possibili valori del parametro "Time" */
	private final List<String> execution_values = Arrays.asList("Sequential","Parallel");
	
	/** Lista dei parametri */
	private final List<Param<?>> params;
	
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
	
	/** Crea una fabbrica di {@code Player} per creare giocatori del tipo OptimalPlayer */
	public OptimalPlayerFactory()
	{
		this.params = Collections.unmodifiableList( Arrays.asList(this.execution) );
		this.strategies = new ArrayList<>();
		this.strategies_dir = null;
	}
	
    @Override
    public String name() { return "Optimal Player"; }

    /** Se la directory non è null, in essa salva e recupera file che contengono le
     * strategie ottimali per giochi specifici. Ogni strategia è salvata nella
     * directory in un file il cui nome rispetta il seguente formato:
     * <pre>
     *     strategy_<i>gameName</i>.dat
     * </pre>
     * dove <code><i>gameName</i></code> è il nome del gioco, cioè quello ritornato
     * dal metodo {@link GameRuler#name()}. La directory di default non è impostata
     * e quindi è null. */
    @Override
    public void setDir(Path dir)
    {
        if( dir!=null && Files.exists(dir, this.NOL) && Files.isDirectory(dir, this.NOL) )
        	this.strategies_dir = dir;
    }

    /** Ritorna una lista con il seguente parametro:
     * <pre>
     *     - name: "Execution"
     *     - prompt: "Threaded execution"
     *     - values: ["Sequential","Parallel"]
     *     - default: "Sequential"
     * </pre>
     * @return la lista con il parametro */
    @Override
    public List<Param<?>> params()
    {
        return this.params;
    }

    /** Ritorna {@link Play#YES} se conosce già la strategia ottimale per il gioco
     * specificato o perché è in un file (nella directory impostata con
     * {@link OptimalPlayerFactory#setDir(Path)}) o perché è in memoria, altrimenti
     * stima se può essere praticamente possibile imparare la strategia
     * ottimale e allora ritorna {@link Play#TRY_COMPUTE} altrimenti ritorna
     * {@link Play#NO}. Il gioco, cioè il {@link GameRuler}, valutato è quello
     * ottenuto dalla {@link GameFactory} specificata. Se non conosce già la
     * strategia ritorna sempre {@link Play#TRY_COMPUTE} eccetto che per i giochi
     * con i seguenti nomi che sa che è impossibile calcolarla:
     * <pre>
     *     Othello8x8, Othello10x10, Othello12x12
     * </pre>
     * Il controllo sull'esistenza di un file con la strategia è effettuato solamente
     * in base al nome (senza tentare di leggere il file, perché potrebbe richiedere
     * troppo tempo). */
    @Override
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF)
    {
    	Objects.requireNonNull(gF);
    	
    	GameRuler<P> gR = null;
    	
    	// Se i nomi non sono stati settati, li setta
    	try
    	{
    		gR = gF.newGame();
    	}
    	catch( IllegalStateException e )
    	{
    		gF.setPlayerNames("A", "B");
    		gR = gF.newGame();
    	}
    	
    	// Giochi impossibili per il player
    	if( gF.maxPlayers()>2 || gR.name().equals("Othello8x8") || gR.name().equals("Othello10x10") || gR.name().equals("Othello12x12") )
    		return Play.NO;
    	
    	// Cerca tra le strategie in memoria
    	for(OptimalStrategy<P> s : this.strategies)
    		if( s.gName().equals(gR.name()) )
    			return Play.YES;
    	
    	// Se è stata settata una directory
    	if( this.strategies_dir!=null )
    	{
    		// Ricava la directory del file della stratecia
        	Path file = Paths.get(this.strategies_dir.toString(), "strategy_"+gR.name()+".dat");
        	
        	// Contorolla se il file esiste
        	if( Files.exists(file, this.NOL) )
        		return Play.YES;
    	}
    	
    	return Play.TRY_COMPUTE;
    }

    /** Tenta di calcolare la strategia ottimale per il gioco specificato. Ovviamente
     * effettua il calcolo solo se il metodo
     * {@link OptimalPlayerFactory#canPlay(GameFactory)} ritorna {@link Play#TRY_COMPUTE}
     * per lo stesso gioco. Il gioco, cioè il {@link GameRuler}, da valutare è quello
     * ottenuto dalla {@link GameFactory} specificata. Se il calcolo ha successo e
     * una directory ({@link OptimalPlayerFactory#setDir(Path)} ) è impostata, tenta
     * di salvare il file con la strategia calcolata, altrimenti la mantiene in
     * memoria. */
    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel,
                             Supplier<Boolean> interrupt)
    {
    	Play play_check = canPlay(gF);
    	
    	if( play_check == Play.YES )
    		return null;
    	
    	if( play_check == Play.NO )
    		return "CANNOT PLAY";
    	
    	if( interrupt!=null && interrupt.get() )
    		return "INTERRUPTED";
    	
    	// I nomi sono già stati settati nel metodo canPlay
    	GameRuler<P> gR = gF.newGame();
    	Mechanics<P> gM = gR.mechanics();
    	
    	try
    	{
    		Map<SitEnc<P>,Winner> strategy_map;
    		
    		if(parallel)
    		{
    			Map<SitEnc<P>,Winner> conc_strategy_map = new ConcurrentHashMap<>();
    			computeStrategyParallel(conc_strategy_map, gM.start, new SitEnc<>(gM, gM.start), gM, interrupt);
    			strategy_map = new HashMap<>(conc_strategy_map);
    		}
    		else
    		{
    			strategy_map = new HashMap<>();
    			computeStrategy(strategy_map, gM.start, new SitEnc<>(gM, gM.start), gM, interrupt);
    		}
    		
    		// Aggiunge la strategia nella lista
    		this.strategies.add(new OptimalStrategy<>(gM, gR.name(), strategy_map));
    		
    		// Tenta di salvare la strategia su un file (se fallisce, non fa nulla)
    		try
        	{
    			this.saveStrategy(gR.name(), strategy_map);
        	}
    		catch( IllegalStateException e ) {}
    		
    		return null;
    	}
    	catch( NullPointerException e ) { return "INTERRUPTED"; }
    	catch( OutOfMemoryError | StackOverflowError e ) { return "OUT OF MEMORY"; }
    }
    
    /** Metodo che tenta di calcolare la strategia ottimale, inserendola nella mappa data come parametro.
     * @param strategy mappa della strategia ottimale
     * @param s situazione iniziale
     * @param s_enc situazione iniziale codificata
     * @param next funzione per ottenere le prossime situazioni
     * @param interr supplier di interrupt
     * @throws NullPointerException in caso di interruzione
     */
    private void computeStrategy(Map<SitEnc<P>, Winner> strategy, Situation<P> s, SitEnc<P> s_enc, Mechanics<P> gM, Supplier<Boolean> interr)
    {
    	// Caso base: la situazione è già stata calcolata
    	if(strategy.containsKey(s_enc))
    		return;
    	
    	// Controllo se è stato interrotto
    	if( interr!=null && interr.get() )
    		throw new NullPointerException();
    	
    	// Caso base: situazione finale
    	if( s.turn <= 0 )
    	{
    		switch(s.turn)
    		{
    			case 0: strategy.put(s_enc, Winner.NONE);
    				break;
    			case -1: strategy.put(s_enc, Winner.PLAYER_A);
					break;
    			case -2: strategy.put(s_enc, Winner.PLAYER_B);
					break;
    		}
    		
    		return;
    	}
    	
    	// Passo induttivo: calcolo la situazione ricorsivamente
    	Winner prec_result, att_player = s.turn==1 ? Winner.PLAYER_A : Winner.PLAYER_B;
    	SitEnc<P> next_enc;
    	boolean patta=false;
    	
		for( Situation<P> next_situation : gM.next.get(s).values() )
    	{
			next_enc = new SitEnc<>(gM, next_situation);
			
			computeStrategy(strategy, next_situation, next_enc, gM, interr);
			
			prec_result = strategy.get(next_enc);
			if(prec_result == att_player)
			{
				strategy.put(s_enc, att_player);
				return;
			}
			else if(prec_result == Winner.NONE)
				patta = true;
    	}
		
		if(patta)
			strategy.put(s_enc, Winner.NONE);
		else
			strategy.put(s_enc, s.turn==1 ? Winner.PLAYER_B : Winner.PLAYER_A);
    }
    
    /** Metodo che tenta di calcolare la strategia ottimale sfruttando il parallelismo, inserendola nella mappa data come parametro.
     * @param strategy mappa della strategia ottimale
     * @param s situazione iniziale
     * @param s_enc situazione iniziale codificata
     * @param next funzione per ottenere le prossime situazioni
     * @param interr supplier di interrupt
     * @throws NullPointerException in caso di interruzione
     */
    private void computeStrategyParallel(Map<SitEnc<P>, Winner> strategy, Situation<P> s, SitEnc<P> s_enc, Mechanics<P> gM, Supplier<Boolean> interr)
    {
    	// Controllo se è stato interrotto
    	if( Thread.currentThread().isInterrupted() || (interr!=null && interr.get()) )
    		throw new NullPointerException();
    	
    	// Caso base: situazione già calcolata
    	if( !strategy.containsKey(s_enc) )
    	{
    		// Caso base: situazione finale
        	if( s.turn <= 0 )
        	{
        		switch(s.turn)
        		{
        			case 0: strategy.put(s_enc, Winner.NONE);
        				break;
        			case -1: strategy.put(s_enc, Winner.PLAYER_A);
    					break;
        			case -2: strategy.put(s_enc, Winner.PLAYER_B);
    					break;
        		}
        	}
        	else
        	{
        		List<ForkJoinTask<Winner>> tasks = new ArrayList<>();
        		
        		for( Situation<P> next_situation : gM.next.get(s).values() )
        		{
        			tasks.add(ForkJoinTask.adapt( () -> {
        				if( Thread.currentThread().isInterrupted() || (interr!=null && interr.get()) )
        				{
        					tasks.forEach( (tt) -> tt.cancel(true) );
        					throw new NullPointerException();
        				}
        				
        				SitEnc<P> next_enc = new SitEnc<>(gM, next_situation);
        				
        				if( strategy.containsKey(s_enc) )
        				{
            				tasks.forEach( (tt) -> tt.cancel(true) );
            				return null;
            			}
        				
        				computeStrategyParallel(strategy, next_situation, next_enc, gM, interr);
        				
        				if( strategy.containsKey(s_enc) )
        				{
            				tasks.forEach( (tt) -> tt.cancel(true) );
            				return null;
            			}
        				else
        					return strategy.get(next_enc);
        				
        				}).fork());
        		}
        		
        		Winner prevision, att_player = s.turn==1 ? Winner.PLAYER_A : Winner.PLAYER_B;
        		boolean patta = false;
        		
        		for( ForkJoinTask<Winner> t : tasks )
        		{
        			try
        			{
        				prevision = t.join();
            			
            			if( prevision == null )
            			{
            				tasks.forEach( (tt) -> tt.cancel(true) );
            				return;
            			}
            			if( prevision == att_player )
            			{
            				strategy.put(s_enc, att_player);
            				tasks.forEach( (tt) -> tt.cancel(true) );
            				return;
            			}
                		else if( prevision == Winner.NONE )
                			patta = true;
        			}
        			catch( CancellationException e )
        			{
        				tasks.forEach( (tt) -> tt.cancel(true) );
        				return;
        			}
        			
        		}
        		
        		if(patta)
        			strategy.put(s_enc, Winner.NONE);
				else
					strategy.put(s_enc, s.turn==1 ? Winner.PLAYER_B : Winner.PLAYER_A);
        	}
    	}
    }
    
    /** Se il metodo {@link OptimalPlayerFactory#canPlay(GameFactory)} ritorna
     * {@link Play#YES} tenta di creare un {@link OptimalPlayer} con la strategia
     * per il gioco specificato cercandola tra quelle in memoria e se la directory
     * è impostata ({@link OptimalPlayerFactory#setDir(Path)}) anche nel file. */
    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name)
    {
    	if( this.canPlay(gF) != Play.YES )
    		throw new IllegalStateException();
        
    	Objects.requireNonNull(name);
    	
    	// I nomi sono già stati settati nel metodo canPlay
    	GameRuler<P> gR = gF.newGame();
    	
    	OptimalStrategy<P> strategy = this.getStrategy(gR.name(), gR.mechanics());
    	
    	// Il flag del parallelismo non è unato, poiché per il getMove non vengono mai usati threads addizionali
    	
    	return new OptimalPlayer<>(name, strategy);
    }
    
    /** Cerca una strategia disponibile per un gioco (in memoria o nei file)
     * @param gName nome del gioco
     * @param gM meccanica del gioco
     * @return la strategia ottimale
     * @throws IllegalStateException in caso di file non trovato o errori di conversione
     */
    private OptimalStrategy<P> getStrategy(String gName, Mechanics<P> gM)
    {
    	// Cerca tra le strategie in memoria
    	for(OptimalStrategy<P> s : this.strategies)
    		if( s.gName().equals(gName) )
    			return s;
    	
    	// Se non c'è in memoria, cerca un file che la contenga
		return this.loadStrategy(gName, gM);
    }
    
    /** Ritorna una strategia letta da un file .dat, dato il nome del gioco.
     * @param gName nome del gioco
     * @param gM meccanica del gioco
     * @return la strategia letta dal file
     * @throws IllegalStateException in caso di file non trovato o errori di conversione
     */
    @SuppressWarnings("unchecked")
	private OptimalStrategy<P> loadStrategy(String gName, Mechanics<P> gM)
    {
    	if( this.strategies_dir==null )
    		throw new IllegalStateException();
    	
    	Path file = Paths.get(this.strategies_dir.toString(), "strategy_"+gName+".dat");
    	
    	try( ObjectInputStream in = new ObjectInputStream(new FileInputStream(file.toFile())) )
    	{
    		Map<SitEnc<P>,Winner> strategy_map = (Map<SitEnc<P>,Winner>) in.readObject();
    		
    		OptimalStrategy<P> strategy = new OptimalStrategy<>(gM, gName, strategy_map);
    		
    		this.strategies.add(strategy);
    		
    		return strategy;
		}
    	catch(ClassNotFoundException | IOException e)
    	{
    		throw new IllegalStateException();
		}
    }
    
    /** Salva una strategia in un file .dat, usando un nome specificato.
     * @param gName nome del gioco
     * @param strategy_map mappa della strategia da salvare
     * @throws IllegalStateException in caso di file non trovato o errori di conversione
     */
	private void saveStrategy(String gName, Map<SitEnc<P>,Winner> strategy_map)
    {
    	if( this.strategies_dir==null )
    		return;
		
		Path file = Paths.get(this.strategies_dir.toString(), "strategy_"+gName+".dat");
    	
    	try( ObjectOutputStream in = new ObjectOutputStream(new FileOutputStream(file.toFile())) )
    	{
    		in.writeObject(strategy_map);
		}
    	catch(IOException e){}
    }
}
