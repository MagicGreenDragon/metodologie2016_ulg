package gapp.gui.scenes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.concurrent.ConcurrentLinkedDeque;

import gapp.gui.board.GameBoard2D;
import gapp.gui.board.GameBoard2D.BoardListener;
import gapp.gui.util.GameObserver;
import gapp.gui.util.PlayerScore;
import gapp.gui.util.ScenesManager;
import gapp.ulg.game.board.Action;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.Utils;
import gapp.ulg.game.util.PlayerGUI.MoveChooser;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

/**
 * 
 * Interfaccia di gioco
 * 
 * Ultima modifica: Pomeriggio - uni - 06/09
 * 
 * @author Gabriele Cavallaro & Daniele Giudice & Daniele Bondi'
 *
 */
public class GameInterface extends Scene
{      
    // Label
    Label label_game_title;
    
    Label label_player_turn;
    Label label_turn;
    
    Label label_time;
    Label label_remaintime;
    
    // Bottoni
    final Button btn_pass;
    final Button btn_choose;
    final Button btn_backmove;
    final Button btn_surrender;
    final Button btn_restart;
    final Button btn_back;
    
    // Contenitori    
    BorderPane root;
    HBox hbox_top;
    VBox vbox_left;
    VBox vbox_center;
    VBox vbox_right;
    VBox vbox_bottom;
    
    // Altro    
    GameBoard2D board; 
    
    // Variabili per il timer
    static private final String LABEL_PREFIX_TURN_PLAYER = "Turno di: ";
    static private final String LABEL_PREFIX_TURN_COUNT = "Turno numero: ";
    static private final String LABEL_PREFIX_TIME_ELAPSED = "Durata Partita: ";
    static private final String LABEL_PREFIX_TIME_REMAINING = "Tempo rimanente,\nper la mossa: ";
    
    private int turn_index;
    private long turn_duration;
    private long match_start_time;
    private long turn_start_time;
    PeriodicGUIUpdater elapsed_time_updater;
    PeriodicGUIUpdater remaining_time_updater;
    
    // Variabili per i punteggi
    public final AtomicBoolean scores = new AtomicBoolean(false);
    private volatile ObservableList<PlayerScore> player_scores;
    
    // GameObserver
    GameObserver<PieceModel<Species>> gameobserver;
    
    // Master
    final Consumer<MoveChooser<PieceModel<Species>>> master;
    final AtomicInteger choose;
    final Deque<gapp.ulg.game.board.Pos> clicked_pos;
    
    // Flag partita
    final AtomicBoolean humanmove;

    // Mappa colori
    final Map<String, Color> pure_colorname_map;
    
    /**
     * Crea l'interfaccia di gioco.
     */
    public GameInterface()
    {
        super(new BorderPane());
        root = (BorderPane)this.getRoot();
             
        root.prefHeightProperty().bind(this.heightProperty());
        root.prefWidthProperty().bind(this.widthProperty()); 
        
        //--sopra        
        label_game_title = new Label(ScenesManager.instance.getActualGameName()+"\n\n");
        label_game_title.setMaxWidth(Double.MAX_VALUE);
        label_game_title.setAlignment(Pos.CENTER);
        
        hbox_top = new HBox(label_game_title);
        hbox_top.setAlignment(Pos.CENTER);
        hbox_top.setFillHeight(true);
        
        // Box
        root.setTop(hbox_top);
        
        //--sinistra
        
        // Elementi timer        
        label_player_turn = new Label(LABEL_PREFIX_TURN_PLAYER);
        label_turn = new Label(LABEL_PREFIX_TURN_COUNT + Integer.toString(1));
        label_time = new Label(LABEL_PREFIX_TIME_ELAPSED + Integer.toString(0));
        label_remaintime = new Label(LABEL_PREFIX_TIME_REMAINING);
        
        // Box left
        vbox_left = new VBox(label_player_turn, label_turn, label_time, label_remaintime);
        vbox_left.setAlignment(Pos.CENTER_LEFT);
        vbox_left.setPadding(new Insets(30, 30, 30, 30)); 
        vbox_left.setSpacing(100);
        vbox_left.setFillWidth(true);         
        vbox_left.setStyle("-fx-border-color: black"); 
        
        //root.setLeft(vbox_left);
        //--centro
        
        // Elementi
        // La board si inizializza all'inserimento dell'observer, questa è temporanea solo per l'aggiunta all'interfaccia          
        
        //--destra    

        // Elementi  
        btn_pass = new Button("Passa il turno");
        btn_pass.setMaxWidth(Double.MAX_VALUE);    
        btn_choose = new Button("Scegli mossa");
        btn_choose.setDisable(true);
        btn_pass.setMaxWidth(Double.MAX_VALUE); 
        btn_backmove = new Button("Annulla");
        btn_backmove.setMaxWidth(Double.MAX_VALUE); 
        btn_surrender = new Button("Arrenditi");
        btn_surrender.setMaxWidth(Double.MAX_VALUE);
        btn_restart = new Button("Ricomincia");
        btn_restart.setMaxWidth(Double.MAX_VALUE);
        
        // Box right
        vbox_right = new VBox(btn_pass, btn_choose, btn_backmove, btn_surrender, btn_restart);
        vbox_right.setAlignment(Pos.CENTER);
        vbox_right.setPadding(new Insets(30, 30, 30, 30));
        vbox_right.setSpacing(100); 
        vbox_right.setFillWidth(true);
        vbox_right.setStyle("-fx-border-color: black"); 
        
        root.setRight(vbox_right);
        
        //--sotto
        
        // Elementi
        btn_back = new Button("Torna al Menu Principale");
        btn_back.setMaxWidth(Double.MAX_VALUE);
        btn_back.setOnAction
        (
                e ->
                {
                    ScenesManager.instance.changescenetoMainMenu();
                    ScenesManager.playgui.stop();
                }
        );
        
        // Box
        vbox_bottom = new VBox(btn_back);
        vbox_bottom.setFillWidth(true);
        vbox_bottom.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        vbox_bottom.setAlignment(Pos.CENTER);
        vbox_bottom.setPadding(new Insets(20, 20, 20, 20)); 
        
        root.setBottom(vbox_bottom);        
        
        // Inzializzazione del master
        master = createMasterConsumer();
        choose = new AtomicInteger(0);
        clicked_pos = new ConcurrentLinkedDeque<>();
        humanmove = new AtomicBoolean(false);
        
        //Azioni
        btn_pass.setOnAction
        (
                e -> 
                {
                    synchronized(choose)
                    {
                        choose.set(2);
                        choose.notify();
                    }
                }
        );
        btn_choose.setOnAction
        (
                e -> 
                {
                    synchronized(choose)
                    {
                        choose.set(3);
                        choose.notify();
                    }
                }
        );
        btn_surrender.setOnAction
        (
                e -> 
                {
                    synchronized(choose)
                    {
                        choose.set(1);
                        choose.notify();
                    }
                }
        );
        btn_backmove.setOnAction
        (
                e -> 
                {
                    synchronized(choose)
                    {
                        choose.set(4);
                        choose.notify();
                    }
                }
        );
        
        //altro
        btn_restart.setOnAction
        (
                e -> 
                {
                    synchronized(choose)
                    {
                        ScenesManager.instance.resetplaygui();
                    }
                }
        );
        
        
        pure_colorname_map = new HashMap<String, Color>();
        
        pure_colorname_map.put("black", Color.BLACK);
        pure_colorname_map.put("white", Color.WHITE);
        pure_colorname_map.put("red", Color.RED);
        pure_colorname_map.put("blue", Color.BLUE);
        pure_colorname_map.put("green", Color.GREEN);
        pure_colorname_map.put("cyan", Color.CYAN);
        pure_colorname_map.put("gray", Color.GRAY);
        pure_colorname_map.put("yellow", Color.YELLOW);
        pure_colorname_map.put("purple", Color.PURPLE);
        pure_colorname_map.put("magenta", Color.MAGENTA);
        pure_colorname_map.put("orange", Color.ORANGE);
        
        pure_colorname_map.put("nero", Color.BLACK);
        pure_colorname_map.put("bianco", Color.WHITE);
        pure_colorname_map.put("rosso", Color.RED);
        pure_colorname_map.put("blu", Color.BLUE);
        pure_colorname_map.put("verde", Color.GREEN);
        pure_colorname_map.put("ciano", Color.CYAN);
        pure_colorname_map.put("grigio", Color.GRAY);
        pure_colorname_map.put("giallo", Color.YELLOW);
        pure_colorname_map.put("viola", Color.PURPLE);
        pure_colorname_map.put("arancione", Color.ORANGE);
    }
    
