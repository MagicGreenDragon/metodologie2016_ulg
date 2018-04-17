package gapp.ulg.game.util;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.board.GameRuler.Situation;

import static gapp.ulg.game.board.GameRuler.Next;
import static gapp.ulg.game.board.GameRuler.Mechanics;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ForkJoinTask;
import java.util.function.Function;

/** <b>IMPLEMENTARE I METODI INDICATI CON "DA IMPLEMENTARE" SECONDO LE SPECIFICHE
 * DATE NEI JAVADOC. Non modificare le intestazioni dei metodi.</b>
 * <br>
 * Metodi per analizzare giochi */
public class Probe
{
    /** Un oggetto {@code EncS} è la codifica compatta di una situazione di gioco
     * {@link GameRuler.Situation}. È utile per mantenere in memoria insiemi con
     * moltissime situazioni minimizzando la memoria richiesta.
     * @param <P>  tipo del modello dei pezzi */
    public static class EncS<P>
    {
    	/** Array di byte che rappresenta una situazione codificata */
    	public final byte[] encoded;
    	
    	/** Crea una codifica compatta della situazione data relativa al gioco la
         * cui meccanica è specificata. La codifica è compatta almeno quanto quella
         * che si ottiene codificando la situazione con un numero e mantenendo in
         * questo oggetto solamente l'array di byte che codificano il numero in
         * binario. Se i parametri di input sono null o non sono tra loro compatibili,
         * il comportamento è indefinito.
         * @param gM  la meccanica di un gioco
         * @param s  una situazione dello stesso gioco */
        public EncS(Mechanics<P> gM, Situation<P> s)
        {
        	BigInteger enc = BigInteger.ZERO, base = BigInteger.valueOf(gM.pieces.size()+1);
        	
        	for( Pos p : gM.positions )
        		enc = enc.multiply(base).add(BigInteger.valueOf(gM.pieces.indexOf(s.get(p))+1));
        	
        	this.encoded = enc.multiply(BigInteger.valueOf(2*gM.np+1)).add(BigInteger.valueOf(s.turn+gM.np)).toByteArray();
        }

        /** Ritorna la situazione codificata da questo oggetto. Se {@code gM} è null
         * o non è la meccanica del gioco della situazione codificata da questo
         * oggetto, il comportamento è indefinito.
         * @param gM  la meccanica del gioco a cui appartiene la situazione
         * @return la situazione codificata da questo oggetto */
        public Situation<P> decode(Mechanics<P> gM)
        {
        	BigInteger base = BigInteger.valueOf(gM.pieces.size()+1);
            BigInteger[] div_result = new BigInteger(this.encoded).divideAndRemainder(BigInteger.valueOf(2*gM.np+1));
            
            Map<Pos, P> conf_map = new HashMap<>();
            int remainder, turn = div_result[1].intValue()-gM.np;
            
            for( int i = gM.positions.size()-1 ; i >= 0 ; i-- )
            {
            	div_result = div_result[0].divideAndRemainder(base);
            	remainder = div_result[1].intValue();
                
                if (remainder > 0)
                	conf_map.put(gM.positions.get(i), gM.pieces.get(remainder-1));
            }
            
            return new Situation<>(conf_map, turn);
        }

        /** Questa oggetto è uguale a {@code x} se e solo se {@code x} è della stessa
         * classe e la situazione codificata è la stessa. Il test è effettuato senza
         * decodificare la situazione, altrimenti sarebbe troppo lento.
         * @param x  un oggetto
         * @return true se {@code x} rappresenta la stessa situazione di questo
         * oggetto */
        @Override
        public boolean equals(Object x)
        {
            if( x!=null && this.getClass() == x.getClass() )
            {
            	EncS<?> other = (EncS<?>)x;
            	
            	return Arrays.equals(this.encoded, other.encoded);
            }
        	
        	return false;
        }

        /** Ridefinito coerentemente con la ridefinizione di {@link EncS#equals(Object)}.
         * @return l'hash code di questa situazione codificata */
        @Override
        public int hashCode()
        {
        	return Arrays.hashCode(this.encoded);
        }
    }
    
    /** Un oggetto per rappresentare il risultato del metodo
     * {@link Probe#nextSituations(boolean, Next, Function, Function, Set)}.
     * Chiamiamo grado di una situazione <i>s</i> il numero delle prossime situazioni
     * a cui si può arrivare dalla situazione <i>s</i>.
     * @param <S>  tipo della codifica delle situazioni */
    public static class NSResult<S>
    {
        /** Insieme delle prossime situazioni */
        public final Set<S> next;
        /** Statistiche: il minimo e il massimo grado delle situazioni di partenza
         * e la somma di tutti gradi */
        public final long min, max, sum;

        public NSResult(Set<S> nx, long mn, long mx, long s)
        {
            next = nx;
            min = mn;
            max = mx;
            sum = s;
        }
    }

