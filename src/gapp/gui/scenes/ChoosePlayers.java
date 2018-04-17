package gapp.gui.scenes;

import gapp.gui.util.GameObserver;
import gapp.gui.util.NameGenerator;
import gapp.gui.util.ScenesManager;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.PlayGUI;
import gapp.ulg.game.util.Utils;
import gapp.ulg.play.PlayerFactories;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * 
 * Ultima modifica: Pomeriggio - uni - 06/09
 * @author Gabriele Cavallaro & Daniele Bondi'
 *
 */
public class ChoosePlayers extends Scene
{
    //bottoni
    Button button_next;
    Button button_back;
    
    //contenitori
    VBox players_vertica_box;
    FlowPane unable_player_pane;    
    
    //altro
    public final String PROMPT_TEXT_STRING = "Scegli tipo giocatore..";
    public final String HUMAN_PLAYER_STRING = "Giocatore Umano";
    List<String> available_players;
    List<String> possibile_new_players;    
    private ExecutorService compute_confinement_thread;
    
    String game_factory;
    
    public class PlayerChooserBox extends HBox
    {
        //elementi
        public TextArea name_textarea;
        public ComboBox<String> combobox;
        
        //variabili
        public final int player_index;
        public ObservableList<String> observable_values;
        
        public PlayerChooserBox(int player_index, List<String> available_values, String selected_value)
        {
            super(10);
            this.player_index = player_index;
            
            this.name_textarea = new TextArea(NameGenerator.getRandomName());
            this.name_textarea.setPromptText("Giocatore " + Integer.toString(this.player_index));
            name_textarea.setPrefHeight(40);
            name_textarea.setPrefWidth(160);
            
            name_textarea.textProperty().addListener(new ChangeListener<String>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) 
                {
                    onChangingItem();
                }
            });
            
            ObservableList<String> observable_values = FXCollections.observableArrayList(available_values);
            this.combobox = new ComboBox<String>(observable_values);
            combobox.setPrefWidth(160);
            if(selected_value != "")
                combobox.getSelectionModel().select(selected_value);
            else
                combobox.setPromptText(PROMPT_TEXT_STRING);
            combobox.setOnAction(e -> onChangingItem());
            
