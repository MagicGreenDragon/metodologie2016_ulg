package gapp.ulg.game.board;

import java.io.Serializable;
import java.util.Objects;

/** <b>IMPLEMENTARE I METODI SECONDO LE SPECIFICHE DATE NEI JAVADOC. Non modificare
 * le intestazioni dei metodi nè i campi pubblici.</b>
 * <br>
 * Un oggetto PieceModel rappresenta un modello di pezzo. Ad esempio il modello
 * di un pezzo per gli scacchi come l'alfiere (bishop) bianco. Gli oggetti
 * PieceModel sono immutabili.
 * @param <S>  il tipo dei valori di specie dei modelli di pezzi */
public class PieceModel<S extends Enum<S>> implements Serializable
{
	/** Le specie di alcuni tra i pezzi più comuni */
    public enum Species
    {
        DISC, DAMA, PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
    }

    /** Specie del modello di pezzo, non è mai null */
    public final S species;
    /** Colore del modello di pezzo, non è mai null */
    public final String color;

    /** Crea un modello di pezzo con le informazioni date.
     * @param s  la specie
     * @param c  il colore
     * @throws NullPointerException se s o c è null */
    public PieceModel(S s, String c)
    {
    	Objects.requireNonNull(s);
    	Objects.requireNonNull(c);
    	
        this.species = s;
        this.color = c;
    }

    /** Ritorna true se e solo se x è un oggetto di tipo compatibile con
     * PieceModel<?> ed ha gli stessi {@link PieceModel#species} e
     * {@link PieceModel#color}.
     * @param x  un oggetto (o null)
     * @return true x se è uguale a questo modello di pezzo */
    @Override
    public boolean equals(Object x)
    {
    	if( x != null && this.getClass() == x.getClass() )
        {
        	PieceModel<?> pm = (PieceModel<?>)x;
    		
    		return (this.species == pm.species) && (this.color.equals(pm.color));
    	}
        else
        	return false;
    }

    /** Ridefinito coerentemente con la ridefinizione di
     * {@link PieceModel#equals(Object)}.
     * @return hash code di questo modello di pezzo */
    @Override
    public int hashCode()
    {
    	return Objects.hash(this.species, this.color);
    }
    
    /** Ritorna il pezzo rappresentato come stringa.
     * @return stringa che rappresenta il pezzo */
    @Override
    public String toString()
    {
    	return "("+this.species.toString()+","+this.color.toLowerCase()+")";
    }
}
