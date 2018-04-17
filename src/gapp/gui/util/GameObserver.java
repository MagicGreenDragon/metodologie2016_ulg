package gapp.gui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.util.PlayGUI.Observer;
import javafx.application.Platform;

/**
 * 
 * Ultima modifica: Mattina - 02/03/2016
 * @author Gabriele Cavallaro
 *
 * @param <P> tipo del modello dei pezzi
 */
public class GameObserver<P> implements Observer<P>
{
    /** {@link GameRuler} del gioco collegato alla classe PlayGUI.*/
    private volatile GameRuler<P> gameruler;
    
    /** Lista degli {@link ObserverListener} */
    private final List<ObserverListener<P>> listeners;
    
    // Lock
    private final AtomicBoolean isready;
    private final AtomicBoolean gameended;

    public interface ObserverListener<P>
    {
        void moved(Move<P> m); 
        
        void gameEnded();
    }
 
    //--------------------
    //METODI COSTRUTTORI
    //--------------------    
    
    /**
     * Costruisce l'oggetto {@link GameObserver<P>} che implementa l'interfaccia {@link Observer<P>},
     * Quest'oggetto rimane comunque inutilizzabile finché non viene impostata una partita.
     * Nel caso venga fatta una chiamata a un metodo di quest'oggetto quando non è ancora pronto,
     * non ritorna errori ma blocca la richiesta finché non viene impostato un gioco.
     */
    public GameObserver()
    {
        isready = new AtomicBoolean(false);
        gameended = new AtomicBoolean(false);
        listeners = new ArrayList<>();
    }
    
    @Override
    public void setGame(GameRuler<P> g)
    {
        synchronized(isready)
        {
            gameruler = g;     
            
            isready.set(true);            
            isready.notifyAll();
        }        
    }
    
    //--------------------
    //METODI D'INTERFACCIA
    //--------------------