    /** Ritorna l'insieme delle prossime situazioni dell'insieme di situazioni date.
     * Per ogni situazione nell'insieme {@code start} ottiene le prossime situazioni
     * tramite {@code nextF}, previa decodifica con {@code dec}, e le aggiunge
     * all'insieme che ritorna, previa codifica con {@code cod}. La computazione può
     * richiedere tempi lunghi per questo è sensibile all'interruzione del thread
     * in cui il metodo è invocato. Se il thread è interrotto, il metodo ritorna
     * immediatamente o quasi, sia che l'esecuzione è parallela o meno, e ritorna
     * null. Se qualche parametro è null o non sono coerenti (ad es. {@code dec} non
     * è il decodificatore del codificatore {@code end}), il comportamento è
     * indefinito.
     * @param parallel  se true il metodo cerca di sfruttare il parallelismo della
     *                  macchina
     * @param nextF  la funzione che ritorna le prossime situazioni di una situazione
     * @param dec  funzione che decodifica una situazione
     * @param enc  funzione che codifica una situazione
     * @param start  insieme delle situazioni di partenza
     * @param <P>  tipo del modello dei pezzi
     * @param <S>  tipo della codifica delle situazioni
     * @return l'insieme delle prossime situazioni dell'insieme di situazioni date o
     * null se l'esecuzione è interrotta. */
    public static <P,S> NSResult<S> nextSituations(boolean parallel, Next<P> nextF,
                                                   Function<S,Situation<P>> dec,
                                                   Function<Situation<P>,S> enc,
                                                   Set<S> start)
    {
    	long min=Integer.MAX_VALUE, max=Integer.MIN_VALUE, sum=0, size;
    	
    	List<S> result;
    	Set<S> next = new HashSet<>();
    	
    	if(!parallel)
    	{
    		for( S start_situation : start)
        	{
    			result = getNextSetEncoded(start_situation, nextF, dec, enc);
    			
    			if ( Thread.currentThread().isInterrupted() || result==null )
        			return null;
    			
    			// Codifico le situazioni ottenute e le inserisco nell'insieme
        		next.addAll(result);
    			
        		// Numero mosse possibili nella situazione ottenuta (grado della situazione)
    			size = result.size();
    			
        		// Aggiorno il grado minimo, massimo, e la somma dei gradi
        		if( size<min )
        			min = size;
        		if( size>max )
        			max = size;
        		
        		sum += size;
        	}
    	}
    	else
    	{
    		List<ForkJoinTask<List<S>>> tasks = new ArrayList<>();
        	
        	// Per ogni situazione di partenza, ottengo le situazioni successive
        	for( S start_situation : start)
        	{
        		tasks.add(ForkJoinTask.adapt( () -> getNextSetEncoded(start_situation, nextF, dec, enc) ));
        	}
    		
        	if( Thread.currentThread().isInterrupted() )
    		{
        		tasks.forEach( (tt) -> tt.cancel(true) );
        		return null;
    		}
        	
        	try
        	{
        		for( ForkJoinTask<List<S>> t : ForkJoinTask.invokeAll(tasks) )
            	{
        			result = t.join();
        			
        			if( Thread.currentThread().isInterrupted() || result==null )
        			{
        				tasks.forEach( (tt) -> tt.cancel(true) );
        				return null;
        			}
        			
        			// Aggiorno l'insieme finale con quello parziale
            		next.addAll(result);
            		
            		// Numero mosse possibili per la situazione attuale (grado della situazione)
        			size = result.size();
            		
            		// Aggiorno il grado minimo, massimo, e la somma dei gradi
            		if( size<min )
            			min = size;
            		if( size>max )
            			max = size;
            		
            		sum += size;
            	}
        	}
        	catch( CancellationException e )
        	{
        		tasks.forEach( (tt) -> tt.cancel(true) );
        		return null;
        	}
    	}
    	
    	return new NSResult<>(next, min, max, sum);
    }
    
    /** Metodo statico che da una situazione codificata restituisce l'insieme delle prossime situazioni (sempre codificate)
     * @param start situazione codificata
     * @param nextF la funzione che ritorna le prossime situazioni di una situazione
     * @param dec funzione che decodifica una situazione
     * @param enc funzione che codifica una situazione
     * @return lista delle situazioni successive codificate
     */
    public static <P,S> List<S> getNextSetEncoded(S start, Next<P> nextF, Function<S,Situation<P>> dec, Function<Situation<P>,S> enc)
    {
    	if( Thread.currentThread().isInterrupted() )
			return null;
    	
    	List<S> res = new ArrayList<>();
    	
    	// Ottengo tutte le prossime situazioni...
    	for( Situation<P> next_s : nextF.get(dec.apply(start)).values() )
    	{
    		if( Thread.currentThread().isInterrupted() )
    			return null;
    		
    		// ...le codifico e le inserisco nell'insieme
    		res.add(enc.apply(next_s));
    	}
    	
    	return res;
    }
}
