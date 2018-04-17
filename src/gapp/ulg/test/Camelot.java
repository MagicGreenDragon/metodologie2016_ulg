package gapp.ulg.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import gapp.ulg.game.board.Action;
import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.Board.Dir;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.board.Move.Kind;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.Utils;


/**
 * GameRuler per giocare a Camelot.
 * 
 * In camelot ci sono 4 tipi di mosse possibili:
 * 
 * - Plain
 * Una mossa "plain" sposta il pezzo in una posizione adiacente vuota (non prevede quindi la cattura di pezzi avversari).
 * 
 * - Canter
 * Una mossa "canter" fa saltare la pedina nella posizione scelta verso una cella vuota a due passi di distanza in una certa direzione, 
 * a condizione che nella medesima direzione ci sia una pedina alleata adiacente (quindi ad un passo di distanza), che non viene però rimossa.
 * Se nella posizione di arrivo è presente una o più pedine alleate con una cella vuota adiacente nella stessa direzione (se ne può scegliere una qualsiasi), 
 * è obbligatorio eseguire un'altra "canter", continuando fino a che non sono più disponibili altre "canter".
 * Una mossa "canter" è rappresentata da tante JUMP (tutte con le posizioni concatenate) quante sono le "canter" da eseguire.
 * 
 * - Jump
 * Una mossa "jump" fa saltare la pedina nella posizione scelta verso una cella vuota a due passi di distanza in una certa direzione, 
 * a condizione che nella medesima direzione ci sia una pedina nemica adiacente (quindi ad un passo di distanza), rimuovendola dalla board.
 * Se nella posizione di arrivo è presente una o più pedine nemiche con una cella vuota adiacente nella stessa direzione (se ne può scegliere una qualsiasi), 
 * è obbligatorio eseguire un'altra "jump", continuando fino a che non sono più disponibili altre "jump".
 * Una mossa "jump" è rappresentata da una o più JUMP (tutte con le posizioni concatenate), ognuna seguita da una REMOVE 
 * che rimuove la pedina nemica catturata, tante volte quante sono le "jump" da eseguire.
 * 
 * - Knight's Charge
 * Una mossa "Knight's Charge" permette ad un cavallo di combinare nella medesima mossa una o più "canter" e una o più "jump" (non le "plain").
 * 
 * Camelot Game Info:
 * - Wiki  = https://en.wikipedia.org/wiki/Camelot_(board_game)
 * - Video = https://www.youtube.com/watch?v=KIdrAVfqAt0
 * 
 * @author Daniele Giudice
 * 
 */
public class Camelot implements GameRuler<PieceModel<Species>>
{
	/** Limite di tempo per una mossa */
	private final long time;
	/** Lista giocatori di Camelot */
    private final List<String> players;
    
    /** Situazione iniziale del gioco */
	private final Situation<PieceModel<Species>> start_situation;
	/** Meccanica del gioco */
	private final Mechanics<PieceModel<Species>> mechanics;
    
	/** Pedone nero */
    private final PieceModel<Species> black_pawn = new PieceModel<>(Species.PAWN, "nero");
    /** Cavallo nero */
    private final PieceModel<Species> black_knight = new PieceModel<>(Species.KNIGHT, "nero");
    /** Pedone bianco */
    private final PieceModel<Species> white_pawn = new PieceModel<>(Species.PAWN, "bianco");
    /** Cavallo bianco */
    private final PieceModel<Species> white_knight = new PieceModel<>(Species.KNIGHT, "bianco");
    /** Lista immutabile dei pezzi */
    private final List<PieceModel<Species>> pieces_list = Collections.unmodifiableList(
    		Arrays.asList(this.black_pawn, this.black_knight, this.white_pawn, this.white_knight));
	
    /** Board di Camelot */
	private final BoardOct<PieceModel<Species>> board;
	/** View immodificabile della Board di Camelot */
	private final Board<PieceModel<Species>> board_view;
	
