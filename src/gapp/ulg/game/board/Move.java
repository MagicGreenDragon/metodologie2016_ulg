package gapp.ulg.game.board;

import java.util.*;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi nè i campi pubblici.</b>
 * <br>
 * Un oggetto Move rappresenta una mossa di un giocatore nel suo turno di gioco.
 * Gli oggetti Move sono immutabili. Le mosse possono essere di vari tipi
 * {@link Move.Kind}, il tipo più importante è {@link Move.Kind#ACTION} che
 * rappresenta una mossa che cambia la disposizione di uno o più pezzi sulla board
 * del gioco. Una mossa di tipo {@link Move.Kind#ACTION} è composta da una sequenza
 * di una o più azioni, cioè oggetti di tipo {@link Action}, ad es. la mossa di una
 * pedina nella Dama che salta e mangia un'altra pedina è composta da un'azione di
 * tipo {@link Action.Kind#JUMP} seguita da un'azione di tipo
 * {@link Action.Kind#REMOVE}.
 * @param <P>  tipo del modello dei pezzi */
public class Move<P>
{
    /** Tipi di una mossa */
    public enum Kind
    {
        /** Effettua una o più azioni ({@link Action}) */
        ACTION,
        /** Passa il turno di gioco */
        PASS,
        /** Abbandona il gioco, cioè si arrende */
        RESIGN
    }

    /** Tipo della mossa, non è mai null */
    public final Kind kind;
    /** Sequenza di azioni della mossa, non è mai null, la lista non è vuota
     * solamente se il tipo della mossa è {@link Kind#ACTION}, la lista è
     * immodificabile */
    public final List<Action<P>> actions;

    /** Crea una mossa che non è di tipo {@link Kind#ACTION}.
     * @param k  tipo della mossa
     * @throws NullPointerException se k è null
     * @throws IllegalArgumentException se k è {@link Kind#ACTION} */
    public Move(Kind k)
    {
    	if( k == null )
        	throw new NullPointerException();
    	
    	if( k == Kind.ACTION )
        	throw new IllegalArgumentException();
    	
    	this.kind = k;
    	this.actions = Collections.emptyList();
    }

    /** Crea una mossa di tipo {@link Kind#ACTION}.
     * @param aa  la sequenza di azioni della mossa
     * @throws NullPointerException se una delle azioni è null
     * @throws IllegalArgumentException se non è data almeno un'azione */
    @SafeVarargs
    public Move(Action<P>...aa)
    {
    	if( aa == null )
        	throw new NullPointerException();
    	
    	for( Action<P> a : aa )
        {
        	if( a == null )
            	throw new NullPointerException();
        }
    	
    	if( aa.length == 0 )
    		throw new IllegalArgumentException();
    	
    	this.kind = Kind.ACTION;
    	this.actions = Arrays.asList(aa);
    }

    /** Crea una mossa di tipo {@link Kind#ACTION}. La lista aa è solamente letta e
     * non è mantenuta nell'oggetto creato.
     * @param aa  la sequenza di azioni della mossa
     * @throws NullPointerException se aa è null o una delle azioni è null
     * @throws IllegalArgumentException se non è data almeno un'azione */
    @SuppressWarnings("unchecked")
	public Move(List<Action<P>> aa)
    {
    	if( aa == null )
        	throw new NullPointerException();
    	
    	if( aa.size() == 0 )
    		throw new IllegalArgumentException();
    	
    	Action<P>[] temp = new Action[aa.size()];
    	
    	int i = 0;
    	for( Action<P> a : aa )
        {
        	if( a == null )
            	throw new NullPointerException();
        	
        	temp[i] = a;
        	i++;
        }
    	
    	this.kind = Kind.ACTION;
    	this.actions = Arrays.asList(temp);
    }

    /** Ritorna true se e solo se x è un oggetto di tipo {@link Move} ed ha gli
     * stessi valori dei campi {@link Move#kind} e {@link Move#actions}.
     * @param x  un oggetto (o null)
     * @return true se x è uguale a questa mossa */
    @Override
    public boolean equals(Object x)
    {
    	if( x != null && this.getClass() == x.getClass() )
    	{
        	Move<?> m = (Move<?>)x;
    		
    		return (this.kind == m.kind) && (this.actions.equals(m.actions));
    	}
        else
        	return false;
    }

    /** Ridefinito coerentemente con la ridefinizione di
     * {@link PieceModel#equals(Object)}.
     * @return hash code di questa mossa */
    @Override
    public int hashCode()
    {
    	return Objects.hash(this.kind, this.actions);
    }
    
    /** Ritorna la mossa rappresentata come stringa.
     * @return stringa che rappresenta la mossa */
    @Override
    public String toString()
    {
    	switch(this.kind)
    	{
			case ACTION: return this.actions.toString();
			case PASS: return "PASS";
			case RESIGN: return "RESIGN";
			default: return "MOSSA_MALFORMATA("+this.hashCode()+")";
    	}
    }
}
