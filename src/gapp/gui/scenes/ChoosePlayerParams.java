package gapp.gui.scenes;

import java.util.List;

import gapp.gui.util.ScenesManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * Classe che permette di scegliere i parametri per i vari giocatori.
 * 
 * Ultima modifica: Pomeriggio - uni - 06/09
 * @author Gabriele Cavallaro & Daniele Bondi'
 *
 */
public class ChoosePlayerParams extends Scene
{    
    Button button_next;
    Button button_back;
    
    /**
     * Classe privata della classe {@link ChoosePlayerParams},
     * il suo scopo è quello di gestire le combobox con degli indici propri per ognuna.
     * 
     */
    private class ComboBoxParam extends ComboBox<Object>
    {
        public final int player_index;
        public final String factory_name;
        public final String param_name;
        
        public ComboBoxParam(int player_index, String factory_name, String param_name, ObservableList<Object> items)
        {
            super(items);
            this.player_index = player_index;
            this.factory_name = factory_name;
            this.param_name = param_name;
        }
    }
    
    /**
     * Metodo costruttore della classe {@link ChoosePlayerParams},
     * prepara la scena a essere utilizzata.
     * Le tab di questa scena potrebbero essere tutte vuote se non ci sono parametri da scegliere
     */
    public ChoosePlayerParams()
    {
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));
    
        // header
        {
            Text header = new Text("Lasciare il mouse sopra la cella di un parametro per vedere la descrizione del parametro. (Se una tab è vuota il giocatore non ha parametri)");
            root.getChildren().add(header);
        }
        
        int playerfactoriescount = ScenesManager.factoriesmanager.getGameFactory_maxPlayers(ScenesManager.instance.getActualGameName());
        
        // tabs
        {
            Tab[] tabs = new Tab[playerfactoriescount];
            List<String> factory_name = ScenesManager.instance.getallPlayerFactory();
    
            for(int i = 0; i < playerfactoriescount; ++i)
            {
                tabs[i] = new Tab("Giocatore " + Integer.toString(i + 1)+ " - "+factory_name.get(i));
                
                if(ScenesManager.playgui.getPlayerFactoryParams(i+1).length != 0)
                {
                    VBox content = this.createTabContent(i+1, factory_name.get(i));
        
                    tabs[i].setClosable(false);
                    tabs[i].setContent(content);
                }
                else
                {
                    Text label = new Text("Non ci sono parametri per questo giocatore");
                    
                    HBox content = new HBox(10, label);
                    content.setAlignment(Pos.CENTER);
                    
                    tabs[i].setClosable(false);
                    tabs[i].setDisable(true);
                    tabs[i].setContent(content);
                }
            }
            
            TabPane tab_pane = new TabPane(tabs);
            VBox.setVgrow(tab_pane, Priority.ALWAYS);
            root.getChildren().add(tab_pane);
        }
    
    
        // buttons
        {
            HBox box = new HBox(10);
            box.setAlignment(Pos.BOTTOM_CENTER);
        
            button_back = new Button("Indietro");
            button_back.setPrefWidth(80);
            button_back.setOnAction(this::onSelectedButtonBack);
            box.getChildren().add(button_back);
            
            button_next = new Button("Gioca");
            button_next.setPrefWidth(80);
            button_next.setOnAction(this::onSelectedButtonNext);
            
            final Tooltip button_next_tooltip = new Tooltip();
            button_next_tooltip.setText("ATTENZIONE: durante la parita la board non può essere ridimenzionata,\nse si vuole rimensiore la board, modificare ora la dimesione della finestra.");
            button_next.setTooltip(button_next_tooltip);
            
            box.getChildren().add(button_next);
        
            root.getChildren().add(box);
        }
    }
    
    private void onSelectedButtonNext(ActionEvent event)
    {
        ScenesManager.instance.play();
    }
    
    private void onSelectedButtonBack(ActionEvent event)
    {
        ScenesManager.instance.changescenetoChoosePlayers();
    }
    
    private void onSelectedComboItem(ActionEvent event)
    {
        ComboBoxParam sender = (ComboBoxParam)event.getSource();
        Object value = sender.getSelectionModel().getSelectedItem();
        
        //imposto i valori inseriti
        ScenesManager.playgui.setPlayerFactoryParamValue(sender.player_index, sender.param_name, value);
        //aggiorno le impostazioni salvate
        ScenesManager.factoriesmanager.setFactoryParam(sender.factory_name, sender.param_name, value);
    }
    
    private VBox createTabContent(int player_index, String factory_name)
    {
        VBox tab = new VBox(10);
        tab.setFillWidth(true);
        tab.setAlignment(Pos.CENTER);
        
        for(String param_name : ScenesManager.factoriesmanager.getFactoryParams(factory_name))
        {
            Text label = new Text(param_name);
        
            Object[] available_values = ScenesManager.factoriesmanager.getFactoryParamValues(factory_name, param_name);
            ObservableList<Object> observable_list = FXCollections.observableArrayList(available_values);
            ComboBoxParam combo = new ComboBoxParam(player_index, factory_name, param_name, observable_list);
            combo.getSelectionModel().select(ScenesManager.factoriesmanager.getFactoryParamValue(factory_name, param_name));            
            combo.setOnAction(this::onSelectedComboItem);
            
            Tooltip combobox_tooltip = new Tooltip();
            combobox_tooltip.setText(ScenesManager.factoriesmanager.getFactoryParamPrompt(factory_name, param_name));
            combo.setTooltip(combobox_tooltip);
            
            if(
                     ScenesManager.playgui.getPlayerFactoryParamValue(player_index, param_name)
                  .equals
                     (ScenesManager.factoriesmanager.getFactoryParamValue(factory_name, param_name))
              )
                ScenesManager.playgui.setPlayerFactoryParamValue(player_index, param_name, ScenesManager.factoriesmanager.getFactoryParamValue(factory_name, param_name));
            
        
            HBox entry = new HBox(10, label, combo);
            entry.setAlignment(Pos.CENTER);
            tab.getChildren().add(entry);
        }
        
        return tab;
    }
}
