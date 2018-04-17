package gapp.ulg.play;

import java.util.Objects;
import java.util.Random;
import java.util.Set;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Player;
import gapp.ulg.game.board.Move.Kind;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi.</b>
 * <br>
 * Un oggetto RandPlayer è un oggetto che può giocare un qualsiasi gioco regolato
 * da un {@link GameRuler} perché, ad ogni suo turno, sceglie in modo random una
 * mossa tra quelle valide esclusa {@link Move.Kind#RESIGN}.
 * @param <P>  tipo del modello dei pezzi */
public class RandPlayer<P> implements Player<P>
{
	/** Nome del giocatore.*/
	private final String name;
	/** Copia del {@link GameRuler} del gioco.*/
    private GameRuler<P> g;
    
	/** Crea un giocatore random, capace di giocare a un qualsiasi gioco, che ad
     * ogni suo turno fa una mossa scelta in modo random tra quelle valide.
     * @param name  il nome del giocatore random
     * @throws NullPointerException se name è null */
    public RandPlayer(String name)
    {
    	Objects.requireNonNull(name);
    	
        this.name = name;
    }

    @Override
    public String name()
    {
    	return this.name;
    }

    @Override
    public void setGame(GameRuler<P> g)
    {
    	Objects.requireNonNull(g);
    	
        if( g.result() != -1 )
    		throw new IllegalArgumentException();
        
    	this.g = g;
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

    @SuppressWarnings("unchecked")
	@Override
    public Move<P> getMove()
    {
    	// Se non c'è un gioco impostato, c'è ma è terminato, 
    	// o se questo non e' il turno del giocatore, solleva l'eccezione
    	if( this.g==null || this.g.result()!=-1 || !this.g.players().get(this.g.turn()-1).equals(this.name) )
        	throw new IllegalStateException();
    	
    	// Ottengo il set delle mosse valide e lo converto in un array
    	Set<Move<P>> moves = this.g.validMoves();
    	
    	Move<P>[] arr_moves = moves.toArray( new Move[moves.size()] );
    	
    	Random rand = new Random();
    	int i;
    	
    	// Si assicura che la mossa non sia una RESIGN
    	do
    	{
    		i = rand.nextInt(arr_moves.length);
    	}while( arr_moves[i].kind == Kind.RESIGN );
    	
    	return arr_moves[i];
    	
    }
}
