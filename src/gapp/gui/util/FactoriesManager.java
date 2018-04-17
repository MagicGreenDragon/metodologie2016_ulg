package gapp.gui.util;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Player;
import gapp.ulg.games.GameFactories;
import gapp.ulg.play.PlayerFactories;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.Utils;

/**
 * Classe per gestire tutte le factory del gioco.
 * Essa è utile per impostare preventivamente dei parametri nelle factory (compresi i path per le strategie).
 * La classe considera ogni factory con un nome univoco, quindi nei metodi in comune non è garantito un corretto funzionamento
 * per games factories e le players factories con lo stesso nome, 
 * perché si suppone che nessuna factory abbia lo stesso nome anche se in tipologie diverse,
 * questo da il vantaggio che i metodi in comune dalle factory possono essere chiamati senza specefiche pedantiche.
 * 
 * Ulteriori info:
 * 
 * https://groups.google.com/forum/#!topic/metodologieprogrammazioneal2016/0ysi-xkqbhQ
 * 
 * Ultima modifica: POMERIGGIO - 22/08/2016
 * 
 * @author Gabriele Cavallaro & Daniele Giudice
 */
public class FactoriesManager
{
    //mappe per salvaggio impostazioni
    private Map<String, GameFactory<GameRuler<PieceModel<Species>>>> gamesfactories_map;
    private Map<String, PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>>> playersfactories_map;
    private Map<String, Path> playersfactories_dir;
    
    //altro
    private ExecutorService confinement_thread;
    
    /**
     * Metodo costuttore che carica tutte le factory dentro il framework
     * nelle corrispetive mappe, accedibili attraverso i metodi della classe.
     * (Lavora sia sulle game factory che sulle player factory)
     */
    public FactoriesManager()
    {
        this.confinement_thread = Executors.newSingleThreadExecutor(Utils.DAEMON_THREAD_FACTORY);
    
        Future<?> future = this.confinement_thread.submit
        (
            () ->
                {
                    //carico le games factories
                    gamesfactories_map = new HashMap<String, GameFactory<GameRuler<PieceModel<Species>>>>();
                    for(String game_factory_name : GameFactories.availableBoardFactories())
                        gamesfactories_map.put(game_factory_name, GameFactories.getBoardFactory(game_factory_name));
                                        
                    //carico le players factories
                    playersfactories_map = new HashMap< String, PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> >();
                    for(String player_factory_name : PlayerFactories.availableBoardFactories())
                        playersfactories_map.put(player_factory_name, PlayerFactories.getBoardFactory(player_factory_name));
                    
                    //inizializzo dir dei player
                    playersfactories_dir = new HashMap<String, Path>();
                }
        );
        
        try
        {
            future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }
    }
    
    /**
     * Controlla se dall'ultimo caricamento o dal lancio del programma sono state inserite nuove factory,
     * quindi aggiorna il dizionario, non richide parametri può essere chiamato in qualsiasi momento,
     * e non modifica le factory già caricate.
     * (Lavora sia sulle game factory che sulle player factory)
     */
    public void checkandloadDifference()
    {
        Future<?> future = this.confinement_thread.submit
        (
            () ->
                {        
                    //carico differenze le games factories
                    Set<String> difference = new HashSet<String>(Arrays.asList(GameFactories.availableBoardFactories()));        
                    difference.removeAll(gamesfactories_map.keySet());
                    
                    for(String factory_name : difference)
                        gamesfactories_map.put(factory_name, GameFactories.getBoardFactory(factory_name));
                            
                    //carico differenze le players factories
                    difference = new HashSet<String>(Arrays.asList(PlayerFactories.availableBoardFactories()));       
                    difference.removeAll(playersfactories_map.keySet());
                    
                    for(String factory_name : difference)
                        playersfactories_map.put(factory_name, PlayerFactories.getBoardFactory(factory_name));
                }
        );     
        
        try
        {
            future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }
    }
    
    //--------------------
    //METODI DI GET SIA DI GAME FACTORY CHE PLAYER FACTORY
    //--------------------
    
    /**
     * Ritorna il numero delle game factories.
     * (Lavora solo sulle game factory)
     * 
     * @return
     */
    public int getGamesFactoriesLength()
    {
        checkandloadDifference();
        return gamesfactories_map.keySet().size();
    }
    
