package gapp.ulg.play;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import java.util.concurrent.RecursiveTask;

import gapp.ulg.game.board.*;
import gapp.ulg.game.board.Move.Kind;
import gapp.ulg.game.util.Utils;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto {@code MCTSPlayer} è un giocatore che gioca seguendo una strategia
 * basata su Monte-Carlo Tree Search e può giocare a un qualsiasi gioco.
 * <br>
 * La strategia che usa è una MCTS (Monte-Carlo Tree Search) piuttosto semplificata.
 * Tale strategia si basa sul concetto di <i>rollout</i> (srotolamento). Un
 * <i>rollout</i> a partire da una situazione di gioco <i>S</i> è l'esecuzione di
 * una partita fino all'esito finale a partire da <i>S</i> facendo compiere ai
 * giocatori mosse random.
 * <br>
 * La strategia adottata da un {@code MCTSPlayer}, è la seguente. In ogni situazione
 * di gioco <i>S</i> in cui deve muovere, prima di tutto ottiene la mappa delle
 * possibili mosse valide da <i>S</i> con le corrispondenti prossime situazioni. Per
 * ogni prossima situazione <i>NS</i> esegue <i>R</i> rollouts e calcola un punteggio
 * di <i>NS</i> dato dalla somma degli esiti dei rollouts. L'esito di un rollout è
 * rappresentato da un intero che è 0 se finisce in una patta, 1 se finisce con la
 * vittoria del giocatore e -1 altrimenti. Infine sceglie la mossa che porta nella
 * prossima situazione con punteggio massimo. Il numero <i>R</i> di rollouts da
 * compiere è calcolato così <i>R = ceil(RPM/M)</i>, cioè la parte intera superiore
 * della divisione decimale del numero di rollout per mossa <i>RPM</i> diviso il
 * numero <i>M</i> di mosse possibili (è sempre esclusa {@link Move.Kind#RESIGN}).
 * @param <P>  tipo del modello dei pezzi */
public class MCTSPlayer<P> implements Player<P>
{
	/** Nome del giocatore */
	private final String name;
	
	/** Copia del {@link GameRuler} del gioco */
    private GameRuler<P> g;
    /** Limite di tempo per una mossa (millisecondi) */
    private long time;
    
	/** Limite rollouts per mossa */
	private final int rpm;
	/** Flag per ricerca parallela delle mosse */
	private final boolean parallel;
    
	/** Flag per segnalare la presenza o meno di limiti all'esecuzione parallela */
	private boolean threadLimits;
    /** Numero massimo di threads addizionali */
    private int maxTh;
    /** Esecutore per le computazioni durante la chiamata di getMove() */
    private ForkJoinPool fjp;
    /** Esecutore per le computazioni in background */
    private ExecutorService bgExec;
    
    /** Risultato immutabile di un rollouts, con indicato la mossa di partenza e il punteggio ottenuto. 
     * @param <P>  tipo del modello dei pezzi
     * */
    private static class RolloutResult<P>
    {
    	/** Mossa che porta a questo punteggio */
    	public final Move<P> move;
    	/** Punteggio del rollout */
    	public final int score;
    	
    	/** Crea un oggetto per memorizzare il risultato di un rollout
    	 * @param m mossa che porta a questo punteggio
    	 * @param r punteggio del rollout
    	 */
    	public RolloutResult(Move<P> m, int r)
    	{
    		this.move=m;
    		this.score=r;
    	}
    }
    
    /** Task eseguito da un ForkJoinPool per scegliere una mossa da un insieme di mosse valide tramite la strategia MCTS.
     * @param <P> tipo del modello dei pezzi
     */
    @SuppressWarnings("serial")
	private static class MCTSTask<P> extends RecursiveTask<RolloutResult<P>>
    {
		/** Copia del {@link GameRuler} del gioco */
		private final GameRuler<P> gR;
		/** Insieme delle mosse valide */
    	private final Set<Move<P>> vm;
    	
    	/** Numero di rollouts */
    	private final int rollouts;
    	/** Tempo in cui si è cominciata la computazione */
    	private final long start_time;
    	/** Tempo limite (in millisecondi) */
    	private final long timeout;
    	
    	/** Risultato dei rollouts */
    	public RolloutResult<P> result;
    	
    	/** Crea un task per scegliere una mossa da un insieme di mosse valide in base alla strategia MCTS usando un ForkJoinPool.
    	 * @param gR copia del {@link GameRuler} del gioco
    	 * @param vm Insieme delle mosse valide
    	 * @param rollouts Numero di rollouts
    	 * @param start_time Tempo in cui si è cominciata la computazione
    	 * @param timeout Tempo limite (in millisecondi)
    	 * @throws NullPointerException se gR o vm è null
    	 * @throws IllegalArgumentException se vm è vuoto o se rollouts, start_time, o timeout è negativo
    	 */
    	public MCTSTask(GameRuler<P> gR, Set<Move<P>> vm, int rollouts, long start_time, long timeout)
    	{
    		Objects.requireNonNull(gR);
    		Objects.requireNonNull(vm);
    		
    		if( vm.isEmpty() || rollouts<=0 || start_time<=0 || rollouts<=0 )
    			throw new IllegalArgumentException();
    		
    		this.gR = gR;
    		this.vm = vm;
    		
    		this.rollouts = rollouts;
    		this.start_time = start_time;
    		this.timeout = timeout;
    		
    		this.result = new RolloutResult<>(vm.iterator().next(), Integer.MIN_VALUE);
    	}
    	
    	@Override
    	public RolloutResult<P> compute()
    	{
    		RolloutResult<P> mid_result;
    		
    		List<ForkJoinTask<RolloutResult<P>>> tasks = new ArrayList<>();
    		
    		for(Move<P> m : this.vm)
	    	{
	    		if(m.kind == Kind.RESIGN)
	    			continue;
	    		
	    		tasks.add( ForkJoinTask.adapt(() -> execRollouts(this.gR, m, this.rollouts, this.start_time, this.timeout)) );
	    	}
	    	
	    	try
	    	{
	    		for (ForkJoinTask<RolloutResult<P>> t : ForkJoinTask.invokeAll(tasks) )
	        	{
	    			if( Thread.currentThread().isInterrupted() || t.isCancelled() )
					{
						tasks.forEach( (tt) -> tt.cancel(true) );
						return this.result;
					}
	    			
	    			mid_result = t.join();
					
					if( Thread.currentThread().isInterrupted() || mid_result == null || Utils.timeoutExceeded(this.start_time, this.timeout) )
					{
						tasks.forEach( (tt) -> tt.cancel(true) );
						return this.result;
					}
	    			else if( mid_result.score > this.result.score )
	    				this.result = mid_result;
	    		}
	    	}
	    	catch( CancellationException e )
	    	{
	    		tasks.forEach( (tt) -> tt.cancel(true) );
	    		return this.result;
	    	}
    		
    		return this.result;
    	}
    }
    
    /**
     * Usa un ExecutorService per scegliere una mossa da un insieme di mosse valide tramite la strategia MCTS.
     * @param exec_pool ExecutorService usato
     * @param vm insieme di mosse valide
     * @param rollouts numero di rollouts
     * @param start_time tempo in cui si è cominciata la computazione
     * @return la mossa scelta
     */
    private RolloutResult<P> computeExecutorService(ExecutorService exec_pool, Set<Move<P>> vm, int rollouts, long start_time)
    {
    	RolloutResult<P> mid_result, att_move = new RolloutResult<>(vm.iterator().next(), Integer.MIN_VALUE);
    	
		List<Future<RolloutResult<P>>> tasks = new ArrayList<>();
		for(Move<P> m : vm)
    	{
    		if(m.kind == Kind.RESIGN)
				continue;
    		
    		tasks.add( exec_pool.submit( () -> execRollouts(this.g, m, rollouts, start_time, this.time) ) );
    	}
		
		try
		{
	        for (Future<RolloutResult<P>> t : tasks)
	        {
	        	if( Thread.currentThread().isInterrupted() || t.isCancelled() )
				{
					tasks.forEach( (tt) -> tt.cancel(true) );
					return att_move;
				}
	        	
	        	mid_result = t.get();
				
	        	if( Thread.currentThread().isInterrupted() || mid_result == null || Utils.timeoutExceeded(start_time, this.time) )
	        	{
	        		tasks.forEach( (tt) -> tt.cancel(true) );
	        		return att_move;
	        	}
				else if( mid_result.score > att_move.score )
					att_move = mid_result;
	        }
		}
		catch (InterruptedException | ExecutionException e)
		{
			tasks.forEach( (tt) -> tt.cancel(true) );
			
			return att_move;
		}
    	
    	return att_move;
    }
    
	/** Crea un {@code MCTSPlayer} con un limite dato sul numero di rollouts per
     * mossa.
     *
     * @param name  il nome del giocatore
     * @param rpm   limite sul numero di rollouts per mossa, se < 1 è inteso 1
     * @param parallel  se true la ricerca della mossa da fare è eseguita cercando
     *                  di sfruttare il parallelismo della macchina
     * @throws NullPointerException se {@code name} è null */
    public MCTSPlayer(String name, int rpm, boolean parallel)
    {
        Objects.requireNonNull(name);
        
    	this.name = name;
        this.rpm = rpm<1 ? 1 : rpm;
        this.parallel = parallel;
        
        this.threadLimits = false;
        this.maxTh = -1;
    	this.fjp = null;
    	this.bgExec = null;
    }

    @Override
    public String name() { return this.name; }

    @Override
    public void setGame(GameRuler<P> g)
    {
    	Objects.requireNonNull(g);
    	
        if( g.result() != -1 )
    		throw new IllegalArgumentException();
        
    	this.g = g;
    	
    	// Ottiene il limite di tempo per una mossa (aggiungendo un margine)
    	long t = this.g.mechanics().time;
    	this.time = t <= 0 ? 0 : t - 60;
    }

    @Override
    public void moved(int i, Move<P> m)
    {
    	Objects.requireNonNull(m);
    	
    	if( g == null || g.result() != -1 )
        	throw new IllegalStateException();
    	
    	g.isPlaying(i);
    	
    	if( !g.isValid(m) )
    		throw new IllegalArgumentException();
    	
    	g.move(m);
    }
    
    @Override
    public Move<P> getMove()
    {
    	// Se non c'è un gioco impostato, c'è ma è terminato, 
    	// o se questo non e' il turno del giocatore, solleva l'eccezione
    	if( this.g==null || this.g.result()!=-1 || !this.g.players().get(this.g.turn()-1).equals(this.name) )
        	throw new IllegalStateException();
    	
    	long start_time = System.currentTimeMillis();
    	
    	// Recupero l'insieme delle mosse valide, ne faccio una copia, ed escluso la mossa RESIGN
    	Set<Move<P>> vm = new HashSet<>();
    	vm.addAll(this.g.validMoves());
    	vm.remove(new Move<>(Kind.RESIGN));
    		
    	// Calcolo il numero di rollouts
    	int rollouts = (int)Math.ceil(this.rpm/vm.size());
    	
    	// Deve esserci sempre almeno un rollouts
    	if( rollouts <= 0 )
    		rollouts = 1;
    	
    	if( !this.parallel || ( this.threadLimits && this.maxTh==0 && this.fjp==null && this.bgExec==null ) )
    	{
    		// Esegue il calcolo sequenzialmente se è false il flag 'parallel' o se non è possibile usare ulteriori threads
    		
    		RolloutResult<P> mid_result, att_move = new RolloutResult<>(vm.iterator().next(), Integer.MIN_VALUE);
    		
    		for(Move<P> m : vm)
        	{
    			if(m.kind == Kind.RESIGN)
    				continue;
    			
    			mid_result = execRollouts(this.g, m, rollouts, start_time, this.time);
    			
        		if( mid_result == null )
        			return att_move.move;
    			else if( mid_result.score > att_move.score )
    				att_move = mid_result;
        	}
    		
    		return att_move.move;
    	}
    	else if( !this.threadLimits )
    	{
    		// Calcolo parallelo senza limitazioni (uso il commonPool di ForkJoin)
    		
    		MCTSTask<P> fjp_task = new MCTSTask<>(this.g.copy(), vm, rollouts, start_time, this.time);
    		
    		return ForkJoinPool.commonPool().invoke(fjp_task).move;
    	}
    	else if( this.maxTh!=0 || this.bgExec!=null )
    	{
    		// Uso un ExecutorService
    		
    		// Valuta se usare 'bgExec' o creare un nuovo ExecutorService con i threads permessi da 'maxTh'
    		boolean new_pool = false;
    		ExecutorService exec_pool;
    		if( this.maxTh<0 )
    		{
    			new_pool = true;
    			exec_pool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    		}
    		else if( this.bgExec==null && this.maxTh>0 )
    		{
    			new_pool = true;
    			exec_pool = Executors.newFixedThreadPool(this.maxTh);
    		}
    		else
    			exec_pool = this.bgExec;
    		
    		// Esegue il calcolo
    		Move<P> m = this.computeExecutorService(exec_pool, vm, rollouts, start_time).move;
    		
    		// Se l'ExecutorService è stato creato ex-novo, allora procedi al suo shutdown
    		if( new_pool )
    	    	exec_pool.shutdownNow();
    		
    		return m;
    	}
    	else
    	{
    		// Uso solo il ForkJoinPool
    		
    		MCTSTask<P> fjp_task = new MCTSTask<>(this.g.copy(), vm, rollouts, start_time, this.time);
    		
    		return this.fjp.invoke(fjp_task).move;
		}
    }
    
    @Override
    public void threads(int maxTh, ForkJoinPool fjp, ExecutorService bgExec)
    {
    	this.threadLimits = true;
    	this.maxTh = maxTh;
    	this.fjp = fjp;
    	this.bgExec = bgExec;
    }
    
    /** Esegue un dato numero di rollouts data una mossa da eseguire (lavorando su una copia del {@link GameRuler}).
     * @param g {@link GameRuler} dove eseguire i rollouts
     * @param m mossa da eseguire prima di fare i rollouts
     * @param rollouts numero di rollouts da eseguire
     * @param start_time tempo di inizio (per controllare il timeout)
     * @param timeout tempo limite (in millisecondi)
     * @return oggetto {@link RolloutResult} che rappresenta il risultato dei rollouts, o null in caso di interruzione e/o timeout
     */
    private static <P> RolloutResult<P> execRollouts(GameRuler<P> g, Move<P> m, int rollouts, long start_time, long timeout)
    {
    	if( Thread.currentThread().isInterrupted() || Utils.timeoutExceeded(start_time, timeout) )
			return null;
    	
    	// Crea una copia del GameRuler
    	GameRuler<P> gR = g.copy();
    	
    	// Ricava il turno attuale dal GameRuler
    	int turn = gR.turn();
    	
    	// Esegue la mossa indicata
    	gR.move(m);
    	
    	// Se la partita è già finita, il punteggio sarà sempre lo stesso (ripetuto per il numero di rollouts)
    	if( gR.result() != -1 )
    	{
    		if( gR.result() == 0)
        		return new RolloutResult<>(m, 0);
        	else
        		return gR.result()==turn ? new RolloutResult<>(m, rollouts) : new RolloutResult<>(m, -rollouts);
    	}
    	
    	int sum = 0, score;
    	
    	for( int i=0 ; i<rollouts ; i++ )
    	{
    		if( Thread.currentThread().isInterrupted() || Utils.timeoutExceeded(start_time, timeout) )
    			return null;
			
    		score = execRollout(gR.copy(), turn, start_time, timeout);
    		
    		if( score == Integer.MIN_VALUE )
    			return null;
    		
    		sum += score;
    	}
    	
    	return new RolloutResult<>(m, sum);
    }
    
    /** Esegue un rollout, controllando periodicamente interruzione e timeout, ritornandone il risultato.
     * @param gR copia del {@link GameRuler} dove eseguire il rollout
     * @param turn turno del giocatore attuale (per controllare il risultato)
     * @param start_time tempo di inizio (per controllare il timeout)
     * @param timeout tempo limite (in millisecondi)
     * @return risultato del rollout, o Integer.MIN_VALUE in caso di interruzione e/o timeout
     */
    @SuppressWarnings("unchecked")
	private static <P> int execRollout(GameRuler<P> gR, int turn, long start_time, long timeout)
    {
    	Set<Move<P>> vm = null;
    	Move<P>[] arr_moves = null;
    	
    	Random rand = new Random();
    	int i;
    	
    	while( gR.result() == -1 )
    	{
    		if( Thread.currentThread().isInterrupted() || Utils.timeoutExceeded(start_time, timeout) )
    			return Integer.MIN_VALUE;
    		
    		vm = gR.validMoves();
    		arr_moves = vm.toArray( new Move[vm.size()] );
        	
        	do
        	{
        		i = rand.nextInt(arr_moves.length);
        	}while( arr_moves[i].kind == Kind.RESIGN );
        	
        	gR.move(arr_moves[i]);
    	}
    	
    	if( gR.result() == 0)
    		return 0;
    	else
    		return gR.result()==turn ? 1 : -1;
    }
}
