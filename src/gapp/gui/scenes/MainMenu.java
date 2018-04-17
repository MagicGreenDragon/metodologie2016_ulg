package gapp.gui.scenes;

import gapp.gui.util.ScenesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * 
 * Scena principale dell'interfaccia,
 * questa scena è il menu principale dell'interfaccia,
 * deve essere la prima scena a prensentarsi all'apertura dell'applicazione
 * 
 * 
 * Ultima modifica: Mattina - 02/03/2016
 * @author Gabriele Cavallaro & Daniele Bondi'
 *
 */
public class MainMenu extends Scene
{ 
    //label
    Text title;
    
    //button
    Button btn_gioca;
    Button btn_impostazioni;
    Button btn_crediti;
    Button btn_esci;
    
    /**
     * Crea la {@link Scene} che rappresenta il menu principale dell'interfaccia
     */
    public MainMenu()
    {
        super(new VBox(10));  
        
        VBox root = (VBox)this.getRoot();
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));   
        
        //elementi visibili interfaccia 
        //--- 
        //label:
        title = new Text("Universal Library Game - GUI");  
        root.getChildren().add(title);
        
        //button:
        btn_gioca = new Button("Gioca");  
        btn_gioca.setMaxWidth(Double.MAX_VALUE);  
        btn_gioca.setOnAction(e -> ScenesManager.instance.changescenetoChooseGames());
        root.getChildren().add(btn_gioca);
                
        btn_impostazioni = new Button("Impostazioni"); 
        btn_impostazioni.setMaxWidth(Double.MAX_VALUE);
        btn_impostazioni.setOnAction(e -> ScenesManager.instance.changescenetoSettings());
        root.getChildren().add(btn_impostazioni);
               
        btn_crediti = new Button("Crediti"); 
        btn_crediti.setMaxWidth(Double.MAX_VALUE);
        btn_crediti.setOnAction(e -> ScenesManager.instance.changescenetoCredits());
        root.getChildren().add(btn_crediti);
        
        btn_esci = new Button("Esci");
        btn_esci.setMaxWidth(Double.MAX_VALUE);    
        btn_esci.setOnAction(e -> ScenesManager.instance.closeWindow());  
        root.getChildren().add(btn_esci);
        //---  
            
    } 
}