    /**
     * Ritorna il numero delle player factories.
     * (Lavora solo sulle player factory)
     * 
     * @return
     */
    public int getPlayerFactoriesLength()
    {
        checkandloadDifference();
        return playersfactories_map.keySet().size();
    }
    
    /**
     * Ritorna il numero delle game factories + player factories.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @return
     */
    public int getTotalFactoriesLength()
    {
        checkandloadDifference();
        return gamesfactories_map.keySet().size()+playersfactories_map.keySet().size();
    }
    
    /**
     * Inserito il nome di una game factory ritorna tutti i nomi dei suoi parametri,
     * nell'ordine in cui sono elencati.
     * (Lavora sia sulle game factory che sulle player factory)
     *      
     * @param factory_name
     * @param param_name
     * @return {@link String[]}
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public String[] getFactoryParams(String factory_name)
    {
        Future<String[]> future = this.confinement_thread.submit
        (
            () ->
                {         
                    String[] result;
                    
                    if( this.gamesfactories_map.containsKey(factory_name) )
                    {
                        result = this.gamesfactories_map.get(factory_name).params().stream().map(Param::name).toArray(String[]::new);
                    }
                    else
                    {
                        result = this.playersfactories_map.get(factory_name).params().stream().map(Param::name).toArray(String[]::new);
                    }
                    
                    return result;
                }
        );
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;            
    }
    
    /**
     * Ritorna se una certa factory ha un parametro di chiamato come param_name.
     * 
     * @param factory_name
     * @param param_name
     * @return
     */
    public boolean getFactoryHasParam(String factory_name, String param_name)
    {     
        try
        {
            getFactoryParamValue(factory_name, param_name);      
            return true;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    /**
     * Inserito il nome di una game factory e un nome di un parametro ritorna i valori ammissibili quel parametro,
     * nell'ordine in cui sono elencati.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @param factory_name
     * @param param_name
     * @return {@link Object[]}
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public Object[] getFactoryParamValues(String factory_name, String param_name)
    {
        Future<Object[]> future = this.confinement_thread.submit
        (
            () ->
                {         
                    Param<?> match;
                    
                    if( this.gamesfactories_map.containsKey(factory_name) )
                    {
                        match = this.gamesfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .filter(item -> item.name().equals(param_name))
                                                                            .findFirst()
                                                                            .orElse(null);
                    }
                    else
                    {
                        match = this.playersfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .filter(item -> item.name().equals(param_name))
                                                                            .findFirst()
                                                                            .orElse(null);
                    }
                    
                    return match.values().toArray();
                }
        );
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;            
    }
    
    /**
     * Inserito il nome di una game factory e un nome di un parametro ritorna il valore contenuto in quel parametro.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @param factory_name
     * @param param_name
     * @return Object valore del parametro
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public Object getFactoryParamValue(String factory_name, String param_name)
    {
        Future<Object> future = this.confinement_thread.submit
        (
            () ->
                {         
        
                	Param<?> match;
                	
                	if( this.gamesfactories_map.containsKey(factory_name) )
                    {
                		match = this.gamesfactories_map.get(factory_name).params()
            																.stream()
            																.filter(param -> param.name().equals(param_name))
            																.findFirst()
            																.orElse(null);
                    }
                    else
                    {
                        match = this.playersfactories_map.get(factory_name).params()
                        													.stream()
            																.filter(param -> param.name().equals(param_name))
            																.findFirst()
            																.orElse(null);
                    }
                	
                    return match.get();
                }
        );
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;            
    }
    
    /**
     * Conta il numero di parametri contenuti in una specifica factory.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @param factory_name
     * @param param_name
     * @return
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public long getFactoryParamCount(String factory_name)
    {        
        Future<Long> future = this.confinement_thread.submit
        (
            () ->
                {         
        
                    long count = 0;
                    
                    if( this.gamesfactories_map.containsKey(factory_name) )
                    {   
                        count = this.gamesfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .count();
                    }
                    else
                    {
                        count = this.playersfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .count();
                    }
                    
                    return count;
                }
        );
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return 0;            
    }
    
    /**
     * Ritorna la descrizione di un parametro indicato.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @param factory_name
     * @param param_name
     * @return {@link String}
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public String getFactoryParamPrompt(String factory_name, String param_name)
    {
        Future<String> future = this.confinement_thread.submit
        (
            () ->
                {         
                    Param<?> match;
                    
                    if( this.gamesfactories_map.containsKey(factory_name) )
                    {
                        match = this.gamesfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .filter(item -> item.name().equals(param_name))
                                                                            .findFirst()
                                                                            .orElse(null);
                    }
                    else
                    {
                        match = this.playersfactories_map.get(factory_name).params()
                                                                            .stream()
                                                                            .filter(item -> item.name().equals(param_name))
                                                                            .findFirst()
                                                                            .orElse(null);
                    }
                    
                    return match.prompt();
                }
        );
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;            
    }
    
    //--------------------
    //METODI DI GET SOLO GAME FACTORY
    //--------------------
    
    /**
     * Ritorna una game factory che può essere modificata,
     * modifica anche i parametri dentro il factoriesmanager,
     * quindi l'oggetto modificato si aggiornerà dentro al factoriesmanager,
     * quindi viene ritornato l'oggetto.
     * (Lavora solo sulle game factory)
     * 
     * @param factory_name
     * @return GameFactory<GameRuler<PieceModel<Species>>> 
     */
    public GameFactory<GameRuler<PieceModel<Species>>> getGameFactory(String factory_name)
    {
        return this.gamesfactories_map.get(factory_name);
    }
    
    /**
     * Ritorna il numero di giocatori minimi per una determinata game factory.
     * 
     * @param factory_name
     * @return {@link int}
     */
    public int getGameFactory_minPlayers(String factory_name)
    {
        return this.gamesfactories_map.get(factory_name).minPlayers();
    }
    
    /**
     * Ritorna il numero di giocatori massimi per una determinata game factory.
     * 
     * @param factory_name
     * @return {@link int}
     */
    public int getGameFactory_maxPlayers(String factory_name)
    {
        return this.gamesfactories_map.get(factory_name).maxPlayers();
    }
    
    //--------------------
    //METODI DI GET SOLO PLAYER FACTORY
    //--------------------
    
    /**
     * Ritorna una player factory che può essere modificata,
     * modifica anche i parametri dentro il factoriesmanager,
     * quindi l'oggetto modificato si aggiornerà dentro al factoriesmanager,
     * quindi viene ritornato l'oggetto.     * 
     * (Lavora solo sulle player factory)
     * 
     * @param factory_name
     * @return PlayerFactory<Player<PieceModel<Species>>, GameRuler<PieceModel<Species>>>
     */
    public PlayerFactory<Player<PieceModel<Species>>, GameRuler<PieceModel<Species>>> getPlayerFactory(String factory_name)
    {
        return this.playersfactories_map.get(factory_name);       
    }
    
    /**
     * Inserito il nome di una game factory e una player factory ritorna se quel giocatore può giocare a quel gioco.
     * (Lavora solo sulle player factory)
     * 
     * @param game_factory_name
     * @param player_factory_name
     * @return
     * 
     * @throws NullPointerException se non esiste il giocatore cercato
     */
    public PlayerFactory.Play getIfCanPlay(String game_factory_name, String player_factory_name)
    {
        Future<PlayerFactory.Play> future = this.confinement_thread.submit
        (
            () ->
                {         
                    return this.playersfactories_map.get(player_factory_name).canPlay(this.gamesfactories_map.get(game_factory_name));
                }
        );                     

        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;         
    }
    
    /**
     * Ritorna una mappa contenente come chiavi i nomi dei giocatori e come 
     * valori l'enum con il risultato del metodo di canPlay della player factory.
     * (Lavora solo sulle player factory)
     * 
     * @param game_factory_name
     * @return
     */
    public Map<String, PlayerFactory.Play> getHowCanPlay(String game_factory_name)
    {
        Future<Map<String, PlayerFactory.Play>> future = this.confinement_thread.submit
        (
            () ->
                {         
                    Map<String, PlayerFactory.Play> canplay_map = new HashMap<>();
                    
                    for(String pf : this.playersfactories_map.keySet())
                    {
                        canplay_map.put(pf, this.playersfactories_map.get(pf).canPlay(this.gamesfactories_map.get(game_factory_name)));
                    }
                    
                    return canplay_map;
                }
        ); 
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null;   
    }
    
    /**
     * Ritorna il path di un giocatore, se non ancora impostato ritorna null.
     * (Lavora solo sulle player factory)
     * 
     * @param game_factory_name
     * @return
     * 
     * @throws NullPointerException se non esiste il parametro cercato
     */
    public Path getPlayerFactoryDir(String game_factory_name)
    {
        Future<Path> future = this.confinement_thread.submit
        (
            () ->
                {  
                    return this.playersfactories_dir.get(game_factory_name);
                }
        ); 
        
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }                    
                    
        return null; 
    }
    
    //--------------------
    //METODI DI SET SIA DI GAME FACTORY CHE PLAYER FACTORY
    //--------------------
    
    /**
     * Passato un parametro di tipo Object, un nome di una factory e il nome del parametro,
     * imposta quel valore dentro quel parametro.
     * (Lavora sia sulle game factory che sulle player factory)
     * 
     * @param factory_name
     * @param param_name
     * @param param_value
     * 
     * @throws NullPointerException se non esiste il parametro da cambiare
     */
    public void setFactoryParam(String factory_name, String param_name, Object param_value)
    {
        Future<?> future = this.confinement_thread.submit
        (
            () ->
                { 
                	Param<?> match;
                	
                	if( this.gamesfactories_map.containsKey(factory_name) )
                    {
                		match = this.gamesfactories_map.get(factory_name).params()
            																.stream()
            																.filter(param -> param.name().equals(param_name))
            																.findFirst()
            																.orElse(null);
                    }
                    else
                    {
                        match = this.playersfactories_map.get(factory_name).params()
                        													.stream()
            																.filter(param -> param.name().equals(param_name))
            																.findFirst()
            																.orElse(null);
                    }
                	
                    match.set(param_value);
                }
        ); 
        
        try
        {
            future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }            
    }
    
    //--------------------
    //METODI DI SET SOLO PLAYER FACTORY
    //--------------------
    
    /**
     * Imposta la dirrectory di un giocatore, se già impostata la sovrascrive.     
     * (Lavora solo sulle player factory)
     * 
     * @param game_factory_name
     * @param dir
     */
    public void setPlayerFactoryDir(String player_factory_name, Path dir)
    {
        Future<?> future = this.confinement_thread.submit
        (
            () ->
                {         
                    this.playersfactories_dir.put(player_factory_name, dir);
                    this.playersfactories_map.get(player_factory_name).setDir(this.playersfactories_dir.get(player_factory_name));
                }
        );         
        
        try
        {
            future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }  
    }
    
    //--------------------
    //ALTRI METODI
    //--------------------
    
    /**
     * Cerca di calcolare la strategia per il gioco selezionato.
     * Ritorna null in caso di successo, altrimenti una stringa con scritto il perchè del fallimento.
     * I possibili return sono:
     * - INTERRUPTED -> il calcolo è stato interrotto tramite il Supplier
     * - OUT OF MEMORY -> il calcolo ha richiesto più memoria di quella disponibile
     * - FAILED! -> qualunque altra causa
     *
     * @param game_factory_name nome della game factory per cui si vuole calcolare la strategia
     * @param parallel flag che indica se si può utilizzare o meno il parallelismo
     * @param interrupt Supplier per interrompere il calcolo
     *
     * @return null se ha successo, altrimenti una stringa con scritto il perchè del fallimento
     */
    public String computeStrategy(String player_factory_name, String game_factory_name, boolean parallel, Supplier<Boolean> interrupt)
    {
        Future<String> future = this.confinement_thread.submit
        (
            () ->
                {        
                    this.playersfactories_map.get(player_factory_name).setDir(this.playersfactories_dir.get(player_factory_name));
                    return this.playersfactories_map.get(player_factory_name).tryCompute(this.gamesfactories_map.get(game_factory_name), parallel, interrupt);
                }
        );
       
        try
        {
            return future.get();
        }
        catch (InterruptedException ie)
        {
            ie.printStackTrace();
        }
        catch (ExecutionException ei)
        {
            ei.printStackTrace();
        }
       
        return "FAILED!";
    }
}
