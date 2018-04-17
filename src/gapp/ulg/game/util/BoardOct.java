package gapp.ulg.game.util;

import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.board.Board.System;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Gli oggetti BoardOct implementano l'interfaccia {@link Board} per rappresentare
 * board generali con sistema di coordinate {@link System#OCTAGONAL}
 * modificabili.
 * @param <P>  tipo del modello dei pezzi */
public class BoardOct<P> implements Board<P>
{
	/** Larghezza della board */
	private final int width;
	/** Altezza della board */
    private final int height;
    
    /** Lista delle posizioni della board */
	private final List<Pos> positions;
	/** Mappa che associa ad ogni posizione della board il suo contenuto */ 
	private Map<Pos,P> pos_map;
	
	/** Crea una BoardOct con le dimensioni date (può quindi essere rettangolare).
     * Le posizioni della board sono tutte quelle comprese nel rettangolo dato e le
     * adiacenze sono tutte e otto, eccetto per le posizioni di bordo.
     * @param width  larghezza board
     * @param height  altezza board
     * @throws IllegalArgumentException se width <= 0 o height <= 0 */
    public BoardOct(int width, int height)
    {
    	if( width <= 0 || height <= 0 )
        	throw new IllegalArgumentException();
    	
        this.width = width;
        this.height = height;
        this.pos_map = new HashMap<>();
        
        List<Pos> pos_temp = new ArrayList<Pos>();
        
        // Crea la lista posizioni in un array temporaneo e inizializza la board
        for( int i=0 ; i<width ; i++ )
        	for( int j=0 ; j<height ; j++ )
        	{
        		Pos p = new Pos(i, j);
        		pos_temp.add(p);
        		this.pos_map.put(p, null);
        	}
        
        this.positions = Collections.unmodifiableList(pos_temp);
    }

    /** Crea una BoardOct con le dimensioni date (può quindi essere rettangolare)
     * escludendo le posizioni in exc. Le adiacenze sono tutte e otto, eccetto per
     * le posizioni di bordo o adiacenti a posizioni escluse. Questo costruttore
     * permette di creare board per giochi come ad es.
     * <a href="https://en.wikipedia.org/wiki/Camelot_(board_game)">Camelot</a>
     * @param width  larghezza board
     * @param height  altezza board
     * @param exc  posizioni escluse dalla board
     * @throws NullPointerException se exc è null
     * @throws IllegalArgumentException se width <= 0 o height <= 0 */
    public BoardOct(int width, int height, Collection<? extends Pos> exc)
    {
    	if( width <= 0 || height <= 0 )
        	throw new IllegalArgumentException();
    	
    	if( exc == null )
        	throw new NullPointerException();
    	
        this.width = width;
        this.height = height;
        this.pos_map = new HashMap<>();
        
        List<Pos> pos_temp = new ArrayList<Pos>();
        
        // Crea la lista posizioni in un array temporaneo e inizializza la board
        for( int i=0 ; i<width ; i++ )
        	for( int j=0 ; j<height ; j++ )
        	{
        		Pos p = new Pos(i, j);
        		
        		if( !exc.contains(p) )
        		{
        			pos_temp.add(p);
            		this.pos_map.put(p, null);
        		}
        	}
        
        this.positions = Collections.unmodifiableList(pos_temp);
    }
    
    /**
     * Crea una copia profonda di una BoardOct data.
     * @param b  una BoardOct
     * @throws NullPointerException se b è null
     */
    private BoardOct(BoardOct<P> b)
    {
    	if( b == null )
        	throw new NullPointerException();
    	
    	this.width = b.width;
    	this.height = b.height;
    	
    	List<Pos> pos_list = new ArrayList<>();
    	pos_list.addAll(b.positions());
        this.positions = Collections.unmodifiableList(pos_list);
        
        this.pos_map = new HashMap<>();
        this.pos_map.putAll(b.pos_map);
    }

    @Override
    public System system()
    {
    	return System.OCTAGONAL;
    }

    @Override
    public int width()
    {
    	return this.width;
    }

    @Override
    public int height()
    {
    	return this.height;
    }

    @Override
    public Pos adjacent(Pos p, Dir d)
    {
    	if( p == null || d == null )
        	throw new NullPointerException();
        
    	if( !this.isPos(p) )
    		return null;
    	
    	int b = p.b;
    	int t = p.t;
    	
    	switch(d)
    	{
    		case UP: return this.get_adjacent(b, t+1);
    		case DOWN: return this.get_adjacent(b, t-1);
    		case LEFT: return this.get_adjacent(b-1, t);
    		case RIGHT: return this.get_adjacent(b+1, t);
    		case UP_L: return this.get_adjacent(b-1, t+1);
    		case UP_R: return this.get_adjacent(b+1, t+1);
    		case DOWN_L: return this.get_adjacent(b-1, t-1);
    		case DOWN_R: return this.get_adjacent(b+1, t-1);
    		default: return null;
    	}
    }
    
    /** Trova la posizione adiacente in base alle sue coordinate
     * @param b valore dell'asse di base
     * @param t valore dell'asse trasversale
     * @return se esiste, la posizione adiacente in base alle coordinate, altrimenti null
     */
    private Pos get_adjacent(int b, int t)
    {
    	if( b<0 || b>=this.width || t<0 || t>=this.height )
    		return null;
    	
    	Pos p = new Pos(b,t);
    	
    	if( this.isPos(p) )
    		return p;
    	else
    		return null;
    }

    @Override
    public List<Pos> positions()
    {
    	return this.positions;
    }

    @Override
    public P get(Pos p)
    {
    	if( p == null )
        	throw new NullPointerException();
        
    	return this.pos_map.get(p);
    }

    @Override
    public boolean isModifiable() { return true; }

    @Override
    public P put(P pm, Pos p)
    {
    	if( this.isModifiable() )
        {
        	if( p == null || pm == null )
        		throw new NullPointerException();
        	
        	if( !this.isPos(p) )
        		throw new IllegalArgumentException();
        	
        	return this.pos_map.put(p, pm);
        }
        else
        	throw new UnsupportedOperationException();
    }

    @Override
    public P remove(Pos p)
    {
    	if( this.isModifiable() )
        {
        	if( p == null )
        		throw new NullPointerException();
        	
        	if( !this.isPos(p) )
        		throw new IllegalArgumentException();
        	
        	return this.pos_map.remove(p);
        }
        else
        	throw new UnsupportedOperationException();
    }
    
    /**
     * Ritorna una copia profonda della BoardOct
     * @return copia profonda della BoardOct
     */
    public BoardOct<P> copy()
    {
    	return new BoardOct<P>(this);
    }
}
