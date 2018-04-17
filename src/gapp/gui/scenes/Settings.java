package gapp.gui.scenes;

import gapp.gui.util.ColorTheme;
import gapp.gui.util.ScenesManager;
import gapp.ulg.games.GameFactories;
import gapp.ulg.play.PlayerFactories;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

/**
 * Ultima modifica: Mattina - 02/03/2016
 *
 * @author Gabriele Cavallaro
 */
public class Settings extends Scene
{
    public final ColorTheme[] AVAILABLE_THEMES = {
        new ColorTheme("Othello Style", Color.DARKGREEN, Color.FORESTGREEN),
        new ColorTheme("Burlywood - White", Color.ANTIQUEWHITE, Color.BURLYWOOD),
        new ColorTheme("Indian Red - Red", Color.ANTIQUEWHITE, Color.INDIANRED),
        new ColorTheme("Darkseagreen - Green", Color.FLORALWHITE, Color.DARKSEAGREEN),
        new ColorTheme("Cornflower Blue - Blue", Color.ALICEBLUE, Color.CORNFLOWERBLUE),
    };
    
    static String scegli_colori_text = "Scegli colori intefaccia, attualmente selezionata: ";
    
    //text
    Text label_liststyle;
    
    //bottoni
    Button btn_indietro;
    
    //contenitori
    VBox root;
    VBox stylebox;
    VBox gamebox;
    VBox playerbox;
    HBox gameplayerbox;
    HBox advancebox;
    VBox box_blocktime;
    VBox box_nativecolor;
    
    public Settings()
    {        
        super(new VBox(50));
        
        root = (VBox)this.getRoot();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(30, 30, 30, 30));        
        
        //elementi visibili interfaccia
        //---
        //text:
        label_liststyle = new Text("Scegli colori intefaccia: (Cliccare due sulla mini-board volte per scegliere)");
        Text label_gamelist = new Text("Giochi Caricati:");
        Text label_infogame = new Text("(Clicca una game factory per cambiare le impostazioni)");
        Text label_playerlist = new Text("Giocatori Caricati");
        Text label_infoplayer = new Text("(Clicca un giocatore per cambiare la cartella delle strategie o per calcolarne una)");
        Text label_advanceboxinfo = new Text("Impostazioni avanzate");
        Text label_blocktimeinfo = new Text("Block time - durante la creazione della partita:\n");
        
        //list:
        FlowPane pane_style = new FlowPane(Orientation.HORIZONTAL, 10, 10);
        pane_style.setAlignment(Pos.CENTER);
        pane_style.setStyle("-fx-border-color: black");
        pane_style.getChildren().addAll(this.createThemePreviews());
        
