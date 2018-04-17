package gapp.ulg.game.board;

import java.io.Serializable;
import java.util.Objects;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi nè i campi pubblici.</b>
 * <br>
 * Un oggetto Pos rappresenta una posizione in una {@link Board}. Gli oggetti
 * Pos sono immutabili. Per il significato delle coordinate vedere {@link Board} e
 * la documentazione relative agli elementi di {@link Board.System}. */
public class Pos implements Serializable
{
	/** Le coordinate della posizione, b è la coordinata rispetto all'asse base
     * e t quella relativa all'asse trasversale. */
    public final int b, t;

    /** Crea una posizione con le coordinate date.
     * @param b  coordinata asse base (non negativa)
     * @param t  coordinata asse trasversale (non negativa)
     * @throws IllegalArgumentException se una delle coordinate è nagativa */
    public Pos(int b, int t)
    {
    	if( b < 0 || t < 0 )
        	throw new IllegalArgumentException();
        
        this.b = b;
        this.t = t;
    }

    /** Questa posizione è uguale a x se e solo se x è della classe {@link Pos}
     * ed ha le stesse coordinate.
     * @param x un oggetto (o null)
     * @return true se x è uguale a questa posizione */
    @Override
    public boolean equals(Object x)
    {
    	if( x != null && this.getClass() == x.getClass() )
    	{
    		Pos p = (Pos)x;
    		
    		return (this.b == p.b) && (this.t == p.t);
    	}
        else
        	return false;
    }

    /** Ridefinito coerentemente con la ridefinizione di {@link Pos#equals(Object)}.
     * @return l'hash code di questa posizione */
    @Override
    public int hashCode()
    {
    	return Objects.hash(this.b, this.t);
    }
    
    /** Ritorna la posizione rappresentata come stringa.
     * @return stringa che rappresenta la posizione */
    @Override
    public String toString()
    {
    	return "("+this.b+","+this.t+")";
    }
}
