package gapp.ulg.games;

import gapp.ulg.game.board.*;
import gapp.ulg.game.board.Board.Dir;
import gapp.ulg.game.board.Move.Kind;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;

import java.util.*;
import java.util.function.Function;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto Othello rappresenta un GameRuler per fare una partita a Othello. Il
 * gioco Othello si gioca su una board di tipo {@link Board.System#OCTAGONAL} 8x8.
 * Si gioca con pezzi o pedine di specie {@link Species#DISC} di due
 * colori "nero" e "bianco". Prima di inziare a giocare si posizionano due pedine
 * bianche e due nere nelle quattro posizioni centrali della board in modo da creare
 * una configurazione a X. Quindi questa è la disposzione iniziale (. rappresenta
 * una posizione vuota, B una pedina bianca e N una nera):
 * <pre>
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . B N . . .
 *     . . . N B . . .
 *     . . . . . . . .
 *     . . . . . . . .
 *     . . . . . . . .
 * </pre>
 * Si muove alternativamente (inizia il nero) appoggiando una nuova pedina in una
 * posizione vuota in modo da imprigionare, tra la pedina che si sta giocando e
 * quelle del proprio colore già presenti sulla board, una o più pedine avversarie.
 * A questo punto le pedine imprigionate devono essere rovesciate (da bianche a nere
 * o viceversa, azione di tipo {@link Action.Kind#SWAP}) e diventano
 * di proprietà di chi ha eseguito la mossa. È possibile incastrare le pedine in
 * orizzontale, in verticale e in diagonale e, a ogni mossa, si possono girare
 * pedine in una o più direzioni. Sono ammesse solo le mosse con le quali si gira
 * almeno una pedina, se non è possibile farlo si salta il turno. Non è possibile
 * passare il turno se esiste almeno una mossa valida. Quando nessuno dei giocatori
 * ha la possibilità di muovere o quando la board è piena, si contano le pedine e si
 * assegna la vittoria a chi ne ha il maggior numero. Per ulteriori informazioni si
 * può consultare
 * <a href="https://it.wikipedia.org/wiki/Othello_(gioco)">Othello</a> */
public class Othello implements GameRuler<PieceModel<Species>>
{
    /** Limite di tempo per una mossa */
	private final long time;
	/** Dimensione della board */
	private final int size;
	/** Lista giocatori di Othello */
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
	
	/** Crea un GameRuler per fare una partita a Othello, equivalente a
     * {@link Othello#Othello(long, int, String, String) Othello(0,8,p1,p2)}.
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se p1 o p2 è null */
    public Othello(String p1, String p2)
    {
    	Objects.requireNonNull(p1);
    	Objects.requireNonNull(p2);
    	
    	// Tempo per una mossa
    	this.time = -1;
    	// Dimensione board
    	this.size = 8;
    	// Lista dei giocatori
    	this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
    	
    	this.board = new BoardOct<>(8,8);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Mette i 4 pezzi iniziali al centro della board
    	this.board.put( this.black_disc, new Pos(3,3) );
    	this.board.put( this.black_disc, new Pos(4,4) );
    	this.board.put( this.white_disc, new Pos(3,4) );
    	this.board.put( this.white_disc, new Pos(4,3) );
    	
    	// Inizia il nero
    	this.ally_piece = this.black_disc;
    	this.enemy_piece = this.white_disc;
    	
    	this.history = new ArrayList<>();
    	
    	// Salvo la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, this::next_situation);
    }

    /** Crea un GameRuler per fare una partita a Othello.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param size  dimensione della board, sono accettati solamente i valori 6,8,10,12
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se size non è uno dei valori 6,8,10 o 12 */
    public Othello(long time, int size, String p1, String p2)
    {
    	Objects.requireNonNull(p1);
    	Objects.requireNonNull(p2);
    	
    	if( size != 6 && size != 8 && size != 10 && size != 12 )
    		throw new IllegalArgumentException();
    	
    	// Tempo per una mossa
    	this.time = time;
    	// Dimensione board
    	this.size = size;
    	// Lista dei giocatori
    	this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
    	
    	this.board = new BoardOct<>(this.size,this.size);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Coordinate dei pezzi centrali
    	int cord1 = this.size/2-1;
    	int cord2 = this.size/2;
    	
    	// Mette i 4 pezzi iniziali al centro della board
    	this.board.put( this.black_disc, new Pos(cord1,cord1) );
    	this.board.put( this.black_disc, new Pos(cord2,cord2) );
    	this.board.put( this.white_disc, new Pos(cord1,cord2) );
    	this.board.put( this.white_disc, new Pos(cord2,cord1) );
    	
    	// Inizia il nero
    	this.ally_piece = this.black_disc;
    	this.enemy_piece = this.white_disc;
    	
    	this.history = new ArrayList<>();
    	
    	// Salvo la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, this::next_situation);
    }
    
    /**
     * Costruttore che crea un nuovo GameRuler di Othello a partire da uno esistente
     * @param ot GameRuler del gioco Othello
     * @throw NullPointerException se ot è null
     */
    private Othello(Othello ot)
    {
    	Objects.requireNonNull(ot);
    	
    	this.time = ot.time;
    	this.size = ot.size;
    	this.players = ot.players;
    	
    	this.board = ot.board.copy();
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	this.game_result = ot.game_result;
    	this.current_turn = ot.current_turn;
    	
    	this.enemy_piece = ot.enemy_piece;
    	this.ally_piece = ot.ally_piece;
    	
    	this.history = new ArrayList<>(ot.history);
    	
    	this.start_situation = ot.start_situation;
    	this.mechanics = ot.mechanics;
    }

    /** Il nome rispetta il formato:
     * <pre>
     *     Othello<i>Size</i>
     * </pre>
     * dove <code><i>Size</i></code> è la dimensione della board, ad es. "Othello8x8". */
    @Override
    public String name()
    {
        return "Othello" + this.size + "x" + this.size;
    }

    @Override
    public <T> T getParam(String name, Class<T> c)
    {
    	Objects.requireNonNull(name);
    	Objects.requireNonNull(c);
    	
		switch(name)
		{
			case "Time": 	return c.cast( time <= 0 ? "No limit" : (time>=60000 ? (time/60000)+"m" : (time/1000)+"s") );
			case "Board":	return c.cast( size + "x" + size );
			default:		throw new IllegalArgumentException();
		}
    }

    @Override
    public List<String> players() { return this.players; }

    /** Assegna il colore "nero" al primo giocatore e "bianco" al secondo. */
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

    /** Se il giocatore di turno non ha nessuna mossa valida il turno è
     * automaticamente passato all'altro giocatore. Ma se anche l'altro giuocatore
     * non ha mosse valide, la partita termina. */
    @Override
    public int turn()
    {
    	if( this.game_result == -1 )
        	return this.current_turn;
        else
        	return 0;
    }

    /** Se la mossa non è valida termina il gioco dando la vittoria all'altro
     * giocatore. */
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
			
			// Esegui SWAP
			for( Pos p : m.actions.get(1).pos )
				this.board.put(this.ally_piece, p);
			
			// Aggiungo la mossa alla history
			this.history.add(m);
			
			// Passo il turno al giocatore successivo
			this.update_turn();
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
    		// Se è una ACTION, annullala
    		
    		this.remove_action();
    	}
    	else if( m.kind == Kind.PASS )
    	{
    		// Se è una PASS, rimuovila (insieme ad un altra eventuale PASS) e annulla la ACTION che c'è prima
    		
    		this.previous_turn();
    		
    		if( this.history.get(this.history.size()-1).kind == Kind.PASS )
    			this.history.remove(this.history.size()-1);
    		
    		this.remove_action();
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

    /** Ogni mossa, eccetto l'abbandono, è rappresentata da una {@link Action} di tipo
     * {@link Action.Kind#ADD} seguita da una {@link Action} di tipo
     * {@link Action.Kind#SWAP}. */
    @Override
    public Set<Move<PieceModel<Species>>> validMoves()
    {
    	if( this.game_result != -1 )
        	throw new IllegalStateException();
    	
    	Set<Move<PieceModel<Species>>> moves_set = new HashSet<>();
    	
    	Pos p_search;
    	Set<Pos> swap_set, swap_set_dir;
    	
    	// Per ogni posizione...
    	for( Pos p_add : this.board.positions() )
    	{
    		// ...vuota...
    		if( this.board.get(p_add) != null )
    			continue;
    		
			swap_set = new HashSet<>();
			
			// ...cerca in ogni direzione una pedina avversaria adiacente
        	for( Dir d : Dir.values() )
        	{
        		swap_set_dir = new HashSet<>();
        		
        		p_search = this.board.adjacent(p_add, d);
    				
				// Fino a quando trovi pedine avversarie contigue...
				while( p_search != null && this.enemy_piece.equals(this.board.get(p_search)) )
				{
					// ...metti le loro posizioni nell'insieme e cercane altre nella stessa direzione
					swap_set_dir.add(p_search);
					p_search = this.board.adjacent(p_search, d);
				}
    				
				// Se l'ultima posizione trovata è nella board e contiene un pezzo alleato, allora vuol dire che 
				// tra le due pedine alleate trovate c'è una fila continua di pedine avversarie che possono essere girate.
				// Inserisce quindi in swap_set tutte le posizioni trovate.
				if( p_search != null && this.ally_piece.equals(this.board.get(p_search))  )
					swap_set.addAll(swap_set_dir);
        	}
        	
        	// Se ci sono pedine da girare, allora crea la mossa
        	if( !swap_set.isEmpty() )
        		moves_set.add( new Move<>( new Action<>(p_add, this.ally_piece), new Action<>(this.ally_piece, swap_set.toArray(new Pos[swap_set.size()])) ) );
    	}
    	
    	// Se non sono state trovate mosse, ritorna un insieme vuoto
        if( moves_set.isEmpty() )
        	return Collections.unmodifiableSet(Collections.emptySet());
        
        moves_set.add(new Move<>(Kind.RESIGN));
        
        return Collections.unmodifiableSet(moves_set);
    }
    
    /** Versione di validMoves che funziona in modo indipendente dall'oggetto attuale
     * @param free_pos inseieme delle posizioni libere
     * @param b_get funzione per ottenere il contenuto di una posizione
     * @param t turno attuale
     * @return l'insieme delle mosse valide
     */
    private Set<Move<PieceModel<Species>>> validMoves(Function<Pos,PieceModel<Species>> b_get, int t)
    {
    	Set<Move<PieceModel<Species>>> moves_set = new HashSet<>();
    	
    	Pos p_search;
    	Set<Pos> swap_set, swap_set_dir;
    	PieceModel<Species> ally, enemy;
    	
    	// Setta i pezzi alleati e nemici
    	if(t==1)
    	{
    		ally = this.black_disc;
    		enemy = this.white_disc;
    	}
    	else
    	{
    		ally = this.white_disc;
    		enemy = this.black_disc;
    	}
    	
    	// Per ogni posizione libera...
    	for( Pos p_add : this.board.positions() )
    	{
			if(b_get.apply(p_add)!=null)
				continue;
    		
    		swap_set = new HashSet<>();
			
			// ...cerca in ogni direzione una pedina avversaria adiacente
        	for( Dir d : Dir.values() )
        	{
        		swap_set_dir = new HashSet<>();
        		
        		p_search = this.board.adjacent(p_add, d);
    				
				// Fino a quando trovi pedine avversarie contigue...
				while( p_search != null && enemy.equals(b_get.apply(p_search)) )
				{
					// ...metti le loro posizioni nell'insieme e cercane altre nella stessa direzione
					swap_set_dir.add(p_search);
					p_search = this.board.adjacent(p_search, d);
				}
    				
				// Se l'ultima posizione trovata è nella board e contiene un pezzo alleato, allora vuol dire che 
				// tra le due pedine alleate trovate c'è una fila continua di pedine avversarie che possono essere girate.
				// Inserisce quindi in swap_set tutte le posizioni trovate.
				if( p_search != null && ally.equals(b_get.apply(p_search))  )
					swap_set.addAll(swap_set_dir);
        	}
        	
        	// Se ci sono pedine da girare, allora crea la mossa
        	if( !swap_set.isEmpty() )
        		moves_set.add( new Move<>( new Action<>(p_add, ally), new Action<>(ally, swap_set.toArray(new Pos[swap_set.size()])) ) );
    	}
    	
        return moves_set;
    }
    
    @Override
    public double score(int i)
    {
    	if (i != 1 && i != 2)
    		throw new IllegalArgumentException();
    	
    	if( i == 1 )
        	return this.board.get(this.black_disc).size();
        else
        	return this.board.get(this.white_disc).size();
    }

    @Override
    public GameRuler<PieceModel<Species>> copy() { return new Othello(this); }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return this.mechanics; }
    
    /** Aggiorna l'indice del giocatore attuale e successivo, 
     * e quale tipo di pezzo è il nemico e quale è l'alleato */
    private void update_turn()
    {
    	// Aggiorna l'indice del giocatore attuale
    	this.current_turn = 3 - this.current_turn;
    	
    	// Aggiorna il tipo di pezzo alleato e nemico
    	this.ally_piece = this.current_turn == 1 ? this.black_disc : this.white_disc;
    	this.enemy_piece = this.current_turn == 1 ? this.white_disc : this.black_disc;
    	
    	// Se non ci sono mosse valide
    	if( this.validMoves().isEmpty() )
    	{
    		// Il giocatore passa il turno
    		this.history.add(new Move<>(Kind.PASS));
    		
    		// Riaggiorna l'indice del giocatore attuale
        	this.current_turn = 3 - this.current_turn;
        	
        	// Riaggiorna il tipo di pezzo alleato e nemico
        	this.ally_piece = this.current_turn == 1 ? this.black_disc : this.white_disc;
        	this.enemy_piece = this.current_turn == 1 ? this.white_disc : this.black_disc;
        	
        	// Se non ci sono ancora mosse valide, chiudi il gioco
        	if( this.validMoves().isEmpty() )
        	{
        		this.history.add(new Move<>(Kind.PASS));
        		this.close_game();
        	}
    	}
    }
    
    /** Termina il gioco e calcola il vincitore */
    private void close_game()
    {
    	// Calcola il punteggio
    	double black_score = this.score(1);
    	double white_score = this.score(2);
    	
    	// Determina il risultato e chiude il gioco
    	if( black_score == white_score )
    		this.game_result = 0;
    	else if( black_score > white_score )
    		this.game_result = 1;
    	else
    		this.game_result = 2;
    }
    
    /** Rimuove l'ultima mossa dalla history e ripristina l'indice di turnazione precedente
     * e lo stato precedente del pezzo alleato e nemico */
    private void previous_turn()
    {
    	// Rimuovo l'ultima mossa dalla history
    	this.history.remove(this.history.size()-1);
    	
    	// Ripristina il turno precedente
		this.current_turn = this.current_turn == 1 ? 2 : 1;
    	
		// Ripristina il settaggio precedente dei pezzi alleati e nemici
    	this.ally_piece = this.current_turn == 1 ? this.black_disc : this.white_disc;
    	this.enemy_piece = this.current_turn == 1 ? this.white_disc : this.black_disc;
    }
    
    /** Se l'ultima mossa della history è una ACTION, la annulla e la rimuove, tornando allo stato precedente.
     * In caso contrario, il comportamento è errato (non ci sono controlli per ridurre i tempi).
     */
    private void remove_action()
    {
    	Move<PieceModel<Species>> m = this.history.get(this.history.size()-1);
    	
    	// Remove ADD, e ottiene il tipo della pedina rimossa
		Pos add_position = m.actions.get(0).pos.get(0);
		PieceModel<Species> pm = this.board.remove(add_position);
		
		// Remove SWAP: se il pezzo rimosso era nero, 
		// fai diventare le pedine bianche, e viceversa
		if( this.black_disc.equals(pm) )
			pm = this.white_disc;
		else
			pm = this.black_disc;
		
		// Inverti lo swap
		for( Pos p : m.actions.get(1).pos )
			this.board.put(pm, p);
		
		// Rimuove la mossa dalla history e ripristina stato precedente
		this.previous_turn();
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
		Action<PieceModel<Species>> swap, add;
		int next_turn, score_black, score_white;
		
		for(Move<PieceModel<Species>> move : validMoves(s::get, s.turn))
		{
			next_board = s.newMap();
			
			// Esegue ADD
			add = move.actions.get(0);
			next_board.put(add.pos.get(0), add.piece);
			
			// Esegue SWAP
			swap = move.actions.get(1);
			for(Pos p : swap.pos)
				next_board.put(p, swap.piece);
			
			next_turn = 3 - s.turn;
			
			// Una vittoria non è possibile con meno di 10 pedine sulla board (da Othello 6x6 in poi)
			if( next_board.size()>=10 )
			{
				if(validMoves(next_board::get, next_turn).isEmpty())
				{
					next_turn = 3 - next_turn;
					
					if(validMoves(next_board::get, next_turn).isEmpty())
					{
						score_black=0;
						score_white=0;
						
						for(PieceModel<Species> p : next_board.values())
						{
							if( this.black_disc.equals(p) )
								score_black++;
							else
								score_white++;
						}
						
						if(score_black==score_white)
							next_turn = 0;
						else if(score_black>score_white)
							next_turn = -1;
						else
							next_turn = -2;
					}
					
				}
			}
			
			possibilites.put(move, new Situation<>(next_board, next_turn));
		}
		
		return Collections.unmodifiableMap(possibilites);
	}
}