        ListView<String> list_game = new ListView<>();
        list_game.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);//necessario?
        list_game.setItems(FXCollections.observableArrayList(GameFactories.availableBoardFactories()));
        list_game.setMaxHeight(24 * GameFactories.availableBoardFactories().length);
        list_game.setOnMouseClicked(e -> ScenesManager.instance.changescenetoChooseParams(0, list_game.getSelectionModel().getSelectedItem()));
        
        ListView<String> list_player = new ListView<>();
        list_player.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);//necessario?
        list_player.setItems(FXCollections.observableArrayList(PlayerFactories.availableBoardFactories()));
        list_player.setMaxHeight(24 * PlayerFactories.availableBoardFactories().length);
        list_player.setOnMouseClicked(e -> ScenesManager.instance.changescenetoChooseParams(1, list_player.getSelectionModel().getSelectedItem()));
        
        //checkbox
        CheckBox parallel_trycompute = new CheckBox("Esecuzione Parallela del calcolo delle strategie");
        parallel_trycompute.setOnAction(e -> ScenesManager.instance.parallel_trycompute = ((CheckBox)e.getSource()).isSelected());
        
        Tooltip tooltip = new Tooltip("Il programma disegna il tavolo da gioco alternando due colori, scegliendo questa opzione,\ninvece che disegnare le pedine alternado quei due colori, utilizza i colori che il gioco assegna ai giocatori.\nSe un gioco non utilizza colori primari puri, scritti in italiano o in inglese, allora ignora questa specifica.");
        CheckBox use_nativecolor = new CheckBox("Usa colori nativi dei giochi");
        use_nativecolor.setTooltip(tooltip);
        use_nativecolor.setOnAction(e -> ScenesManager.instance.nativecolor = ((CheckBox)e.getSource()).isSelected());
        
        //combobox
        ComboBox<String> combobox_tol = new ComboBox<String>();   
        combobox_tol.getItems().addAll("No limit","1s","2s","3s","5s","10s","20s","30s","1m","2m","5m");
        combobox_tol.getSelectionModel().select(0);
        combobox_tol.setOnAction(e -> ScenesManager.instance.maxBlockTime = getTimeParamFromString(String.valueOf(((ComboBox)e.getSource()).getValue())));
        
        //button:
        btn_indietro = new Button("Indietro");
        btn_indietro.setMaxWidth(Double.MAX_VALUE);
        btn_indietro.setOnAction(e -> ScenesManager.instance.changescenetoMainMenu());
        //---
        
        //contenitori
        //---
        //style (box in alto/top)
        stylebox = new VBox();
        stylebox.getChildren().addAll(label_liststyle, pane_style);
        VBox.setVgrow(pane_style, Priority.ALWAYS);
        VBox.setVgrow(stylebox, Priority.ALWAYS);
        root.getChildren().add(stylebox);
        
        //giochi
        gamebox = new VBox();
        gamebox.getChildren().addAll(label_gamelist, label_infogame, list_game);
        VBox.setVgrow(list_game, Priority.ALWAYS);        
        
        //player
        playerbox = new VBox();
        playerbox.getChildren().addAll(label_playerlist, label_infoplayer, list_player, parallel_trycompute);
        VBox.setVgrow(list_player, Priority.ALWAYS);
        
        //box centrale
        gameplayerbox = new HBox(50);
        gameplayerbox.setAlignment(Pos.CENTER);
        gameplayerbox.setFillHeight(true);
        gameplayerbox.getChildren().addAll(gamebox, playerbox);
        HBox.setHgrow(gamebox, Priority.ALWAYS);
        HBox.setHgrow(playerbox, Priority.ALWAYS);
        VBox.setVgrow(gameplayerbox, Priority.ALWAYS);
        root.getChildren().add(gameplayerbox);
                
        //box bassa/bottom
        root.getChildren().add(label_advanceboxinfo);
        
        box_blocktime = new VBox(label_blocktimeinfo,combobox_tol);
        box_blocktime.setAlignment(Pos.CENTER);
        
        box_nativecolor = new VBox(use_nativecolor);
        box_nativecolor.setAlignment(Pos.CENTER);
        
        advancebox = new HBox(50);
        advancebox.setAlignment(Pos.CENTER);
        advancebox.setFillHeight(true);        
        advancebox.getChildren().addAll(box_blocktime, box_nativecolor);
        root.getChildren().add(advancebox);
        
        //aggiungo bottone alla fine
        VBox.setVgrow(btn_indietro, Priority.ALWAYS);
        root.getChildren().add(btn_indietro);
    }
    
    private ImageView[] createThemePreviews()
    {
        ImageView[] result = new ImageView[AVAILABLE_THEMES.length];
        for(int i = 0; i < result.length; ++i)
        {
            result[i] = new BoardPreview(AVAILABLE_THEMES[i]);
            result[i].setOnMouseClicked(this::onSelectedThemePreview);
        }
        return result;
    }
    
    private void onSelectedThemePreview(MouseEvent event)
    {
        if(event.getClickCount() < 2)
            return;
        
        BoardPreview preview = (BoardPreview)event.getSource();
        ScenesManager.instance.boardcolor.firstcolor = preview.theme.color_primary;
        ScenesManager.instance.boardcolor.secondcolor = preview.theme.color_secondary;
        
        label_liststyle.setText(scegli_colori_text+preview.theme.name.toUpperCase());
    }
    
    private static class BoardPreview extends ImageView
    {
        public final ColorTheme theme;
        
        
        public BoardPreview(ColorTheme theme)
        {
            this.theme = theme;
            
            double size = 240;
            Canvas canvas = new Canvas(size, size);
            GraphicsContext context = canvas.getGraphicsContext2D();
            context.setLineWidth(1);
            context.setStroke(new Color(1.0, 1.0, 1.0, 1.0));
    
            
            int cell_count = 2;
            double cell_size = size / cell_count;
            
            // colore con cui si inizia a disegnare la riga corrente
            Color curr_row_color = theme.color_primary;
            // colore con cui si inizia a disegnare la prossima riga
            Color next_row_color = theme.color_secondary;
            
            for(int row = 0; row < cell_count; row++)
            {
                // colore della cella disegnata nella colonna corrente
                Color curr_color = curr_row_color;
                // colore della cella disegnata nella prossima colonna
                Color next_color = next_row_color;
                
                for(int col = 0; col < cell_count; col++)
                {
                    context.setFill(curr_color);
                    context.fillRect(col * cell_size, row * cell_size, cell_size, cell_size);
                    context.strokeRoundRect(col * cell_size, row * cell_size, cell_size, cell_size, 10, 10);
            
                    // swappa i colori per disegnare la prossima cella
                    Color prev_color = curr_color;
                    curr_color = next_color;
                    next_color = prev_color;
                }
    
                // swappa i colori con cui si iniziera' a disegnare la prossima riga
                Color prev_row_color = curr_row_color;
                curr_row_color = next_row_color;
                next_row_color = prev_row_color;
            }
    
            WritableImage image = new WritableImage((int)size, (int)size);
            canvas.snapshot(null, image);
            this.setImage(image);
        }
    }
    
    private long getTimeParamFromString(String timeparam)
    {
        String value = String.valueOf(timeparam);
        
        if(value.equals("No limit"))
            return -1;
        
        long t = Long.parseLong(value.substring(0, value.length()-1));
        
        return (value.substring(value.length()-1).equals("s") ? t : t*60)*1000;
    }
}
