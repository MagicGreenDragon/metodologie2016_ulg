package gapp.gui.scenes;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import gapp.gui.util.ScenesManager;
import gapp.ulg.game.PlayerFactory.Play;
import gapp.ulg.game.util.Utils;
import gapp.ulg.games.GameFactories;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * 
 * Ultima modifica: Sera - 31/08/2016
 * @author Gabriele Cavallaro & Daniele Bondi'
 *
 */
public class ChooseParams extends Scene
{    
    //variabili per il timer
    static public long turn_duration_hack = -1L;
    static private final Map<String, Long> available_turn_durations_hack = new HashMap<>();
    
    static
    {
        available_turn_durations_hack.put("No limit", -1L);
        available_turn_durations_hack.put("1s", 1000L);
        available_turn_durations_hack.put("2s", 2000L);
        available_turn_durations_hack.put("3s", 3000L);
        available_turn_durations_hack.put("5s", 5000L);
        available_turn_durations_hack.put("10s", 10000L);
        available_turn_durations_hack.put("20s", 20000L);
        available_turn_durations_hack.put("30s", 30000L);
        available_turn_durations_hack.put("1m", 60000L);
        available_turn_durations_hack.put("2m", 120000L);
        available_turn_durations_hack.put("5m", 300000L);
    }
    
    //label
    private Text label_title;
    private Text label_suggest;
    private Text label_compute;
    
    //bottoni
    private Button btn_indietro;
    private Button btn_avanti;
    private Button btn_compute;
    private Button btn_salva;
    
    //contenitori
    private HBox box_btns;
    private VBox param_vertical_box;
    private ScrollPane param_vertical_scrollpane;
    private HBox compute_box;
    
    //oggetti variabili
    private String[] comboboxname;
    
    @SuppressWarnings("rawtypes")
    private ComboBox[] comboboxarray;
    
    //array delle liste contenenti i valori delle combobox
    private ObservableList<Object>[] comboboxarraylists;
    
    //altro
    private int playerindex;
    private String myfactory;
    private boolean managingonly;
    
    private ExecutorService compute_confinement_thread;
    
    private boolean autochaging;
    
