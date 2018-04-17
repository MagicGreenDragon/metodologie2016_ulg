package gapp.gui.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import gapp.gui.scenes.ChooseGames;
import gapp.gui.scenes.ChooseParams;
import gapp.gui.scenes.ChoosePlayerParams;
import gapp.gui.scenes.ChoosePlayers;
import gapp.gui.scenes.Credits;
import gapp.gui.scenes.GameInterface;
import gapp.gui.scenes.GeneralParameters;
import gapp.gui.scenes.MainMenu;
import gapp.gui.scenes.ScoreBoard;
import gapp.gui.scenes.Settings;
import gapp.gui.scenes.WaitScreen;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.PlayGUI;
import gapp.ulg.game.util.PlayerGUI.MoveChooser;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * 
 * Oggetto per la gestione delle scene dell'interfaccia,
 * ogni passaggio di scena avviene da una chiamata su questo oggetto,
 * e durante la creazione della partita amministra le scene per la preparazione
 * 
 * 
 * Ultima modifica: Pomeriggio - uni - 06/09
 * @author Gabriele Cavallaro & Daniele Bondi'
 *
 */
public class ScenesManager 
{
    //variabili finalizzate
    //impostazioni schermo:
    static public final double WINDOW_MINIMUM_WIDTH = 1280; 
    static public final double WINDOW_MINIMUM_HEIGHT = 720;
    static public final double WINDOW_PREFERRED_WIDTH = 1360;
    static public final double WINDOW_PREFERRED_HEIGHT = 768;
    static public final double WINDOW_MAXIMUM_WIDTH = Screen.getPrimary().getVisualBounds().getWidth();
    static public final double WINDOW_MAXIMUM_HEIGHT = Screen.getPrimary().getVisualBounds().getHeight();
    
    //variabili utili
    static public ScenesManager instance;
    static public FactoriesManager factoriesmanager;
    private Stage window;
    
    //scene statiche che vanno mantenute una volta create
    private MainMenu mainmanu;
    private Settings settings;
    
    //coda di gioco
    private ChooseGames choosegames;
    private GeneralParameters generalparameters;
    private ChooseParams choosegameparams;
    private ChoosePlayers chooseplayers;
    private ChoosePlayerParams chooseplayerparams;
    private GameInterface gameinterface;
    
    //variabili di crezione partita
    private String gamename;
    
    //variabili di collegamento al framework
    public static GameObserver<PieceModel<Species>> gameobserver;
    public static PlayGUI<PieceModel<Species>> playgui;
    
    //variabili di setting
    public volatile boolean parallel_trycompute;
    public volatile long maxBlockTime;
    
    //variabili di setting di style
    public volatile ColorTupla boardcolor;
    public volatile boolean nativecolor;
        
    /**
     * Crea un oggetto per la gestione delle scene da mostrare all'utente
     */
    public ScenesManager(Stage primaryStage)
    {          
        ScenesManager.instance = this;        
        factoriesmanager = new FactoriesManager();
        
        window = primaryStage;
        
        //parametri base
        //---
        window.setTitle("Universal Library Game - GUI");
        
        window.setMinWidth(WINDOW_MINIMUM_WIDTH);
        window.setMinHeight(WINDOW_MINIMUM_HEIGHT);
    
        window.setWidth(WINDOW_PREFERRED_WIDTH);
        window.setHeight(WINDOW_PREFERRED_HEIGHT);
    
        window.setMaxWidth(WINDOW_MAXIMUM_WIDTH);
        window.setMaxHeight(WINDOW_MAXIMUM_HEIGHT);
        //---
        
        //impostazioni
        parallel_trycompute = false;
        maxBlockTime=-1;
        
        //style
        boardcolor = new ColorTupla(Color.ANTIQUEWHITE, Color.INDIANRED);
        nativecolor = false;
    }
        
    //--------------------
    //METODI PER CAMBIARE SCENA
    //--------------------
    
