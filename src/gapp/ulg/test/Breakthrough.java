package gapp.ulg.test;

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
 * GameRuler per giocare a Breakthrough.
 * 
 * Breakthrough Game Wiki = https://en.wikipedia.org/wiki/Breakthrough_(board_game)
 * 
 * @author Daniele Giudice
 * 
 */
public class Breakthrough implements GameRuler<PieceModel<Species>>
{
    /** Limite di tempo per una mossa */
	private final long time;
	/** Larghezza della board */
	private final int width;
	/** Altezza della board */
	private final int height;
	/** Lista giocatori di Breakthrough */
    private final List<String> players;
    
    /** Pedone Nero */
    private final PieceModel<Species> black_pawn = new PieceModel<>(Species.PAWN, "nero");
    /** Pedone Bianco */
    private final PieceModel<Species> white_pawn = new PieceModel<>(Species.PAWN, "bianco");
    /** Lista immutabile dei pezzi */
    private final List<PieceModel<Species>> pieces_list = Collections.unmodifiableList(Arrays.asList(this.black_pawn, this.white_pawn));
    
    /** Direzioni di muovimento e cattura pezzi neri */
    private final Dir[] black_capture_dir = { Dir.DOWN_L, Dir.DOWN_R };
    /** Direzioni di solo muovimento pezzi neri */
    private final Dir[] black_move_dir = { Dir.DOWN };
    
    /** Direzioni di muovimento e cattura pezzi bianchi */
    private final Dir[] white_capture_dir = { Dir.UP_L, Dir.UP_R };
    /** Direzioni di solo muovimento pezzi bianchi */
    private final Dir[] white_move_dir = { Dir.UP };
    
    /** Situazione iniziale del gioco */
	private final Situation<PieceModel<Species>> start_situation;
	/** Meccanica del gioco */
	private final Mechanics<PieceModel<Species>> mechanics;
    
    /** Board di Breakthrough */
	private final BoardOct<PieceModel<Species>> board;
	/** View immodificabile della Board di Breakthrough */
	private final Board<PieceModel<Species>> board_view;
    
    /** Stato corrente del gioco */
    private int game_result = -1;
    /** Indice del giocatore di turno */
    private int current_turn = 1;
    
    /** Tipo di pezzo alleato per il giocatore di turno */
    private PieceModel<Species> ally_piece;
    /** Tipo di pezzo nemico per il giocatore di turno */
    private PieceModel<Species> enemy_piece;
    
