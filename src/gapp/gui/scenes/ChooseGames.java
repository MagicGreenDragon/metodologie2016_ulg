package gapp.gui.scenes;

import gapp.gui.util.ScenesManager;
import gapp.ulg.games.GameFactories;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;

/**
 * Questa scena permette l'utente di selezionare un gioco per iniziare a preparare la partita
 * 
 * Ultima modifica: Mattina - 02/03/2016
 * @author Gabriele Cavallaro
 *
 */
public class ChooseGames extends Scene
{
    //label
    Text label_header_gameflow;
        
    //bottoni
    Button btn_indietro;
    
    //contenitori
    FlowPane gameflow;
    
    //elementi temporanei
    Rectangle tmp_rtg;
    Text tmp_txt;
    StackPane tmp_stackpane;
    
    /**
     * Costurisce la {@link Scene} di tipo ChooseGames
     */
    public ChooseGames()
    {
        //imposto root
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));  
        
        //elementi visibili interfaccia 
        //--- 
        //inizializzo text:
        label_header_gameflow = new Text("Scegli un gioco (un click su un'icona per scegliere il gioco)");    
        root.getChildren().add(label_header_gameflow);        
        
        //inizializzo contenitore per i giochi
        gameflow = new FlowPane();  
        int max_game_row = (int)(ScenesManager.instance.getWindowWidth()-110)/110;
        gameflow.setVgap(max_game_row);
        gameflow.setHgap(1+GameFactories.availableBoardFactories().length/max_game_row);
        gameflow.setPadding(new Insets(10, 10, 10, 10));  
        gameflow.setStyle("-fx-border-color: black"); 
        
        //inserisco i giochi             
        for(String game_name: GameFactories.availableBoardFactories())
        {            
            tmp_rtg = new Rectangle(100,100);
            tmp_rtg.setFill(Color.TRANSPARENT);
            tmp_rtg.setStroke(Color.BLACK);
            tmp_txt = new Text(game_name.substring(0, 1).toUpperCase() + game_name.substring(1));  
            tmp_stackpane = new StackPane();
            tmp_stackpane.setPadding(new Insets(10, 10, 10, 10));  
            tmp_stackpane.getChildren().addAll(tmp_rtg, tmp_txt);
            tmp_stackpane.setOnMouseClicked
            (
                e ->
                {
                    ScenesManager.instance.changescenetoGeneralParameters();
                    ScenesManager.instance.setActualGameName(game_name);
                }  
            );
            gameflow.getChildren().addAll(tmp_stackpane);
        }
        
        root.getChildren().add(gameflow);
        
        //inizializzo bottoni:
        Button btn_indietro = new Button("Torna al Menu Principale");
        btn_indietro.setMaxWidth(Double.MAX_VALUE);       
        btn_indietro.setOnAction(e -> ScenesManager.instance.changescenetoMainMenu());  
        root.getChildren().add(btn_indietro);
        //---         
        
        VBox.setVgrow(gameflow, Priority.ALWAYS); 
    }
    
}
