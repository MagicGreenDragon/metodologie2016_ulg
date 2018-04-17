package gapp.ulg.game.util;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.*;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.games.GameFactories;
import gapp.ulg.play.PlayerFactories;
import gapp.ulg.play.PlayerGUIFactory;

import static gapp.ulg.game.util.PlayerGUI.MoveChooser;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Un {@code PlayGUI} è un oggetto che facilita la gestione di partite in una
 * applicazione controllata da GUI. Un {@code PlayGUI} segue lo svolgimento di una
 * partita dalla scelta della {@link GameFactory} e dei {@link PlayerFactory} e di
 * tutte le mosse fino alla fine naturale della partita o alla sua interruzione.
 * Inoltre, un {@code PlayGUI} aiuta sia a mantenere la reattività della GUI che a
 * garantire la thread-safeness usando un thread di confinamento per le invocazioni
 * di tutti i metodi e costruttori degli oggetti coinvolti in una partita.
 *
 * @param <P> tipo del modello dei pezzi
 */
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
public class PlayGUI<P>
{
	private Observer<P> observer;
    private long max_block_time;
    private ExecutorService confinement_thread;
    
    // Variabili da usare solo nel thread di confinamento
    private volatile GameFactory<GameRuler<PieceModel<Species>>> game_factory;
    private volatile List<PlayerFactory<Player<PieceModel<Species>>, GameRuler<PieceModel<Species>>>> player_factories;
    private volatile String[] player_names;
    private volatile List<Player<PieceModel<Species>>> players;
    private volatile GameRuler<PieceModel<Species>> game_ruler;
    
    /** Flag che indica se c'è una partita in corso */
    private volatile boolean is_match_in_progress;
    
    /**
     * Un {@code Observer} è un oggetto che osserva lo svolgimento di una o più
     * partite. Lo scopo principale è di aggiornare la GUI che visualizza la board
     * ed eventuali altre informazioni a seguito dell'inizio di una nuova partita e
     * di ogni mossa eseguita.
     *
     * @param <P> tipo del modello dei pezzi
     */
    public interface Observer<P>
    {
        /**
         * Comunica allo {@code Observer} il gioco (la partita) che sta iniziando.
         * Può essere nello stato iniziale o in uno stato diverso, ad es. se la
         * partita era stata sospesa ed ora viene ripresa. L'oggetto {@code g} è
         * una copia del {@link GameRuler} ufficiale del gioco. Lo {@code Observer}
         * può usare e modificare {@code g} a piacimento senza che questo abbia
         * effetto sul {@link GameRuler} ufficiale. In particolare lo {@code Observer}
         * può usare {@code g} per mantenersi sincronizzato con lo stato del gioco
         * riportando in {@code g} le mosse dei giocatori, vedi
         * {@link Observer#moved(int, Move)}. L'uso di {@code g} dovrebbe avvenire
         * solamente nel thread in cui il metodo è invocato.
         * <br>
         * <b>Il metodo non blocca, non usa altri thread e ritorna velocemente.</b>
         *
         * @param g un gioco, cioè una partita
         * @throws NullPointerException se {@code g} è null
         */
        void setGame(GameRuler<P> g);
        
        /**
         * Comunica allo {@code Observer} la mossa eseguita da un giocatore. Lo
         * {@code Observer} dovrebbe usare tale informazione per aggiornare la sua
         * copia del {@link GameRuler}. L'uso del GameRuler dovrebbe avvenire
         * solamente nel thread in cui il metodo è invocato.
         * <br>
         * <b>Il metodo non blocca, non usa altri thread e ritorna velocemente.</b>
         *
         * @param i indice di turnazione di un giocatore
         * @param m la mossa eseguita dal giocatore
         * @throws IllegalStateException    se non c'è un gioco impostato o c'è ma è
         *                                  terminato.
         * @throws NullPointerException     se {@code m} è null
         * @throws IllegalArgumentException se {@code i} non è l'indice di turnazione
         *                                  di un giocatore o {@code m} non è una mossa valida nell'attuale situazione
         *                                  di gioco
         */
        void moved(int i, Move<P> m);
        
        /**
         * Comunica allo {@code Observer} che il giocatore con indice di turnazione
         * {@code i} ha violato un vincolo sull'esecuzione (ad es. il tempo concesso
         * per una mossa). Dopo questa invocazione il giocatore {@code i} è
         * squalificato e ciò produce gli stessi effetti che si avrebbero se tale
         * giocatore si fosse arreso. Quindi lo {@code Observer} per sincronizzare
         * la sua copia con la partita esegue un {@link Move.Kind#RESIGN} per il
         * giocatore {@code i}. L'uso del GameRuler dovrebbe avvenire solamente nel
         * thread in cui il metodo è invocato.
         *
         * @param i   indice di turnazione di un giocatore
         * @param msg un messaggio che descrive il tipo di violazione
         * @throws NullPointerException     se {@code msg} è null
         * @throws IllegalArgumentException se {@code i} non è l'indice di turnazione
         *                                  di un giocatore
         */
        void limitBreak(int i, String msg);
        