    /**
     * Crea il consumer del master.
     * Il master si avvia tramite il metodo accept, tramite cui si passa il MoveChooser.
     * Il master esegue le computazioni in un thread separato, quindi il metodo accept ritorna immediatamente.
     * 
     * @return il consumer del master
     */
    private Consumer<MoveChooser<PieceModel<Species>>> createMasterConsumer()
    {
    	/*
         * Definizioni:
         * - Un'azione di movimento è un'azione di MOVE o di JUMP.
         * - Una cattura indiretta è rappresentata da un'azione di movimento seguita da una di REMOVE (es: Camelot e Dama).
         * - Una cattura diretta è rappresentata da un'azione di REMOVE seguita da una di movimento (es: Scacchi e Breakthrough).
         * - Due o più catture dirette si dicono 'convergenti' se hanno in comune la mossa REMOVE.
         * - Le mosse che iniziano con REMOVE sono catture dirette.
         * - Le mosse che iniziano con JUMP/MOVE sono semplici azioni di movimento (con al massimo catture indirette).
         * - Ogni volta che si passa da padre in figlio, deve essere eseguita la sotto-mossa corrispondente.
         * - Ogni volta che si passa da figlio a padre con il metodo back(), deve essere eseguita la sotto-mossa inversa corrispondente.
         * 
         * Assunzioni:
         * - Un dato pezzo, in un dato momento, può essere soggetto ad un solo tipo di azione tra SWAP, REMOVE e MOVE/JUMP.
         * - Ogni mossa MOVE riguarda una sola posizione alla volta.
         * - Nel caso di un solo figlio selezionato, se sono possibili catture dirette, allora le altre mosse selezionate sono tutte 
         * catture indirette e semplici azioni di movimento.
         * - Una cattura diretta per eseere eseguita ha bisogno al più di due scelte: quella per selezionare la REMOVE ed eventualmente 
         * un'altra per selezionare la mossa di movimento (nel caso di catture dirette convergenti).
         * - Se è necessario un doppio back per annullare l'ultima azione, allora si è in un nodo finale (non si può avanzare)
         * 
         * Quindi:
         * - Qualunque sia il numero di posizioni selezionate, ogni nodo figlio deve cominciare con lo stesso tipo di mossa.
         * - Se è selezionata una sola posizione, tutti i tipi sono possibili.
         * - Se sono selezionate più posizioni, sono possibili solo i tipi REMOVE (senza cattura) e SWAP.
         * - Unica eccezione sono le catture dirette, in cui possono comparire sotto-mosse che iniziano con JUMP, MOVE e REMOVE.
         */
        
        return mc -> 
        {
            Executors.newSingleThreadExecutor(Utils.DAEMON_THREAD_FACTORY).execute
            (
                new Runnable() 
                {
                	/** Ultima mossa eseguita dall'utente */
                	private Move<PieceModel<Species>> latest_move = null;
                	
                	/** Flag che indica se bisogna eseguire un doppio back */
                	private boolean do_double_back = false;
                	
                	/** Indice del pezzo scelto dall'utente nella lista dei pezzi possibili */
                	private volatile int chosen_piece_index = -1;
                	
                	// METODI DI UTILITA' MASTER
                	
                    /** Data un'azione di tipo MOVE, calcola la posizione di arrivo.
                     * La MOVE deve riguardare una board ottagonale, e riguardare una sola posizione.
                     * 
                     * @param action azione MOVE di cui calcolare la posizione di arrivo
                     * @return posizione di arrivo della MOVE
                     */
                    private gapp.ulg.game.board.Pos computeDestinationPos(Action<PieceModel<Species>> action)
                    {
                        int b = action.pos.get(0).b;
                        int t = action.pos.get(0).t;
                        
                        for( int i=0 ; i<action.steps ; i++)
                        {
                            switch(action.dir)
                            {
                                case UP: t++;
                                    break;
                                case DOWN: t--;
                                    break;
                                case LEFT: b--;
                                    break;
                                case RIGHT: b++;
                                    break;
                                case UP_L: {b--; t++;}
                                    break;
                                case UP_R: {b++; t++;}
                                    break;
                                case DOWN_L: {b--; t--;}
                                    break;
                                case DOWN_R: {b++; t--;}
                                    break;
                            }
                        }
                        
                        return new gapp.ulg.game.board.Pos(b,t);
                    }
                    
                    /**
                     * Cerca eventuali sotto-mosse di cattura diretta da un insieme di sotto-mosse e una posizione selezionata.
                     * 
                     * @param submoves lista di sottomosse
                     * @param p posizione della board selezionata
                     * @return ritorna la lista con le sotto-mosse di cattura presenti
                     */
                    private List<Move<PieceModel<Species>>> selectDirectCaptureMove(List<Move<PieceModel<Species>>> submoves, gapp.ulg.game.board.Pos selected_pos)
                    {
                        /*
                         * - Workaround
                         * In caso siano presenti catture multiple convergenti, si esplora ulteriormente l'albero per selezionarle
                         */
                    	
                    	List<Move<PieceModel<Species>>> capture_moves = new ArrayList<>();
                        Action<PieceModel<Species>> action1, action2, sub_action;
                        gapp.ulg.game.board.Pos pos_remove, sourcePos, destinationPos;
                        
                        for( Move<PieceModel<Species>> m : submoves )
                        {
                        	action1 = m.actions.get(0);
                        	
                        	// Se la prima azione è una REMOVE con una sola posizione
                        	if( action1.kind==Action.Kind.REMOVE && action1.pos.size()==1 )
                        	{
                        		// Salvo la posizione della REMOVE
                        		pos_remove = action1.pos.get(0);
                        		
                        		if( m.actions.size()==1  )
                            	{
                                	// Seleziono il nodo con la REMOVE
                                	mc.select(pos_remove);
                                	mc.doSelection(null);
                                	
                                	// Per ogni sotto-mossa dei nodi figli della REMOVE
                            		for( Move<PieceModel<Species>> mm : mc.childrenSubMoves() )
                            		{
                            			// Se la prima azione è di movimento...
                            			sub_action = mm.actions.get(0);
                            			if( sub_action.kind==Action.Kind.JUMP || sub_action.kind==Action.Kind.MOVE )
                                        {
                                            sourcePos = sub_action.pos.get(0);
                                            destinationPos = sub_action.kind==Action.Kind.JUMP ? sub_action.pos.get(1) : computeDestinationPos(sub_action);
                                            
                                            // ... e la posizione selezionata coincide con la posizione di partenza dell'azione di movimento, 
                                            // e la posizione da rimuovere coincide con la destinazione dell'azione di movimento...
                                            if( selected_pos.equals(sourcePos) && pos_remove.equals(destinationPos) )
                                            {
                                                // Allora la posizione 'selected_pos' seleziona una cattura diretta, quindi aggiungi la mossa alla lista
                                            	capture_moves.add(m);
                                            }
                                        }
                            		}
                                	
                                	// Ripristino il nodo corrente precedente
                                	mc.back();
                            	}
                            	else
                                {
                                    action2 = m.actions.get(1);
                                    
                                    // e la seconda è un azione di movimento, ...
                                    if( action2.kind==Action.Kind.JUMP || action2.kind==Action.Kind.MOVE )
                                    {
                                        sourcePos = action2.pos.get(0);
                                        destinationPos = action2.kind==Action.Kind.JUMP ? action2.pos.get(1) : computeDestinationPos(action2);
                                        
                                        // ... la posizione selezionata coincide con la posizione di partenza dell'azione di movimento, 
                                        // e la posizione da rimuovere coincide con la destinazione dell'azione di movimento...
                                        if( selected_pos.equals(sourcePos) && pos_remove.equals(destinationPos) )
                                        {
                                            // Allora la posizione 'p' seleziona una cattura diretta, aggiunge quindi la mossa alla lista
                                            capture_moves.add(m);
                                        }
                                    }
                                }
                        	}
                        	
                        }
                        
                        return capture_moves;
                    }
                    
                    /**
                     * Dato una lista di sotto-mosse che iniziano con azioni di movimento o catture dirette, ritorna una mappa che associa 
                     * ogni posizione di destinazione di tali azioni alla sotto-mossa di appartenenza.
                     * 
                     * @param moves lista di mosse che iniziano con azioni di movimento o catture dirette 
                     * @return mappa DestinationPos -> Move
                     */
                    private Map<gapp.ulg.game.board.Pos,Move<PieceModel<Species>>> buildPosMap(List<Move<PieceModel<Species>>> moves)
                    {
                        // Questo metodo va bene sia in presenza che in assenza di catture dirette, poiché senza catture dirette
                        // non capiterà mai il caso REMOVE
                        
                        Map<gapp.ulg.game.board.Pos,Move<PieceModel<Species>>> pos_move_map = new HashMap<>();
                        Action<PieceModel<Species>> action1;
                        
                        for( Move<PieceModel<Species>> m : moves )
                        {
                            action1 = m.actions.get(0);
                            
                            if( action1.kind == Action.Kind.MOVE )
                            {
                                // Se è una MOVE, calcolo la posizione di destinazione
                                pos_move_map.put(computeDestinationPos(action1), m);
                            }
                            else if( action1.kind == Action.Kind.JUMP )
                            {
                                // Se è una JUMP, la posizione di arrivo è la seconda della lista 'pos' della action
                                pos_move_map.put(action1.pos.get(1), m);
                            }
                            else if( action1.kind == Action.Kind.REMOVE )
                            {
                                // Se è una REMOVE, allora è una cattura diretta, 
                                // e la posizione di arrivo è la prima (e unica) della lista 'pos' della action
                                pos_move_map.put(action1.pos.get(0), m);
                            }
                        }
                        
                        return pos_move_map;
                    }
                    
                    /** 
                     * Data una lista di sotto-mosse, esamina le prime action di tutti i nodi figli, 
                     * raccogliendo in una lista le posizioni coinvolte utili per la GUI in una lista.
                     * 
                     * @param moves lista di sotto-mosse
                     * @return lista delle posizioni coinvolte nelle prime action delle sotto-mosse date
                     */
                    private List<gapp.ulg.game.board.Pos> parseFirstActionsPos(List<Move<PieceModel<Species>>> moves)
                    {
						List<gapp.ulg.game.board.Pos> final_pos_actions = new ArrayList<>();
						List<Move<PieceModel<Species>>> remove_sub_moves;
                    	Set<gapp.ulg.game.board.Pos> pos_actions = new HashSet<>();
                        
                        Action<PieceModel<Species>> action1, action2, sub_action;
                        
                        gapp.ulg.game.board.Pos pos_remove;
                        
                        for(Move<PieceModel<Species>> move : moves)
                        {
                            // Ottengo la prima e la seconda azione (se presente)
                            action1 = move.actions.get(0);
                            action2 = move.actions.size() > 1 ? move.actions.get(1) : null;
                            
                            switch(action1.kind)
                            {
                                case ADD:
                                case JUMP:
                                    {
                                        pos_actions.add(action1.pos.get(0));
                                    }
                                    break;            
                            
                                case MOVE:
                                case SWAP:
                                    {
                                        pos_actions.addAll(action1.pos);
                                    }
                                    break;
                                
                                case REMOVE:
                                    {
                                        /*
                                         * - Workaround:
                                         * Per eseguire una cattura diretta in modo naturale (selezionare prima il pezzo che mangia e poi il pezzo mangiato), 
                                         * e sopratutto distinguerla dalle comuni REMOVE, bisogna vedere se la sotto-mossa che ha per prima action la REMOVE ha 
                                         * un'altra action dopo:
                                         * - Se la ha una action dopo, essa è una cattura diretta solo se quest'ultima è o una JUMP o una MOVE, altrimenti è 
                                         * una normale REMOVE.
                                         * - Se non ha una action dopo, allora bisogna vedere quente pos essa rimuove:
                                         * -- Se rimuove più di una pos, allora è una normale REMOVE multipla (non di interesse, quindi non considerata).
                                         * -- Se rimuove una sola pos, allora bisogna scendere al nodo figlio che contiene la sotto-mossa con la sola action 
                                         * REMOVE (con una 'select(pos_da_rimuovere)' e una 'doSelection(null)'), e controllare se ha dei nodi figli:
                                         * --- Se non li ha, allora è una normale REMOVE
                                         * --- Se li ha e hanno tutti sotto-mosse che iniziano con o con JUMP o con MOVE, allora sono due o più catture dirette, 
                                         * altrimenti è una normale REMOVE
                                         */
                                    	
                                    	// Nel caso di una mossa di cattura (la REMOVE è seguita da una mossa di movimento), allora considera la seconda azione.
                                        // Altriemnti (caso di una mossa di semplice rimozione) considera la prima.
                                        if( action2!=null && (action2.kind == Action.Kind.JUMP || action2.kind == Action.Kind.MOVE) )
                                        {
                                            if(action2.kind == Action.Kind.JUMP)
                                                pos_actions.add(action2.pos.get(0));
                                            else
                                                pos_actions.addAll(action2.pos);
                                        }
                                        else
                                        {
                                            if( action1.pos.size()==1 )
                                            {
                                            	pos_remove = action1.pos.get(0);
                                            	
                                            	// Seleziono il nodo con la REMOVE
                                            	mc.select(pos_remove);
                                            	mc.doSelection(null);
                                            	
                                            	// Calcolo le sotto-mosse dei nodi figli della REMOVE
                                            	remove_sub_moves = mc.childrenSubMoves();
                                            	
                                            	if( remove_sub_moves.isEmpty() )
                                            	{
                                            		// È una normale REMOVE
                                                	pos_actions.add(action1.pos.get(0));
                                            	}
                                            	else
                                            	{
                                            		// Sono due o più catture dirette convergenti
                                            		for( Move<PieceModel<Species>> m : remove_sub_moves )
                                            		{
                                            			sub_action = m.actions.get(0);
                                            			
                                            			if(sub_action.kind == Action.Kind.JUMP)
                                            				pos_actions.add(sub_action.pos.get(0));
                                                        else if(sub_action.kind == Action.Kind.MOVE)
                                                        	pos_actions.addAll(sub_action.pos);
                                            		}
                                            	}
                                            	
                                            	// Ripristino il nodo corrente precedente
                                            	mc.back();
                                            }
                                        }
                                    }
                                    break;
                            }
                        }
                        
                        // Metto tutte le posizioni in un set per evitare duplicati
                        final_pos_actions.addAll(pos_actions);
                        
                        return final_pos_actions;
                    }
                    
                    /**
                     * Data una mossa di movimento, restituisce la posizione finale del pezzo dopo l'esecuzione della mossa.
                     * Se la mossa non è di movimento, ritorna null.
                     * 
                     * @param m mossa di cui calcolare la posizione finale
                     * @return la posizione finale della mossa data se essa è di movimento, altrimenti null.
                     */
                    private gapp.ulg.game.board.Pos computeDestinationPosMove(Move<PieceModel<Species>> m)
                    {
                    	if( m==null )
                    		return null;
                    	
                    	Action<PieceModel<Species>> last_action;
                    	int i = m.actions.size()-1;
                    	
                    	do
                    	{
                    		last_action = m.actions.get(i);
                    		i--;
                    	}while( i>=0 && last_action.kind!=Action.Kind.JUMP && last_action.kind!=Action.Kind.MOVE );
                    	
                    	if( last_action.kind==Action.Kind.JUMP )
                    		return last_action.pos.get(1);
                    	else if( last_action.kind==Action.Kind.MOVE )
                    		return this.computeDestinationPos(last_action);
                    	else
                    		return null;
                    }
                    
                    /**
                     * Esegue le operazioni necessarie a tornare indietro:
                     * - Se c'è una mossa da annullare, esegue la sotto-mossa inversa e defocussa l'ultima cella pigiata.
                     * - Se non è stata eseguita alcuna mossa, ma c'è una cella selezionata, allora pulisci la selezione e defocussa tutte le celle.
                     * - Se non c'è alcuna cella selezionata non fa nulla.
                     */
                    private void doBack()
                    {
                    	if( clicked_pos.isEmpty() )
                    		return;
                    	
                    	// Ottiene la sotto-mossa inversa attuale (se presente)
                    	Move<PieceModel<Species>> inverse_sub_move = mc.back();
                        
                        if( inverse_sub_move==null )
                        {
                        	// Non c'è nessuna mossa da annullare, quindi pulisce la selezione corrente
                        	mc.clearSelection();
                        	
                        	// Rimuove l'ultima cella pigiata dallo stack e defocussa tutte le celle
                        	clicked_pos.pop();
                        	this.javaFXDefocusAllBoardCell();
                        }
                        else
                        {
                        	// Esegue la sotto-mossa inversa
                        	this.javaFXMove(inverse_sub_move);
                        	
                        	// Se indicato, eseguire un ulteriore back e resettare il flag 'do_double_back'
                        	if( this.do_double_back )
                        	{
                        		this.javaFXMove(mc.back());
                        		this.do_double_back = false;
                        	}
                        	
                        	// Rimuove l'ultima cella pigiata dallo stack e la defocussa
                        	this.javaFXDefocusBoardCell(clicked_pos.pop());
                        }
                    }
                    
                    // METODI DI JAVAFX
                    
                    /** Esegue una mosa nella board grafica.
                     * @param m mossa da eseguire
                     */
                    private void javaFXMove(Move<PieceModel<Species>> m)
                    {
                        // Tiene in memoria l'ultima mossa eseguita
                    	this.latest_move = m;
                    	
                    	Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                board.execMove(m);
                            }
                        });                            
                    }
                    
                    /** Focussa una posizione della board grafica.
                     * @param pos posizione da focussare
                     */
                    private void javaFXFocusBoardCell(gapp.ulg.game.board.Pos pos)
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                board.focusCell(pos);
                            }
                        });
                    }
                    
                    /** Defocussa una posizione della board grafica.
                     * @param pos posizione da defocussare
                     */
                    private void javaFXDefocusBoardCell(gapp.ulg.game.board.Pos pos)
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                board.defocusCell(pos);
                            }
                        });  
                    }
                    
                    /**
                     * Focussa una collezione di posizioni della board grafica.
                     * @param pos_collection collezione di posizioni da focussare
                     */
                    private void javaFXFocusBoardCell(Collection<? extends gapp.ulg.game.board.Pos> pos_collection)
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                board.focusAllCell(pos_collection);
                            }
                        });  
                    }
                    
                    /**
                     * Defocussa tutte le posizioni della board grafica.
                     */
                    private void javaFXDefocusAllBoardCell()
                    {
                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                board.defocusAllCell();
                            }
                        });  
                    }
                    
                    /**
                     * Inizializza lo stato dei bottoni principali ('btn_pass', 'btn_choose', e 'btn_backmove').
                     * @param canPass flag che indica se è possibile passare il turno
                     * @param canChoose flag che indica se è possibile scegliere il nodo corrente
                     */
                    private void javaFXInitButtons(boolean canPass, boolean canChoose)
                    {
                    	Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // Disabilita il bottone se non può passare
                                btn_pass.setDisable(!canPass);
                                
                                // Disabilita il bottone se non si può scegliere il nodo corrente
                                btn_choose.setDisable(!canChoose);
                                
                                // Se non c'è nulla di selezionato, non c'è nulla da annullare
                                if(clicked_pos.isEmpty())
                                    btn_backmove.setDisable(true);
                            }
                        });
                    }
                    
                    /**
                     * Data una lista di pezzi, apre una finestra con una combobox per sceglierne uno, scrivendo il messaggio dato.
                     * I pezzi sono mostrati nell'ordine dato, rappresentati dal nome del modello del pezzo 
                     * (è assunto che siano tutti dello stesso colore).
                     * Il risultato viene inserito in 'chosen_piece_index', che inizialmente è settato a '-1', e può essere:
                     * - L'index del pezzo scelto nella lista data (sempre >=0) se la scelta è andata a buon fine.
                     * - Il valore '-2' in caso i parametri non siano validi (almeno uno è null, la lista è vuota, o il tipo dell'azione non è né ADD né SWAP), 
                     * o la scelta non è andata a buon fine (la finestra viene chiusa dall'utente o ci sono errori inattesi)
                     * 
                     * @param pieces_list lista dei pezzi fra cui scegliere
                     * @param actionKind tipo di azione per cui è richiesta la scelta (o ADD o SWAP)
                     */
                    private void javaFXPieceChoiceDialog(List<PieceModel<Species>> pieces_list, Action.Kind actionKind)
                    {
                    	// Se i parametri non sono validi, setta 'chosen_piece_index' a '-2' e ritorna immediatamente
                    	if( pieces_list==null || pieces_list.isEmpty() || actionKind==null || (actionKind!=Action.Kind.ADD && actionKind!=Action.Kind.SWAP) )
                    	{
                    		this.chosen_piece_index = -2;
                    		return;
                    	}
                    	
                    	// Resetta 'chosen_piece_index' al valore iniziale
                    	this.chosen_piece_index = -1;
                    	
                    	// Lancia il listenere nel JavaFX Application Thread
                    	Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                            	// Seleziona i messaggi da scrivere nello ChoiceDialog
                            	String windowTitle, headerMsg;
                            	if( actionKind==Action.Kind.ADD )
                            	{
                            		windowTitle = "Scegli pezzo da inserire";
                            		headerMsg = "Puoi inserire un prezzo tra i seguenti";
                            	}
                            	else
                            	{
                            		windowTitle = "Scegli pezzo da scambiare";
                            		headerMsg = "Puoi scambiare il pezzo selezionato con uno tra i seguenti";
                            	}
                            	
                            	// Converte la lista dei pezzi in un array dei tipi di pezzi
                                String[] piece_names = pieces_list.stream().map(p -> p.species.name()).toArray(String[]::new);
                                
                                // Creo e setto il ChoiceDialog
                                ChoiceDialog<String> pieces_dialog = new ChoiceDialog<String>(null, piece_names);
                                pieces_dialog.setTitle(windowTitle);
                                pieces_dialog.setHeaderText(headerMsg);
                                pieces_dialog.setContentText("Pezzo: ");
                                
                                // Mostro la finestra, attendo il risultato, e aggiorno 'chosen_piece_index'
                                Optional<String> result = pieces_dialog.showAndWait();
                                if( result.isPresent() )
                                {
                                	int result_index = Arrays.binarySearch(piece_names, result.get());
                                	chosen_piece_index = result_index<0 ? -2 : result_index;
                                }
                                else
                                	chosen_piece_index = -2;
                            }
                        });
                    }
                    
                    // MAIN DEL MASTER
                    
                    @Override
                    public void run()
                    {
                        try
                        {
                        	// Valore ritornato dal listener principale
                            int user_choice = 0;
                            
                            // Flag per la scelta effettuata
                            boolean choose_done = false;
                            
                            // Flag per bottoni board
                            boolean canChoose, canPass = mc.mayPass();
                            
                            // Sotto-mosse dei nodi figli (tutte)
                            List<Move<PieceModel<Species>>> child_sub_moves;
                            
                            // Liste di posizioni utilizzabili nella board
                            List<gapp.ulg.game.board.Pos> pos_action_list;
                            
                            // Sotto-mosse dei nodi figli selezionate
                            List<Move<PieceModel<Species>>> selected_move_list;
                            
                            // Sotto-mosse di cattura diretta
                            List<Move<PieceModel<Species>>> direct_capture_moves;
                            
                            // Lista dei pezzi selezionati dalla selezione corrente
                            List<PieceModel<Species>> pieces_list;
                            
                            // Variabili di appoggio
                            Map<gapp.ulg.game.board.Pos,Move<PieceModel<Species>>> pos_map;
                            Move<PieceModel<Species>> previous_move;
                            Action<PieceModel<Species>> first_action, next_action;
                            gapp.ulg.game.board.Pos finalPos, startPos, destinationPos;
                            
                            // Esegue eventuale sotto-mossa della radice
                            Optional<Move<PieceModel<Species>>> root_move = mc.subMove();
                            if(root_move.isPresent()) this.javaFXMove(root_move.get());
                            
                            // Fino a quando l'utente non ha effettuato la scelta
                            while( !choose_done )
                            {
                                // Ottiene le sottomosse dei figli del nodo attuale
                                child_sub_moves = mc.childrenSubMoves();
                                
                                // Esamina le prime action di tutti i nodi figli, raccogliendo le posizioni da esse coinvolte in una lista
                                pos_action_list = parseFirstActionsPos(child_sub_moves);
                                
                                // Verifico se il nodo corrente è finale (contiene una mossa completa)
                                canChoose = mc.isFinal();
                                
                                // Ciclo di attesa scelte utente
                                while( true )
                                {
                                    // Resetta la lista dei pezzi data da 'selectionPieces'
                                	pieces_list = new ArrayList<>();
                                    
                                    /*
                                     * Aspetta il listener principale della GUI
                                     * 
                                     * Esso attende fino a che 
                                     * - l'utente modifica la selezione corrente della board
                                     * - il tempo a disposizione scade
                                     * - alla pressione del bottone di Resign, di Pass, o di scelta della mossa nel nodo corrente
                                     * 
                                     * Input listener:
                                     * - 'canChoose' -> se è 'true', il nodo corrente può essere scelto
                                     * - 'canPass' -> se è 'true', l'utente può passare il turno
                                     * - 'pos_action_list' -> posizioni della board selezionabili dall'utente
                                     * 
                                     * Output listener:
                                     * - 'selected_pos_list' -> posizioni della board correntemente selezionate
                                     * - 'user_choice' -> azione effettuata dall'utente
                                     * 
                                     * 'user_choice' può avere i seguenti valori:
                                     * - 0 -> se l'utente ha modificato la selezione corrente
                                     * - 1 -> se l'utente ha scelto la resa (o è scaduto il tempo per la mossa)
                                     * - 2 -> se l'utente ha scelto di passare il turno
                                     * - 3 -> se l'utente ha scelto il nodo corrente
                                     * - 4 -> se l'utente vuole annullare l'ultima azione
                                     */
                                    
                                    // Aggiorna i bottoni
                                    this.javaFXInitButtons(canPass, canChoose);
                                    
                                    if( clicked_pos.isEmpty() && !canChoose )
                                    {
                                    	// Prima selezione di un pezzo
                                    	
                                    	//--- blocco di ascolto
                                    	synchronized(choose)
                                        {
                                            try
                                            {                                
                                                do
                                                {                                                    
                                                    choose.wait();                                
                                                    
                                                    user_choice = choose.get();  
                                                    
                                                    if(user_choice != 0)
                                                    {
                                                        break;
                                                    }
                                                    else if(pos_action_list.containsAll(clicked_pos))
                                                    {
                                                        Platform.runLater(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                btn_backmove.setDisable(false);
                                                            }
                                                        }); 
                                                        this.javaFXFocusBoardCell(clicked_pos.peek());
                                                        break;
                                                    }
                                                    else
                                                        clicked_pos.pop();
                                                }
                                                while(true);
                                            }
                                            catch (InterruptedException e) { return; }
                                        }
                                    	//--- blocco di ascolto
                                    	
                                    	// Se l'utente non ha modificato la board, ma ha premuto un bottone, allora esegui l'azione corrispondente
                                        if( user_choice!=0 )
                                        {
                                            if( user_choice==1 )
                                            {
                                                // Arrenditi
                                                mc.resign();
                                                
                                                choose_done = true;
                                            }
                                            else if( user_choice==2 )
                                            {
                                                // Passa il turno
                                                mc.pass();
                                                
                                                choose_done = true;
                                            }
                                            else if( user_choice==3 )
                                            {
                                                // Scegli il nodo corrente
                                                mc.move();
                                                
                                                choose_done = true;
                                            }
                                            else
                                            {
                                            	// Annulla la selezione                                            	
                                            	this.doBack();
                                            }
                                            
                                            break; // Se user_choice!=0, in ogni caso esce dal ciclo interno
                                        }
                                        
                                        // Disabilito pulsanti di scelta escluso backmove
                                        Platform.runLater(new Runnable()
                                        {
                                            @Override
                                            public void run()
                                            {
                                                btn_pass.setDisable(true);
                                                btn_choose.setDisable(true);
                                            }
                                        });
                                        
                                        // Se non ci sono posizioni selezionate dall'utente, ritorna alla chiamata del listener
                                        if( clicked_pos.isEmpty() ) continue;
                                        
                                        // Ottiene le sotto-mosse selezionate dall'attuale insieme di posizioni selezionate
                                        selected_move_list = mc.select(clicked_pos.toArray(new gapp.ulg.game.board.Pos[clicked_pos.size()]));
                                        
                                        // Cerca inoltre la lista di eventuali sotto-mosse di cattura diretta selezionate dall'attuale insieme di posizioni selezionate
                                        direct_capture_moves = selectDirectCaptureMove(child_sub_moves, clicked_pos.peek());
                                    }
                                    else if( clicked_pos.size()==1 && !canChoose )
                                    {
                                    	// È stata fatta una selezione che non esegue un'azione senza un'altro input dell'utente
                                    	
                                    	// Illumina la cella della posizione selezionata
                                    	this.javaFXFocusBoardCell(clicked_pos.peek());
                                    	
                                    	// Ottiene le sotto-mosse selezionate dall'attuale insieme di posizioni selezionate
                                        selected_move_list = mc.select(clicked_pos.peek());
                                        
                                        // Cerca inoltre la lista di eventuali sotto-mosse di cattura diretta selezionate dall'attuale insieme di posizioni selezionate
                                        direct_capture_moves = selectDirectCaptureMove(child_sub_moves, clicked_pos.peek());
                                    }
                                    else if( canChoose && child_sub_moves.isEmpty() )
                                    {
                                		// Sono arrivato ad un nodo foglia dell'albero delle mosse
                                    	
										/*
										 * - Workaround:
										 * Se una mossa è divisa in pù scelte, all'ultima scelta non illumina le celle corrette.
										 * Quindi mi tengo ogni volta l'ultima mossa eseguita dall'utente, e se è di movimento, 
										 * allora ottengo la pos di destinazione finale, tolgo l'ultima posizone da quelle pigiate, vi inserisco 
										 * la destinazione finale, e illumino quest'ultima.
										 * Tale workaround viene eseguito solo se:
										 * - lo stack ha almeno un'elemento
										 * - la posizione calcolata e quella in cima allo stack sono diverse.
										 */
                                    	if( !clicked_pos.isEmpty() )
                                    	{
                                    		finalPos = this.computeDestinationPosMove(this.latest_move);
                                    		if( finalPos!=null && !finalPos.equals(clicked_pos.peek()) )
    										{
    											this.javaFXDefocusBoardCell(clicked_pos.pop());
    											this.javaFXFocusBoardCell(finalPos);
    											clicked_pos.push(finalPos);
    										}
                                    	}
                                    	
                                		//--- blocco di ascolto
                                    	synchronized(choose)
                                        {
                                            try
                                            {
                                                do
                                                {                                                    
                                                    choose.wait();                              
                                                    
                                                    user_choice = choose.get();  
                                                    
                                                    if(user_choice != 0)
                                                    {
                                                        break;
                                                    }
                                                    else
                                                        clicked_pos.pop();
                                                }
                                                while(true);
                                            }
                                            catch (InterruptedException e) { return; }
                                        }
                                    	//--- blocco di ascolto
                                    	
                                    	// Se l'utente non ha modificato la board, ma ha premuto un bottone, allora esegui l'azione corrispondente
                                        if( user_choice!=0 )
                                        {
                                            if( user_choice==1 )
                                            {
                                                // Arrenditi
                                                mc.resign();
                                                
                                                choose_done = true;
                                            }
                                            else if( user_choice==2 )
                                            {
                                                // Passa il turno
                                                mc.pass();
                                                
                                                choose_done = true;
                                            }
                                            else if( user_choice==3 )
                                            {
                                                // Scegli il nodo corrente
                                                mc.move();
                                                
                                                choose_done = true;
                                            }
                                            else
                                            {
                                            	// Annulla mossa                                                
                                            	this.doBack();
                                            }
                                            
                                            break; // Se user_choice!=0, in ogni caso esce dal ciclo interno
                                        }
                                        
                                        // Servono per evitare che Java dica che non sono stati inizializzati
                                        selected_move_list = new ArrayList<>();
                                        direct_capture_moves = new ArrayList<>();
                                    }
                                    else
                                    {
                                    	// C'è almeno un'altra scelta da poter fare.
                                    	// La posizione attuale del pezzo da muovere è in 'pos_action_list.get(0)'.
                                    	
                                    	// Rimuove l'ultima posizione pigiata
                                    	clicked_pos.pop();
                                		// Aggiunge la posizione attuale del pezzo a quelle pigiate
                                		clicked_pos.push(pos_action_list.get(0));
                                		
                                		// Defocussa tutte le celle e focussa quelle attuali
                                		this.javaFXDefocusAllBoardCell();
                                		this.javaFXFocusBoardCell(pos_action_list.get(0));
                                		
                                		// Ottiene le sotto-mosse selezionate dall'ultima posizione selezionata
                                        selected_move_list = mc.select(pos_action_list.get(0));
                                        
                                        // Cerca inoltre la lista di eventuali sotto-mosse di cattura diretta selezionate dall'ultima posizione selezionata
                                        direct_capture_moves = selectDirectCaptureMove(child_sub_moves, pos_action_list.get(0));
                                    }
                                    
                                    // Se non ci sono sotto-mosse selezionate (nè catture dirette possibili), ritorna al listener principale
                                    if( selected_move_list.isEmpty() && direct_capture_moves.isEmpty() ) continue;
                                    
                                    /*
                                     *  Ottengo la lista dei pezzi coinvolti nelle prime action delle sotto-mosse dei nodi figli selezionati, 
                                     *  da cui posso dedurre le possibili scelte che ha l'utente.
                                     *  La lista viene ordinata, così da essere usabile per la finestra di scelta dei pezzi.
                                     *  Nel caso di catture dirette non è necessario effettuare il selectionPieces, poiché la lista sarebbe sempre vuota.
                                     */
                                    if( direct_capture_moves.isEmpty() )
                                    {
                                    	pieces_list = mc.selectionPieces();
                                    	Collections.sort(pieces_list, (p1, p2) -> p1.species.toString().compareTo(p2.species.toString()) );
                                    }
                                    
                                    // In base ad essa, scelgo cosa fare
                                    if( !direct_capture_moves.isEmpty() )
                                    {
                                        // Sono disponibili delle catture dirette
                                        
                                        // Aggiunge alle sotto-mosse selezionate le mosse di cattura diretta
                                        selected_move_list.addAll(direct_capture_moves);
                                        
                                        // Converto l'insieme delle mosse di movimento selezionate in una mappa DestinationPos -> Move
                                        pos_map = buildPosMap(selected_move_list);
                                        
                                        // Salvo la posizione di partenza della possibile cattura diretta
                                        startPos = clicked_pos.peek();
                                        
                                        /*
                                         * Fa scegliere all'utente una posizione di arrivo tra quelle in 'pos_map.keySet()'
                                         * Una volta scelta la posizione, inserirla in 'pos_choosed'
                                         */        
                                        //--- blocco di ascolto
                                        synchronized(choose)
                                        {
                                            try
                                            {
                                            	// Focussa le posizioni dove si può muovere il pezzo
                                            	this.javaFXFocusBoardCell(pos_map.keySet());
                                                
                                                do
                                                {
                                                    choose.wait();
                                                    
                                                    user_choice = choose.get();                        
                                                    
                                                    if( user_choice==1 )
                                                    {
                                                        // Arrenditi
                                                        mc.resign();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==2 )
                                                    {
                                                        // Passa il turno
                                                        mc.pass();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==3 )
                                                    {
                                                        // Scegli il nodo corrente
                                                        mc.move();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==4 )
                                                    {
                                                    	// Annulla mossa                                                        
                                                    	this.doBack();
                                                        
                                                        break;
                                                    }
                                                    else if(pos_map.keySet().contains(clicked_pos.peek()))
                                                    {
                                                    	this.javaFXFocusBoardCell(clicked_pos.peek());
                                                        break;
                                                    }
                                                    else
                                                        clicked_pos.pop();
                                                }
                                                while(true);
                                                
                                                // Defocussa tutte le celle dove il pezzo poteva muoversi
                                                for(gapp.ulg.game.board.Pos p : pos_map.keySet())
                                                    if(!clicked_pos.contains(p))
                                                    	this.javaFXDefocusBoardCell(p);
                                            }
                                            catch (InterruptedException e) { return; }
                                        }
                                        
                                        // Se è stata effettuata una scelta terminale o un back, allora esci dal ciclo interno
                                        if( user_choice>=1 && user_choice<=4 ) break;
                                        
                                        //--- blocco di ascolto
                                        
                                        // Aggiorno la selezione (potrebbe essere cambiata dopo un back)
                                    	mc.select(startPos);
                                    	
                                        // Se è un azione di movimento, sposta il nodo corrente al figlio che muove il pezzo verso la posizione scelta
                                        // Se è una cattura diretta, sposta il nodo corrente al figlio che rimuove il pezzo mangiato ed effettua il movimento di cattura
                                    	destinationPos = clicked_pos.peek();
                                        first_action = pos_map.get(destinationPos).actions.get(0);
                                        if( first_action.kind==Action.Kind.JUMP )
                                        {
                                        	this.javaFXMove( mc.jumpSelection(first_action.pos.get(1) ));
                                        }
                                        else if( first_action.kind==Action.Kind.MOVE )
                                        {
                                        	this.javaFXMove( mc.moveSelection(first_action.dir, first_action.steps) );
                                        }
                                        else
                                        {
                                        	// Nel caso di una cattura diretta, riesegui la selezione il pos della REMOVE
                                            mc.select(first_action.pos.get(0));
                                            
                                            // Ora è selezionato un solo figlio che comincia con una REMOVE, quindi esegui doSelection
                                            this.javaFXMove( mc.doSelection(null) );
                                            
                                            if( !mc.isFinal() )
                                            {
                                            	/*
                                            	 * - Workaround
                                            	 * Supporto alle catture dirette convergenti.
                                            	 * Se a questo punto non si è in un nodo finale, allora ci sono più catture dirette che 
                                            	 * convergono ad una sola posizone.
                                            	 * In questo caso si seleziona la posizione di partenza del movimento, e e si scorrono le sotto-mosse 
                                            	 * selezionate fino a quando non si trova quella che inizia per l'azione di movimento che sposta il pezzo da 
                                            	 * 'startPos' a 'destinationPos', eseguendo poi tale azione.
                                            	 * Fatto questo, si abilita il flag 'done_double_capture_move' per ricordasi di effettuare un'ulteriore back 
                                            	 * nel caso si voglia tornare indietro.
                                            	 */
                                            	for( Move<PieceModel<Species>> remove_sub_move : mc.select(startPos) )
                                            	{
                                            		next_action = remove_sub_move.actions.get(0);
                                            		if( next_action.kind==Action.Kind.JUMP && next_action.pos.get(1).equals(destinationPos) )
                                            		{
                                            			this.javaFXMove( mc.jumpSelection(destinationPos) );
                                            			this.do_double_back = true;
                                            			break;
                                            		}
                                            		else if( next_action.kind==Action.Kind.MOVE && this.computeDestinationPos(next_action).equals(destinationPos) )
                                            		{
                                            			this.javaFXMove( mc.moveSelection(next_action.dir, next_action.steps) );
                                            			this.do_double_back = true;
                                            			break;
                                            		}
                                            	}
                                            }
                                        }
                                    }
                                    else if( pieces_list.size()==1 && pieces_list.get(0)==null )
                                    {
                                        // Se la lista contiene solo un valore null, allora 
                                        // c'è un unico figlio selezionato, che inizia con una REMOVE (e non è una cattura diretta)
                                        
                                        // Sposta il nodo corrente al nodo figlio che rimuove i pezzi nelle posizioni scelte
                                        // Non è necessaria ulteriore interazione dell'autente
                                    	this.javaFXMove(mc.doSelection(null));
                                    }
                                    else if( !pieces_list.isEmpty() && pieces_list.get(0)!=null )
                                    {
                                        // Se la lista non è vuota e ha il primo valore non nullo, allora tutte le prime azioni sono ADD o SWAP.
                                        // Ogni figlio rappresenta un pezzo che è possibile inserire (o scambiare)
                                        
                                        if( pieces_list.size()==1 )
                                        {
                                            // Se c'è un solo pezzo possibile, sposta direttamente il nodo corrente al figlio che aggiunge (o scambia) tale pezzo
                                            // Non è necessaria un ulteriore interazione dell'utente
                                        	
                                        	this.javaFXMove(mc.doSelection(pieces_list.get(0)));
                                        }
                                        else
                                        {
                                            // Se ce n'è più di uno, allora fai effettuare la scelta all'utente
                                            
                                        	// Salvo l'ultima mossa effettuata
                                        	previous_move = this.latest_move;
                                        	
                                            // Fa scegliere all'utente un pezzo tra quelli in 'pieces_list'
                                        	this.javaFXPieceChoiceDialog(pieces_list, selected_move_list.get(0).actions.get(0).kind);
                                            
                                        	// Attende che la scelta sia effettuata (lo è quando 'chosen_piece_index' non è più '-1')
                                            while( this.chosen_piece_index==-1 );
                                            
                                            // Se nell'ultima mossa eseguita prima della SWAP o della ADD c'è un'azione di movimento iniziata dall'utente e 
                                            // con destinazione l'ultima posizione pigiata, allora segna che per annullare servono due back
                                            destinationPos = this.computeDestinationPosMove(previous_move);
                                            if( destinationPos!=null && destinationPos.equals(clicked_pos.peek()) )
                                            	this.do_double_back = true;
                                            
                                            // Se 'chosen_piece_index' diventa '-2', allora la scelta è fallita
                                            if( this.chosen_piece_index==-2 )
                                            {
                                            	// Annulla la selezione corrente (ed eventualmente la mossa di movimento precedente)
                                            	this.doBack();
                                            	break;
                                            }
                                            
                                            // Se 'chosen_piece_index' è maggiore di zero, allora la scelta è andata a buon fine, 
                                            // quindi porta il nodo corrente al figlio che aggiunge il pezzo scelto dall'utente
                                            this.javaFXMove(mc.doSelection(pieces_list.get(this.chosen_piece_index)));
                                        }
                                    }
                                    else
                                    {
                                        // C'è solo una posizione selezionata, in cui c'è un pezzo che può essere mosso (e catturare solo indirettamente)
                                        // Questa procudura unifica la gestione delle JUMP e delle MOVE
                                        
                                        // Converto l'insieme delle mosse di movimento selezionate in una mappa DestinationPos -> Move
                                        pos_map = buildPosMap(selected_move_list);
                                        
                                        // Salvo la posizione di partenza della mossa di movimento
                                        startPos = clicked_pos.peek();
                                        
                                        /*
                                         * Fa scegliere all'utente una posizione di arrivo tra quelle in 'pos_map.keySet()'
                                         */
                                        //--- blocco di ascolto
                                        synchronized(choose)
                                        {
                                            try
                                            {
                                                // Focussa le posizioni dove si può muovere il pezzo
                                            	this.javaFXFocusBoardCell(pos_map.keySet());
                                            	
                                                do
                                                {
                                                    choose.wait();
                                                    
                                                    user_choice = choose.get();
                                                    
                                                    if( user_choice==1 )
                                                    {
                                                        // Arrenditi
                                                        mc.resign();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==2 )
                                                    {
                                                        // Passa il turno
                                                        mc.pass();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==3 )
                                                    {
                                                        // Scegli il nodo corrente
                                                        mc.move();
                                                        
                                                        choose_done = true;
                                                        
                                                        break;
                                                    }
                                                    else if( user_choice==4 )
                                                    {
                                                    	// Annulla mossa                                                        
                                                    	this.doBack();
                                                        
                                                        break;
                                                    }
                                                    else if(pos_map.keySet().contains(clicked_pos.peek()))
                                                    {
                                                    	this.javaFXFocusBoardCell(clicked_pos.peek());
                                                        break;
                                                    }
                                                    else
                                                    	clicked_pos.pop();
                                                }
                                                while(true);
                                                
                                                // Defocussa tutte le celle dove il pezzo poteva muoversi
                                                for(gapp.ulg.game.board.Pos p : pos_map.keySet())
                                                    if(!clicked_pos.contains(p))
                                                    	this.javaFXDefocusBoardCell(p);
                                            }
                                            catch (InterruptedException e) { return; }
                                        }
                                        
                                        // Se è stata effettuata una scelta terminale o un back, allora esci dal ciclo interno
                                        if( user_choice>=1 && user_choice<=4 ) break;
                                        
                                        //--- blocco di ascolto
                                        
                                        // Aggiorno la selezione (potrebbe essere cambiata per il calcolo di catture dirette convergenti)
                                        mc.select(startPos);
                                        
                                        // Sposta il nodo corrente al figlio, che muove il pezzo verso la posizione scelta
                                        first_action = pos_map.get(clicked_pos.peek()).actions.get(0);
                                        if( first_action.kind==Action.Kind.JUMP )
                                        {
                                        	this.javaFXMove( mc.jumpSelection(first_action.pos.get(1)) );
                                        }
                                        else
                                        {
                                        	this.javaFXMove( mc.moveSelection(first_action.dir, first_action.steps) );
                                        }
                                    }
                                    
                                    break; // È stata eseguita un'azione, quindi esci dal ciclo interno
                                }
                            }
                            
                            // Resetto la selezione e defocusso tutte le celle
                            clicked_pos.clear();
                            this.javaFXDefocusAllBoardCell();
                            humanmove.set(true);
                        }
                        catch(IllegalStateException e)
                        {
                        	// Resetto la selezione e defocusso tutte le celle
                            clicked_pos.clear();
                            this.javaFXDefocusAllBoardCell();
                            humanmove.set(true);
                            
                            return; // Il MoveChooser è inutilizzabile, quindi esci
                        }
                    }
                }
            );
        };
    }
    
    /**
     * Restituisce il consumer del master.
     * @return il consumer che esegue il master
     */
    public Consumer<MoveChooser<PieceModel<Species>>> getMasterConsumer()
    {
        return master;        
    }
    
    /**
     * Inizializza il gioco che sarà osservato dal {@link GameObserver} fornito.
     * @param in_gameobserver {@link GameObserver} del gioco
     */
    @SuppressWarnings("unchecked")
	public void observethisGame(GameObserver<PieceModel<Species>> in_gameobserver)
    {
        gameobserver = in_gameobserver;

        gameobserver.addNewListener
        (
                new GameObserver.ObserverListener<PieceModel<Species>>()
                {
                    @Override
                    public void moved(Move<PieceModel<Species>> m)
                    {
                        if(humanmove.get())
                        {
                            humanmove.set(false); 
                        }
                        else
                            board.execMove(m);
                    }
                    
                    @Override
                    public void gameEnded()
                    {
                    	board.loadBoard(gameobserver.getBoardView());
                    	ScenesManager.instance.changescenetoScoreBoard(gameobserver, board.getBoardSnapShot());
                    }
                }
        );
        
        //inizializzo e disgno la board
        int dimension = (int) (Math.min(ScenesManager.instance.getWindowWidth(), ScenesManager.instance.getWindowHeight())*0.80);
        int each = dimension/Math.max(gameobserver.getBoardView().width(), gameobserver.getBoardView().height());
               
        if(!ScenesManager.instance.nativecolor)
        {
            board = new GameBoard2D
            (
                each*gameobserver.getBoardView().width(),
                each*gameobserver.getBoardView().height(),
                gameobserver.getBoardView().width(),
                gameobserver.getBoardView().height(),
                ScenesManager.instance.boardcolor.firstcolor,
                ScenesManager.instance.boardcolor.secondcolor,
                gameobserver.getincludedPosition(),
                gameobserver.getPiecesColors()
            );
        }
        else
        {
            Color piece_first_color, piece_second_color;
            
            piece_first_color  = ScenesManager.instance.boardcolor.firstcolor;
            piece_second_color = ScenesManager.instance.boardcolor.secondcolor;
            
            List<String> colorlist = gameobserver.getPiecesColors();
            
            if
            (
                pure_colorname_map.containsKey(colorlist.get(0).toLowerCase().trim()) &&
                pure_colorname_map.containsKey(colorlist.get(1).toLowerCase().trim())
            )
            {
                piece_first_color = pure_colorname_map.get(colorlist.get(0).toLowerCase().trim());
                piece_second_color = pure_colorname_map.get(colorlist.get(1).toLowerCase().trim());
            }
            
            board = new GameBoard2D
            (
                each*gameobserver.getBoardView().width(),
                each*gameobserver.getBoardView().height(),
                gameobserver.getBoardView().width(),
                gameobserver.getBoardView().height(),
                ScenesManager.instance.boardcolor.firstcolor,
                ScenesManager.instance.boardcolor.secondcolor,
                gameobserver.getincludedPosition(),
                piece_first_color,
                piece_second_color,
                colorlist
            );
        }
        
        //aggiungo listener sulla board
        board.addNewListener
        (
            new BoardListener()
            {
                @Override
                public void setOnCellClick(MouseEvent e, int cell_x, int cell_y) {}

                @Override
                public void setOnPrimaryButtonDown(MouseEvent e, int cell_x, int cell_y) 
                {                           
                    synchronized(choose)
                    {
                    	choose.set(0);
                    	clicked_pos.push(new gapp.ulg.game.board.Pos(cell_x,board.getYtoTInx(cell_y)));
                    	choose.notifyAll();
                    }
                }

                @Override
                public void setOnSecondaryButtonDown(MouseEvent e, int cell_x, int cell_y) 
                {
                    if(!btn_backmove.isDisable())
                    {
                        synchronized(choose)
                        {
                            choose.set(4);
                            choose.notify();
                        }
                    }
                }                
            }
        );
        
        //aggiungo i pezzi sopra la board
        board.loadBoard(gameobserver.getBoardView());
                
        //box
        vbox_center = new VBox(board);    
        
        this.root.setCenter(vbox_center); 
        
        //INIZIALIZZO SCORE
        scores.set( gameobserver.gameHasScores() );
        
        if(scores.get())
        {
            Tab[] left_tabs = new Tab[2];
            
            //statistiche
            left_tabs[0] = new Tab("Statistiche");
            left_tabs[0].setClosable(false);
            left_tabs[0].setContent(vbox_left);
            
            //punteggi
            left_tabs[1] = new Tab("Punteggi");
            left_tabs[1].setClosable(false);
            
            TableView<PlayerScore> score_table = new TableView<PlayerScore>();
            score_table.setEditable(false);
            
            TableColumn<PlayerScore, String> playernameclm = new TableColumn<PlayerScore, String>("Giocatore");
            playernameclm.setMinWidth(130);
            playernameclm.setCellValueFactory(new PropertyValueFactory<PlayerScore, String>("playername"));
     
            TableColumn<PlayerScore, String> scoreclm = new TableColumn<PlayerScore, String>("Punteggio");
            scoreclm.setMinWidth(130);
            scoreclm.setCellValueFactory(new PropertyValueFactory<PlayerScore, String>("playerscore"));
            
            //inizializzo i nomi dei giocatori     
            player_scores = FXCollections.observableArrayList();            
            score_table.setItems(player_scores);
            
            int counter = 0;
            double[] scores = gameobserver.getScores();
            
            //immetto nuovi valori            
            for(String playername : gameobserver.getPlayerList())
            {
                player_scores.add(new PlayerScore(playername,scores[counter]));
                counter++;
            }  
            
            //preparo la tabella per essere aggiunta alla tab
            score_table.getColumns().addAll(playernameclm, scoreclm);
            score_table.setStyle("-fx-border-color: black"); 
            
            //aggiungo la tabella degli score
            left_tabs[1].setContent(score_table);
            
            //aggiungo al root
            TabPane left_tab_pane = new TabPane(left_tabs);
            VBox.setVgrow(left_tab_pane, Priority.ALWAYS);
            root.setLeft(left_tab_pane);
            
        }
        else
        {
            //aggiungo al root
            root.setLeft(vbox_left);
        }
        
        //INIZIALIZZO TIMER
        
        //listener del timer
        gameobserver.addNewListener(new MatchEventHandler(this));
        
        // reset delle variabili usate per tenere traccia di una partita
        this.turn_index = 0;
        this.turn_duration = ChooseParams.turn_duration_hack;
        this.match_start_time = System.currentTimeMillis();
        this.turn_start_time = System.currentTimeMillis();
        
        // aggiorna interfaccia con valori iniziali partita
        this.updateTurnCounter(this.turn_index);
        this.updateCurrentPlayerName(this.turn_index);
        this.updateElapsedTime(this.match_start_time);
        this.updateRemainingTime(this.turn_start_time);        
        
        // avvia i timer
        long update_period = 500;
        this.elapsed_time_updater = new PeriodicGUIUpdater(this::updateElapsedTime, update_period);
        this.elapsed_time_updater.start();
        
        if(this.turn_duration > 0)
        {
            this.remaining_time_updater = new PeriodicGUIUpdater(this::updateRemainingTime, update_period);
            this.remaining_time_updater.start();
        }
    }
    
    /**
     * Cambia la modalità di gioco da spettatore a giocabile da un umano,
     * nascondendo la barra destra dei comandi
     * @param b
     */
    public void changeSpectateMode(boolean b)
    {
        vbox_right.setVisible(!b);
    }
    
    /**
     * Aggiorna l'indice del turno nella GUI.
     * @param turn_index indice del nuiovo giocatore
     */
    private void updateTurnCounter(int turn_index)
    {
        String turn_number = Integer.toString(turn_index + 1);
        this.label_turn.setText(LABEL_PREFIX_TURN_COUNT + turn_number);
    }
    
    /**
     * Aggiorna il nome del giocatore nella GUI.
     * @param turn_index indice del nuiovo giocatore
     */
    private void updateCurrentPlayerName(int turn_index)
    {
        List<String> player_names = ScenesManager.instance.getallPlayerName();
        int player_index = turn_index % player_names.size();
        String player_name = player_names.get(player_index);
        this.label_player_turn.setText(LABEL_PREFIX_TURN_PLAYER + player_name);
    }
    
    /**
     * Aggiorna il tempo trascorso dall'inizio del gioco nella GUI.
     * @param current_time tempo attuale
     */
    private void updateElapsedTime(long current_time)
    {
        long elapsed_time = current_time - this.match_start_time;
        
        long elapsed_secs = elapsed_time / 1000;
        long elapsed_mins = elapsed_secs / 60;
        long elapsed_hours = elapsed_mins / 60;
        String elapsed_string = String.format("%02d:%02d:%02d", elapsed_hours, elapsed_mins % 60, elapsed_secs % 60);
        this.label_time.setText(LABEL_PREFIX_TIME_ELAPSED + elapsed_string);
    }
    
    /**
     * Aggiorna il tempo rimanente per la mossa nella GUI.
     * @param current_time tempo attuale
     */
    private void updateRemainingTime(long current_time)
    {
        if(this.turn_duration > 0)
        {
            long elapsed_time = current_time - this.turn_start_time;
            long remaining_time = this.turn_duration - elapsed_time;
            
            long remaining_secs = remaining_time / 1000;
            long remaining_mins = remaining_secs / 60;
            String elapsed_string = String.format("%02d:%02d", remaining_mins, remaining_secs % 60);
            this.label_remaintime.setText(LABEL_PREFIX_TIME_REMAINING + elapsed_string);
        }
        else
        {
            this.label_remaintime.setText(LABEL_PREFIX_TIME_REMAINING + "--:--");
        }
    }
    
    /**
     * 
     * Classe che implementa il listener usato dall'observer per controllare ogni partita.
     */
    private static class MatchEventHandler implements GameObserver.ObserverListener<PieceModel<Species>>
    {
        private final GameInterface gui;
        
        public MatchEventHandler(GameInterface gui)
        {
            this.gui = gui;
        }
        
        @Override
        public void moved(Move<PieceModel<Species>> m)
        {
            this.gui.turn_index++;
            this.gui.turn_start_time = System.currentTimeMillis();
            
            this.gui.updateTurnCounter(this.gui.turn_index);
            this.gui.updateCurrentPlayerName(this.gui.turn_index);
            this.gui.updateRemainingTime(this.gui.turn_start_time);
            
            if(this.gui.scores.get())
            {
                //this.gui.data.removeAll();
                
                //dati
                int counter = 0;
                double[] scores = this.gui.gameobserver.getScores();
                
                //immetto nuovi valori
                for(PlayerScore playername : this.gui.player_scores)
                {
                    playername.setPlayerScore(scores[counter]);
                    counter++;
                }
                
            }
        }
        
        @Override
        public void gameEnded()
        {
            if(this.gui.elapsed_time_updater != null)
            {
                this.gui.elapsed_time_updater.kill();
                this.gui.elapsed_time_updater = null;
            }
            
            if(this.gui.remaining_time_updater != null)
            {
                this.gui.remaining_time_updater.kill();
                this.gui.remaining_time_updater = null;
            }
        }
    }
    
    /**
     * 
     * Updater per la gestione del timer durante la partita
     */
    @SuppressWarnings("unused")
    private static class PeriodicGUIUpdater
    {
        private final Consumer<Long> callback;
        private final long period;
        private volatile boolean is_running;
        private volatile boolean is_shut_down;
        
        private Thread update_thread;
        
        public PeriodicGUIUpdater(Consumer<Long> callback, long period)
        {
            assert callback != null;
            if(period < 0)
                period = 0;
            
            this.callback = callback;
            this.period = period;
            
            this.update_thread = new Thread(this::asyncUpdate);
            this.update_thread.setName(this.getClass().getSimpleName());
            this.update_thread.setDaemon(true);
            this.update_thread.start();
        }
        
        /**
         * Fa partire il thread che calcola il tempo trascorso a partire dal momento
         * in cui questa funzione viene chiamata
         * Se e' stato chiamato shutDown, non fa nulla
         */
        public void start()
        {
            if(this.is_shut_down)
                return;
            this.is_running = true;
        }
        
        /**
         * Termina il thread contatore. Rende il timer inutilizzabile
         */
		public void pause()
        {
            if(this.is_shut_down)
                return;
            this.is_running = false;
        }
        
        /**
         * Uccide il timer.
         */
        public void kill()
        {
            this.is_running = false;
            this.is_shut_down = true;
        }
        
        /**
         * Esegue periodicamente, sul thread dell'interfaccia, la funzione
         * che aggiorna il timer
         */
        private void asyncUpdate()
        {
            try
            {
                while(!this.is_shut_down)
                {
                    if(this.is_running)
                    {
                        final long current_time = System.currentTimeMillis();
                        Platform.runLater(() -> this.callback.accept(current_time));
                    }
                    
                    Thread.sleep(this.period);
                }
            }
            catch(InterruptedException e){}
        }
    } 
}
