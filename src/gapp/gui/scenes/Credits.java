package gapp.gui.scenes;

import gapp.gui.util.ScenesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class Credits extends Scene
{
    public Credits()
    {
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setSpacing(30);
        root.setPadding(new Insets(20, 20, 20, 20));
    
        Text text = new Text("Daniele Bondi'");
        root.getChildren().add(text);
    
        text = new Text("Daniele Giudice");
        root.getChildren().add(text);
    
        text = new Text("Gabriele Cavallaro");
        root.getChildren().add(text);
        
        Button button_back = new Button("Indietro");
        button_back.setPrefWidth(120);
        button_back.setOnAction(event -> ScenesManager.instance.instance.changescenetoMainMenu());
        root.getChildren().add(button_back);
    }
}