    @Override
    public void moved(int i, Move<P> m)
    {
        Objects.requireNonNull(m);
        
        if( gameruler == null || gameruler.result() != -1 )
            throw new IllegalStateException();
        
        if( i<1 || i>gameruler.players().size() || !gameruler.isValid(m) )
            throw new IllegalArgumentException();
        
        waitReadyLock();  
        
        gameruler.move(m);   
        
        //listener calls
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized(listeners)
                {
                    for (ObserverListener<P> l : listeners) l.moved(m);
                    
                    if(gameruler.turn() == 0 && !gameended.get())
                    {
                        for (ObserverListener<P> l : listeners) l.gameEnded();
                        gameended.set(true);
                    }
                }
            }
        });
    }

    @Override
    public void limitBreak(int i, String msg)
    {        
       if(msg==null)
           throw new NullPointerException();
       if(i<1 || i>gameruler.players().size())
           throw new IllegalArgumentException(); 
       
       waitReadyLock();  
       
       //listener calls
       Platform.runLater(new Runnable()
       {
           @Override
           public void run()
           {
               synchronized(listeners)
               {
                   if(!gameended.get())
                       for (ObserverListener<P> l : listeners) l.gameEnded();
                   
                   gameended.set(true);
               }
           }
       });
       
       // Nota: il professore ha detto sul googlegroup che qui non serve eseguire la mossa resign
       // Note: MA nella descrizione del metodo limitBreak bisogna eseguire Move.Kind#RESIGN, quindi per evitare una gestione interna del vincitore..
       gameruler.move(new Move<P>(Move.Kind.RESIGN));
    }

    @Override
    public void interrupted(String msg)
    {   
        waitReadyLock(); 
        
        //listener calls
        Platform.runLater(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized(listeners)
                {
                    if(!gameended.get())
                        for (ObserverListener<P> l : listeners) l.gameEnded();
                    
                    gameended.set(true);
                }
            }
        });    
    }
    
    //--------------------
    //METODI DI GET
    //--------------------    

    /**
     * Ritorna una board immodificabile che rappresenta lo stato attuale del gioco.
     * Questa è una semplice chiamata sul gameruler del gioco che si sta osservando.
     * 
     * @return Board<P> 
     */
    public Board<P> getBoardView()
    {
        waitReadyLock(); 
        
        return gameruler.getBoard();
    }
    
    /**
     * Ritorna la lista dei giocatori dal gamerueler.
     * 
     * @return lista dei giocatori
     */
    public List<String> getPlayerList()
    {
        waitReadyLock();  
        
        return gameruler.players();
    }
    
    /**
     * Ritorna la lista dei colori dei pezzi presenti nella partita,
     * se i giocatori non hanno dei colori specifici per i pezzi ritorna null.
     * 
     * @return lista dei colori dei pezzi o null
     */
    public List<String> getPiecesColors()
    {
        waitReadyLock(); 
        
        List<String> tmplst = new ArrayList<String>();
        
        for(String player_name: gameruler.players())
        {
            if(gameruler.color(player_name).equals(null)) return null;
            tmplst.add(gameruler.color(player_name));
        }
        
        return tmplst;
    }
    
    /**
     * Ritorna le posizioni incluse nella board del gioco.
     * 
     * @return posizioni incluse
     */
    public List<Pos> getincludedPosition()
    {
        waitReadyLock(); 
        
        return gameruler.getBoard().positions();
    }
    
    public String getPlayerName(int i)
    {
        synchronized(isready)
        {
            try
            {
                if(!isready.get()) isready.wait();
            }
            catch (InterruptedException e)
            {
                //andrebbe gestito..
            }
        }  
        
        return gameruler.players().get(i);
    }
    
    /**
     * Ritorna l'indice del giocatore che ha vinto la partita.
     * Questa è una semplice chiamata sul gameruler del gioco che si sta osservando,
     * Questo caso chiama il metodo result()
     * 
     * @return int, può essere sia positivo che negativo
     */
    public int getWinnerIndex()
    {
        waitReadyLock();  
        
        return gameruler.result();
    }
    
    /**
     * Ritorna il nome del giocatore che ha vinto la partita.
     * 
     * @return String
     */
    public String getWinnerName()
    {
        waitReadyLock();  
        
        try
        {
            return gameruler.players().get(gameruler.result()-1);
        }
        catch(Exception e)
        {
            return "";
        }
        
    }
    
    /**
     * Ritorna un valore boleano che rappresenta se nella paritita che si sta osservando,
     * sono presenti dei punteggi.
     * Ritorna sempre
     * 
     * @return boolean
     */
    public boolean gameHasScores()
    {
        waitReadyLock();  
        
        try
        {
            gameruler.score(1);
            return true;
        }
        catch(UnsupportedOperationException uoe)
        {
            return false;
        }
    }
    
    /**
     * Ritorna gli score dei giocatori,
     * Se non possibile, ritorna null. 
     * 
     * @return double[]
     */
    public double[] getScores()
    {
        waitReadyLock();  
        
        try
        {
            double[] scores = new double[gameruler.players().size()];
            
            for(int i=0;i<gameruler.players().size();i++)
                scores[i]=gameruler.score(i+1);
            
            return scores;
        }
        catch(UnsupportedOperationException uoe)
        {
            return null;
        }
    }
    
    //--------------------
    //METODI DI SET
    //--------------------
    
    /**
     * Aggiunge un listener su questo oggetto di tipo {@link ObserverListener<P>},
     * Questo listener permette di intercettare quando viene chiamata una mossa,
     * e quando viene conclusa la partita.
     * Le chiamate ai listener utilizzano autonomamente Platform.runLater().
     * 
     * @param ol
     */
    public void addNewListener(ObserverListener<P> ol)
    {
        synchronized(listeners)
        {
            listeners.add(ol);
        }
    }
    
    //--------------------
    //ALTRI METODI
    //--------------------

    /**
     * Blocca le funzioni chiamate finchè non viene impostato un gioco
     */
    public void waitReadyLock()
    {
        synchronized(isready)
        {
            try
            {
                if(!isready.get()) isready.wait();
            }
            catch (InterruptedException e)
            {
                //andrebbe gestito..
            }
        }          
    }
}
