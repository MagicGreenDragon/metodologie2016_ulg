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
 * GameRuler per giocare a Swap Game.
 * 
 * @author Daniele Giudice
 * 
 */
public class SwapGame implements GameRuler<PieceModel<Species>>
{
    /** Limite di tempo per una mossa */
	private final long time;
	/** Lista giocatori di Swap Game */
    private final List<String> players;
    
    /** Pezzi Bianchi */
    private final PieceModel<Species> white_pawn = new PieceModel<>(Species.PAWN, "bianco");
    private final PieceModel<Species> white_disc = new PieceModel<>(Species.DISC, "bianco");
    private final PieceModel<Species> white_dama = new PieceModel<>(Species.DAMA, "bianco");
    private final PieceModel<Species> white_knight = new PieceModel<>(Species.KNIGHT, "bianco");
    private final PieceModel<Species> white_bishop = new PieceModel<>(Species.BISHOP, "bianco");
    private final PieceModel<Species> white_rook = new PieceModel<>(Species.ROOK, "bianco");
    private final PieceModel<Species> white_queen = new PieceModel<>(Species.QUEEN, "bianco");
    private final PieceModel<Species> white_king = new PieceModel<>(Species.KING, "bianco");
    private final List<PieceModel<Species>> white_swap_list = Arrays.asList(
    		this.white_disc, this.white_dama, this.white_knight, this.white_bishop, this.white_rook, this.white_queen, this.white_king );
    
    /** Pezzi Neri */
    private final PieceModel<Species> black_pawn = new PieceModel<>(Species.PAWN, "nero");
    private final PieceModel<Species> black_disc = new PieceModel<>(Species.DISC, "nero");
    private final PieceModel<Species> black_dama = new PieceModel<>(Species.DAMA, "nero");
    private final PieceModel<Species> black_knight = new PieceModel<>(Species.KNIGHT, "nero");
    private final PieceModel<Species> black_bishop = new PieceModel<>(Species.BISHOP, "nero");
    private final PieceModel<Species> black_rook = new PieceModel<>(Species.ROOK, "nero");
    private final PieceModel<Species> black_queen = new PieceModel<>(Species.QUEEN, "nero");
    private final PieceModel<Species> black_king = new PieceModel<>(Species.KING, "nero");
    private final List<PieceModel<Species>> black_swap_list = Arrays.asList(
    		this.black_disc, this.black_dama, this.black_knight, this.black_bishop, this.black_rook, this.black_queen, this.black_king );
    
    /** Lista immutabile dei pezzi */
    private final List<PieceModel<Species>> pieces_list = Collections.unmodifiableList(
    		Arrays.asList(this.white_pawn, this.white_disc, this.white_dama, this.white_knight, this.white_bishop, this.white_rook, this.white_queen, this.white_king, 
    				this.black_pawn, this.black_disc, this.black_dama, this.black_knight, this.black_bishop, this.black_rook, this.black_queen, this.black_king )
    		);
    
    /** Situazione iniziale del gioco */
	private final Situation<PieceModel<Species>> start_situation;
	/** Meccanica del gioco */
	private final Mechanics<PieceModel<Species>> mechanics;
    
    /** Board di Swap Game */
	private final BoardOct<PieceModel<Species>> board;
	/** View immodificabile della Board di Swap Game */
	private final Board<PieceModel<Species>> board_view;
    
    /** Stato corrente del gioco */
    private int game_result = -1;
    /** Indice del giocatore di turno */
    private int current_turn = 1;
    
    /** Colore dei pezzi alleato per il giocatore di turno */
    private String ally_color = "bianco";
    /** Colore dei pezzi nemico per il giocatore di turno */
    private String enemy_color = "nero";
    