    public ChooseParams(int playerindex, String in_name, boolean in_managingonly)
    {
        //inizializz root
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));
        
        //varialili da mantenere
        this.playerindex = playerindex;
        this.myfactory = in_name;
        this.managingonly = in_managingonly;
        
        autochaging = false;
                
        //inizializzo titolo
        if(playerindex < 1)
        {
            label_title = new Text("Parametri del Gioco "+myfactory);  
        }
        else
        {
            label_title = new Text("Parametri del Giocatore "+myfactory);
        }
        
        root.getChildren().add(label_title);
        
        //inizializzo label suggerimento
        label_suggest = new Text("Lasciare il mouse sopra la cella di un parametro per vedere la descrizione del parametro, quando un parametro viene cambiato,\n in alcuni casi, gli altri parametri possono cambiare,\n di conseguenza se non trovate dei valori desiderati, probabilmente dovete impostare in modo diverso gli altri parametri.");
        root.getChildren().add(label_suggest);
        
        //inizializzo pane dei parametri
        param_vertical_box = new VBox(10);
        param_vertical_box.setAlignment(Pos.CENTER);
        
        param_vertical_scrollpane = new ScrollPane();
        param_vertical_scrollpane.setFitToWidth(true);
        param_vertical_scrollpane.setFitToHeight(true);
        param_vertical_scrollpane.setContent(param_vertical_box);
        VBox.setVgrow(param_vertical_scrollpane, Priority.ALWAYS);
        param_vertical_scrollpane.setStyle("-fx-border-color: black"); 
        root.getChildren().add(param_vertical_scrollpane);
        
        //se possibile carico le combobox
        try
        {        
            loadComboBox();
        }
        catch(IllegalArgumentException iae) {}
        
        if(!managingonly)
        {            
            btn_indietro = new Button("Indietro");
            btn_indietro.setOnAction
            (e ->
                ScenesManager.instance.changescenetoGeneralParameters()
            );    
            
            btn_avanti = new Button("Avanti");
            btn_avanti.setOnAction
            (e -> 
                ScenesManager.instance.changescenetoChoosePlayers()
            );
            
            box_btns = new HBox(btn_indietro,btn_avanti);
            box_btns.setSpacing(30);
            box_btns.setAlignment(Pos.CENTER);
            root.getChildren().add(box_btns);
        }
        else 
        {
            if(playerindex >= 1)
            {
                List<String> game_to_compute = new ArrayList<>();
                
                for(String factory_name : GameFactories.availableBoardFactories())
                    if(ScenesManager.factoriesmanager.getIfCanPlay(factory_name, myfactory).equals(Play.TRY_COMPUTE))
                        game_to_compute.add(factory_name);
                
                btn_compute = new Button("Calcola una Strategia");
                if(!game_to_compute.isEmpty())
                {
                    btn_compute = new Button("Calcola una Strategia");
                    
                    btn_compute.setOnAction
                    (e ->
                        {
                            ChoiceDialog<String> dialog = new ChoiceDialog<>(game_to_compute.get(0), game_to_compute);
                            dialog.setTitle("Scegli un gioco..");
                            dialog.setHeaderText("Scegli un gioco da elaborare, i giochi verrano elaborati con le impostazioni correnti,\n se si vogliono calcolare nuove strategie per altre impostazioni è necessario tornare indietro e cambiare i loro parametri.");
                            dialog.setContentText("Giochi che necessitano di essere calcolati:");
        
                            Optional<String> result = dialog.showAndWait();
                            if (result.isPresent())
                            {
                                trycompute(result.get());
                            }
                        }
                    );     
                    
                    label_compute = new Text("Questo giocatore può prepararsi a giocare dei giochi: ");
                    
                    compute_box = new HBox(label_compute,btn_compute);
                    compute_box.setAlignment(Pos.CENTER);
                    compute_box.setSpacing(30);
                    
                    param_vertical_box.getChildren().add(compute_box);
                }
            }
            
            btn_salva = new Button("Salva e torna alle impostaizoni");
            btn_salva.setOnAction
            (e -> 
                ScenesManager.instance.changescenetoSettings()
            );
            
            btn_salva.setMaxWidth(Double.MAX_VALUE);
            root.getChildren().add(btn_salva);
        }

    }    
    
    /**
     * Imposta le combobox, questa funzione andrebbe chiamata solo la prima volta che la scena viene inizializzata,
     * ma richiamarla non causa problemi di funzionamento.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void loadComboBox()
    {
        int counter = 0;
        Object playgui_param, saved_param;
        
        comboboxarray      = new ComboBox[(int)ScenesManager.factoriesmanager.getFactoryParamCount(myfactory)];
        comboboxarraylists = new ObservableList[comboboxarray.length];
        comboboxname       = new String[comboboxarray.length];
        
        String[] paramnames = ScenesManager.factoriesmanager.getFactoryParams(myfactory);
        
        //scelgo tipo di factory da costruire, se maggiore di 1 allora è di tipo player
        for (String paramname : paramnames)
        {            
            comboboxname[counter] = paramname;
            Text tmp_label = new Text(paramname);
                            
            comboboxarraylists[counter] = FXCollections.observableArrayList(ScenesManager.factoriesmanager.getFactoryParamValues(myfactory,paramname));
            comboboxarray[counter] = new ComboBox(comboboxarraylists[counter]); 
            comboboxarray[counter].setPrefWidth(160);
            
            //caricamento impostazioni esistenti
            //---
            saved_param = ScenesManager.factoriesmanager.getFactoryParamValue(myfactory, paramname);
            if(!managingonly)
            {
                if(playerindex < 1)
                {
                    playgui_param = ScenesManager.playgui.getGameFactoryParamValue(paramname);
                    
                    if(!playgui_param.equals(saved_param))
                    {
                        ScenesManager.playgui.setGameFactoryParamValue(paramname, saved_param);
                    }
                }
                else
                {                    
                    playgui_param = ScenesManager.playgui.getPlayerFactoryParamValue(playerindex, paramname);
                    
                    if(!playgui_param.equals(saved_param))
                    {
                        ScenesManager.playgui.setPlayerFactoryParamValue(playerindex, paramname, saved_param); 
                    }                   
                }
            }
            //---
            
            comboboxarray[counter].getSelectionModel().select(saved_param);
            
            
            //questo intero serve alla funzione evento della combobox per riconosce quale parametro aggiornare
            final int index = counter;
            
            comboboxarray[counter].setOnAction
            (   
                e -> 
                {    
                    if(!autochaging)
                    {
                        //inserisco cambiamenti nelle impostazioni da salvare
                        ScenesManager.factoriesmanager.setFactoryParam(myfactory, paramname, comboboxarray[index].getValue());
                        
                        //inserisco cambiamenti nella costruzione del gioco
                        if(!managingonly)
                        {
                            if(playerindex < 1)
                            {
                                ScenesManager.playgui.setGameFactoryParamValue(paramname, comboboxarray[index].getValue());
                                
                                // HACK: salviamo la durata del turno per farla leggere alla gui
                                if(paramname.equals("Time"))
                                {
                                    String combo_value = (String)comboboxarray[index].getValue();
                                    Long turn_duration = ChooseParams.available_turn_durations_hack.get(combo_value);
                                    if(turn_duration != null)
                                    {
                                        ChooseParams.turn_duration_hack = turn_duration;
                                    }
                                }
                            }
                            else
                            {
                                ScenesManager.playgui.setPlayerFactoryParamValue(playerindex, paramname, comboboxarray[index].getValue());                   
                            }
                        }       
                        
                        updateComboBox(index);
                    }
                }
            );            
                
            Tooltip combobox_tooltip = new Tooltip();
            combobox_tooltip.setText(ScenesManager.factoriesmanager.getFactoryParamPrompt(myfactory, paramname));
            comboboxarray[counter].setTooltip(combobox_tooltip);
            
            HBox box = new HBox(tmp_label,comboboxarray[counter]);
            box.setAlignment(Pos.CENTER);
            box.setSpacing(30);
            
            param_vertical_box.getChildren().add(box);
            
            counter++;
        }
    }
    
    /**
     * Aggiorna il contenuto delle liste osservate dalle combobox,
     * aggiorna quindi, tutte le combobox esclusa quella indicata
     * nell'intero richiesto tra i parametri.
     * Se si vogliono aggiornare tutte le combobox basta inserire un valore negativo.
     * ATTEZIONE, se si aggiornano tutte le combobox e assieme si effettua un
     * getValue(), si incombe a un errore minore di indici,
     * nella maggior parte dei casi è comunque irrilevante tutte le chiamate vanno a buon fine.
     * 
     * @param ignore indice della combobox da ignorare
     */
    @SuppressWarnings("unchecked")
    private void updateComboBox(int ignore)
    {
        int counter = 0;

        autochaging = true;
        
        for (String paramname : comboboxname)
        {
            if(counter!=ignore)
            {
                comboboxarraylists[counter].setAll(ScenesManager.factoriesmanager.getFactoryParamValues(myfactory, paramname));
                comboboxarray[counter].getSelectionModel().select(ScenesManager.factoriesmanager.getFactoryParamValue(myfactory, paramname));
            }             
            counter++;
        }
        
        autochaging = false;
    }    
    
    private void trycompute(String game_factory_name)
    {
       Path folder_path = ScenesManager.factoriesmanager.getPlayerFactoryDir(myfactory);
       
       if(folder_path==null)
       {
           Alert folder_dialog = new Alert(AlertType.CONFIRMATION);
           folder_dialog.setTitle(myfactory+" - Opzioni");
           folder_dialog.setHeaderText("Questo giocatore non ha una cartella impostata, è necessario che il giocatore abbia una cartella per essere utilizzato, se ne vuole impostare una?");
           folder_dialog.setContentText("Se non si vuole impostare una cartella si userà il percorso da dove è stato lanciato il programma, se il programma non può scrivere in quella cartella, il risultato di questa operazione potrebbe fallire.\nPremere \"Annulla\" per non salvare la strategia sul file; il giocatore restera inutilizzabile.");

           ButtonType btn_choose = new ButtonType("Scegli Cartella");
           ButtonType btn_puthere = new ButtonType("Usa cartella dove è stato lanciato il programma");
           ButtonType btn_cancel = new ButtonType("Annulla", ButtonData.CANCEL_CLOSE);

           folder_dialog.getButtonTypes().setAll(btn_choose, btn_puthere, btn_cancel);

           Optional<ButtonType> result = folder_dialog.showAndWait();
                      
           if (result.get() == btn_choose)
           {
               folder_path = ScenesManager.instance.openscenetoDirectoryChooser();
           } 
           else if (result.get() == btn_puthere) 
           {
               folder_path = Paths.get(new File("").getAbsolutePath());
           }
           else
           {
               return;
           }           
           
           ScenesManager.factoriesmanager.setPlayerFactoryDir(myfactory, folder_path);
       }
       
       AtomicBoolean stopcompute = new AtomicBoolean(false);           
       Supplier<Boolean> supplier = () -> stopcompute.get();
       
       ScenesManager.instance.changescenetoWait(stopcompute, "Attendere mentre la strategia viene calcolata..", this);
       
       compute_confinement_thread = Executors.newSingleThreadExecutor(Utils.DAEMON_THREAD_FACTORY);
       
       compute_confinement_thread.execute
          (
              new Runnable() 
              {
                  public void run() 
                  {                      
                      String computation_result = 
                          ScenesManager.factoriesmanager.computeStrategy
                          (
                              myfactory,
                              game_factory_name,
                              ScenesManager.instance.parallel_trycompute,
                              supplier
                          );
                      
                      Platform.runLater(new Runnable()
                      {
                          @Override
                          public void run()
                          {               
                              if(Objects.nonNull(computation_result))
                              {
                                  Alert compute_fail_message = new Alert(AlertType.WARNING);
                                  compute_fail_message.setTitle("Calcolo Fallito!");
                                  compute_fail_message.setHeaderText("Qualcosa è andato storto..");
                                  compute_fail_message.setContentText("Il programma non è riuscito a calcolare la strategia per il seguente motivo: "+ computation_result);

                                  compute_fail_message.showAndWait();
                              }
                              ScenesManager.instance.changeSceneTo(ChooseParams.this);
                          }
                      });
                  }
              }
          );                
    }
}