    //scene globali
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ChooseGames
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoChooseGames()
    {
        if(choosegames==null) choosegames = new ChooseGames();  
        window.setScene(choosegames);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a MainMenu
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoMainMenu()
    {
        if(mainmanu==null) mainmanu = new MainMenu(); 
        window.setScene(mainmanu);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a Settings
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoSettings()
    {
        if(settings==null) settings = new Settings(); 
        window.setScene(settings);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a Credits
     * la scena non viene mantenuta.  
     */
    public void changescenetoCredits()
    {
        window.setScene(new Credits());
    }
    
    //scene per costruire il gioco(in ordine)
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a GeneralParameters
     * la vecchia scena viene lasciata allo ScenesManager.
     * 
     */
    public void changescenetoGeneralParameters()
    {
        if(generalparameters==null) generalparameters = new GeneralParameters();
        window.setScene(generalparameters);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ChooseParams
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoChooseGamesParam()
    {
        if(choosegameparams==null) choosegameparams = new ChooseParams(0, gamename, false);
        window.setScene(choosegameparams);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ChoosePlayers
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoChoosePlayers()
    {
        chooseplayers = new ChoosePlayers(gamename);
        window.setScene(chooseplayers);
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ChoosePlayerParams
     * la vecchia scena viene lasciata allo ScenesManager.
     */
    public void changescenetoChoosePlayersParams()
    {
        chooseplayerparams = new ChoosePlayerParams();
        window.setScene(chooseplayerparams);
    }
    
    //altri cambi di scena
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ScoreBoard,
     * non viene mantenuta.
     * Necessita di avere il {@link GameObserver<PieceModel<Species>>},
     * per prendere i dati della partita, e un immagine che rappresenta la fine della partita.
     * 
     * @param go
     * @param img
     */
    public void changescenetoScoreBoard(GameObserver<PieceModel<Species>> go, Image img)
    {
        window.setScene(new ScoreBoard(go, img));
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a ChooseParams
     * la vecchia scena viene lasciata allo ScenesManager.
     * Necessita di sapere se si fa riferimento a un giocatore specifico,
     * o a uno qualunque, o a un gioco mettendo isaplayer < 1,
     * e il nome della factory del giocatore o del gioco a cui si fa riferimento.
     * 
     * @param isaplayer
     * @param factory_name
     */
    public void changescenetoChooseParams(int isaplayer, String factory_name)
    {
        window.setScene(new ChooseParams(isaplayer, factory_name, true));
    }
    
    /**
     * Apre una finestra per scegliere una cartella.
     * Blocca la finestra attualmente in uso.
     * 
     * @return cartella scelta dall'utente
     */
    public Path openscenetoDirectoryChooser()
    {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Scegli la cartella dove posizionare le strategie");
        return Paths.get(chooser.showDialog(window).getAbsolutePath());
    }
    
    /**
     * Cambia la scena corrette nel window stage nel main
     * la scena passa a WaitScreen
     * la vecchia scena viene lasciata allo ScenesManager.
     * Necessita di avere un {@link AtomicBoolean} collegato alla funzione di calcolo,
     * per bloccara se si torna in dietro, il testo da mostrare all'utente,
     * e a quale finestra deve tornare.
     * 
     * @param stopcompute
     * @param text
     * @param caller
     */
    public void changescenetoWait(AtomicBoolean stopcompute, String text, Scene caller)
    {
        window.setScene(new WaitScreen(stopcompute, text, caller));
    }
    
    /**
     * Cambia la scena window verso una scena passata nei prametri.
     *  
     * @param s
     */
    public void changeSceneTo(Scene s)
    {
        window.setScene(s);
    }
    
    //--------------------
    //METODI DI RESET SCENA
    //--------------------
    
    /**
     * Elimina i riferimeti alla finestra di scelta parametri per la game_factory del gioco corrente.
     */
    public void resetsceneChooseGamesParam()
    {
        choosegameparams=null;
    }
    
    /**
     * Elimina i riferimeti alla finestra di scelta dei giocatori del gioco corrente.
     */
    public void resetsceneChoosePlayers()
    {
        chooseplayers=null;
    }
    
    /**
     * Elimina i riferimeti alla finestra di scelta parametri per le player_factory del gioco corrente.
     */
    public void resetsceneChoosePlayersParams()
    {
        chooseplayerparams=null;
    }
    
    //--------------------
    //METODI DI GET
    //--------------------
    
    /**
     * Ritorna l'attuale Width della finestra.
     * 
     * @return
     */
    public double getWindowWidth()
    {
        return window.getWidth();
    }
    
    /**
     * Ritorna l'attuale Height della finestra.
     * 
     * @return
     */
    public double getWindowHeight()
    {
        return window.getHeight();
    }
    
    public String getActualGameName()
    {
        return gamename;
    }
    
    /**
     * Ritorna la lista dei nomi dei giocatori,
     * attualmente scelti nella costruzione del gioco.
     * 
     * @return
     */
    public List<String> getallPlayerName()
    {
    	if(Objects.isNull(chooseplayers))
    			return null;
    			
    	return chooseplayers.getallPlayerName();
    }
    
    /**
     * Ritorna la lista delle factory dei giocatori,
     * attualmente scelti nella costruzione del gioco.
     * 
     * @return
     */
    public List<String> getallPlayerFactory()
    {
    	if(Objects.isNull(chooseplayers))
    			return null;
    			
    	return chooseplayers.getallPlayerFactory();
    }
    
    //--------------------
    //METODI DI SET
    //--------------------
    
    /**
     * Ritorna il nome del gioco attuale, ho l'ultimo giocato.
     * 
     * @param gamename
     */
    public void setActualGameName(String gamename)
    {
        this.gamename = gamename;
    }
    
    //--------------------
    //ALTRI METODI
    //--------------------
    
    /**
     * Inizia una coda di finestre per preparare il gioco, quando viene chiamata,
     * passa alla schermata {@link ChooseParam} dove si sceglieranno i parametri del gioco.
     * Suppone che ci sia un gioco già impostato attraverso setActualGameName(String gamename) 
     * 
     * @return Scene prima scena della impostazione dei parametri della partita
     */
    public void createNewGameAssemblyQueue()
    {
        //creo Observer e PlayGUI
        gameobserver = new GameObserver<PieceModel<Species>>();
        playgui = new PlayGUI<PieceModel<Species>>(gameobserver, maxBlockTime);
        
        //imposto il gioco
        playgui.setGameFactory(gamename);
        
        //passo 1        
        //pulisco i parametri del gioco 
        choosegameparams = null;
        
        //passo 2        
        //pulisco interfaccia per scegliere i giocatori
        chooseplayers = null;
        
        //passo 3 
        //pulisco interfaccia per scegliere i parametri dei giocatori
        chooseplayerparams = null;
        
        //passo 4 aggiungo schermata di gioco    
        //creo la partita per ottenere il master in futuro
        gameinterface = new GameInterface();
                
        //passo 5
        //mostro la prima schermata
        changescenetoChooseGamesParam();
    }
    
    public Consumer<MoveChooser<PieceModel<Species>>> getActaulMaster()
    {
        return gameinterface.getMasterConsumer();
    }
    
    public void play()
    {        
        gameinterface.changeSpectateMode(!chooseplayers.thereisHumans());
        
        playgui.play
        (
            generalparameters.getTolerance(),
            generalparameters.getTimeout(), 
            generalparameters.getMinTime(),
            generalparameters.getMaxThreads(), 
            generalparameters.getForkjoinpoolSize(), 
            generalparameters.getBackgroundThreadsSize()
        );
        gameinterface.observethisGame(gameobserver);
        
        window.setScene(gameinterface);
    }
    
    /**
     * Ricrea la partita dalle precedenti impostazioni scelte.
     */
    public void resetplaygui()
    {
        //resetto play gui a uno stadio impostabile, stoppandolo precedentemente
        playgui.stop();
        //playgui è più facile da impostare se ricreato
        gameobserver = new GameObserver<PieceModel<Species>>();        
        playgui = new PlayGUI<PieceModel<Species>>(gameobserver, maxBlockTime);
        
        //resetto la schermata di gioco
        gameinterface = new GameInterface();
        
        //setto il gioco
        playgui.setGameFactory(gamename);
        
        //setto le impostazioni del gioco
        for(String param : factoriesmanager.getFactoryParams(gamename))
            playgui.setGameFactoryParamValue(param, factoriesmanager.getFactoryParamValue(gamename, param));
        
        //setto i giocatori
        List<String> players_factory = chooseplayers.getallPlayerFactory();
        List<String> players_name = chooseplayers.getallPlayerName();
        
        for(int i = 0; i < players_factory.size();i++)
        {
            if(players_factory.get(i).equals(chooseplayers.HUMAN_PLAYER_STRING))
            {
                ScenesManager.playgui.setPlayerGUI
                (
                   i+1,
                   players_name.get(i),
                   ScenesManager.instance.getActaulMaster()
                );  
            }
            else
            {
                ScenesManager.playgui.setPlayerFactory
                (
                        i+1,
                        players_factory.get(i),
                        players_name.get(i),
                        ScenesManager.factoriesmanager.getPlayerFactoryDir(players_factory.get(i))
                ); 
            
                for(String param : factoriesmanager.getFactoryParams(players_factory.get(i)))
                    playgui.setPlayerFactoryParamValue(i+1, param, factoriesmanager.getFactoryParamValue(players_factory.get(i), param));
            }

        }
        
        play();
    }
    
    /**
     * Chiude la finestra, e con essa la gui.
     */
    public void closeWindow()
    {
        window.close();
    }        
}