            this.setAlignment(Pos.CENTER);
            this.getChildren().addAll(name_textarea, combobox);
        }
        
        private void onChangingItem()
        {
            updatePlayGui();
            updateButtonNextState();
        } 
        
        public void updatePlayGui()
        {
            if(!this.name_textarea.getText().equals("") && !Objects.isNull(combobox.getValue()))
            {    
                String factory_name = combobox.getValue();
                assert factory_name != null;
                        
                if(factory_name.equals(HUMAN_PLAYER_STRING))
                {            
                    ScenesManager.playgui.setPlayerGUI
                    (
                        this.player_index,
                        this.name_textarea.getText(),
                       ScenesManager.instance.getActaulMaster()
                    );
                }
                else
                {            
                    ScenesManager.playgui.setPlayerFactory
                    (
                            this.player_index,
                            factory_name,
                            this.name_textarea.getText(),
                            ScenesManager.factoriesmanager.getPlayerFactoryDir(factory_name)
                    );
                }
            }
        }
    }
    
    public class PossibilePlayerBox extends HBox
    {
        String player_factory_name;
        
        public PossibilePlayerBox(String player_factory_name)
        {
            super();
            
            this.player_factory_name = player_factory_name;
            
            Text tmp_label = new Text(this.player_factory_name);
            Button compute_btn = new Button("Prepara giocatore");
            compute_btn.setOnAction(e -> trycompute(tmp_label.getText()));
            
            this.getChildren().addAll(tmp_label, compute_btn);
            this.setAlignment(Pos.CENTER);
            this.setSpacing(30);
        }
    }
    
    public ChoosePlayers(String game_factory)
    {
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));  
        
        this.game_factory = game_factory;
        
        // header
        {
            Text header = new Text("Scegli giocatori");
            root.getChildren().add(header);
        }
        
        // player list
        {            
            players_vertica_box = new VBox(10);
            players_vertica_box.setAlignment(Pos.CENTER);
            
            Text title = new Text("Scegli nome e tipo dei giocatori di questa partita:");
            
            players_vertica_box.getChildren().add(title);
            
            //controllo giocatori che possono giocare
            available_players     = new ArrayList<String>();
            possibile_new_players = new ArrayList<String>();
            Map<String, PlayerFactory.Play> howcanplay = ScenesManager.factoriesmanager.getHowCanPlay(game_factory); 
            
            available_players.add(HUMAN_PLAYER_STRING);
            
            for(String player_factory_name : PlayerFactories.availableBoardFactories())
            {                
                if(howcanplay.get(player_factory_name).equals(PlayerFactory.Play.YES))
                {
                    available_players.add(player_factory_name);
                }
                else if (howcanplay.get(player_factory_name).equals(PlayerFactory.Play.TRY_COMPUTE))
                {
                    possibile_new_players.add(player_factory_name);
                }
            }         
            
            //aggiungo combobox per gli available_players            
            for(int i = 1; i < ScenesManager.factoriesmanager.getGameFactory_maxPlayers(game_factory)+1; i++)
            {                
                PlayerChooserBox entry = new PlayerChooserBox(i, available_players, "");
                players_vertica_box.getChildren().add(entry);
            }
            
            ScrollPane scrollpane = new ScrollPane();
            scrollpane.setFitToWidth(true);
            scrollpane.setFitToHeight(true);
            VBox.setVgrow(scrollpane, Priority.ALWAYS);
            scrollpane.setContent(players_vertica_box);
            scrollpane.setStyle("-fx-border-color: black"); 
            
            root.getChildren().add(scrollpane);
            
            //aggiungo un pannello per i possibile_new_players
            unable_player_pane = new FlowPane(Orientation.VERTICAL);
            unable_player_pane.setAlignment(Pos.TOP_CENTER);
            unable_player_pane.setHgap(1);        
            unable_player_pane.setVgap(possibile_new_players.size());
            unable_player_pane.setStyle("-fx-border-color: black"); 
            unable_player_pane.setPadding(new Insets(20, 20, 20, 20));
                        
            Text tmp_header = new Text("Questi giocatori non umani necessitano di una computazione per poter giocare a questo gioco:\n");            
            unable_player_pane.getChildren().add(tmp_header);
            
            for(String player_factory_name : possibile_new_players)
            {
                PossibilePlayerBox player_box = new PossibilePlayerBox(player_factory_name);
                unable_player_pane.getChildren().add(player_box);
            }
            
            root.getChildren().add(unable_player_pane);
        }
        
        // buttons
        {
            HBox box = new HBox(10);
            box.setAlignment(Pos.BOTTOM_CENTER);
            
            button_back = new Button("Indietro");
            button_back.setPrefWidth(80);
            button_back.setOnAction(this::onSelectedButtonBack);
            box.getChildren().add(button_back);
            
            button_next = new Button("Avanti");
            button_next.setPrefWidth(80);
            button_next.setOnAction(this::onSelectedButtonNext);
            box.getChildren().add(button_next);
            
            root.getChildren().add(box);
        }
        
        this.updateButtonNextState();
    }
    
    private void onSelectedButtonNext(ActionEvent event)
    {
        ScenesManager.instance.changescenetoChoosePlayersParams();
    }
    
    private void onSelectedButtonBack(ActionEvent event)
    {
        //resetto gioco
        //creo Observer e PlayGUI
        ScenesManager.instance.createNewGameAssemblyQueue();
    }
    
    public void updateButtonNextState()
    {
        boolean allcomboset = true;
        boolean allnotempty = true;
        
        //controllo combobox
        
        //contro textarea
        for(Node child : players_vertica_box.getChildren())
        {
            if(child instanceof PlayerChooserBox)
            {
                if(((PlayerChooserBox)child).name_textarea.getText().equals(""))
                    allnotempty = false;
                if(Objects.isNull(((PlayerChooserBox)child).combobox.getValue()))
                	allcomboset = false;
                ((PlayerChooserBox)child).updatePlayGui();
            }
        }        
            
        this.button_next.setDisable(!(allcomboset && allnotempty));
    }
    
    public void approveplayer(String player_factory_name)
    {
        if(ScenesManager.factoriesmanager.getIfCanPlay(game_factory, player_factory_name).equals(gapp.ulg.game.PlayerFactory.Play.YES))
        {
            for(Node children : unable_player_pane.getChildren())
            {
                if(children instanceof PossibilePlayerBox)
                {
                    if(((PossibilePlayerBox)children).player_factory_name.equals(player_factory_name))
                    {
                        children.setVisible(false);
                        break;
                    }
                }
            }
            
            for(Node children : players_vertica_box.getChildren())
            {
                if(children instanceof PlayerChooserBox)
                {
                    ((PlayerChooserBox) children).combobox.getItems().add(player_factory_name);
                }
            }
        }                
    }
    
    public List<String> getallPlayerName()
    {
        List<String> player_name = new ArrayList<String>();
        
        for(Node children : players_vertica_box.getChildren())
        {
            if(children instanceof PlayerChooserBox)
            {
                player_name.add((String) ((PlayerChooserBox)children).name_textarea.getText());
            }
        }
        
        return player_name;
    }
    
    public List<String> getallPlayerFactory()
    {
        List<String> player_factory = new ArrayList<String>();
        
        for(Node children : players_vertica_box.getChildren())
        {
            if(children instanceof PlayerChooserBox)
            {
                player_factory.add((String) ((PlayerChooserBox)children).combobox.getValue());
            }
        }
        
        return player_factory;
    }
    
    public boolean thereisHumans()
    {    	
        for(Node children : players_vertica_box.getChildren())
        {
            if(children instanceof PlayerChooserBox)
            {
                if(HUMAN_PLAYER_STRING.equals(((PlayerChooserBox)children).combobox.getValue()))
                	return true;
            }
        }
        
        return false;
    }
    
    public boolean isAllHumans()
    {    	
    	boolean allhuman = true;
    	
        for(Node children : players_vertica_box.getChildren())
        {
            if(children instanceof PlayerChooserBox)
            {
                if(!HUMAN_PLAYER_STRING.equals(((PlayerChooserBox)children).combobox.getValue()))
                	allhuman = false;
            }
        }
        
        return allhuman;
    }
    
    private void trycompute(String player_factory_name)
    {
       Path folder_path = ScenesManager.factoriesmanager.getPlayerFactoryDir(player_factory_name);
       
       if(folder_path==null)
       {
           Alert folder_dialog = new Alert(AlertType.CONFIRMATION);
           folder_dialog.setTitle(player_factory_name+" - Opzioni");
           folder_dialog.setHeaderText("Questo giocatore non ha una cartella impostata, è necessario che il giocatore abbia una cartella per essere utilizzato, se ne vuole impostare una?");
           folder_dialog.setContentText("Se non si vuole impostare una cartella si userà il percorso da dove è stato lanciato il programma, se il programma non può scrivere in quella cartella, il risultato di questa operazione potrebbe fallire.\nPremere \"Annulla\" per non salvare la strategia sul file; il giocatore restera inutilizzabile.");

           ButtonType btn_choose  = new ButtonType("Scegli Cartella");
           ButtonType btn_puthere = new ButtonType("Usa cartella dove è stato lanciato il programma");
           ButtonType btn_cancel  = new ButtonType("Annulla", ButtonData.CANCEL_CLOSE);

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
           
           ScenesManager.factoriesmanager.setPlayerFactoryDir(player_factory_name, folder_path);
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
                              player_factory_name,
                              ScenesManager.instance.getActualGameName(),
                              ScenesManager.instance.parallel_trycompute,
                              supplier
                          );
                       
                      Platform.runLater(new Runnable()
                      {
                          @Override
                          public void run()
                          {
                              if(Objects.isNull(computation_result))
                              {
                                  approveplayer(player_factory_name);
                              }
                              else
                              {
                                  Alert compute_fail_message = new Alert(AlertType.WARNING);
                                  compute_fail_message.setTitle("Calcolo Fallito!");
                                  compute_fail_message.setHeaderText("Qualcosa è andato storto..");
                                  compute_fail_message.setContentText("Il programma non è riuscito a calcolare la strategia per il seguente motivo: "+ computation_result);

                                  compute_fail_message.showAndWait();
                              }
                              
                              ScenesManager.instance.changescenetoChoosePlayers();
                          }
                      });
                  }
              }
          );                
    }
}
