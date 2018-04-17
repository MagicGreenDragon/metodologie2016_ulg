package gapp.ulg.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import gapp.gui.board.GameBoard2D;
import gapp.gui.board.GameBoard2D.BoardListener;
import gapp.ulg.game.board.Pos;
import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class TestGameBoard2D extends Application
{     
    ComboBox<String> cmb_one;
    ComboBox<String> cmb_two;
    ComboBox<String> cmb_piece;
    Label label_x;
    Label label_y;
    Label label_clmrow;
    Label label_realclmrow;
    
    @Override
    public void start(Stage stage) throws Exception
    {
        //inizializzazione
        final Group rootGroup = new Group();
        final Scene scene = new Scene(rootGroup, 1050, 850, Color.GHOSTWHITE);
        stage.setMinWidth(1050);
        stage.setMinHeight(850);        
        stage.setScene(scene);
        stage.setTitle("CanvasGameBoard");
        stage.show(); 
        
        //test posizioni 
        List<Pos> poss = new ArrayList<Pos>();
        for( int i=0 ; i<8 ; i++ )
            for( int j=0 ; j<8 ; j++ )
            {
                Pos p = new Pos(i, j);
                poss.add(p);
            }
        
        poss.remove(new Pos(0,0));
        poss.remove(new Pos(0,7));
        poss.remove(new Pos(7,0));
        poss.remove(new Pos(7,7));
         
        
        //crezione board  
        
        //combinazione Othello QUESTA COLORAZIONE DA PROBLEMI
        //ATTENDERE AGGIORNAMENTO DELLA BOARD SULLE COLORAZIONI, WIP
        //GameBoard2D board = new GameBoard2D(800,800,8,8,Color.DARKGREEN   , Color.FORESTGREEN);
        
        //combinazione Casual  
        //GameBoard2D board = new GameBoard2D(800,800,8,8,Color.ANTIQUEWHITE, Color.BURLYWOOD);
        
        //combinazione Red     
        GameBoard2D board = new GameBoard2D(800,800,4,4,Color.ANTIQUEWHITE, Color.INDIANRED);
        
        //combinazione Green   
        //GameBoard2D board = new GameBoard2D(800,800,8,8,Color.FLORALWHITE , Color.DARKSEAGREEN);
        
        //combinazione Blue    
        //GameBoard2D board = new GameBoard2D(800,800,8,8,Color.ALICEBLUE   , Color.CORNFLOWERBLUE);
        
        class MyListener implements BoardListener
        {

            @Override
            public void setOnCellClick(MouseEvent e, int cell_x, int cell_y) 
            {                
                label_x.setText("cordinata x: "+String.valueOf(e.getX()));
                label_y.setText("cordinata y: "+String.valueOf(e.getY()));
                label_clmrow.setText("clm/x "+cell_x+", row/y "+cell_y);
                label_realclmrow.setText("clm/b "+cell_x+", row/t "+board.getYtoTInx(cell_y));
            }

            @Override
            public void setOnPrimaryButtonDown(MouseEvent e, int cell_x, int cell_y) 
            {   
                if(cmb_one.getValue()=="Focus")
                    board.focusCell(cell_x, cell_y);
                if(cmb_one.getValue()=="Defocus")
                    board.defocusCell(cell_x, cell_y);
                if(cmb_one.getValue()=="DecofusAll")
                    board.defocusAllCell();
                if(cmb_one.getValue()=="PlacePiece")
                    board.putPiece(cmb_piece.getValue(), cell_x, board.getYtoTInx(cell_y));
                if(cmb_one.getValue()=="RemovePiece")
                    board.removePiece(cell_x, board.getYtoTInx(cell_y));
            }

            @Override
            public void setOnSecondaryButtonDown(MouseEvent e, int cell_x, int cell_y) 
            {
                if(cmb_two.getValue()=="Focus")
                    board.focusCell(cell_x, cell_y);
                if(cmb_two.getValue()=="Defocus")
                    board.defocusCell(cell_x, cell_y);
                if(cmb_two.getValue()=="DecofusAll")
                    board.defocusAllCell();
                if(cmb_two.getValue()=="PlacePiece")
                    board.putPiece(String.valueOf(cmb_piece.getValue()), cell_x, board.getYtoTInx(cell_y));
                if(cmb_two.getValue()=="RemovePiece")
                    board.removePiece(cell_x, board.getYtoTInx(cell_y));
            }
            
        }
        
        board.addNewListener(new MyListener());
        
        //altra roba
        BorderPane borderpane = new BorderPane();        
        
        borderpane.setCenter(board);
        borderpane.setPrefSize(1000, 800);   
        
        borderpane.prefHeightProperty().bind(scene.heightProperty());
        borderpane.prefWidthProperty().bind(scene.widthProperty());      
        
        //test color
        
        ColorPicker colorPicker1 = new ColorPicker();    
        ColorPicker colorPicker2 = new ColorPicker();    
        
        colorPicker1.setOnAction(e -> board.changeFistColor(colorPicker1.getValue()));        
        colorPicker2.setOnAction(e -> board.changeSecondColor(colorPicker2.getValue()));
        
        //test label
        
        label_x = new Label("");
        label_y = new Label("");
        label_clmrow = new Label("");
        label_realclmrow = new Label("");
        
        //test modalità
        
        cmb_one = new ComboBox<String>(); 
        cmb_one.getItems().addAll("Focus","Defocus","DecofusAll","PlacePiece","RemovePiece","Niente");
        cmb_one.getSelectionModel().select(0);
        
        cmb_two = new ComboBox<String>(); 
        cmb_two.getItems().addAll("Focus","Defocus","DecofusAll","PlacePiece","RemovePiece","Niente");
        cmb_two.getSelectionModel().select(1);
        
        cmb_piece = new ComboBox<String>(); 
        cmb_piece.getItems().addAll("???","DISC", "DAMA", "PAWN", "KNIGHT", "BISHOP", "ROOK", "QUEEN", "KING");
        cmb_piece.getSelectionModel().select(0);
        
        VBox vbox = new VBox(label_x, label_y, label_clmrow, label_realclmrow, colorPicker1, colorPicker2, cmb_one, cmb_two, cmb_piece);
        vbox.setSpacing(20);
        
        borderpane.setRight(vbox);
        
        rootGroup.getChildren().add(borderpane);        
    }
    
    public static void main(final String[] arguments)
    {
        Application.launch(arguments);
    }
}