        /**
         * Comunica allo {@code Observer} che la partita è stata interrotta. Ad es.
         * è stato invocato il metodo {@link PlayGUI#stop()}.
         *
         * @param msg una stringa con una descrizione dell'interruzione
         */
        void interrupted(String msg);
    }
    
    /**
     * Crea un oggetto {@link PlayGUI} per partite controllate da GUI. L'oggetto
     * {@code PlayGUI} può essere usato per giocare più partite anche con giochi e
     * giocatori diversi. Per garantire che tutti gli oggetti coinvolti
     * {@link GameFactory}, {@link PlayerFactory}, {@link GameRuler} e {@link Player}
     * possano essere usati tranquillamente anche se non sono thread-safe, crea un
     * thread che chiamiamo <i>thread di confinamento</i>, in cui invoca tutti i
     * metodi e costruttori di tali oggetti. Il thread di confinamento può cambiare
     * solo se tutti gli oggetti coinvolti in una partita sono creati ex novo. Se
     * durante una partita un'invocazione (ad es. a {@link Player#getMove()}) blocca
     * il thread di confinamento per un tempo superiore a {@code maxBlockTime}, la
     * partita è interrotta.
     * <br>
     * All'inizio e durante una partita invoca i metodi di {@code obs}, rispettando
     * le specifiche di {@link Observer}, sempre nel thread di confinamento.
     * <br>
     * <b>Tutti i thread usati sono daemon thread</b>
     *
     * @param obs          un osservatore del gioco
     * @param maxBlockTime tempo massimo in millisecondi di attesa per un blocco
     *                     del thread di confinamento, se < 0, significa nessun
     *                     limite di tempo
     * @throws NullPointerException se {@code obs} è null
     */
    public PlayGUI(Observer<P> obs, long maxBlockTime)
    {
        if(obs == null)
            throw new NullPointerException();
        
        this.observer = obs;
        this.max_block_time = maxBlockTime;
        this.confinement_thread = Executors.newSingleThreadExecutor(Utils.DAEMON_THREAD_FACTORY);
    }
    
    /**
     * Imposta la {@link GameFactory} con il nome dato. Usa {@link GameFactories}
     * per creare la GameFactory nel thread di confinamento. Se già c'era una
     * GameFactory impostata, la sostituisce con la nuova e se c'erano anche
     * PlayerFactory impostate le cancella. Però se c'è una partita in corso,
     * fallisce.
     *
     * @param name nome di una GameFactory
     * @throws NullPointerException     se {@code name} è null
     * @throws IllegalArgumentException se {@code name} non è il nome di una
     *                                  GameFactory
     * @throws IllegalStateException    se la creazione della GameFactory fallisce o se
     *                                  c'è una partita in corso.
     */
    public void setGameFactory(String name)
    {
        if(name == null)
            throw new NullPointerException();
        if(!Arrays.asList(GameFactories.availableBoardFactories()).contains(name))
            throw new IllegalArgumentException();
        
        Future<?> future = this.confinement_thread.submit(
            () ->
            {
                if(this.is_match_in_progress)
                    throw new IllegalStateException();
                try
                {
                    this.game_factory = GameFactories.getBoardFactory(name);
                    
                    this.player_factories = new ArrayList<>(this.game_factory.maxPlayers());
                    this.player_names = new String[this.game_factory.maxPlayers()];
                    
                    for(int i = 0; i < this.game_factory.maxPlayers(); ++i)
                    {
                        this.player_factories.add(null);
                    }
                }
                catch(IllegalArgumentException e)
                {
                    throw new IllegalStateException();
                }
            }
        );
        
        try
        {
            future.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
        }
    }
    