    /** Crea un GameRuler per fare una partita a Swap Game.
     * @param time  tempo in millisecondi per fare una mossa, se <= 0 significa nessun limite
     * @param p1  il nome del primo giocatore
     * @param p2  il nome del secondo giocatore
     * @throws NullPointerException se {@code p1} o {@code p2} è null */
    public SwapGame(long time, String p1, String p2)
    {
    	Objects.requireNonNull(p1);
    	Objects.requireNonNull(p2);
    	
    	// Tempo per una mossa
    	this.time = time;
    	// Lista dei giocatori
    	this.players = Collections.unmodifiableList(Arrays.asList(p1, p2));
    	
    	this.board = new BoardOct<>(8, 8);
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	// Setta la board iniziale
    	this.board.put(this.white_pawn, new Pos(1,6));
    	this.board.put(this.white_pawn, new Pos(3,6));
    	this.board.put(this.black_pawn, new Pos(5,6));
    	this.board.put(this.black_pawn, new Pos(7,6));
    	
    	// Salvo la situazione iniziale
    	this.start_situation = this.getSituation();
    	
    	// Definisco la meccanica del gioco (non è necessario inserire la funzione next)
    	this.mechanics = new Mechanics<PieceModel<Species>>(this.time, this.pieces_list, this.board.positions(), 2, start_situation, null);
    }
    
    /**
     * Costruttore che crea un nuovo GameRuler di Swap Game a partire da uno esistente
     * @param ot GameRuler del gioco Swap Game
     * @throw NullPointerException se ot è null
     */
    private SwapGame(SwapGame ch)
    {
    	Objects.requireNonNull(ch);
    	
    	this.time = ch.time;
    	this.players = ch.players;
    	
    	this.board = ch.board.copy();
    	this.board_view = Utils.UnmodifiableBoard(this.board);
    	
    	this.game_result = ch.game_result;
    	this.current_turn = ch.current_turn;
    	
    	this.ally_color = ch.ally_color;
    	this.enemy_color = ch.enemy_color;
    	
    	this.start_situation = ch.start_situation;
    	this.mechanics = ch.mechanics;
    }
    
    @Override
    public String name()
    {
        return "Swap Game";
    }

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
			
			if( this.board.get(this.black_pawn).isEmpty() && this.board.get(this.white_pawn).isEmpty() )
			{
				this.game_result = 0; // Patta
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
    	
    	if( this.ally_color.equals("bianco") )
    	{
    		if( this.board.get(new Pos(1,6))!=null && this.board.get(new Pos(1,6)).equals(this.white_pawn) )
        	{
        		for( PieceModel<Species> piece : this.white_swap_list )
        			moves_set.add( new Move<>(new Action<>(Dir.UP, 1, new Pos(1,6)), new Action<>(piece, new Pos(1,7))) );
        	}
        	
    		if( this.board.get(new Pos(3,6))!=null && this.board.get(new Pos(3,6)).equals(this.white_pawn) )
        		moves_set.add( new Move<>(new Action<>(Dir.UP, 1, new Pos(3,6)), new Action<>(this.white_queen, new Pos(3,7))) );
    	}
    	else if( this.ally_color.equals("nero") )
    	{
    		if( this.board.get(new Pos(5,6))!=null && this.board.get(new Pos(5,6)).equals(this.black_pawn) )
        	{
        		for( PieceModel<Species> piece : this.black_swap_list )
        			moves_set.add( new Move<>(new Action<>(Dir.UP, 1, new Pos(5,6)), new Action<>(piece, new Pos(5,7))) );
        	}
    		
        	if( this.board.get(new Pos(7,6))!=null && this.board.get(new Pos(7,6)).equals(this.black_pawn) )
        		moves_set.add( new Move<>(new Action<>(Dir.UP, 1, new Pos(7,6)), new Action<>(this.black_queen, new Pos(7,7))) );
    	}
    	
    	// Se non sono state trovate mosse, ritorna un insieme vuoto
        if( moves_set.isEmpty() )
        	return Collections.unmodifiableSet(Collections.emptySet());
        
        moves_set.add(new Move<>(Kind.RESIGN));
        
        return Collections.unmodifiableSet(moves_set);
    }
    
    @Override
    public GameRuler<PieceModel<Species>> copy() { return new SwapGame(this); }

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
}