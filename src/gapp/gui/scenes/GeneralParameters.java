package gapp.gui.scenes;

import gapp.gui.util.ScenesManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

/**
 * 
 * 
 * Ultima modifica: Pomeriggio - 30/08/2016
 * @author Gabriele Cavallaro
 *
 */
public class GeneralParameters extends Scene
{    
    //label
    Text label_playerparam;
    Text label_tol;
    Text label_timedout;
    Text label_notplayerparam;
    Text label_mintime;
    Text label_maxthr;
    Text label_fjp;
    Text label_bgnthr;
    
    //combobox
    ComboBox<Integer> combobox_notumnplyr;
    ComboBox<String> combobox_tol;
    ComboBox<String> combobox_timedout;
    ComboBox<String> combobox_mintime;
    ComboBox<Integer> combobox_maxthr;    
    ComboBox<Integer> combobox_fjp;
    ComboBox<Integer> combobox_bgnthr;
    
    //bottoni
    Button btn_avanti;
    Button btn_indietro;
    
    //contenitori
    HBox box_tol;
    HBox box_timedout;
    HBox box_mintime;
    HBox box_maxthr;
    HBox box_fjp;
    HBox box_bgnthr;
    HBox box_btns;
    
    public GeneralParameters()
    {
        super(new VBox(10));
        VBox root = (VBox)this.getRoot();
        root.setFillWidth(true);
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20)); 
        
        //qui sono dichiarate tutte le combobox della scena nel seguente modo:
        //1)dichiarazione
        //2)aggiunta valori
        //3)valore base
        //4)listener
        //5)inserimento in un gruppo di contenimento
        
        label_playerparam = new Text("Imposta parametri per i giocatori:");
        
        //tolleranza
        label_tol = new Text("Scegli il tempo di tolleranza: ");
        combobox_tol = new ComboBox<String>();   
        combobox_tol.getItems().addAll("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
        combobox_tol.getSelectionModel().select(0);
        box_tol = new HBox(label_tol,combobox_tol);
        box_tol.setAlignment(Pos.CENTER);
        
        //timeout
        label_timedout = new Text("Scegli il tempo di timeout: ");
        combobox_timedout = new ComboBox<String>();
        combobox_timedout.getItems().addAll("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
        combobox_timedout.getSelectionModel().select(0);
        box_timedout = new HBox(label_timedout,combobox_timedout);
        box_timedout.setAlignment(Pos.CENTER);
        
        label_notplayerparam = new Text("Imposta parametri per i non giocatori:");
        
        //mintime
        label_mintime = new Text("Scegli il tempo Minimo tra le mosse: ");
        combobox_mintime = new ComboBox<String>();  
        combobox_mintime.getItems().addAll("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
        combobox_mintime.getSelectionModel().select(0);
        box_mintime = new HBox(label_mintime,combobox_mintime);
        box_mintime.setAlignment(Pos.CENTER);
        
        //maxthr
        label_maxthr = new Text("Scegli il Massimo numero di Thread: ");
        combobox_maxthr = new ComboBox<Integer>();   
        for (int i=0; i <= Runtime.getRuntime().availableProcessors()*2;i++)
            combobox_maxthr.getItems().add(i);
        combobox_maxthr.getSelectionModel().select(0);
        box_maxthr = new HBox(label_maxthr,combobox_maxthr);
        box_maxthr.setAlignment(Pos.CENTER);
        
        //fjp
        label_fjp = new Text("Scegli il numero di Thread (FJP): ");
        combobox_fjp = new ComboBox<Integer>();  
        for (int i=0; i <= Runtime.getRuntime().availableProcessors()*2;i++)
            combobox_fjp.getItems().add(i);
        combobox_fjp.getSelectionModel().select(0);
        box_fjp = new HBox(label_fjp,combobox_fjp);
        box_fjp.setAlignment(Pos.CENTER);
        
        //bgnthr
        label_bgnthr = new Text("Scegli il numero di Thread in Background: ");
        combobox_bgnthr = new ComboBox<Integer>();  
        for (int i=0; i <= Runtime.getRuntime().availableProcessors()*2;i++)
            combobox_bgnthr.getItems().add(i);
        combobox_bgnthr.getSelectionModel().select(0);
        box_bgnthr = new HBox(label_bgnthr,combobox_bgnthr);
        box_bgnthr.setAlignment(Pos.CENTER);
             
        //inizializzo bottoni
        
        //indieto
        btn_indietro = new Button("Indietro");
        btn_indietro.setOnAction(e -> ScenesManager.instance.changescenetoChooseGames());
        btn_indietro.setMaxWidth(Double.MAX_VALUE);
        
        //avanti
        btn_avanti = new Button("Avanti");
        btn_avanti.setMaxWidth(Double.MAX_VALUE);
        btn_avanti.setOnAction
        (e -> 
            ScenesManager.instance.createNewGameAssemblyQueue()
        );
        
        //box bottoni
        box_btns = new HBox(btn_indietro,btn_avanti);
        box_btns.setMaxWidth(Double.MAX_VALUE);
        box_btns.setSpacing(30);
        box_btns.setAlignment(Pos.CENTER);
        
        //aggiungo tutti gli elementi inizializzati al root
        root.getChildren().addAll(label_playerparam,box_tol,box_timedout,label_notplayerparam,box_mintime,box_maxthr,box_fjp,box_bgnthr,box_btns);
    }
    
    private long getTimeParamFromString(String timeparam)
    {
        String value = String.valueOf(timeparam);
        
        if(value.equals("No limit"))
            return -1;
        
        long t = Long.parseLong(value.substring(0, value.length()-1));
        
        return (value.substring(value.length()-1).equals("s") ? t : t*60)*1000;
    }
    
    public long getTolerance()
    {
        return getTimeParamFromString(combobox_tol.getValue());
    }
    
    public long getTimeout()
    {
        return getTimeParamFromString(combobox_timedout.getValue());
    }
    
    public long getMinTime()
    {
        return getTimeParamFromString(combobox_mintime.getValue());
    }
    
    public int getMaxThreads()
    {
        return combobox_maxthr.getValue();
    }
    
    public int getForkjoinpoolSize()
    {
        return combobox_fjp.getValue();
    }
    
    public int getBackgroundThreadsSize()
    {
        return combobox_bgnthr.getValue();
    }
}