    /**
     * Ritorna i nomi dei parametri della {@link GameFactory} impostata. Se la
     * GameFactory non ha parametri, ritorna un array vuoto.
     *
     * @return i nomi dei parametri della GameFactory impostata
     * @throws IllegalStateException se non c'è una GameFactory impostata
     */
    public String[] getGameFactoryParams()
    {
        Future<String[]> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                
                String[] result = this.game_factory.params().stream().map(Param::name).toArray(String[]::new);
                return result;
            });
        try
        {
            String[] result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna il prompt del parametro con il nome specificato della
     * {@link GameFactory} impostata.
     *
     * @param paramName nome del parametro
     * @return il prompt del parametro con il nome specificato della GameFactory
     * impostata.
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     *                                  parametro di nome {@code paramName}
     * @throws IllegalStateException    se non c'è una GameFactory impostata
     */
    public String getGameFactoryParamPrompt(String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<String> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                
                Param<?> param = this.game_factory.params()
                                                  .stream()
                                                  .filter(item -> item.name().equals(paramName))
                                                  .findFirst()
                                                  .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.prompt();
            });
        try
        {
            String result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna i valori ammissibili per il parametro con nome dato della
     * {@link GameFactory} impostata.
     *
     * @param paramName nome del parametro
     * @return i valori ammissibili per il parametro della GameFactory impostata
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     *                                  parametro di nome {@code paramName}
     * @throws IllegalStateException    se non c'è una GameFactory impostata
     */
    public Object[] getGameFactoryParamValues(String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<Object[]> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                
                Param<?> param = this.game_factory.params()
                                                  .stream()
                                                  .filter(item -> item.name().equals(paramName))
                                                  .findFirst()
                                                  .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.values().toArray();
            });
        try
        {
            Object[] result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna il valore del parametro di nome dato della {@link GameFactory}
     * impostata.
     *
     * @param paramName nome del parametro
     * @return il valore del parametro della GameFactory impostata
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     *                                  parametro di nome {@code paramName}
     * @throws IllegalStateException    se non c'è una GameFactory impostata
     */
    public Object getGameFactoryParamValue(String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<Object> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                
                Param<?> param = this.game_factory.params()
                                                  .stream()
                                                  .filter(item -> item.name().equals(paramName))
                                                  .findFirst()
                                                  .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.get();
            });
        try
        {
            Object result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Imposta il valore del parametro di nome dato della {@link GameFactory}
     * impostata.
     *
     * @param paramName nome del parametro
     * @param value     un valore ammissibile per il parametro
     * @throws NullPointerException     se {@code paramName} o {@code value} è null
     * @throws IllegalArgumentException se la GameFactory impostata non ha un
     *                                  parametro di nome {@code paramName} o {@code value} non è un valore
     *                                  ammissibile per il parametro
     * @throws IllegalStateException    se non c'è una GameFactory impostata o è già
     *                                  stato impostata la PlayerFactory di un giocatore
     */
    public void setGameFactoryParamValue(String paramName, Object value)
    {
        if(paramName == null)
            throw new NullPointerException();
        if(value == null)
            throw new NullPointerException();
        
        Future<?> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                if(this.player_factories.stream().anyMatch(Objects::nonNull))
                    throw new IllegalStateException();
                
                Param<?> param = this.game_factory.params()
                                                  .stream()
                                                  .filter(item -> item.name().equals(paramName))
                                                  .findFirst()
                                                  .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                param.set(value);
            });
        try
        {
            future.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
        }
    }
    
    /**
     * Imposta un {@link PlayerGUI} con il nome e il master dati per il giocatore
     * di indice {@code pIndex}. Se c'era già un giocatore impostato per quell'indice,
     * lo sostituisce.
     *
     * @param pIndex indice di un giocatore
     * @param pName  nome del giocatore
     * @param master il master
     * @throws NullPointerException     se {@code pName} o {@code master} è null
     * @throws IllegalArgumentException se {@code pIndex} non è un indice di giocatore
     *                                  valido per la GameFactory impostata
     * @throws IllegalStateException    se non c'è una GameFactory impostata o se c'è
     *                                  una partita in corso.
     */
	public void setPlayerGUI(int pIndex, String pName, Consumer<MoveChooser<P>> master)
    {
        if(pName == null)
            throw new NullPointerException();
        if(master == null)
            throw new NullPointerException();
        
        Future<?> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                if(this.is_match_in_progress)
                    throw new IllegalStateException();
                if(pIndex < 1 || pIndex > this.game_factory.maxPlayers())
                    throw new IllegalArgumentException();
                
                int index = pIndex - 1;
                PlayerGUIFactory<PieceModel<PieceModel.Species>> factory = new PlayerGUIFactory<>((Consumer)master);
                this.player_factories.set(index, factory);
                this.player_names[index] = pName;
            });
        try
        {
            future.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
        }
    }
    
    /**
     * Imposta la {@link PlayerFactory} con nome dato per il giocatore di indice
     * {@code pIndex}. Usa {@link PlayerFactories} per creare la PlayerFactory nel
     * thread di confinamento. La PlayerFactory è impostata solamente se il metodo
     * ritorna {@link PlayerFactory.Play#YES}. Se c'era già un giocatore impostato
     * per quell'indice, lo sostituisce.
     *
     * @param pIndex indice di un giocatore
     * @param fName  nome di una PlayerFactory
     * @param pName  nome del giocatore
     * @param dir    la directory della PlayerFactory o null
     * @return un valore (vedi {@link PlayerFactory.Play}) che informa sulle
     * capacità dei giocatori di questa fabbrica di giocare al gioco specificato.
     * @throws NullPointerException     se {@code fName} o {@code pName} è null
     * @throws IllegalArgumentException se {@code pIndex} non è un indice di giocatore
     *                                  valido per la GameFactory impostata o se non esiste una PlayerFactory di nome
     *                                  {@code fName}
     * @throws IllegalStateException    se la creazione della PlayerFactory fallisce o
     *                                  se non c'è una GameFactory impostata o se c'è una partita in corso.
     */
    public PlayerFactory.Play setPlayerFactory(int pIndex, String fName, String pName, Path dir)
    {
        if(fName == null)
            throw new NullPointerException();
        if(pName == null)
            throw new NullPointerException();
        
        Future<PlayerFactory.Play> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                if(this.is_match_in_progress)
                    throw new IllegalStateException();
                if(pIndex < 1 || pIndex > this.game_factory.maxPlayers())
                    throw new IllegalArgumentException();
                if(!Arrays.asList(PlayerFactories.availableBoardFactories()).contains(fName))
                    throw new IllegalArgumentException();
                
                int index = pIndex - 1;
                try
                {
                    PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>>
                        factory = PlayerFactories.getBoardFactory(fName);
                    factory.setDir(dir);
                    PlayerFactory.Play result = factory.canPlay(this.game_factory);
                    if(result == PlayerFactory.Play.YES)
                    {
                        this.player_factories.set(index, factory);
                        this.player_names[index] = pName;
                    }
                    return result;
                }
                catch(IllegalArgumentException e)
                {
                    throw new IllegalStateException();
                }
            });
        try
        {
            PlayerFactory.Play result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna i nomi dei parametri della {@link PlayerFactory} di indice
     * {@code pIndex}. Se la PlayerFactory non ha parametri, ritorna un array vuoto.
     *
     * @param pIndex indice di un giocatore
     * @return i nomi dei parametri della PlayerFactory di indice dato
     * @throws IllegalArgumentException se non c'è una PlayerFactory di indice
     *                                  {@code pIndex}
     */
    public String[] getPlayerFactoryParams(int pIndex)
    {
        if(pIndex < 1 || pIndex > this.player_factories.size())
            throw new IllegalArgumentException();
        
        Future<String[]> future = this.confinement_thread.submit(
            () ->
            {
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(pIndex - 1);
                if(factory == null)
                    throw new IllegalArgumentException();
                
                String[] result = factory.params().stream().map(Param::name).toArray(String[]::new);
                return result;
            });
        try
        {
            String[] result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna il prompt del parametro con il nome specificato della
     * {@link PlayerFactory} di indice {@code pIndex}.
     *
     * @param pIndex    indice di un giocatore
     * @param paramName nome del parametro
     * @return il prompt del parametro con il nome specificato della PlayerFactory
     * di indice dato
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     *                                  nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex}
     */
    public String getPlayerFactoryParamPrompt(int pIndex, String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<String> future = this.confinement_thread.submit(
            () ->
            {
                if(pIndex < 1 || pIndex > this.player_factories.size())
                    throw new IllegalArgumentException();
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(pIndex - 1);
                if(factory == null)
                    throw new IllegalArgumentException();
                
                Param<?> param = factory.params()
                                        .stream()
                                        .filter(item -> item.name().equals(paramName))
                                        .findFirst()
                                        .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.prompt();
            });
        try
        {
            String result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna i valori ammissibili per il parametro di nome dato della
     * {@link PlayerFactory} di indice {@code pIndex}.
     *
     * @param pIndex    indice di un giocatore
     * @param paramName nome del parametro
     * @return i valori ammissibili per il parametro di nome dato della PlayerFactory
     * di indice dato.
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     *                                  nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex}
     */
    public Object[] getPlayerFactoryParamValues(int pIndex, String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<Object[]> future = this.confinement_thread.submit(
            () ->
            {
                if(pIndex < 1 || pIndex > this.player_factories.size())
                    throw new IllegalArgumentException();
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(pIndex - 1);
                if(factory == null)
                    throw new IllegalArgumentException();
                
                Param<?> param = factory.params()
                                        .stream()
                                        .filter(item -> item.name().equals(paramName))
                                        .findFirst()
                                        .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.values().toArray();
            });
        try
        {
            Object[] result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Ritorna il valore del parametro di nome dato della {@link PlayerFactory} di
     * indice {@code pIndex}.
     *
     * @param pIndex    indice di un giocatore
     * @param paramName nome del parametro
     * @return il valore del parametro di nome dato della PlayerFactory di indice
     * dato
     * @throws NullPointerException     se {@code paramName} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     *                                  nome {@code paramName} o non c'è una PlayerFactory di indice {@code pIndex}
     */
    public Object getPlayerFactoryParamValue(int pIndex, String paramName)
    {
        if(paramName == null)
            throw new NullPointerException();
        
        Future<Object> future = this.confinement_thread.submit(
            () ->
            {
                if(pIndex < 1 || pIndex > this.player_factories.size())
                    throw new IllegalArgumentException();
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(pIndex - 1);
                if(factory == null)
                    throw new IllegalArgumentException();
                
                Param<?> param = factory.params()
                                        .stream()
                                        .filter(item -> item.name().equals(paramName))
                                        .findFirst()
                                        .orElse(null);
                if(param == null)
                    throw new IllegalArgumentException();
                return param.get();
            });
        try
        {
            Object result = future.get();
            return result;
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
            return null;
        }
    }
    
    /**
     * Imposta il valore del parametro di nome dato della {@link PlayerFactory}
     * di indice {@code pIndex}.
     *
     * @param pIndex    indice di un giocatore
     * @param paramName nome del parametro
     * @param value     un valore ammissibile per il parametro
     * @throws NullPointerException     se {@code paramName} o {@code value} è null
     * @throws IllegalArgumentException se la PlayerFactory non ha un parametro di
     *                                  nome {@code paramName} o {@code value} non è un valore ammissibile per il
     *                                  parametro o non c'è una PlayerFactory di indice {@code pIndex}
     * @throws IllegalStateException    se c'è una partita in corso
     */
    public void setPlayerFactoryParamValue(int pIndex, String paramName, Object value)
    {
        if(paramName == null)
            throw new NullPointerException();
        if(value == null)
            throw new NullPointerException();
        
        Future<?> future = this.confinement_thread.submit(
            () ->
            {
                if(this.is_match_in_progress)
                    throw new IllegalStateException();
                if(pIndex < 1 || pIndex > this.player_factories.size())
                    throw new IllegalArgumentException();
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(pIndex - 1);
                if(factory == null)
                    throw new IllegalArgumentException();
                
                Param<?> match = factory.params()
                                        .stream()
                                        .filter(param -> param.name().equals(paramName))
                                        .findFirst()
                                        .orElse(null);
                if(match == null)
                    throw new IllegalArgumentException();
                match.set(value);
            });
        try
        {
            future.get();
        }
        catch(InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
        }
    }
    
    
    /**
     * Inizia una partita con un gioco fabbricato dalla GameFactory impostata e i
     * giocatori forniti da {@link PlayerGUI} impostati o fabbricati dalle
     * PlayerFactory impostate. Se non c'è una GameFactory impostata o non ci sono
     * sufficienti giocatori impostati o c'è già una partita in corso, fallisce. Se
     * sono impostati dei vincoli sui thread per le invocazioni di
     * {@link Player#getMove}, allora prima di iniziare la partita invoca i metodi
     * {@link Player#threads(int, ForkJoinPool, ExecutorService)} di tutti i giocatori,
     * ovviamente nel thread di confinamento.
     * <br>
     * Il metodo ritorna immediatamente, non attende che la partita termini. Quindi
     * usa un thread per gestire la partita oltre al thread di confinamento usato
     * per l'invocazione di tutti i metodi del GameRuler e dei Player.
     *
     * @param tol        massimo numero di millisecondi di tolleranza per le mosse, cioè se
     *                   il gioco ha un tempo limite <i>T</i> per le mosse, allora il tempo di
     *                   attesa sarà <i>T</i> + {@code tol}; se {@code tol} <= 0, allora
     *                   nessuna tolleranza
     * @param timeout    massimo numero di millisecondi per le invocazioni dei metodi
     *                   dei giocatori escluso {@link Player#getMove()}, se <= 0,
     *                   allora nessun limite
     * @param minTime    minimo numero di millisecondi tra una mossa e quella successiva,
     *                   se <= 0, allora nessuna pausa
     * @param maxTh      massimo numero di thread addizionali permessi per
     *                   {@link Player#getMove()}, se < 0, nessun limite è imposto
     * @param fjpSize    numero di thread per il {@link ForkJoinTask ForkJoin} pool,
     *                   se == 0, non è permesso alcun pool, se invece è < 0, non c'è
     *                   alcun vincolo e possono usare anche
     *                   {@link ForkJoinPool#commonPool() Common Pool}
     * @param bgExecSize numero di thread permessi per esecuzioni in background, se
     *                   == 0, non sono permessi, se invece è < 0, non c'è alcun
     *                   vincolo
     * @throws IllegalStateException se non c'è una GameFactory impostata o non ci
     *                               sono sufficienti PlayerFactory impostate o la creazione del GameRuler o quella
     *                               di qualche giocatore fallisce o se già c'è una partita in corso.
     */
    public void play(long tol, long timeout, long minTime, int maxTh, int fjpSize, int bgExecSize)
    {
        Future<?> future = this.confinement_thread.submit(
            () ->
            {
                if(this.game_factory == null)
                    throw new IllegalStateException();
                if(this.is_match_in_progress)
                    throw new IllegalStateException();
                if(this.player_factories.stream().filter(Objects::nonNull).count() < this.game_factory.minPlayers())
                    throw new IllegalStateException();
            });
        try
        {
            future.get();
        }
        catch(InterruptedException ignored)
        {
        }
        catch(ExecutionException e)
        {
            throwActualConfinementThreadException(e.getCause());
        }
        
        // Se esistono due vincoli di tempo, si usa il piu' restrittivo
        if(this.max_block_time >= 0 && timeout > 0)
        {
            timeout = Long.min(this.max_block_time, timeout);
        }
        
        long move_choice_tolerance = tol > 0 ? tol : 0;
        long controller_thread_timeout = timeout > 0 ? timeout : 0;
        long minimum_turn_duration = minTime > 0 ? minTime : 0;
        
        this.is_match_in_progress = true;
        
        // Inizializza il gioco
        this.confinement_thread.submit(() -> this.initializeGame(maxTh,
                                                                 fjpSize,
                                                                 bgExecSize,
                                                                 controller_thread_timeout));
        
        // Fa partire il gioco
        this.confinement_thread.submit(() -> this.executeGameLoop(move_choice_tolerance,
                                                                  controller_thread_timeout,
                                                                  minimum_turn_duration));
    }
    
    /**
     * Se c'è una partita in corso la termina immediatamente e ritorna true,
     * altrimenti non fa nulla e ritorna false.
     *
     * @return true se termina la partita in corso, false altrimenti
     */
    public boolean stop()
    {
        if(!this.is_match_in_progress)
            return false;
        
        this.is_match_in_progress = false;
        this.confinement_thread.shutdownNow();
        this.game_ruler = null;
        this.game_factory = null;
        this.player_factories = null;
        this.player_names = null;
        
        this.confinement_thread = Executors.newSingleThreadExecutor(Utils.DAEMON_THREAD_FACTORY);
        return true;
    }
    
    /**
     * Esegue le operazioni necessarie per iniziare una partita.
     * Chiamata dal thread di confinamento.
     *
     * @param get_move_max_threads      Passato come primo argomento di Player::threads
     * @param forkjoinpool_thread_count Usato per creare il secondo argomento di Player::threads
     * @param background_thread_count   Usato per creare il terzo argomento di Player::threads
     * @param timeout                   Usato per creare il terzo argomento di Player::threads
     */
    private void initializeGame(int get_move_max_threads,
                                int forkjoinpool_thread_count,
                                int background_thread_count,
                                long timeout)
    {
        this.players = new ArrayList<>();
        AsyncController async_controller = new AsyncController(this::onControllerThreadTimeout);
        
        try
        {
            for(int i = 0; i < this.player_factories.size(); ++i)
            {
                PlayerFactory<Player<PieceModel<PieceModel.Species>>, GameRuler<PieceModel<PieceModel.Species>>> factory = this.player_factories.get(i);
                if(factory == null)
                    continue;
                
                async_controller.startTimer(timeout, 0, "Time out newPlayer");
                Player<PieceModel<PieceModel.Species>> player = factory.newPlayer(this.game_factory,
                                                                                  this.player_names[i]);
                async_controller.stopTimer();
                
                ForkJoinPool fjp = forkjoinpool_thread_count == 0 ? null
                                                                  : (forkjoinpool_thread_count > 0 ? new ForkJoinPool(forkjoinpool_thread_count)
                                                                                                   : ForkJoinPool.commonPool());
                ExecutorService executor = background_thread_count == 0 ? null
                                                                        : (background_thread_count > 0 ? Executors.newFixedThreadPool(background_thread_count)
                                                                                                       : Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
                player.threads(get_move_max_threads, fjp, executor);
                this.players.add(player);
            }
        }
        catch(Exception e)
        {
            async_controller.shutDown();
            throw new IllegalStateException(e);
        }
        
        try
        {
            async_controller.startTimer(timeout, 0, "Time out setPlayerNames");
            this.game_factory.setPlayerNames(this.player_names);
            async_controller.stopTimer();
            
            async_controller.startTimer(timeout, 0, "Time out newGame");
            this.game_ruler = game_factory.newGame();
            async_controller.stopTimer();
            
            for(int i = 0; i < this.players.size(); ++i)
            {
                Player<PieceModel<PieceModel.Species>> player = this.players.get(i);
                
                async_controller.startTimer(timeout, i + 1, "Time out setGame");
                player.setGame(this.game_ruler.copy());
                async_controller.stopTimer();
            }
            
            async_controller.startTimer(timeout, 0, "Time out setGame observer");
            this.observer.setGame((GameRuler<P>)this.game_ruler.copy());
            async_controller.stopTimer();
        }
        catch(Exception e)
        {
            throw new IllegalStateException(e);
        }
        finally
        {
            async_controller.shutDown();
        }
    }
    
    /**
     * Entra in un ciclo che gioca tutta la partita. Gira sul thread di confinamento.
     * Puo' essere interrotto chiamando il metodo stop.
     *
     * @param move_choice_tolerance Tempo aggiunto a Mechnics::time per determinarer il timeout di getMove.
     * @param timeout               Tempo a diposizione per eseguire tutto tranne getMove.
     * @param minimum_turn_duration Tempo minimo dall'inizio di un turno all'inizio del turno successivo.
     */
    private void executeGameLoop(long move_choice_tolerance,
                                 long timeout,
                                 long minimum_turn_duration)
    {
        AsyncController async_controller = new AsyncController(this::onControllerThreadTimeout);
        
        try
        {
            int match_result = this.game_ruler.result();
            while(match_result < 0 && this.is_match_in_progress)
            {
                long turn_begin_time = System.currentTimeMillis();
                
                // Get current player
                int current_player_id = this.game_ruler.turn();
                Player<PieceModel<PieceModel.Species>> current_player = this.players.get(current_player_id - 1);
                
                
                // Choose move
                long get_move_wait_time = this.game_ruler.mechanics().time;
                if(get_move_wait_time >= 0)
                    get_move_wait_time += move_choice_tolerance;
                
                async_controller.startTimer(get_move_wait_time, current_player_id, "Move choice timed out");
                Move<PieceModel<PieceModel.Species>> move = current_player.getMove();
                async_controller.stopTimer();
    
    
                // Enforce a minimum turn duration. Do not delay for interactive players
                if(!this.player_factories.get(current_player_id - 1).name().equals(PlayerGUIFactory.NAME))
                {
                    long elapsed_turn_time = System.currentTimeMillis() - turn_begin_time;
                    while(elapsed_turn_time < minimum_turn_duration)
                    {
                        Thread.sleep(minimum_turn_duration - elapsed_turn_time);
                        elapsed_turn_time = System.currentTimeMillis() - turn_begin_time;
                    }
                }
                
                
                // Execute move
                async_controller.startTimer(timeout, current_player_id, "Move execution in GameRuler timed out");
                this.game_ruler.move(move);
                async_controller.stopTimer();
                
                for(int i = 0; i < this.players.size(); ++i)
                {
                    Player<PieceModel<PieceModel.Species>> player = this.players.get(i);
                    
                    async_controller.startTimer(timeout, i + 1, String.format("Move execution of player %d timed out", i + 1));
                    player.moved(current_player_id, move);
                    async_controller.stopTimer();
                }
                
                async_controller.startTimer(timeout, 0, "Move execution in GameRuler timed out");
                this.observer.moved(current_player_id, (Move<P>)move);
                async_controller.stopTimer();
                
                match_result = this.game_ruler.result();
            }
        }
        catch(InterruptedException ignored)
        {
        }
        finally
        {
            async_controller.shutDown();
        }
    }
    
    /**
     * Callback per il timer della partita.
     *
     * @param player_id Turno del player che ha causato il timeout. 0 se non e'
     *                  causato da alcun player
     * @param message   Messaggio informativo sulla causa del timeout
     */
    private void onControllerThreadTimeout(int player_id, String message)
    {
        this.observer.limitBreak(player_id, message);
        this.observer.interrupted(message);
    }
    
    
    /**
     * Implementa il thread di controllo del timeout delle funzioni di una partita
     */
    private class AsyncController
    {
        private Thread thread;
        private final BiConsumer<Integer, String> timeout_callback;
        
        private volatile long timeout;
        private volatile int current_player_id;
        private volatile String timeout_message;
        
        private volatile boolean is_running;
        private volatile boolean is_shut_down;
        
        /**
         * Crea il thread (demone) di controllo e ci fa partire un timer in stato di stop.
         * In caso di timeout invoca timeout_callback dal thread di controllo.
         *
         * @param timeout_callback Parametri: turno di un player, messaggio informativo.
         *                         Puo' essere null.
         */
        public AsyncController(BiConsumer<Integer, String> timeout_callback)
        {
            this.timeout_callback = timeout_callback;
            this.is_running = false;
            
            this.thread = new Thread(this::execute);
            this.thread.setDaemon(true);
            this.thread.start();
        }
        
        /**
         * Notifica il thread di controllo di iniziare a misurare il tempo.
         * Se il timer non e' in stato di stop, non fa niente.
         * Se il timer e' stato ucciso, non fa niente.
         * Se il parametro timeout <= 0, non fa niente (equivalente a non andare mai in timeout)
         *
         * @param timeout         Il tempo da attendere prima di notificare l'observer del timeout.
         * @param player_turn     Il turno del giocatore che causa il timeout. Viene passato all'observer.
         * @param timeout_message Il messaggio che informa sulla causa del timeout. Viene passato all'observer.
         */
        synchronized public void startTimer(long timeout, int player_turn, String timeout_message)
        {
            if(timeout <= 0)
                return;
            
            if(this.is_shut_down)
                return;
            if(this.is_running)
                return;
            
            this.timeout = timeout;
            this.current_player_id = player_turn;
            this.timeout_message = timeout_message;
            
            this.is_running = true;
            
            this.notify();
        }
        
        /**
         * Notifica il thread di controllo di smettere di misurare il tempo.
         * Non e' garantito che stoppare il timer non causi un timeout.
         * Chiamare questa funzione senza avere avviato il timer non ha nessun effetto.
         */
        synchronized public void stopTimer()
        {
            if(this.is_shut_down)
                return;
            if(this.is_running == false)
                return;
            
            this.is_running = false;
            
            this.notify();
        }
        
        /**
         * Blocca il timer e causa la terminazione del thread di controllo.
         * Se e' gia' stato ucciso, non succede nulla.
         */
        synchronized public void shutDown()
        {
            if(this.is_shut_down)
                return;
            
            this.is_running = false;
            this.is_shut_down = true;
            
            this.notify();
        }
        
        /**
         * Sta in attesa finche' non viene richiesto di misurare del tempo.
         * Questo metodo parte alla construzione dell'oggetto e gira su un
         * thread demone indipendente
         * Per fermare il thread e' necessario chiamare shutDown
         */
        synchronized private void execute()
        {
            try
            {
                ON_SHUTDOWN:
                while(this.is_shut_down == false)
                {
                    // aspetta finche' non viene chiamata startTimer o shutDown
                    while(this.is_running == false)
                    {
                        this.wait();
                        
                        // se il risveglio viene da shutDown, si esce
                        if(this.is_shut_down)
                        {
                            break ON_SHUTDOWN;
                        }
                    }
                    
                    
                    long remaining_time = this.timeout;
                    long start_time = System.currentTimeMillis();
                    
                    while(remaining_time > 0)
                    {
                        this.wait(remaining_time);
                        
                        long elapsed_time = System.currentTimeMillis() - start_time;
                        remaining_time = this.timeout - elapsed_time;
                        
                        if(remaining_time > 0) // risveglio a caso oppure notificato
                        {
                            if(this.is_running) // risveglio a caso
                            {
                                continue;
                            }
                            else // risveglio notificato
                            {
                                break;
                            }
                        }
                        else // risveglio per timeout
                        {
                            this.is_running = false;
                            
                            // per essere sicuri che un timeout a partita finita non rompe qualcosa
                            // dovrebbe essere impossibile, ma non si sa mai
                            if(is_match_in_progress)
                            {
                                if(this.timeout_callback != null)
                                {
                                    this.timeout_callback.accept(this.current_player_id, this.timeout_message);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            catch(InterruptedException ignored)
            {
                
            }
        }
    }
    
    
    /**
     * Utility per fare il get di un Future senza peroccuparsi di timeout negativi
     */
    static private <R> R getFutureResult(Future<R> future, long timeout)
        throws InterruptedException, ExecutionException, TimeoutException
    {
        R result;
        if(timeout < 0)
        {
            result = future.get();
        }
        else
        {
            result = future.get(timeout, TimeUnit.MILLISECONDS);
        }
        return result;
    }
    
    /**
     * Se il thread di confinamento tira un'eccezione, Future::get tirera' una
     * ExecutionException, il cui metodo getCause ritorna la vera eccezione
     * tipata come Throwable. Siccome non si puo' tirare un Throwable senza
     * modificare l'intestazione di un metodo, questo metodo casta l'eccezione
     * prima di tirarla.
     * https://groups.google.com/forum/#!topic/metodologieprogrammazioneal2016/ZZdnDd14mHs
     *
     * @param t L'eccezione ritornata da ExecutionException::getCause.
     */
    static private void throwActualConfinementThreadException(Throwable t)
    {
        if(t instanceof IllegalArgumentException)
            throw (IllegalArgumentException)t;
        
        if(t instanceof IllegalStateException)
            throw (IllegalStateException)t;
        
        if(t instanceof NullPointerException)
            throw (NullPointerException)t;
        
        throw new RuntimeException(t);
    }
}
