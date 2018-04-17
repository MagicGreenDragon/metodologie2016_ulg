package gapp.gui.scenes;

import gapp.gui.util.GameObserver;
import gapp.gui.util.ScenesManager;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.PieceModel.Species;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * 
 * Ultima modifica: Pomeriggio - uni - 06/09
 * 
 * @author Gabriele Cavallaro
 *
 */
public class ScoreBoard extends Scene
{
    //Label
    Label label_title;
    Label label_playerlist;
    Label label_boardresult;
    
    //Bottoni
    Button btn_again;
    Button btn_playother;
    Button btn_back_to_menu;
    
    //contenitori
    VBox root;
    HBox game_box;
    
    //altro
    ImageView board_iv;
    
    public ScoreBoard(GameObserver<PieceModel<Species>> go, Image board_img)
    {
        //TODO
        //DA RISISTEMARE!
        //imposto root
        super(new VBox(10));
        root = (VBox)this.getRoot();
        root.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(20, 20, 20, 20)); 
             
        if(!go.getWinnerName().equals(""))
            label_title = new Label("Vince il giocatore "+go.getWinnerIndex()+ ": "+go.getWinnerName()+"!");
        else
            label_title = new Label("Patta!");
        root.getChildren().add(label_title);
        
        if(go.gameHasScores())
        {
            //score
            double[] scores = go.getScores();
            
            ListView<String> player_list = new ListView<String>();
            for(int i=0;i<scores.length;i++)
                player_list.getItems().add("Il giocatore "+(i+1)+": "+go.getPlayerName(i)+" ha totalizzato "+((int)scores[i])+" punti.");
            
            //board                                
            board_iv = new ImageView(board_img);

            game_box = new HBox(player_list, board_iv);
            game_box.setAlignment(Pos.CENTER);
            root.getChildren().add(game_box);
        }
        else
        {
            //solo board
            board_iv = new ImageView(board_img);
            root.getChildren().add(board_iv);
        }
        
        board_iv.setFitHeight(board_img.getHeight()/2);
        board_iv.setFitWidth(board_img.getWidth()/2);
        board_iv.setPreserveRatio(true);
        
        btn_again        = new Button("Rigioca");
        btn_again.setOnAction(e -> ScenesManager.instance.resetplaygui());
        btn_again.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(btn_again);
        
        btn_playother    = new Button("Gioca un altro gioco");
        btn_playother.setOnAction(e -> ScenesManager.instance.changescenetoChooseGames());
        btn_playother.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(btn_playother);
        
        btn_back_to_menu = new Button("Torna al menu principale");
        btn_back_to_menu.setOnAction(e -> ScenesManager.instance.changescenetoMainMenu());  
        btn_back_to_menu.setMaxWidth(Double.MAX_VALUE);
        root.getChildren().add(btn_back_to_menu);
        
        
    }
}
