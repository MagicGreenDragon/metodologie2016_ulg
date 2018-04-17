package gapp.ulg.play;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Pos;
import gapp.ulg.play.OptimalPlayerFactory.Strategy;
import gapp.ulg.game.board.GameRuler.Mechanics;
import gapp.ulg.game.board.GameRuler.Next;
import gapp.ulg.game.board.GameRuler.Situation;

/** Implementazione di una strategia */
public class OptimalStrategy<P> implements Strategy<P>
{
	/** Versione Serializzabile di una strategia codificata (clone di EncS) */
	@SuppressWarnings("serial")
	public static class SitEnc<P> implements Serializable
	{
		private final byte[] encoded;
		
		public SitEnc(Mechanics<P> gM, Situation<P> s)
		{
			BigInteger enc = BigInteger.ZERO, base = BigInteger.valueOf(gM.pieces.size()+1);
        	
        	for( Pos p : gM.positions )
        		enc = enc.multiply(base).add(BigInteger.valueOf(gM.pieces.indexOf(s.get(p))+1));
        	
        	this.encoded = enc.multiply(BigInteger.valueOf(2*gM.np+1)).add(BigInteger.valueOf(s.turn+gM.np)).toByteArray();
		}
		
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
		
		@Override
        public boolean equals(Object x)
        {
            if( x!=null && this.getClass() == x.getClass() )
            {
            	SitEnc<?> other = (SitEnc<?>)x;
            	
            	return Arrays.equals(this.encoded, other.encoded);
            }
        	
        	return false;
        }
		
        @Override
        public int hashCode()
        {
        	return Arrays.hashCode(this.encoded);
        }
	}
	
	/** Possibili vincitori di un gioco */
    public enum Winner
    {
        PLAYER_A,
        PLAYER_B,
        NONE
    }
	
    /** Nome del gioco per cui vale questa strategia */
	private final String name;
	/** Mappa dellla strategia */
	private final Map<SitEnc<P>,Winner> strategy;
	/** Meccanica del gioco */
	private final Mechanics<P> gM;
	
	/** Crea una strategia ottimale con il nome, la meccanica del gioco, e la mappa della strategia
	 * @param gM meccanica del gioco
	 * @param name nome del gioco
	 * @param strategy mappa contenente la strategia
	 */
	public OptimalStrategy(Mechanics<P> gM, String name, Map<SitEnc<P>,Winner> strategy)
    {
		Objects.requireNonNull(name);
		Objects.requireNonNull(strategy);
		Objects.requireNonNull(gM);
		
		this.name = name;
    	this.strategy = strategy;
    	this.gM = gM;
    }
	
	@Override
    public String gName() { return this.name; }
	
    @Override
    public Move<P> move(Situation<P> s, Next<P> next)
    {
    	// Il limite di tempo per una mossa non è controllato, poiché ci mette sempre meno di un secondo
    	
    	Objects.requireNonNull(s);
    	Objects.requireNonNull(next);
    	
    	Winner prediction, att_player = s.turn==1 ? Winner.PLAYER_A : Winner.PLAYER_B;
    	Move<P> mossa_patta = null, lose = null, unknown = null;
    	
    	Map<Move<P>, Situation<P>> next_map = next.get(s);
    	
		for( Map.Entry<Move<P>, Situation<P>> entry : next_map.entrySet() )
		{
			prediction = this.strategy.get(new SitEnc<>(this.gM, entry.getValue()));
			
			if( prediction == att_player )
				return entry.getKey();
			else if( prediction == Winner.NONE )
				mossa_patta = entry.getKey();
			else if( prediction == null )
				unknown = entry.getKey();
			else
				lose = entry.getKey();
		}
		
		if( mossa_patta==null )
			return unknown==null ? lose : unknown;
		else
			return mossa_patta;
    }
    
	@Override
    public boolean equals(Object x)
    {
        if( x!=null && this.getClass() == x.getClass() )
        {
        	OptimalStrategy<?> other = (OptimalStrategy<?>)x;
        	
        	return this.name.equals(other.name) && this.gM.equals(other.gM) && this.strategy.equals(other.strategy);
        }
    	
    	return false;
    }

    @Override
    public int hashCode()
    {
    	return Objects.hash(this.gM, this.name, this.strategy);
    }
};