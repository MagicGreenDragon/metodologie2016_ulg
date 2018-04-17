package gapp.gui.util;

import java.util.Objects;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Quest'oggetto serve per facilitare la gestione dei dati nella tabella della partita,
 * utilizzano una {@link SimpleStringProperty} per il nome di un giocatore,
 * e una {@link SimpleDoubleProperty} per gestire il suo punteggio.
 * Questi dati vengono propriamente gestiti da un thread e posizionati in una tabella.
 * 
 * @author Gabriele Cavallaro
 *
 */
public class PlayerScore
{
    private final SimpleStringProperty playername;
    private final SimpleDoubleProperty playerscore;
    
    /**
     * Costruisce l'oggetto richiedendo solo un nome,
     * il punteggio verrà inizializzato a zero
     * 
     * @param playername
     * 
     * @throws NullPointerException se un parametro è nullo
     */
    public PlayerScore(String playername)
    {
        Objects.requireNonNull(playername);
        
        this.playername  = new SimpleStringProperty(playername);
        this.playerscore = new SimpleDoubleProperty(0f);
    }
    
    /**
     *  Costruisce l'oggetto richiedendo il nome del giocatore,
     *  e il suo attuale score.
     * 
     * @param playername
     * @param playerscore
     * 
     * @throws NullPointerException se un parametro è nullo
     */
    public PlayerScore(String playername, double playerscore)
    {
        Objects.requireNonNull(playername);
        Objects.requireNonNull(playerscore);
        
        this.playername  = new SimpleStringProperty(playername);
        this.playerscore = new SimpleDoubleProperty(playerscore);
    }
    
    //metodi accesso PlayerName
    
    /**
     * Ritorna il nome del giocatore
     * 
     * @return
     */
    public String getPlayerName()
    {
        return playername.get();
    }
    
    /**
     * Utilizzato dalla tabella per accedere al valore playername attraverso la {@link PropertyValueFactory}
     * 
     * @return {@link SimpleStringProperty}
     */
    public SimpleStringProperty playernameProperty() { return playername; }
    
    //metodi accesso PlayerScore
    
    /**
     * Ritorna l'attuale punteggio del giocatore
     * @return
     */
    public double getPlayerScore()
    {
        return playerscore.get();
    }

    /**
     * Permette di impostare l'attuale punteggio del giocatore
     * @param playerscore
     */
    public void setPlayerScore(double playerscore)
    {
        this.playerscore.set(playerscore);
    }
    
    /**
     * Utilizzato dalla tabella per accedere al valore playerscore attraverso la {@link PropertyValueFactory}
     * 
     * @return {@link SimpleDoubleProperty}
     */
    public SimpleDoubleProperty playerscoreProperty() { return playerscore; }
}
