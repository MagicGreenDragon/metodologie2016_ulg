package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.board.Board.Dir;
import gapp.ulg.game.board.Move.Kind;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto {@code MNKgame} rappresenta un GameRuler per fare una partita a un
 * (m,n,k)-game, generalizzazioni del ben conosciuto Tris o Tic Tac Toe.
 * <br>
 * Un gioco (m,n,k)-game si gioca su una board di tipo {@link Board.System#OCTAGONAL}
 * di larghezza (width) m e altezza (height) n. Si gioca con pezzi o pedine di specie
 * {@link Species#DISC} di due colori "nero" e "bianco". All'inizio la board è vuota.
 * Poi a turno ogni giocatore pone una sua pedina in una posizione vuota. Vince il
 * primo giocatore che riesce a disporre almeno k delle sue pedine in una linea di
 * posizioni consecutive orizzontale, verticale o diagonale. Chiaramente non è
 * possibile passare il turno e una partita può finire con una patta.
 * <br>
 * Per ulteriori informazioni si può consultare
 * <a href="https://en.wikipedia.org/wiki/M,n,k-game">(m,n,k)-game</a> */
public class MNKgame implements GameRuler<PieceModel<Species>>
{
	/** Limite di tempo per una mossa */
	private final long time;
	/** Larghezza della board */
    private final int m;
    /** Altezza della board */
    private final int n;
    /** Lunghezza minima della riga per la vittoria */
    private final int k;
	/** Lista dei giocatori */
    private final List<String> players;
    
    /** Disco nero */
    private final PieceModel<Species> black_disc = new PieceModel<>(Species.DISC, "nero");
    /** Disco Bianco */
    private final PieceModel<Species> white_disc = new PieceModel<>(Species.DISC, "bianco");
    /** Lista immutabile dei pezzi */
    private final List<PieceModel<Species>> pieces_list = Collections.unmodifiableList(Arrays.asList(this.black_disc, this.white_disc));
	
    /** Situazione iniziale del gioco */
	private final Situation<PieceModel<Species>> start_situation;
	/** Meccanica del gioco */
	private final Mechanics<PieceModel<Species>> mechanics;
    
    /** Board di Othello */
	private final BoardOct<PieceModel<Species>> board;
	/** View immodificabile della Board di Othello */
	private final Board<PieceModel<Species>> board_view;
    
	/** Stato corrente del gioco */
    private int game_result = -1;
    /** Indice del giocatore di turno */
    private int current_turn = 1;
    
    /** Tipo di pezzo alleato per il giocatore di turno */
    private PieceModel<Species> ally_piece;
    /** Tipo di pezzo nemico per il giocatore di turno */
    private PieceModel<Species> enemy_piece;
	
	/** Lista delle mosse fatte in precedenza */
	private List<Move<PieceModel<Species>>> history;
	
	/** Crea un {@code MNKgame} con le impostazioni date.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param m  larghezza (width) della board
     * @param n  altezza (height) della board
     * @param k  lunghezza della linea
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se i valori di {@code m,n,k} non soddisfano
     * le condizioni 1 <= {@code k} <= max{{@code M,N}} <= 20 e 1 <= min{{@code M,N}} */
    public MNKgame(long time, int m, int n, int k, String p1, String p2)
    {
        Objects.requireNonNull(p1);
        Objects.requireNonNull(p2);
        
        int min = Math.min(m,n);
        int max = Math.max(m,n);
        
        if( !( k >= 1 && k <= max && max <= 20 && min >= 1 ) )
        	throw new IllegalArgumentException();
        
        // Tempo per una mossa
    	this.time = time;
        // Larghezza board
    	this.m = m;
        // Altezza board
        this.n = n;
        // Lunghezza minima della riga per la vittoria
        this.k = k;
        // Lista dei giocatori
        this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
        
        this.board = new BoardOct<>(m,n);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Inizia il nero
    	this.ally_piece = this.black_disc;
    	this.enemy_piece = this.white_disc;
    	
    	this.history = new ArrayList<>();
    	
        // Definisco la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, this::next_situation);
    }
    
    /**
     * Costruttore che crea un nuovo GameRuler di MNKgame a partire da uno esistente
     * @param mnk GameRuler del gioco MNKgame
     * @throws NullPointerException se mnk è null
     */
    private MNKgame(MNKgame mnk)
    {
    	Objects.requireNonNull(mnk);
    	
    	this.time = mnk.time;
    	this.m = mnk.m;
        this.n = mnk.n;
        this.k = mnk.k;
    	this.players = mnk.players;
    	
    	this.board = mnk.board.copy();
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	this.game_result = mnk.game_result;
    	this.current_turn = mnk.current_turn;
    	
    	this.enemy_piece = mnk.enemy_piece;
    	this.ally_piece = mnk.ally_piece;
    	
    	this.history = new ArrayList<>(mnk.history);
    	
    	this.start_situation = mnk.start_situation;
    	
    	this.mechanics = mnk.mechanics;
    }

    /** Il nome rispetta il formato:
     * <pre>
     *     <i>M,N,K</i>-game
     * </pre>
     * dove <code><i>M,N,K</i></code> sono i valori dei parametri M,N,K, ad es.
     * "4,5,4-game". */
    @Override
    public String name()
    {
    	return this.m + "," + this.n + "," + this.k + "-game";
    }
    
	@Override
    public <T> T getParam(String name, Class<T> c)
    {
    	Objects.requireNonNull(name);
    	Objects.requireNonNull(c);
    	
		switch(name)
		{
			case "Time": 	return c.cast( time <= 0 ? "No limit" : (time>=60000 ? (time/60000)+"m" : (time/1000)+"s") );
			case "M":		return c.cast( Integer.valueOf(this.m) );
			case "N":		return c.cast( Integer.valueOf(this.n) );
			case "K":		return c.cast( Integer.valueOf(this.k) );
			default:		throw new IllegalArgumentException();
		}
    }

    @Override
    public List<String> players() { return this.players; }

    /** @return il colore "nero" per il primo giocatore e "bianco" per il secondo */
    @Override
    public String color(String name)
    {
    	Objects.requireNonNull(name);
    	
    	if( !this.players.contains(name) )
        	throw new IllegalArgumentException();
    	
        if( this.players.get(0) == name )
        	return "nero";
        else
        	return "bianco";
    }

    @Override
    public Board<PieceModel<Species>> getBoard() { return this.board_view; }

    @Override
    public int turn()
    {
    	if( this.game_result == -1 )
        	return this.current_turn;
        else
        	return 0;
    }

    /** Se la mossa non è valida termina il gioco dando la vittoria all'altro
     * giocatore.
     * Se dopo la mossa la situazione è tale che nessuno dei due giocatori può
     * vincere, si tratta quindi di una situazione che può portare solamente a una
     * patta, termina immediatamente il gioco con una patta. Per determinare se si
     * trova in una tale situazione controlla che nessun dei due giocatori può
     * produrre una linea di K pedine con le mosse rimanenti (in qualsiasi modo siano
     * disposte le pedine rimanenti di entrambi i giocatori). */
    @Override
    public boolean move(Move<PieceModel<Species>> m)
    {
    	Objects.requireNonNull(m);
    	
    	if( this.game_result != -1 )
    		throw new IllegalStateException();
    	
    	// Se la mossa non è valida, da la vittoria ll'altro giocatore e ritorna false
    	if( !isValid(m) )
    	{
    		this.game_result = 3 - this.current_turn;
    		return false;
    	}
    	
    	// Se la mossa è una ACTION eseguila, altrimenti termina il gioco dando la vittoria all'altro giocatore
    	if( m.kind == Kind.ACTION )
    	{
    		// Esegui ADD
			Pos add_position = m.actions.get(0).pos.get(0);
			this.board.put(this.ally_piece, add_position);
			
			// Aggiungo la mossa alla history
			this.history.add(m);
			
			if( this.checkVictory() )
			{
				// Se un giocatore ha vinto, chiudo il gioco
				this.game_result = this.current_turn;
			}
			else if( this.canPlayFurther(1) || this.canPlayFurther(2) )
			{
				// Se si può ancora giocare, passo il turno al giocatore successivo
				this.current_turn = 3 - this.current_turn;
				
				// Aggiorna il tipo di pezzo alleato e nemico
		    	this.ally_piece = this.current_turn == 1 ? this.black_disc : this.white_disc;
		    	this.enemy_piece = this.current_turn == 1 ? this.white_disc : this.black_disc;
			}
			else
			{
				// Se la patta è l'unica fine possibile, allora termina subito con una patta
				this.game_result = 0;
			}
    	}
    	else
    	{
    		this.history.add(new Move<>(Kind.RESIGN));
    		
    		this.game_result = 3 - this.current_turn;
    	}
    	
    	return true;
    }

    @Override
    public boolean unMove()
    {
    	if( this.history.isEmpty() )
        	return false;
        
    	// Recupero l'ultima mossa fatta
    	Move<PieceModel<Species>> m = this.history.get(this.history.size()-1);
    	
    	if( m.kind == Kind.ACTION )
    	{
    		// Se è una ACTION, rimuovila dalla history
    		this.history.remove(this.history.size()-1);
    		
    		// Annullala
			this.board.remove( m.actions.get(0).pos.get(0) );
			
			// Se il gioco è già attivo
			if( this.result() == -1 )
			{
				// Ripristina il turno precedente
				this.current_turn = this.current_turn == 1 ? 2 : 1;
		    	
				// Ripristina il settaggio precedente dei pezzi alleati e nemici
		    	this.ally_piece = this.current_turn == 1 ? this.black_disc : this.white_disc;
		    	this.enemy_piece = this.current_turn == 1 ? this.white_disc : this.black_disc;
			}
    	}
    	else
    	{
    		// Altrimenti è una RESIGN, quindi rimuovila (non serve tornare al turno precedente)
    		this.history.remove(this.history.size()-1);
    	}
    	
		// Il gioco è attivo
		this.game_result = -1;
		
    	return true;
    }

    @Override
    public boolean isPlaying(int i)
    {
    	if (i != 1 && i != 2)
    		throw new IllegalArgumentException();
        
        return this.game_result == -1;
    }

    @Override
    public int result() { return this.game_result; }

    /** Ogni mossa (diversa dall'abbandono) è rappresentata da una sola {@link Action}
     * di tipo {@link Action.Kind#ADD}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves()
    {
    	if( this.game_result != -1 )
        	throw new IllegalStateException();
    	
    	Set<Move<PieceModel<Species>>> moves_set = new HashSet<>();
    	
    	for( Pos p : this.board.positions() )
        {
    		if( this.board.get(p) == null )
    			moves_set.add( new Move<>(new Action<>(p, this.ally_piece) ));
        }
    	
    	if( !moves_set.isEmpty() )
    		moves_set.add(new Move<>(Kind.RESIGN));
    	
    	return Collections.unmodifiableSet( moves_set );
    }
    
    /** Versione generica di validMoves */
    private Set<Move<PieceModel<Species>>> validMoves(Map<Pos,PieceModel<Species>> board_map, PieceModel<Species> ally)
    {
    	Set<Move<PieceModel<Species>>> moves_set = new HashSet<>();
    	
    	for( Pos p : this.board.positions() )
        {
    		if( board_map.get(p) == null )
    			moves_set.add( new Move<>(new Action<>(p, ally) ));
        }
    	
    	return moves_set;
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() { return new MNKgame(this); }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return this.mechanics; }
    
    /** Verifica se il giocatore attuale ha vinto 
     * @return true se il giocatore attuale ha vinto, false altrimenti
     */
    private boolean checkVictory()
    {
    	if( this.history.size()<this.k )
    		return false;
    	
    	Pos p_search;
    	int line_lenght;
    	
    	// Per ogni posizione della board occupata...
    	for( Pos p : this.board.get() )
    	{
    		// ...da un pezzo alleato...
    		if( this.ally_piece.equals(this.board.get(p)) )
    		{
	    		// ...e per ogni direzione di adiacenza
	    		for( Dir d : Dir.values() )
	        	{
	    			line_lenght = 1;
	    			p_search = this.board.adjacent(p, d);
	    			
	    			// Per ogni posizione della board nella medesima direzione occupata da un pezzo alleato...
	    			while( p_search != null && this.ally_piece.equals(this.board.get(p_search)) )
	    			{
	    				// Incrementa la linea e continua la ricerca
	    				line_lenght++;
	    				p_search = this.board.adjacent(p_search, d);
	    			}
	    			
	    			if( line_lenght >= this.k )
	    				return true;
	        	}
    		}
    	}
    	
    	return false;
    }
    
    /** Verifica se il giocatore attuale ha vinto (versione generica)
     * @param board_map mappa che rappresenta la board
     * @param ally modello di pezzo del giocatore attuale
     * @return true se il giocatore attuale ha vinto, false altrimenti
     */
    private boolean checkVictory(Map<Pos,PieceModel<Species>> board_map, PieceModel<Species> ally)
    {
    	if( board_map.size()<this.k )
    		return false;
    	
    	Pos p_search;
    	int line_lenght;
    	
    	// Per ogni posizione della board occupata...
    	for( Pos p : board_map.keySet() )
    	{
    		// ...da un pezzo alleato...
    		if( ally.equals(board_map.get(p)) )
    		{
	    		// ...e per ogni direzione di adiacenza
	    		for( Dir d : Dir.values() )
	        	{
	    			line_lenght = 1;
	    			p_search = this.board.adjacent(p, d);
	    			
	    			// Per ogni posizione della board nella medesima direzione occupata da un pezzo alleato...
	    			while( p_search != null && ally.equals(board_map.get(p_search)) )
	    			{
	    				// Incrementa la linea e continua la ricerca
	    				line_lenght++;
	    				p_search = this.board.adjacent(p_search, d);
	    			}
	    			
	    			if( line_lenght >= this.k )
	    				return true;
	        	}
    		}
    	}
    	
    	return false;
    }
    
    /** Verifica se nell'attuale situazione del gioco un dato giocatore può ancora vincere
     * @param player indice del giocatore
     * @return true se è possibile giocare ancora, false altrimenti
     */
    private boolean canPlayFurther(int player)
    {
    	if( this.history.size()<this.k )
    		return true;
    	
    	if( this.history.size()==this.m*this.n )
    		return false;
    	
    	int free_spaces = (this.m*this.n)-this.history.size();
    	int player_free_spaces;
    	
    	// Calcolo quante caselle può ancora riempire il giocatore selezionato
    	if( free_spaces%2 == 0 )
    		player_free_spaces = free_spaces/2;
    	else
    		player_free_spaces = player==1 ? (free_spaces/2)+1 : (free_spaces/2);
    	
    	// Se non ci sono posizioni vuote, allora può solo finire con una patta
    	if( player_free_spaces==0 )
    		return false;
    	
    	PieceModel<Species> pos_content, piece_adjacent, enemy = player == 1 ? this.white_disc : this.black_disc;
    	Pos p_search;
    	int line_lenght, available;
    	
    	// Per ogni posizione della board ...
    	for( Pos p : this.board.positions() )
    	{
    		pos_content = this.board.get(p);
    		
    		// ...vuota o con un pezzo alleato...
    		if( !enemy.equals(pos_content) )
    		{
    			// ...e per ogni direzione di adiacenza
	    		for( Dir d : Dir.values() )
	        	{
	    			line_lenght = 1;
	    			
	    			// Se la posizione di partenza è vuota, si usa subito una posizione
	    			available = pos_content==null ? player_free_spaces-1 : player_free_spaces;
	    			
	    			p_search = this.board.adjacent(p, d);
	    			
	    			// Per ogni posizione della board nella medesima direzione...
	    			while( p_search != null )
	    			{
	    				// ...verifica il contenuto della posizione
	    				piece_adjacent = this.board.get(p_search);
	    				
	    				// Se la posizione è vuota, posso usarla solo se ho pezzi disponibili
	    				if( piece_adjacent==null )
	    				{
	    					if( available>0 )
	    						available--;
	    					else
	    						break;
	    				}
	    				else if( enemy.equals(piece_adjacent) ) // Se nella posizione c'è una pedina nemica, termina
	    					break;
	    				
	    				// Se la pedina è alleata, aumenta la lunghezza della linea e continua la ricerca
	    				line_lenght++;
	    				p_search = this.board.adjacent(p_search, d);
	    			}
	    			
	    			if( line_lenght >= this.k )
	    				return true;
	        	}
    		}
    	}
    	
    	return false;
    }
    
    /** Verifica se nell'attuale situazione del gioco un dato giocatore può ancora vincere (versione generica)
     * @param board_map mappa che rappresenta la board
     * @param player indice del giocatore
     * @return true se è possibile giocare ancora, false altrimenti
     */
    private boolean canPlayFurther(Map<Pos,PieceModel<Species>> board_map, int player)
    {
    	if( board_map.size()<this.k )
    		return true;
    	
    	if( board_map.size()==this.m*this.n )
    		return false;
    	
    	int free_spaces = (this.m*this.n)-board_map.size();
    	int player_free_spaces;
    	
    	// Calcolo quante caselle può ancora riempire il giocatore selezionato
    	if( free_spaces%2 == 0 )
    		player_free_spaces = free_spaces/2;
    	else
    		player_free_spaces = player==1 ? (free_spaces/2)+1 : (free_spaces/2);
    	
    	// Se non ci sono posizioni vuote, allora può solo finire con una patta
    	if( player_free_spaces==0 )
    		return false;
    	
    	PieceModel<Species> pos_content, piece_adjacent, enemy = player==1 ? this.white_disc : this.black_disc;
    	Pos p_search;
    	int line_lenght, available;
    	
    	// Per ogni posizione della board ...
    	for( Pos p : this.board.positions() )
    	{
    		pos_content = board_map.get(p);
    		
    		// ...vuota o con un pezzo alleato...
    		if( !enemy.equals(pos_content) )
    		{
    			// ...e per ogni direzione di adiacenza
	    		for( Dir d : Dir.values() )
	        	{
	    			line_lenght = 1;
	    			
	    			// Se la posizione di partenza è vuota, si usa subito una posizione
	    			available = pos_content==null ? player_free_spaces-1 : player_free_spaces;
	    			
	    			p_search = this.board.adjacent(p, d);
	    			
	    			// Per ogni posizione della board nella medesima direzione...
	    			while( p_search != null )
	    			{
	    				// ...verifica il contenuto della posizione
	    				piece_adjacent = board_map.get(p_search);
	    				
	    				// Se la posizione è vuota, posso usarla solo se ho pezzi disponibili
	    				if( piece_adjacent==null )
	    				{
	    					if( available>0 )
	    						available--;
	    					else
	    						break;
	    				}
	    				else if( enemy.equals(piece_adjacent) ) // Se nella posizione c'è una pedina nemica, termina
	    					break;
	    				
	    				// Se la pedina è alleata, aumenta la lunghezza della linea e continua la ricerca
	    				line_lenght++;
	    				p_search = this.board.adjacent(p_search, d);
	    			}
	    			
	    			if( line_lenght >= this.k )
	    				return true;
	        	}
    		}
    	}
    	
    	return false;
    }

	/**
     * Ritorna l'oggetto {@code Situation} relativo allo stato attuale del gioco
     * @return l'oggetto {@code Situation} relativo allo stato attuale del gioco
     */
    private Situation<PieceModel<Species>> getSituation()
    {
        Map<Pos,PieceModel<Species>> c = new HashMap<>();
        
        for (Pos p : this.board.positions() )
            if (this.board.get(p) != null)
            	c.put(p, this.board.get(p));
        
        // Se la situazione è finale, ritorna l'opposto del risultato, altrimenti l'indice del turno corrente
        int situation_turn = this.game_result == -1 ? this.current_turn : -this.game_result;
        
        return new Situation<>(c, situation_turn);
    }
    
    /**Funzione che restituisce la mappa delle situazioni successive*/
    private Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> next_situation(Situation<PieceModel<Species>> s)
    {
    	Objects.requireNonNull(s);
		
		if(s.turn <= 0)
			return Collections.emptyMap();
		
		Map<Move<PieceModel<Species>>, Situation<PieceModel<Species>>> possibilites = new HashMap<>();
		Map<Pos, PieceModel<Species>> next_board;
		PieceModel<Species> ally = s.turn==1 ? this.black_disc : this.white_disc;
		int next_turn;
		
		for(Move<PieceModel<Species>> move : validMoves(s.newMap(), ally))
		{
			next_board = s.newMap();
			
			// Esegue ADD
			next_board.put(move.actions.get(0).pos.get(0), move.actions.get(0).piece);
			
			if( this.checkVictory(next_board, ally) )
			{
				// Se un giocatore ha vinto, chiudo il gioco
				next_turn = -s.turn;
			}
			else if( this.canPlayFurther(next_board, 1) || this.canPlayFurther(next_board, 2) )
			{
				// Se si può ancora giocare, passo il turno al giocatore successivo
				next_turn = 3 - s.turn;
			}
			else
			{
				// Se la patta è l'unica fine possibile, allora termina subito con una patta
				next_turn = 0;
			}
			
			possibilites.put(move, new Situation<>(next_board, next_turn));
		}
		
		return Collections.unmodifiableMap(possibilites);
	};
}
