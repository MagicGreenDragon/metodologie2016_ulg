package gapp.ulg.game.util;

import gapp.ulg.game.board.*;
import gapp.ulg.game.GameFactory;

import static gapp.ulg.game.board.PieceModel.Species;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/** <b>IMPLEMENTARE I METODI INDICATI CON "DA IMPLEMENTARE" SECONDO LE SPECIFICHE
 * DATE NEI JAVADOC. Non modificare le intestazioni dei metodi.</b>
 * <br>
 * Metodi di utilità */
public class Utils
{
	/**
     * ThreadFactory che genera thread demoni.
     * Utile per gli esecutori.
     */
    static public final ThreadFactory DAEMON_THREAD_FACTORY = runnable ->
    {
        Thread result = new Thread(runnable);
        result.setDaemon(true);
        return result;
    };
	
	/** Dato un tempo di partenza e un limite di tempo (entrambi in millisecondi), controlla se esso è stato superato.
	 * Se il limite di tempo è <= 0, si assume che non ci sia alcun limite.
     * @param st tempo di parternza (in millisecondi)
     * @param timeout tempo limite (in millisecondi)
     * @return se non ci sono limiti di tempo, ritorna sempre false. Se ci sono, ritorna true se il 
     * limite di tempo è stato superato, false altrimenti
     */
    public static boolean timeoutExceeded(long st, long timeout)
    {
    	return timeout<=0 ? false : (System.currentTimeMillis() - st) > timeout;
    }
	
	/** Verifica se ci sono posizioni duplicate in un array di Pos 
     * @param pp  array di posizioni da controllare
     * @return true se pp contiene duplicati, false altrimenti*/
    public static boolean duplicates(Pos...pp)
    {
		int i, j;
    	
    	for ( i=0 ; i<pp.length ; i++ )
	      for ( j=i+1 ; j<pp.length ; j++ )
	        if ( pp[i].equals(pp[j]) )
	          return true;
	    
	    return false;
    }
	
	/** Ritorna una view immodificabile della board b. Qualsiasi invocazione di uno
     * dei metodi che tentano di modificare la view ritornata lancia
     * {@link UnsupportedOperationException} e il metodo {@link Board#isModifiable()}
     * ritorna false. Inoltre essendo una view qualsiasi cambiamento della board b è
     * rispecchiato nella view ritornata.
     * @param b  una board
     * @param <P>  tipo del modello dei pezzi
     * @return una view immodificabile della board b
     * @throws NullPointerException se b è null */
    public static <P> Board<P> UnmodifiableBoard(Board<P> b)
    {
    	if( b == null )
        	throw new NullPointerException();
    	
        return new Board<P>()
	        {
	            @Override
	            public System system() { return b.system(); }
	            @Override
	            public int width() { return b.width(); }
	            @Override
	            public int height() { return b.height(); }
	            @Override
	            public Pos adjacent(Pos p, Dir d) { return b.adjacent(p, d); }
	            @Override
	            public List<Pos> positions() { return b.positions(); }
	            @Override
	            public P get(Pos p) { return b.get(p); }
	        };
    }

    /** Imposta i valori dei parametri specificati nella GameFactory gf, i nomi dei
     * giocatori pp poi ottiene il GameRuler dalla gf, passa a ogni giocatore una
     * copia del GameRuler e gioca la partita del GameRuler con i giocatori dati.
     * L'esito della partita sarà registrato nel GameRuler che è ritornato. Gli
     * eventuali parametri di gf non sono impostati.
     * @param gf  una GameFactory
     * @param pp  i giocatori
     * @param <P>  tipo del modello dei pezzi
     * @return il GameRuler usato per fare la partita
     * @throws NullPointerException se gf o uno degli elementi di pp è null
     * @throws IllegalArgumentException se il numero di giocatori in pp non è
     * compatibile con quello richiesto dalla GameFactory gf oppure se il valore di
     * un parametro è errato */
    @SafeVarargs
    public static <P> GameRuler<P> play(GameFactory<? extends GameRuler<P>> gf, Player<P>...pp)
    {
    	Objects.requireNonNull(gf);
    	Objects.requireNonNull(pp);
    	
        for( Player<P> player : pp )
        {
        	if( player == null )
        		throw new NullPointerException();
        }
        
        if( pp.length < gf.minPlayers() || pp.length > gf.maxPlayers() )
        	throw new IllegalArgumentException();
        
        // Ottengo i nomi dei giocatori in ordine
        String[] names = new String[pp.length];
        
        for( int i=0 ; i<pp.length ; i++ )
        	names[i] = pp[i].name();
        
        // Li setto nel GameFactory
        gf.setPlayerNames(names);
        
        // Ottengo il GameRuler
        GameRuler<P> gr = gf.newGame();
        
        // Passo a tutti i giocatori una copia del GameRuler
        for( Player<P> player : pp )
        	player.setGame( gr.copy() );
        
        // Indice di turnazione del giocatore
        int player_id;
        
        Move<P> m;
        
        /* Gioca la partita */
        while( gr.result() == -1 )
        {
        	// Aggiorna l'indice di turnazione
        	player_id = gr.turn();
        	
        	// Ottieni la mossa del giocatore attuale
        	m = pp[player_id-1].getMove();
        	
        	// Esegui la mossa
        	gr.move(m);
        	
			// Aggiorna tutti i GameRuler
        	for( Player<P> player : pp )
        		player.moved(player_id, m);
        }
        
    	return gr;
    }