	/** Lista posizioni escluse */
	private final List<Pos> exc_pos = Arrays.asList(
			new Pos(0,2), // DOWN_L
			new Pos(0,1), 
			new Pos(1,1), 
			new Pos(0,0), 
			new Pos(1,0), 
			new Pos(2,0), 
			new Pos(3,0), 
			new Pos(4,0), 
			
			new Pos(0,13), // UP_L
			new Pos(0,14), 
			new Pos(1,14), 
			new Pos(0,15), 
			new Pos(1,15), 
			new Pos(2,15), 
			new Pos(3,15), 
			new Pos(4,15), 
			
			new Pos(11,2), // DOWN_R
			new Pos(10,1), 
			new Pos(11,1), 
			new Pos(7,0), 
			new Pos(8,0), 
			new Pos(9,0), 
			new Pos(10,0), 
			new Pos(11,0), 
			
			new Pos(11,13), // UP_R
			new Pos(10,14), 
			new Pos(11,14), 
			new Pos(7,15), 
			new Pos(8,15), 
			new Pos(9,15), 
			new Pos(10,15), 
			new Pos(11,15)
		);
	/** Array posizioni pedoni neri */
	private final Pos[] black_pawn_pos = {
			new Pos(3,10), 
			new Pos(4,10), 
			new Pos(5,10), 
			new Pos(6,10), 
			new Pos(7,10), 
			new Pos(8,10), 
			new Pos(4,9), 
			new Pos(5,9), 
			new Pos(6,9), 
			new Pos(7,9)
		};
	/** Array posizioni cavalieri neri */
	private final Pos[] black_knight_pos = {
			new Pos(2,10), 
			new Pos(9,10), 
			new Pos(3,9), 
			new Pos(8,9)
		};
	/** Array posizioni pedoni bianchi */
	private final Pos[] white_pawn_pos = {
			new Pos(3,5), 
			new Pos(4,5), 
			new Pos(5,5), 
			new Pos(6,5), 
			new Pos(7,5), 
			new Pos(8,5), 
			new Pos(4,6), 
			new Pos(5,6), 
			new Pos(6,6), 
			new Pos(7,6)
		};
	/** Array posizioni cavalieri bianchi */
	private final Pos[] white_knight_pos = {
			new Pos(2,5), 
			new Pos(9,5), 
			new Pos(3,6), 
			new Pos(8,6)
		};
    
    /** Stato corrente del gioco */
    private int game_result = -1;
    /** Indice del giocatore di turno */
    private int current_turn = 1;
    
    /** Colore dei pezzi alleato per il giocatore di turno */
    private String ally_color = "bianco";
    /** Colore dei pezzi nemico per il giocatore di turno */
    private String enemy_color = "nero";
    
