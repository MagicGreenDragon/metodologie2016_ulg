package gapp.gui.scenes;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import gapp.gui.util.ScenesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class WaitScreen extends Scene
{
    Text label_waiting;
    
    Button btn_stop;
    
    public WaitScreen(AtomicBoolean stopcompute, String waiting_text, Scene caller)
    {
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20));
        
        label_waiting = new Text(waiting_text);
        root.getChildren().add(label_waiting);
        
        if(Objects.nonNull(stopcompute))
        {
            btn_stop = new Button("Annulla computazione");
            
            btn_stop.setOnAction
            (
                    e ->
                    {
                        stopcompute.set(true);
                        ScenesManager.instance.changeSceneTo(caller);

                    }
            );
            
            root.getChildren().add(btn_stop);
        }
    }
}