    /** Ritorna un oggetto funzione che per ogni oggetto di tipo {@link PieceModel}
     * produce una stringa corta che lo rappresenta. Specificatamente la stringa
     * prodotta consiste di due caratteri il primo identifica la specie del pezzo e
     * il secondo il colore. Il primo carattere è determinato come segue per le
     * diverse specie:
     * <table>
     *     <tr><th>Specie</th><th>Carattere</th></tr>
     *     <tr><td>DISC</td><td>T</td></tr>
     *     <tr><td>DAMA</td><td>D</td></tr>
     *     <tr><td>PAWN</td><td>P</td></tr>
     *     <tr><td>KNIGHT</td><td>J</td></tr>
     *     <tr><td>BISHOP</td><td>B</td></tr>
     *     <tr><td>ROOK</td><td>R</td></tr>
     *     <tr><td>QUEEN</td><td>Q</td></tr>
     *     <tr><td>KING</td><td>K</td></tr>
     * </table>
     * Il secondo è il carattere iniziale del nome del colore. L'oggetto ritornato
     * dovrebbe essere sempre lo stesso.
     * @return un oggetto funzione per rappresentare tramite stringhe corte i
     * modelli dei pezzi di tipo {@link PieceModel} */
    public static Function<PieceModel<Species>,String> PieceModelToString()
    {
    	Function<PieceModel<PieceModel.Species>, String> pmToStr = pm ->
    	{
    		if( pm == null )
            	throw new NullPointerException();
    		
    		String species, color;
    	    
    	    switch(pm.species)
        	{
        		case DISC: 		species = "T";
        			break;
        		case DAMA: 		species = "D";
        			break;
        		case PAWN: 		species = "P";
        			break;
        		case KNIGHT: 	species = "J";
        			break;
        		case BISHOP: 	species = "B";
        			break;
        		case ROOK: 		species = "R";
        			break;
        		case QUEEN: 	species = "Q";
        			break;
        		case KING: 		species = "K";
        			break;
        		
        		default:		species = "?";
        	}
    	    
    	    if( pm.color.isEmpty() )
    	    	color = "";
    	    else
    	    	color = pm.color.substring(0, 1);
    	    
    	    return species + color;
    	};
    	
    	return pmToStr;
    }

    /** Ritorna un oggetto funzione che per ogni oggetto di tipo {@link Board} con
     * tipo del modello dei pezzi {@link PieceModel} produce una stringa rappresenta
     * la board. La stringa prodotta usa la funzione pmToStr per rappresentare i
     * pezzi sulla board.
     * @param pmToStr  funzione per rappresentare i pezzi
     * @return un oggetto funzione per rappresentare le board */
    public static Function<Board<PieceModel<Species>>,String> BoardToString(
            Function<PieceModel<Species>,String> pmToStr)
    {
    	Function<Board<PieceModel<PieceModel.Species>>, String> boardToStr = board ->
    	{
    		if( board == null )
            	throw new NullPointerException();
    		
    		String b = "";
    		PieceModel<PieceModel.Species> pm;
			Pos p;
			
			for( int i=board.height()-1 ; i>=0 ; i-- )
			{
	        	for( int j=0; j<board.width() ; j++ )
	        	{
	        		p = new Pos(j, i);
	        		
	        		pm = board.get(p);
	        		
	        		// Numeri di colonna
	        		if( j==0 )
	        		{
	        			if( i<10 )
	        				b += " "+i+"  ";
	        			else
	        				b += i+"  ";
	        		}
	        		
	        		// Contenuto cella
	        		if( !board.isPos(p) )
	        			b += "   ";
	        		else if( pm == null )
						b += ".  ";
					else
						b += pmToStr.apply(pm) + " ";
	        	}
	        	
	        	b += "\n";
			}
			
			// Numeri di riga
			b += "\n   ";
			for( int j=0; j<board.width() ; j++ )
        	{
				if( j<9 )
    				b += " "+j+" ";
				else if( j==9 )
    				b += " "+j+"  ";
    			else
    				b += j+" ";
        	}
			b += "\n";
			
			return b;
    	};
    	
    	return boardToStr;
    }

    /** Tramite UI testuale permette all'utente di scegliere dei valori per gli
     * eventuali parametri della GameFactory gf, chiede all'utente i nomi per i
     * giocatori che giocano tramite UI che sono np - pp.length, poi imposta tutti
     * gli np nomi nella gf e ottiene da gf il GameRuler. Infine usa il GameRuler
     * per giocare una partita visualizzando sulla UI testuale la board dopo ogni
     * mossa e chiedendo la mossa a ogni giocatore che gioca con la UI.
     * @param gf  una GameFactory
     * @param pToStr  funzione per rappresentare i pezzi
     * @param bToStr  funzione per rappresentare la board
     * @param np  numero totale di giocatori
     * @param pp  i giocatori che non giocano con la UI
     * @param <P>  tipo del modello dei pezzi
     * @return il GameRuler usato per fare la partita
     * @throws NullPointerException se gf, pToStr, bToStr o uno degli elementi di pp
     * è null
     * @throws IllegalArgumentException se np non è compatibile con il numero di
     * giocatori della GameFactory gf o se il numero di giocatori in pp è maggiore
     * di np */
    @SafeVarargs
    public static <P> GameRuler<P> playTextUI(GameFactory<GameRuler<P>> gf,
                                              Function<P,String> pToStr,
                                              Function<Board<P>,String> bToStr,
                                              int np, Player<P>...pp) {
        throw new UnsupportedOperationException("OPZIONALE");
    }
}