    /** Crea un GameRuler per fare una partita a Camelot.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null */
    public Camelot(long time, String p1, String p2)
    {
    	Objects.requireNonNull(p1);
    	Objects.requireNonNull(p2);
    	
    	// Tempo per una mossa
    	this.time = time;
    	// Lista dei giocatori
    	this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
    	
    	// Creo la board (con posizioni escluse)
    	this.board = new BoardOct<>(12, 16, this.exc_pos);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Setta la board iniziale
    	for( Pos p : black_pawn_pos )
    		this.board.put( this.black_pawn, p );
    	
    	for( Pos p : white_pawn_pos )
    		this.board.put( this.white_pawn, p );
    	
    	for( Pos p : black_knight_pos )
    		this.board.put( this.black_knight, p );
    	
    	for( Pos p : white_knight_pos )
    		this.board.put( this.white_knight, p );
    	
    	// Salvo la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco (FUNZIONE NEXT NON NECESSARIA)
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, null);
    }
    
    /**
     * Costruttore che crea un nuovo GameRuler di Camelot a partire da uno esistente
     * @param ot GameRuler del gioco Camelot
     * @throw NullPointerException se ot è null
     */
    private Camelot(Camelot ca)
    {
    	Objects.requireNonNull(ca);
    	
    	this.time = ca.time;
    	this.players = ca.players;
    	
    	this.board = ca.board.copy();
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	this.game_result = ca.game_result;
    	this.current_turn = ca.current_turn;
    	
    	this.ally_color = ca.ally_color;
    	this.enemy_color = ca.enemy_color;
    	
    	this.start_situation = ca.start_situation;
    	this.mechanics = ca.mechanics;
    }
    
    @Override
    public String name() { return "Camelot"; }

    @Override
    public <T> T getParam(String name, Class<T> c)
    {
    	Objects.requireNonNull(name);
    	Objects.requireNonNull(c);
    	
		switch(name)
		{
			case "Time": 	return c.cast( time <= 0 ? "No limit" : (time>=60000 ? (time/60000)+"m" : (time/1000)+"s") );
			default:		throw new IllegalArgumentException();
		}
    }

    @Override
    public List<String> players() { return this.players; }

    /** Assegna il colore "bianco" al primo giocatore e "nero" al secondo. */
    @Override
    public String color(String name)
    {
    	Objects.requireNonNull(name);
    	
    	if( !this.players.contains(name) )
        	throw new IllegalArgumentException();
    	
        if( this.players.get(0) == name )
        	return "bianco";
        else
        	return "nero";
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
    	
    	// Se la mossa non è valida, da la vittoria all'altro giocatore e ritorna false
    	if( !isValid(m) )
    	{
    		this.game_result = 3 - this.current_turn;
    		return false;
    	}
    	
    	// Se la mossa è una ACTION eseguila, altrimenti termina il gioco dando la vittoria all'altro giocatore
    	if( m.kind == Kind.ACTION )
    	{
			for( Action<PieceModel<Species>> action : m.actions )
			{
				switch(action.kind)
				{
					case ADD:
		    			{
		    				this.board.put(action.piece, action.pos.get(0));
		    			}
						break;
					
					case REMOVE:
		    			{
		    				for( Pos p : action.pos )
		    					this.board.remove(p);
		    			}
		    			break;
					
					case MOVE:
		    			{
		    				for(Pos p : action.pos)
		    				{
		    					int b = p.b;
		    			    	int t = p.t;
		    			    	
		    			    	for( int i=0 ; i<action.steps ; i++)
		    			    	{
		    			    		switch(action.dir)
			    			    	{
			    			    		case UP: t++;
			    			    			break;
			    			    		case DOWN: t--;
		    			    				break;
			    			    		case LEFT: b--;
		    			    				break;
			    			    		case RIGHT: b++;
		    			    				break;
			    			    		case UP_L: {b--; t++;}
		    			    				break;
			    			    		case UP_R: {b++; t++;}
		    			    				break;
			    			    		case DOWN_L: {b--; t--;}
		    			    				break;
			    			    		case DOWN_R: {b++; t--;}
		    			    				break;
			    			    	}
		    			    	}
		    			    	
		    			    	this.board.put(board.get(p), new Pos(b,t));
		    			    	this.board.remove(p);
		    				}
		    			}
						break;
					
					case JUMP:
		    			{
		    				this.board.put(board.get(action.pos.get(0)), action.pos.get(1));
		    				this.board.remove(action.pos.get(0));
		    			}
						break;
					
					case SWAP:
		    			{
		    				for(Pos p : action.pos)
		    					this.board.put(action.piece, p);
		    			}
						break;
				}
			}
			
			// Ottengo lo stato aggiornato del gioco
			int game_status = this.checkGameStatus();
			
			if( game_status != -1 )
			{
				// Il gioco è finito (con l'esito calcolato)
				this.game_result = game_status;
			}
			else
			{
				// Il gioco continua, quindi aggiorna l'indice e il colore del giocatore attuale
		    	this.current_turn = 3 - this.current_turn;
		    	this.ally_color = this.ally_color.equals("bianco") ? "nero" : "bianco";
		    	this.enemy_color = this.enemy_color.equals("nero") ? "bianco" : "nero";
			}
    	}
    	else
    		this.game_result = 3 - this.current_turn;
    	
    	return true;
    }

    @Override
    public boolean unMove()
    {
    	// METODO NON NECESSARIO
    	
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
    
    @Override
    public Set<Move<PieceModel<Species>>> validMoves()
    {
    	if( this.game_result != -1 )
        	throw new IllegalStateException();
    	
    	Set<Move<PieceModel<Species>>> moves_set = new HashSet<>();
    	
    	// Calcolo l'insieme di posizioni che contengono i pezzi alleati
    	Set<Pos> pos_ally_pieces = new HashSet<>();
    	pos_ally_pieces.addAll( this.board.get( this.ally_color.equals("bianco") ? this.white_knight : this.black_knight ) );
    	pos_ally_pieces.addAll( this.board.get( this.ally_color.equals("bianco") ? this.white_pawn : this.black_pawn ) );
    	
    	// Per ogni pezzo alleato, calcola le possibili mosse (fra i 4 tipi disponibili)
    	for( Pos p : pos_ally_pieces )
    	{
    		moves_set.addAll( this.computePlainMoves(p)  );
    		moves_set.addAll( this.computeCanterMoves(p) );
    		moves_set.addAll( this.computeJumpMoves(p) );
    		
    		if( this.board.get(p).species == Species.KNIGHT )
    			moves_set.addAll( this.computeChargeMoves(p) );
    	}
    	
    	// Se non sono state trovate mosse, ritorna un insieme vuoto
        if( moves_set.isEmpty() )
        	return Collections.emptySet();
        
        moves_set.add(new Move<>(Kind.RESIGN));
        
        return Collections.unmodifiableSet(moves_set);
    }
    
    @Override
    public GameRuler<PieceModel<Species>> copy() { return new Camelot(this); }

    @Override
    public Mechanics<PieceModel<Species>> mechanics() { return this.mechanics; }
    
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
    
    // METODI CALCOLO VITTORIA
    
    /**
     * Analizza la partita, e ritorna lo stato aggiornato del gioco.
     * @return -1 se la partita deve continuare, altrimenti il risultato (0, 1, o 2)
     */
    private int checkGameStatus()
    {
    	PieceModel<Species> castle1, castle2;
    	
    	if( this.current_turn==1 )
    	{
    		// Se ha appena mosso il bianco, controlla il castello nero
    		castle1 = this.board.get(new Pos(5,15));
    		castle2 = this.board.get(new Pos(6,15));
    	}
    	else
    	{
    		// Se ha appena mosso il nero, controlla il castello bianco
    		castle1 = this.board.get(new Pos(5,0));
    		castle2 = this.board.get(new Pos(6,0));
    	}
    	
    	if( castle1!=null && castle2!=null && castle1.color.equals(this.ally_color) && castle1.color.equals(this.ally_color) )
    	{
    		return this.current_turn; // Vittoria -> Il giocatore attuale ha occupato il castello avversario
    	}
    	else
    	{
    		Set<Pos> board_pieces = this.board.get();
    		
    		long ally_pieces = board_pieces.stream().filter(item -> this.board.get(item).color.equals(this.ally_color)).count();
    		long enemy_pieces = board_pieces.size() - ally_pieces;
    		
    		if( ally_pieces<2 && enemy_pieces==0 )
    		{
    			return 0; // Patta -> Il giocatore attuale ha mangiato tutte le pedine nemiche, ma gli rimangono meno di 2 pedine
    		}
    		else if( ally_pieces>=2 && enemy_pieces==0 )
    		{
    			return this.current_turn; // Vittoria -> Il giocatore attuale ha mangiato tutte le pedine nemiche, e gli rimangono almeno 2 pedine
    		}
    		else
    		{
    			return -1; // Il gioco deve continuare
    		}
    	}
    }
    
    // METODI CALCOLO MOSSE VALIDE
    
    /** Data una posizione contenente una pedina qualsiasi, calcola tutte le possibili mosse di tipo "plain".
     * Una mossa "plain" sposta il pezzo in una posizione adiacente vuota (non prevede quindi la cattura di pezzi avversari).
     * 
     * @param p posizione contenente il pezzo da muovere
     * @return insieme delle mosse di tipo "plain"
     */
    private Set<Move<PieceModel<Species>>> computePlainMoves(Pos p)
    {
    	Set<Move<PieceModel<Species>>> plain_moves_set = new HashSet<>();
    	
    	Pos adj;
    	
    	for( Dir d : Dir.values() )
    	{
    		adj = this.board.adjacent(p, d);
    		
    		if( adj!=null && this.board.get(adj)==null )
    			plain_moves_set.add( new Move<>(new Action<>(d, 1, p)) );
    	}
    	
    	return plain_moves_set;
    }
    
    /** Data una posizione contenente una pedina qualsiasi, calcola tutte le possibili mosse di tipo "canter".
     * 
     * Una mossa "canter" fa saltare la pedina nella posizione scelta verso una cella vuota a due passi di distanza in una certa direzione, 
     * a condizione che nella medesima direzione ci sia una pedina alleata adiacente (quindi ad un passo di distanza), che non viene però rimossa.
     * Se nella posizione di arrivo è presente una o più pedine alleate con una cella vuota adiacente nella stessa direzione (se ne può scegliere una qualsiasi), 
     * è obbligatorio eseguire un'altra "canter", continuando fino a che non sono più disponibili altre "canter".
     * 
     * Una mossa "canter" è rappresentata da tante JUMP (tutte con le posizioni concatenate) quante sono le "canter" da eseguire.
     * 
     * @param p posizione contenente il pezzo da muovere
     * @return insieme delle mosse di tipo "canter"
     */
    private Set<Move<PieceModel<Species>>> computeCanterMoves(Pos p)
    {
    	Set<Move<PieceModel<Species>>> canter_moves = new HashSet<>();
    	
    	for( List<Action<PieceModel<Species>>> l : canterRecursion(new HashSet<>(),p) )
    		canter_moves.add( new Move<>(l) );
    	
    	return canter_moves;
    }
    
    /**
     * Calcola ricorsivamente ogni possibile lista di azioni che rappresenta una mossa di tipo "canter".
     * 
     * @param exc posizioni da non controllare (per controllare la ricorsione)
     * @param p posizione di partenza
     * @return insieme delle liste di azioni che rappresentano mosse di tipo "canter"
     */
    private Set<List<Action<PieceModel<Species>>>> canterRecursion(Set<Pos> exc, Pos p)
    {
    	Set<List<Action<PieceModel<Species>>>> mid_canters, canters = new HashSet<>();
    	
    	Pos adj1, adj2;
    	Action<PieceModel<Species>> att_jump;
    	List<Action<PieceModel<Species>>> att_action_list;
    	
    	for( Dir d : Dir.values() )
    	{
    		// Ricava la posizione adiacente
    		adj1 = this.board.adjacent(p, d);
    		
    		// Se c'è una pedina alleata (non inclusa nell'insieme "exc")...
    		if( adj1!=null && !exc.contains(adj1) && this.board.get(adj1)!=null && this.board.get(adj1).color.equals(this.ally_color) )
    		{
    			// ...calcola la sua posizione adiacente nella medesima direzione...
    			adj2 = this.board.adjacent(adj1, d);
    			
    			// ...e se la posizione trovata è una cella vuota...
    			if( adj2!=null && this.board.get(adj2)==null )
        		{
    				// Allora è possibile iniziare una "canter"
    				
    				// Inserico la posizione del pezzo nell'insieme di esclusione
    				exc.add(adj1);
    				
    				// Crea la prima JUMP della "canter"
    				att_jump = new Action<>(p,adj2);
    				
    				// Calcola ricorsivamente le liste delle possibili JUMP successive
    				mid_canters = this.canterRecursion(exc, adj2);
    				
    				if( mid_canters.isEmpty() )
    				{
    					// Se non ce ne sono, aggiunge all'insieme finale una lista con solo la prima JUMP
    					att_action_list = new ArrayList<>();
    					att_action_list.add(att_jump);
    					canters.add(att_action_list);
    				}
    				else
    				{
    					// Se ce ne è almeno una, le aggiunge tutte all'insieme finale, 
    					// inserendo in ognuna la prima JUMP come prefisso
    					for( List<Action<PieceModel<Species>>> jump_list : mid_canters )
        				{
        					att_action_list = new ArrayList<>();
        					att_action_list.add(att_jump);
        					att_action_list.addAll(jump_list);
        					canters.add(att_action_list);
        				}
    				}
        		}
    		}
    	}
    	
    	return canters;
    }
    
    /** Data una posizione contenente una pedina qualsiasi, calcola tutte le possibili mosse di tipo "jump".
     * 
     * Una mossa "jump" fa saltare la pedina nella posizione scelta verso una cella vuota a due passi di distanza in una certa direzione, 
     * a condizione che nella medesima direzione ci sia una pedina nemica adiacente (quindi ad un passo di distanza), rimuovendola dalla board.
     * Se nella posizione di arrivo è presente una o più pedine nemiche con una cella vuota adiacente nella stessa direzione (se ne può scegliere una qualsiasi), 
     * è obbligatorio eseguire un'altra "jump", continuando fino a che non sono più disponibili altre "jump".
     * 
     * Una mossa "jump" è rappresentata da una o più JUMP (tutte con le posizioni concatenate), ognuna seguita da una REMOVE 
     * che rimuove la pedina nemica catturata, tante volte quante sono le "jump" da eseguire.
     * 
     * @param p posizione contenente il pezzo da muovere
     * @return insieme delle mosse di tipo "jump"
     */
    private Set<Move<PieceModel<Species>>> computeJumpMoves(Pos p)
    {
    	Set<Move<PieceModel<Species>>> jump_moves = new HashSet<>();
    	
    	for( List<Action<PieceModel<Species>>> l : jumpRecursion(new HashSet<>(),p) )
    		jump_moves.add( new Move<>(l) );
    	
    	return jump_moves;
    }
    
    /**
     * Calcola ricorsivamente ogni possibile lista di azioni che rappresenta una mossa di tipo "jump".
     * 
     * @param exc posizioni da non controllare (per controllare la ricorsione)
     * @param p posizione di partenza
     * @return insieme delle liste di azioni che rappresentano mosse di tipo "jump"
     */
    private Set<List<Action<PieceModel<Species>>>> jumpRecursion(Set<Pos> exc, Pos p)
    {
    	Set<List<Action<PieceModel<Species>>>> mid_jumps, jumps = new HashSet<>();
    	
    	Action<PieceModel<Species>> att_jump, att_remove;
    	List<Action<PieceModel<Species>>> att_action_list;
    	
    	Pos adj1, adj2;
    	
    	for( Dir d : Dir.values() )
    	{
    		// Ricava la posizione adiacente
    		adj1 = this.board.adjacent(p, d);
    		
    		// Se in essa c'è una pedina avversaria...
    		if( adj1!=null && !exc.contains(adj1) && this.board.get(adj1)!=null && this.board.get(adj1).color.equals(this.enemy_color) )
    		{
    			// ...calcola la sua posizione adiacente nella medesima direzione...
    			adj2 = this.board.adjacent(adj1, d);
    			
    			// ...e se la posizione trovata è una cella vuota...
    			if( adj2!=null && this.board.get(adj2)==null )
        		{
    				// Allora è possibile iniziare una "jump"
    				
    				// Inserico la posizione del pezzo mangiato nell'insieme di esclusione
    				exc.add(adj1);
    				
    				// Crea la JUMP e la REMOVE attuali
    				att_jump = new Action<>(p,adj2);
    				att_remove = new Action<>(adj1);
    				
    				// Calcola ricorsivamente l'insieme delle liste delle possibili JUMP e REMOVE successive
    				mid_jumps = this.jumpRecursion(exc, adj2);
    				
    				if( mid_jumps.isEmpty() )
    				{
    					// Se è vuoto, aggiunge all'insieme finale le sole JUMP e REMOVE attuali
    					att_action_list = new ArrayList<>();
    					att_action_list.add(att_jump);
    					att_action_list.add(att_remove);
    					
    					jumps.add(att_action_list);
    				}
    				else
    				{
    					// Se non lo è, aggiunge la JUMP e la REMOVE attuali come prefisso ad ogni lista di mosse, 
    					// poi inserisce ogni nuova lista nell'insieme finale
    					for( List<Action<PieceModel<Species>>> next_actions : mid_jumps )
        				{
        					att_action_list = new ArrayList<>();
        					att_action_list.add(att_jump);
        					att_action_list.add(att_remove);
        					att_action_list.addAll(next_actions);
        					
        					jumps.add(att_action_list);
        				}
    				}
        		}
    		}
    	}
    	
    	return jumps;
    }
    
    /** Data una posizione contenente una cavallo, calcola tutte le possibili mosse di tipo "Knight's Charge"
     * 
     * Una mossa "Knight's Charge" permette ad un cavallo di combinare nella medesima mossa una o più "canter" e una o più "jump" (non le "plain").
     * 
     * @param p posizione contenente il cavallo da muovere
     * @return insieme delle mosse di tipo "Knight's Charge"
     */
    private Set<Move<PieceModel<Species>>> computeChargeMoves(Pos p)
    {
    	Set<Move<PieceModel<Species>>> charge_moves = new HashSet<>();
    	
    	for( List<Action<PieceModel<Species>>> l : chargeRecursion(new HashSet<>(),p) )
    		charge_moves.add( new Move<>(l) );
    	
    	return charge_moves;
    }
    
    /**
     * Calcola ricorsivamente ogni possibile lista di azioni che rappresenta una mossa di tipo "Knight's Charge".
     * 
     * @param exc posizioni da non controllare (per controllare la ricorsione)
     * @param p posizione di partenza
     * @return insieme delle liste di azioni che rappresentano mosse di tipo "Knight's Charge"
     */
    private Set<List<Action<PieceModel<Species>>>> chargeRecursion(Set<Pos> exc, Pos p)
    {
    	Set<List<Action<PieceModel<Species>>>> mid_charges, charges = new HashSet<>();
    	
    	Action<PieceModel<Species>> att_charge, att_remove;
    	List<Action<PieceModel<Species>>> att_action_list;
    	
    	Pos adj1, adj2;
    	PieceModel<Species> att_piece;
    	
    	for( Dir d : Dir.values() )
    	{
    		// Ricava la posizione adiacente
    		adj1 = this.board.adjacent(p, d);
    		
    		// Se essa non è valida, passa alla prossima adiacenza
    		if( adj1==null || exc.contains(adj1) )
    			continue;
    		
    		// Se in essa c'è una pedina...
    		att_piece = this.board.get(adj1);
    		if( att_piece!=null )
    		{
    			// ...calcola la sua posizione adiacente nella medesima direzione...
    			adj2 = this.board.adjacent(adj1, d);
    			
    			// ...e se la posizione trovata è una cella vuota...
    			if( adj2!=null && this.board.get(adj2)==null )
        		{
    				// Allora è possibile iniziare una "charge"
    				
    				// Inserico la posizione del pezzo nell'insieme di esclusione
    				exc.add(adj1);
    				
    				// Crea la JUMP (mossa di carica) e la REMOVE attuali.
    				// La REMOVE sarà presa in considerazione solo se 'att_piece' contiene un pezzo avversario
    				att_charge = new Action<>(p,adj2);
    				att_remove = new Action<>(adj1);
    				
    				// Calcola ricorsivamente l'insieme delle liste delle possibili JUMP e REMOVE successive
    				mid_charges = this.chargeRecursion(exc, adj2);
    				
    				if( mid_charges.isEmpty() )
    				{
    					// Se è vuoto, aggiunge all'insieme finale le sole JUMP e REMOVE (se necessaria) attuali
    					att_action_list = new ArrayList<>();
    					att_action_list.add(att_charge);
    					
    					if( att_piece.color.equals(this.enemy_color) )
    						att_action_list.add(att_remove);
    					
    					charges.add(att_action_list);
    				}
    				else
    				{
    					// Se non lo è, aggiunge la JUMP e la REMOVE (se necessaria) attuali come prefisso ad ogni lista di mosse, 
    					// poi inserisce ogni nuova lista nell'insieme finale
    					for( List<Action<PieceModel<Species>>> next_actions : mid_charges )
        				{
    						att_action_list = new ArrayList<>();
        					att_action_list.add(att_charge);
        					
        					if( att_piece.color.equals(this.enemy_color) )
        						att_action_list.add(att_remove);
        					
        					att_action_list.addAll(next_actions);
        					
        					charges.add(att_action_list);
        				}
    				}
        		}
    		}
    	}
    	
    	return charges;
    }
}