    /** Crea un GameRuler per fare una partita a Breakthrough.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun
     *              limite
     * @param width  dimensione della board, sono accettati solamente i numeri interi da 2 a 12
     * @param height  dimensione della board, sono accettati solamente i numeri interi da 2 a 12
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null
     * @throws IllegalArgumentException se width o height non sono validi */
    public Breakthrough(long time, int width, int height, String p1, String p2)
    {
    	Objects.requireNonNull(p1);
    	Objects.requireNonNull(p2);
    	
    	if( width<2 || width>12 || height<2 || height>12 )
    		throw new IllegalArgumentException();
    	
    	// Tempo per una mossa
    	this.time = time;
    	// Dimensione board
    	this.width = width;
    	this.height = height;
    	// Lista dei giocatori
    	this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
    	
    	this.board = new BoardOct<>(this.width, this.height);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Setta la board iniziale
    	for( int x=0 ; x<width ; x++ )
    	{
    		this.board.put(this.white_pawn, new Pos(x, 0));
    		this.board.put(this.white_pawn, new Pos(x, 1));
    		this.board.put(this.black_pawn, new Pos(x, height-1));
    		this.board.put(this.black_pawn, new Pos(x, height-2));
    	}
    	
    	// Inizia il bianco
    	this.ally_piece = this.white_pawn;
    	this.enemy_piece = this.black_pawn;
    	
    	// Salvo la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco (non è necessario inserire la funzione next)
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, null);
    }
    
    /**
     * Costruttore che crea un nuovo GameRuler di Breakthrough a partire da uno esistente
     * @param ot GameRuler del gioco Breakthrough
     * @throw NullPointerException se ot è null
     */
    private Breakthrough(Breakthrough br)
    {
    	Objects.requireNonNull(br);
    	
    	this.time = br.time;
    	this.width = br.width;
    	this.height = br.height;
    	this.players = br.players;
    	
    	this.board = br.board.copy();
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	this.game_result = br.game_result;
    	this.current_turn = br.current_turn;
    	
    	this.enemy_piece = br.enemy_piece;
    	this.ally_piece = br.ally_piece;
    	
    	this.start_situation = br.start_situation;
    	this.mechanics = br.mechanics;
    }
    
    @Override
    public String name()
    {
        return "Breakthrough" + this.width + "x" + this.height;
    }

    @Override
    public <T> T getParam(String name, Class<T> c)
    {
    	Objects.requireNonNull(name);
    	Objects.requireNonNull(c);
    	
		switch(name)
		{
			case "Time": 	return c.cast( time <= 0 ? "No limit" : (time>=60000 ? (time/60000)+"m" : (time/1000)+"s") );
			case "Width":	return c.cast( this.width );
			case "Height":	return c.cast( this.height );
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
		    	this.ally_piece = this.ally_piece.equals(this.white_pawn) ? this.black_pawn : this.white_pawn;
		    	this.enemy_piece = this.enemy_piece.equals(this.black_pawn) ? this.white_pawn : this.black_pawn;
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
    	
    	Pos adj;
    	PieceModel<Species> piece;
    	
    	// In base al giocatore corrente, recupero le direzioni di muovimento e cattura possibili
    	Dir[] capture_dir, move_dir;
    	if( this.ally_piece.color.equals("bianco") )
    	{
    		capture_dir = this.white_capture_dir;
    		move_dir = this.white_move_dir;
    	}
    	else
    	{
    		capture_dir = this.black_capture_dir;
    		move_dir = this.black_move_dir;
    	}
    	
    	// Per ogni pezzo alleato, calcola le possibili mosse
    	for( Pos p : this.board.get(this.ally_piece) )
    	{
    		// Genero le mosse di cattura (o anche di muovimento)
    		for( Dir d : capture_dir )
    		{
    			adj = this.board.adjacent(p, d);
        		if( adj!=null )
        		{
        			piece = this.board.get(adj);
        			if( piece==null  )
        				moves_set.add( new Move<>(new Action<>(d, 1, p)) ); // Muovi
        			else if( piece.equals(this.enemy_piece) )
        				moves_set.add( new Move<>(new Action<>(adj), new Action<>(d, 1, p)) ); // Muovi e mangia
        		}
    		}
    		
    		// Genero le mosse di solo muovimento (senza cattura)
    		for( Dir d : move_dir )
    		{
    			adj = this.board.adjacent(p, d);
        		if( adj!=null )
        		{
        			piece = this.board.get(adj);
        			if( piece==null  )
        				moves_set.add( new Move<>(new Action<>(d, 1, p)) ); // Muovi
        		}
    		}
    	}
    	
    	// Se non sono state trovate mosse, ritorna un insieme vuoto
        if( moves_set.isEmpty() )
        	return Collections.unmodifiableSet(Collections.emptySet());
        
        moves_set.add(new Move<>(Kind.RESIGN));
        
        return Collections.unmodifiableSet(moves_set);
    }
    
    @Override
    public GameRuler<PieceModel<Species>> copy() { return new Breakthrough(this); }

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
    
    /**
     * Analizza la partita, e ritorna lo stato aggiornato del gioco.
     * @return -1 se la partita deve continuare, altrimenti il risultato (0, 1, o 2)
     */
    private int checkGameStatus()
    {
    	PieceModel<Species> piece;
    	
    	// Controlla se la base avversaria è stata invasa
    	int row = this.current_turn==1 ? this.height-1 : 0;
    	for( int x=0 ; x<this.width ; x++ )
    	{
    		piece = this.board.get(new Pos(x, row));
    		if( piece!=null && piece.equals(this.ally_piece) )
    			return this.current_turn; // Vittoria -> Il giocatore attuale ha raggiunto la base avversaria
    	}
    	
		if( this.board.get(this.enemy_piece).size()==0 )
		{
			return this.current_turn; // Vittoria -> Il giocatore attuale ha mangiato tutte le pedine nemiche
		}
		else
		{
			return -1; // Il gioco deve continuare
		}
    }
}