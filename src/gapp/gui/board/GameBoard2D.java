package gapp.gui.board;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javafx.geometry.Point2D;
import javafx.geometry.VPos;
import javafx.event.EventHandler;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import gapp.gui.util.ColorTupla;
import gapp.ulg.game.board.Action;
import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.board.PieceModel.Species;

/**
 * 
 * Disegna una board 2d per giocare a giochi da tavolo che utilizzano una scacchiera,
 * puo' disegnare solo board quadrate, e disegnare pezzi di due combinazioni di colori.
 * Questo oggetto uno {@link StackPane} contenente due oggetti {@link Canvas} che usa per disegnare,
 * sul primo layer la board di gioco e sul secondo superiore i pezzi,
 * l'oggetto è dotato di opportuni metodi per interagire con la board.
 * 
 * Per aggiungere eventi sulle celle utilizzare l'interfaccia BoardListener,
 * e poi aggiungerela attraverso il metodo AddListener della board. 
 *  
 * Si consiglia di non usare il meno possibile i metodi ereditati dalla classe {@link StackPane}.
 *  
 * Questa classe si appoggia su {@link DrawingFunctions} per ottenere le funzioni di disegno.
 *  
 * Ultima modifica: Pomeriggio - 30/08/2016
 * @author Gabriele Cavallaro
 *
 */
public class GameBoard2D extends StackPane
{
    //variabili di gestione
    private double cell_width;
    private double cell_height;
    private int    board_width;
    private int    board_height;
    private volatile Collection<? extends Pos> positions;    
    
    //variabili per componenti grafiche   
    //layer della board
    private final Canvas          board_layer;
    private final GraphicsContext board_gc;
    
    //layer dei pezzi
    private final Canvas          pieces_layer;
    private final GraphicsContext pieces_gc;
    
    private volatile Color           firstcolor;
    private volatile Color           secondcolor;   
    private volatile Set<Point2D>    focusedcells; 
    
    //questa mappa potrebb essere migliorata, sopratutto nel costruttore, dato che si considerano solo 2 spazi,
    //mantenuta cosi per conformità con il framework
    private volatile Map<String, ColorTupla> piececolors;
    
    //variabili di gestione del framework
    private volatile Map<Pos, PieceModel<Species>> board_map;    
    
    //-altro-
    //handler
    private volatile List<BoardListener> listeners;
    
    //interfacce classi di utilità
    
    public interface BoardListener 
    {
        void setOnCellClick(MouseEvent e, int cell_x, int cell_y);
        
        void setOnPrimaryButtonDown(MouseEvent e, int cell_x, int cell_y);
        
        void setOnSecondaryButtonDown(MouseEvent e, int cell_x, int cell_y);
    }
    
    //--------------------
    //METODI COSTRUTTORI
    //--------------------
    
    /**
     * Construsce l'oggetto {@link GameBoard2D} con i paramentri comuni e necessari a tutti i costruttori,
     * questo metodo costruttore è necessario e utilizzabile solo dagli altri costruttori.
     * 
     * @param width
     * @param height
     * @param in_board_width
     * @param in_board_height
     * @param in_firstcolor
     * @param in_secondcolor
     * @param squareboard
     */
    private GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, boolean squareboard)
    {
        this.setMinSize(width, height);
        
        //layer su cui è disegnata la board
        board_layer  = new Canvas(width,height);
        //layer su cui vengono disegnati i pezzi
        pieces_layer = new Canvas(width,height);        
        
        this.getChildren().add(board_layer);
        this.getChildren().add(pieces_layer);
        
        //inizializzo i GraphicsContext, verrà usato per disegnare anche in futuro
        board_gc  = board_layer.getGraphicsContext2D();
        pieces_gc = pieces_layer.getGraphicsContext2D();
        
        //inizializzo variabili di gestione, non dovrebbero essere modificate
        //ma si puo' adattare per un resize
        firstcolor  = in_firstcolor;
        secondcolor = in_secondcolor;
        board_width = in_board_width;
        board_height= in_board_height; 
        cell_width  = width/board_width;
        cell_height = height/board_height;   
        
        //inizializzo lista per celle selezionate
        focusedcells = new HashSet<Point2D>();
        
        //inizializzo mappa per la gestione del framework
        board_map = new HashMap<>();  
        
        if(squareboard)
        {
            //disegno la board
            drawBoard();    
        }
        
        //handler
        listeners = new ArrayList<>();
        
        pieces_layer.setOnMouseClicked
        (
                e ->
                {
                    try
                    {
                        if(positions==null || positions.contains(new Pos((int)(e.getX()/cell_width),getYtoTInx((int)(e.getY()/cell_height)))))
                            for (BoardListener l : listeners)
                                l.setOnCellClick(e, (int)(e.getX()/cell_width), (int)(e.getY()/cell_height));
                    }
                    catch(NullPointerException listerner_npe){}
                    catch(Exception listerner_exception){}
                }           
        );       
        
        pieces_layer.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent e) 
            {
                try
                {
                    if(e.isPrimaryButtonDown()) 
                        if(positions==null || positions.contains(new Pos((int)(e.getX()/cell_width),getYtoTInx((int)(e.getY()/cell_height)))))
                            for (BoardListener l : listeners)
                                l.setOnPrimaryButtonDown(e, (int)(e.getX()/cell_width), (int)(e.getY()/cell_height));
                }
                catch(NullPointerException listerner_npe){}
                catch(Exception listerner_exception){}
                        
                try
                {
                    if(e.isSecondaryButtonDown())
                        if(positions==null || positions.contains(new Pos((int)(e.getX()/cell_width),getYtoTInx((int)(e.getY()/cell_height)))))
                            for (BoardListener l : listeners)
                                l.setOnSecondaryButtonDown(e, (int)(e.getX()/cell_width), (int)(e.getY()/cell_height));
                }
                catch(NullPointerException listerner_npe){}
                catch(Exception listerner_exception){}
            }            
        });     
    }
    
    /**
     * 
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * 
     * @param width,  larghezza del Canvas
     * @param height, altezza del Canvas
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor)
    {       
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, true);
    }
    
    /**
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * Questo metodo costruttore accetta, come ultimo paramentro, un array di nomi da assegnare ai vari pezzi.
     * In questo caso, i pezzi posso avere solo due combinazioni di colori utilizzando quelli della scacchiera.
     * 
     * @param width,  larghezza del Canvas
     * @param height, altezza del Canvas
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     * 
     * @param piececolors, array contente il nome dei pezzi di colore diverso, (es: nero, bianco)
     * 
     * @throws IllegalArgumentException se in_piececolors contiene più di due elementi.
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, List<String> in_piececolors)
    {       
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, true);
        
        //creo mappatura per il colore dei pezzi
        if(in_piececolors.size() > 2) throw new IllegalArgumentException("in_piececolors maggiore di due.");
        
        if(in_piececolors.size() == 2)
        {
            piececolors = new HashMap<String, ColorTupla>();
            piececolors.put(in_piececolors.get(0), new ColorTupla(in_firstcolor, in_secondcolor));
            piececolors.put(in_piececolors.get(1), new ColorTupla(in_secondcolor, in_firstcolor));
        }
    }
    
    /**
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * Questo metodo costruttore accetta, come ultimo paramentro, un array di nomi da assegnare ai vari pezzi.
     * In questo caso, i pezzi posso avere solo due combinazioni di colori utilizzando quelli della scacchiera,
     * e i colori devono essere specificati nei parametri in_firstpiececolor e in_secondpiececolor.
     * 
     * Questo metodo è particolarmente utile quando si utilizzano giochi come l'Othello, dove la scacchiera ha colori diversi
     * dai pezzi.
     * 
     * @param width,  larghezza del Canvas
     * @param height, altezza del Canvas
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     * 
     * @param in_firstpiececolor, primo colore dei pezzi
     * @param in_secondpiececolor, secondo colore dei pezzi
     * @param piececolors, array contente il nome dei pezzi di colore diverso, (es: nero, bianco)
     * 
     * @throws IllegalArgumentException se in_piececolors contiene più di due elementi.
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, Color in_firstpiececolor, Color in_secondpiececolor, List<String> in_piececolors)
    {       
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, true);    
        
        //creo mappatura per il colore dei pezzi
        if(in_piececolors.size() > 2) throw new IllegalArgumentException("in_piececolors maggiore di due.");
        
        if(in_piececolors.size() == 2)
        {
            piececolors = new HashMap<String, ColorTupla>();
            piececolors.put(in_piececolors.get(0), new ColorTupla(in_firstpiececolor, in_secondpiececolor));
            piececolors.put(in_piececolors.get(1), new ColorTupla(in_secondpiececolor, in_firstpiececolor));
        }
    }
    
    //METODI CON POSIZIONI ESCLUSE
    
    /**
     * 
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * Questo costruttore accetta una lista di Pos e disegnerà solo le posizioni specificate nella lista.
     * 
     * @param width,  larghezza del {@link Canvas}
     * @param height, altezza del {@link Canvas}
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     * 
     * @param included, le uniche posizioni che la board deve disegnare e considerare
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, Collection<? extends Pos> included)
    {
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, false);
        
        //differenza tra la normale e questa
        positions = included;  
        
        //disegno la board
        drawBoard();
    }
    
    /**
     * 
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * Questo costruttore accetta una lista di Pos e disegnerà solo le posizioni specificate nella lista.
     * In oltre, questo metodo costruttore accetta, come ultimo paramentro, un array di nomi da assegnare ai vari pezzi.
     * In questo caso, i pezzi posso avere solo due combinazioni di colori utilizzando quelli della scacchiera.
     * 
     * @param width,  larghezza del {@link Canvas}
     * @param height, altezza del {@link Canvas}
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     * 
     * @param included, le uniche posizioni che la board deve disegnare e considerare
     * 
     * @param piececolors, array contente il nome dei pezzi di colore diverso, (es: nero, bianco)
     * 
     * @throws IllegalArgumentException se in_piececolors contiene più di due elementi.
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, Collection<? extends Pos> included, List<String> in_piececolors)
    {
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, false);
        
        //differenza tra la normale e questa
        positions = included;  
        
        //disegno la board
        drawBoard();
        
        //creo mappatura per il colore dei pezzi
        if(in_piececolors.size() > 2) throw new IllegalArgumentException("in_piececolors maggiore di due.");
        
        if(in_piececolors.size() == 2)
        {
            piececolors = new HashMap<String, ColorTupla>();
            piececolors.put(in_piececolors.get(0), new ColorTupla(in_firstcolor, in_secondcolor));
            piececolors.put(in_piececolors.get(1), new ColorTupla(in_secondcolor, in_firstcolor));
        }
    }
    
    /**
     * 
     * Costruisce un oggetto {@link StackPane} con disegnata sopra una board con i parmetri specificati,
     * con medoti opportuni per il suo utilizzo.
     * Questo costruttore accetta una lista di Pos e disegnerà solo le posizioni specificate nella lista.
     * In oltre, questo metodo costruttore accetta, come ultimo paramentro, un array di nomi da assegnare ai vari pezzi.
     * In questo caso, i pezzi posso avere solo due combinazioni di colori utilizzando quelli della scacchiera,
     * e i colori devono essere specificati nei parametri in_firstpiececolor e in_secondpiececolor.
     * 
     * Questo metodo è particolarmente utile quando si utilizzano giochi come l'Othello, dove la scacchiera ha colori diversi
     * dai pezzi.
     * 
     * @param width,  larghezza del {@link Canvas}
     * @param height, altezza del {@link Canvas}
     * @param in_board_width, numero di celle in orizzontale sulla board 
     * @param in_board_height, numero di celle in verticale sulal board
     * @param in_firstcolor, primo colore delle celle
     * @param in_secondcolor, secondo colore delle celle
     * 
     * @param included, le uniche posizioni che la board deve disegnare e considerare
     * 
     * @param in_firstpiececolor, primo colore dei pezzi
     * @param in_secondpiececolor, secondo colore dei pezzi
     * @param piececolor, array contente il nome dei pezzi di colore diverso, (es: nero, bianco)
     * 
     * @throws IllegalArgumentException se in_piececolors contiene più di due elementi.
     */
    public GameBoard2D(double width, double height, int in_board_width, int in_board_height, Color in_firstcolor, Color in_secondcolor, Collection<? extends Pos> included, Color in_firstpiececolor, Color in_secondpiececolor, List<String> in_piececolors)
    {
        this(width, height, in_board_width, in_board_height, in_firstcolor, in_secondcolor, false);
        
        //differenza tra la normale e questa
        positions = included;  
        
        //disegno la board
        drawBoard();
        
        //creo mappatura per il colore dei pezzi
        if(in_piececolors.size() > 2) throw new IllegalArgumentException("in_piececolors maggiore di due.");
        
        if(in_piececolors.size() == 2)
        {
            piececolors = new HashMap<String, ColorTupla>();
            piececolors.put(in_piececolors.get(0), new ColorTupla(in_firstpiececolor, in_secondpiececolor));
            piececolors.put(in_piececolors.get(1), new ColorTupla(in_secondpiececolor, in_firstpiececolor));
        }
    }
    
    //--------------------
    //METODI PER GESTIONE DELLE COORDINATE & METODI DI GET
    //--------------------
    
    /**
     * Ritorna l'indice della riga della cella
     * si muove sull'asse verticale,
     * e ritorna la cella in posizione trasversale.
     * 
     * @param coordy
     * @return indice della riga
     */
    public int getCellRow(double coordy)
    {
        return (int) (coordy/cell_height);
    }  
    
    /**
     * Ritorna l'indice della colonna delal cella
     * si muove sull'asse orizzontale,
     * cercando la cella sull'asse base.
     * 
     * @param coordx
     * @return
     */
    public int getCellColumn(double coordx)
    {
        return (int) (coordx/cell_width);
    }
   
    /**
     * Ritorna la coordinata x di inizio cella dalla coordinata indicata.
     * 
     * @param coordx
     * @return x d'inizio cella
     */
    public double getCellxCoord(double coordx)
    {
        return getCellRow(coordx)*cell_width;
    }
    
    /**
     * Ritorna la coordinata y di inizio cella dalla coordinata indicata.
     * 
     * @param coordx
     * @return y d'inizio cella
     */
    public double getCellyCoord(double coordy)
    {
        return getCellColumn(coordy)*cell_height;
    }
    
    /**
     * Ritorna la coordinata x di inizio cella dalla colonna indicata.
     * 
     * @param coordx
     * @return y d'inizio cella
     */
    public double getCellxCoordFromClm(int clm)
    {
        return clm*cell_width;
    }
    
    /**
     * Ritorna la coordinata y di inizio cella dalla riga indicata.
     * 
     * @param coordy
     * @return y d'inizio cella
     */
    public double getCellyCoordFromRow(int row)
    {
        return row*cell_height;
    }

    /**
     * Traduce le cordinate verticali della board in cordinate t del framework.
     * 
     * @param y
     * @return t
     */
    public int getYtoTInx(int y)
    {
        return board_height - y -1;
    }
    
    /**
     * Traduce le cordinate t del framework in verticali della board.
     * 
     * @param t
     * @return y
     */
    public int getTtoYInx(int t)
    {
        return board_height - t -1;
    }
    
    /**
     * Converte una cordinata di una cella in una Pos,
     * autonomamente il medoto converte la y in modo corretto.
     * 
     * @param x
     * @param y
     * @return Pos convertita
     */
    public Pos convertCoordinatetoPos(int x, int y)
    {
        return new Pos(x, getYtoTInx(y));
    }
    
    /**
     * Ritorna un {@link Image} che rappresenta ciò che è attualmente disegnato sulla board.
     * 
     * @return {@link Image} screenshot dell'attuale stato della board
     */
    public Image getBoardSnapShot()
    {
        SnapshotParameters params = new SnapshotParameters();
        params.setFill(Color.TRANSPARENT);        
        return this.snapshot(params, null);
    }

    //--------------------
    //METODI DI GESTIONE PEZZI
    //--------------------
    
    /**
     * Mette un pezzo sulla board, nella posizione indicata,
     * se c'è già un pezzo sopra, rimuove quello esistente,
     * e disegna il nuovo pezzo.
     *  
     * @param piece
     * @param b
     * @param t
     */
    public void putPiece(String piece, int b, int t)
    {   
        Pos tmp_pos = new Pos(b, t);
        
        if(board_map.containsKey(tmp_pos))
            clearCell(b, getTtoYInx(t));
        else
            board_map.put(tmp_pos, new PieceModel<Species>(PieceModel.Species.valueOf(piece), "none"));
        
        drawPiece(piece, b, getTtoYInx(t));
    }
    
    /**
     * Mette un pezzo sulla board, nella posizione indicata,
     * se c'è già un pezzo sopra, rimuove quello esistente,
     * e disegna il nuovo pezzo.
     * A differenza della sua fuzione gemella putPiece(String piece, int b, int t),
     * in questa può essere specificato il colore del pezzo da disegnare.
     * 
     * @param piece
     * @param b
     * @param t
     * @param piece_firstcolor
     * @param piece_second
     */
    public void putPiece(String piece, int b, int t, Color piece_firstcolor, Color piece_secondcolor)
    {   
        Pos tmp_pos = new Pos(b, t);
        
        if(board_map.containsKey(tmp_pos))
            clearCell(b, getTtoYInx(t));
        else
            board_map.put(tmp_pos, new PieceModel<Species>(PieceModel.Species.valueOf(piece), "none"));
        
        drawPiece(piece, b, getTtoYInx(t), piece_firstcolor, piece_secondcolor);
    }
    
    /**
     * Mette un pezzo sulla board, nella posizione indicata,
     * se c'è già un pezzo sopra, rimuove quello esistente,
     * e disegna il nuovo pezzo.
     * A differenza della sua fuzione gemella putPiece(String piece, int b, int t, Color piece_firstcolor, Color piece_secondcolor),
     * in questa può essere specificato il colore del pezzo da disegnare utilizzando il colore del pezzo come riferimento.
     * Il compito principale di questa funzione è disegnare i pezzi di provenienti da un gioco dove l'unico rifermento al colore,
     * è una stringa
     * 
     * @param piece
     * @param b
     * @param t
     * @param piececolor
     */
    public void putPiece(String piece, int b, int t, String piececolor)
    {   
        if(piececolors == null)
        {
            putPiece(piece,  b,  t);
            return;
        }
        
        Pos tmp_pos = new Pos(b, t);
        
        if(board_map.containsKey(tmp_pos))
            clearCell(b, getTtoYInx(t));
        else
            board_map.put(tmp_pos, new PieceModel<Species>(PieceModel.Species.valueOf(piece), piececolor));
        
        drawPiece(piece, b, getTtoYInx(t), piececolors.get(piececolor).firstcolor, piececolors.get(piececolor).secondcolor);
    }
    
    /**
     * Rimuove un pezzo dalla posizione indicata, se presente.
     * 
     * @param piece
     * @param b
     * @param t
     */
    public void removePiece(int b, int t)
    {        
        Pos tmp_pos = new Pos(b, t);
        
        if(board_map.containsKey(tmp_pos))
        {
            board_map.remove(tmp_pos);
            clearCell(b, getTtoYInx(t));
        }
    }
    
    /**
     * Rimuove tutti i pezzi dalla board.
     * Pulisce anche le registrazioni dei pezzi,
     * quindi non risulterà nessun pezzo sulla board.
     */
    public void removeAllPieces()
    {
        for(int h=0 ; h<board_height ; h++)
            for(int w=0; w<board_width ; w++)
                removePiece(h, w);
    }
    
    //--------------------
    //METODI DI DISEGNO
    //--------------------
    
    /**
     * Disegna la board sul layer board_layer di questo oggetto con le posizioni presenti nella board,
     * la board può essere ridisegnata e quindi cambiare colore senza cambiare colore ai pezzi,
     * la se cambiano le dimensioni non viene garantito un adattamento ai pezzi precedentemente predisposti.
     * 
     * @param evencolor
     * @param oddcolor
     * @param cellborder
     */
    private void drawBoardwithPositions(Color evencolor, Color oddcolor, Color cellborder)
    {
        board_gc.setLineWidth(1);
        board_gc.setStroke(cellborder);  
        
        int tmp_t;
        double height;
        
        for (int i = 0; i < board_height ;i++)
        {
            tmp_t = getYtoTInx(i);
            height = i*cell_height;
            
            for (int k = 0; k < board_width; k++)
            {     
                if(positions.contains(new Pos(k, tmp_t)))
                {
                    if((k+i)%2 == 0)
                        board_gc.setFill(evencolor);
                    else
                        board_gc.setFill(oddcolor); 
                    
                    board_gc.fillRect(k*cell_width, height, cell_width, cell_height);
                    board_gc.strokeRoundRect(k*cell_width, height, cell_width, cell_height, 10, 10);    
                }
            }
        }
        
        //lasciare commentanto, serve per mostrare i bordi della board
        //mygc.strokeRect(0, 0, cell_width*board_width, cell_height*board_height);          
    }

    /**
     * Disegna la board sul layer board_layer di questo oggetto,
     * la board può essere ridisegnata e quindi cambiare colore senza cambiare colore ai pezzi,
     * la se cambiano le dimensioni non viene garantito un adattamento ai pezzi precedentemente predisposti.
     * 
     * @param evencolor
     * @param oddcolor
     * @param cellborder
     */
    public void drawBoard(Color evencolor, Color oddcolor, Color cellborder)
    {
        if(positions != null)
        {
            drawBoardwithPositions(evencolor, oddcolor, cellborder);
            return;
        }
        
        board_gc.setLineWidth(1);
        board_gc.setStroke(cellborder);  
        
        int modifier;
        double height;
        
        for (int i = 0;i < board_height ;i++)
        {
            modifier = i%2;
            height = i*cell_height;
            board_gc.setFill(evencolor); 
            
            /* disegna tutte le celle del primo colore,
             * partendo dalla prima cella e saltandone una fino alla fine
             */
            for (int k = 0;k < board_width;k+=2)
            {
                board_gc.fillRect((k+modifier)*cell_width, height, cell_width, cell_height);
                board_gc.strokeRoundRect((k+modifier)*cell_width, height, cell_width, cell_height, 10, 10);                
            }  
            
            board_gc.setFill(oddcolor);            

            /* disegna tutte le celle del secondo colore,
             * partendo dalla seconda cella e saltandone una fino alla fine
             */
            
            for (int k = 1;k < board_width+1;k+=2)
            {
                board_gc.fillRect((k-modifier)*cell_width, height, cell_width, cell_height);
                board_gc.strokeRoundRect((k-modifier)*cell_width, height, cell_width, cell_height, 10, 10);
            } 
        }
        
        //lasciare commentanto, serve per mostrare i bordi della board
        //mygc.strokeRect(0, 0, cell_width*board_width, cell_height*board_height);  
    } 

    /**
     * Disegna la board di gioco utilizzando le impostazioni di default
     */
    public void drawBoard()
    {
        drawBoard(firstcolor, secondcolor, Color.BLACK);
    }  
    
    /**
     * Ridisegna una specifica cella della board
     * che si trova nella posizione delle coordinate passate.
     * 
     * @param x (posizione del pixel sull'asse x)
     * @param y (posizione del pixel sull'asse y)
     */
    private void repaintCell(int clm, int row)
    {
        board_gc.setLineWidth(1);
        board_gc.setStroke(Color.BLACK); 
        
        if((clm+row*(board_width-1))%2==0)
            board_gc.setFill(firstcolor);     
        else
            board_gc.setFill(secondcolor);      
        
        board_gc.fillRect(clm*cell_width, row*cell_height, cell_width, cell_height);
        board_gc.strokeRoundRect(clm*cell_width, row*cell_height, cell_width, cell_height, 10, 10);
    }
    
    /**
     * Ridisegna una specifica cella della board
     * che si trova nella posizione delle coordinate passate.
     * 
     * @param x (posizione del pixel sull'asse x)
     * @param y (posizione del pixel sull'asse y)
     */
    private void repaintCell(double x, double y)
    {
        board_gc.setStroke(Color.BLACK); 
        
        int clm = getCellColumn(x);
        int row = getCellRow(y);
        
        repaintCell(clm, row);
    }
    
    /**
     * Pulisce una specifica cella della board
     * cancellando il pezzo soprastante sulla cella
     * nelle posizione delle coordinate passate.
     * 
     * @param x (posizione del pixel sull'asse x)
     * @param y (posizione del pixel sull'asse y)
     */
    private void clearCell(double x, double y)
    {
        int clm = getCellColumn(x);
        int row = getCellRow(y);
        
        pieces_gc.clearRect(clm*cell_width, row*cell_height, cell_width, cell_height);       
    }    
    
    /**
     * Pulisce una specifica cella della board
     * cancellando il pezzo soprastante sulla cella
     * nelle posizione delle coordinate passate.
     * 
     * @param x (posizione del pixel sull'asse x)
     * @param y (posizione del pixel sull'asse y)
     */
    private void clearCell(int x, int y)
    {        
        pieces_gc.clearRect(x*cell_width, y*cell_height, cell_width, cell_height);       
    }
    
    /**
     * Cancella tutti i pezzi dalla board.
     * Azione solo grafica, la board non si annota i pezzi cancellati.
     */
    private void clearAllCell()
    {
        for(int h=0 ; h<board_height ; h++)
            for(int w=0; w<board_width ; w++)
                clearCell(h, w);
    }
    
    /**
     * Cancella la cella indicata.
     * 
     * @param x
     * @param y
     */
    private void eraseCell(double x, double y)
    {
        int clm = getCellColumn(x);
        int row = getCellRow(y);
        
        board_gc.clearRect(clm*cell_width-1, row*cell_height-1, cell_width+1, cell_height+1);       
    }
    
    /**
     * Questa funzione modifica la visibilità della board, oscurandola,
     * e illuminando tutte le posizioni aggiunte attraverso questa funzione.
     * In questo caso la funzione accetta due parametri, corrispondenti
     * alle coordinate della board grafica.
     * 
     * @param clm
     * @param row
     */
    public void focusCell(int clm, int row)
    {
        focusedcells.add(new Point2D(clm, row));
        
        if(focusedcells.size()==1)
            drawBoard(firstcolor.darker(), secondcolor.darker(), Color.BLACK);   
        
        board_gc.setLineWidth(1);
        board_gc.setStroke(Color.BLACK);
        
        if((clm+row)%2==0)
            board_gc.setFill(firstcolor);     
        else
            board_gc.setFill(secondcolor);      
        
        board_gc.fillRect(clm*cell_width, row*cell_height, cell_width, cell_height);
        board_gc.strokeRoundRect(clm*cell_width, row*cell_height, cell_width, cell_height, 10, 10);
    }  
    
    /**
     * Questa funzione modifica la visibilità della board, oscurandola,
     * e illuminando tutte le posizioni aggiunte attraverso questa funzione.
     * In questo caso la funzione accetta un parametro, corrispondente
     * alla posizione della board in cui si sta giocando.
     * 
     * @param p
     */
    public void focusCell(Pos p)
    {
        focusCell(p.b,getTtoYInx(p.t));
    }
    
    /**
     * Questa funzione modifica la visibilità della board, oscurandola,
     * e illuminando tutte le posizioni aggiunte attraverso questa funzione.
     * In questo caso la funzione accetta un parametro, corrispondente
     * alle posizioni della board in cui si sta giocando.
     * 
     * @param pos_list
     */
    public void focusAllCell(Collection<? extends Pos> pos_list)
    {
        for(Pos p : pos_list)
            focusCell(p.b,getTtoYInx(p.t));
    }
    
    /**
     * Questa funzione rimuove dell'elenco delle posizioni illuminate,
     * la posizione aggiunta precedentemente corrispondente ai parametri inseriti.
     * Se le posizioni illuminate finiscono, la funzione ripristina il colore orginale della board.
     * In questo caso la funzione accetta due parametri, corrispondenti
     * alle coordinate della board grafica.
     * 
     * @param clm
     * @param row
     */
    public void defocusCell(int clm, int row)
    {
        focusedcells.remove(new Point2D(clm, row));
        
        if(focusedcells.size() != 0)
        {
            board_gc.setLineWidth(1);
            board_gc.setStroke(Color.BLACK);
            
            if((clm+row)%2==0)
                board_gc.setFill(firstcolor.darker());     
            else
                board_gc.setFill(secondcolor.darker());      
            
            board_gc.fillRect(clm*cell_width, row*cell_height, cell_width, cell_height);
            board_gc.strokeRoundRect(clm*cell_width, row*cell_height, cell_width, cell_height, 10, 10);
        }
        else
        {
            drawBoard();
        }
    }
    
    /**
     * Questa funzione rimuove dell'elenco delle posizioni illuminate,
     * la posizione aggiunta precedentemente corrispondente ai parametri inseriti.
     * Se le posizioni illuminate finiscono, la funzione ripristina il colore orginale della board.
     * In questo caso la funzione accetta un parametro, corrispondente
     * alla posizione della board in cui si sta giocando.  
     * 
     * @param p
     */
    public void defocusCell(Pos p)
    {
        defocusCell(p.b,getTtoYInx(p.t));
    }
    
    /**
     * Questa funzione rimuove dell'elenco delle posizioni illuminate,
     * le posizioni aggiunte precedentemente corrispondente ai parametri inseriti.
     * Se le posizioni illuminate finiscono, la funzione ripristina il colore orginale della board. 
     * In questo caso la funzione accetta un parametro, corrispondente
     * alla posizione della board in cui si sta giocando. 
     * 
     * @param p
     */
    public void defocusAllCell(Collection<? extends Pos> pos_list)
    {
        for(Pos p : pos_list)
            defocusCell(p.b,getTtoYInx(p.t));
    }
    
    /**
     * Questa funzione rimuove dell'elenco delle posizioni illuminate,
     * tutte le posizioni precedentemente inserite,
     * e ripristina il colore della board.
     *  
     */
    public void defocusAllCell()
    {
        focusedcells.removeAll(focusedcells);
        drawBoard();
    }  
    
    /**
     * Cambia nella board come primo colore quello indicato
     * ATTENZIONE: la board non viene ricreata
     * quindi i pezzi futuri inseriti avranno colori diversi
     * dalla board nel primo colore. 
     * 
     * @param in_firstcolor
     */
    public void setFistColor(Color in_firstcolor)
    {
        firstcolor  = in_firstcolor;
    }
    
    /**
     * Cambia nella board come secondo colore quello indicato
     * ATTENZIONE: la board non viene ricreata
     * quindi i pezzi futuri inseriti avranno colori diversi
     * dalla board nel secondo colore. 
     * 
     * @param in_secondcolor
     */
    public void setSecondColor(Color in_secondcolor)
    {
        secondcolor = in_secondcolor;
    }
    
    /**
     * Cambia il primo colore della board,
     * successivamente ridisegna la board,
     * i precedentemente inseriti non vengono ridisegnati.
     * 
     * @param in_firstcolor
     */
    public void changeFistColor(Color in_firstcolor)
    {
        firstcolor  = in_firstcolor;
        drawBoard();
    }
    
    /**
     * Cambia il primo colore della board,
     * successivamente ridisegna la board,
     * i precedentemente inseriti non vengono ridisegnati.
     * 
     * @param in_secondcolor
     */
    public void changeSecondColor(Color in_secondcolor)
    {
        secondcolor = in_secondcolor;
        drawBoard();
    }  
    
    /**
     * Disegna un pezzo sulla board,
     * questo metodo accetta il nome del pezzo da cui determina la funzione con cui disegnarlo,
     * l'indice di quale colonna e di quale riga da cui determina la posizione in cui disegnarlo.
     * Se non esiste una funzione per disegnare il pezzo richiesto disegna un punto interrogativo.
     * 
     * @param piece
     * @param clm
     * @param row
     */
    private void drawPiece(String piece, int clm, int row)
    {
        double x = getCellxCoordFromClm(clm);
        double y = getCellyCoordFromRow(row);
        
        if(DrawingFunctions.getDrawPieceFuncionList().contains(piece))
            DrawingFunctions.getDrawPieceFuncion(piece).accept(new DrawingInfo(pieces_gc, clm, row, x, y, cell_width, cell_height, firstcolor, secondcolor));
        else
        {
            //se il pezzo non ha una funzione per essere disegnato, viene sostituito con una scritta
            if(piece.length() > 7) piece="?";
            pieces_gc.setTextAlign(TextAlignment.CENTER);
            pieces_gc.setTextBaseline(VPos.CENTER);
            pieces_gc.setFill(firstcolor);
            pieces_gc.setStroke(secondcolor);            
            pieces_gc.setFont(new Font("Arial", cell_width/piece.length()));
            pieces_gc.fillText(piece, x+cell_width/2, y+cell_height/2);
            pieces_gc.strokeText(piece, x+cell_width/2, y+cell_height/2);
        }          
    }
    
    /**
     * Disegna un pezzo sulla board,
     * questo metodo accetta il nome del pezzo da cui determina la funzione con cui disegnarlo,
     * l'indice di quale colonna e di quale riga da cui determina la posizione in cui disegnarlo.
     * Se non esiste una funzione per disegnare il pezzo richiesto disegna un punto interrogativo.
     * Il colore del pezzo può essere specificato nei parametri piece_firstcolor e piece_secondcolor,
     * si intende che il color piece_firstcolor sia il colore principale del pezzo.
     * 
     * @param piece
     * @param clm
     * @param row
     * @param piece_firstcolor
     * @param piece_secondcolor
     */
    private void drawPiece(String piece, int clm, int row, Color piece_firstcolor, Color piece_secondcolor)
    {
        double x = getCellxCoordFromClm(clm);
        double y = getCellyCoordFromRow(row);
        
        if(DrawingFunctions.getDrawPieceFuncionList().contains(piece))
            DrawingFunctions.getDrawPieceFuncion(piece).accept(new DrawingInfo(pieces_gc, clm, row, x, y, cell_width, cell_height, piece_firstcolor, piece_secondcolor));
        else
        {
            if(piece.length() > 7) piece="?";
            pieces_gc.setTextAlign(TextAlignment.CENTER);
            pieces_gc.setTextBaseline(VPos.CENTER);
            pieces_gc.setFill(firstcolor);
            pieces_gc.setStroke(secondcolor);            
            pieces_gc.setFont(new Font("Arial", cell_width/piece.length()));
            pieces_gc.fillText(piece, x+cell_width/2, y+cell_height/2);
            pieces_gc.strokeText(piece, x+cell_width/2, y+cell_height/2);
        }          
    }
    
    //--------------------
    //METODI DI GET GLOBALI
    //--------------------    
    
    /**
     * Ritorna la larghezza di una cella,
     * nell'esatta dimensione che occupa sullo schermo.
     * 
     * @return cell_width
     */
    public double getCellWidth()
    {
        return cell_width;
    }
    
    /**
     * Ritorna l'altezza di una cella,
     * nell'esatta dimensione che occupa sullo schermo.
     * 
     * @return cell_height
     */
    public double getCellHeight()
    {
        return cell_height;
    }
    
    /**
     * Ritorna il numero di celle della board in larghezza.
     * 
     * @return board_width
     */
    public int getBoardWidth()
    {
        return board_width;
    }
    
    /**
     * Ritorna il numero di celle della board in altezza.
     * 
     * @return board_width
     */
    public int getBoardHeight()
    {
        return board_height;
    }
    
    /**
     * Ritorna il primo colore della board,
     * utilizzato per disegnare le celle pari.
     * 
     * @return firstcolor
     */
    public Color getFirstColor()
    {
        return firstcolor;
    }
    
    /**
     * Ritorna il secondo colore della board,
     * utilizzato per disegnare le celle dispari.
     * 
     * @return secondcolor
     */
    public Color getSecondColor()
    {
        return secondcolor;
    }
    
    /**
     * Ritorna un {@link Set} immodificabile con le posizoni attualmente illuminate sulla board.
     * Le posizioni ritornate sono di tipo {@link Point2D} che corrispondo a una coordinata della board.
     * @return Set<Point2D> focusedcells
     */
    public Set<Point2D> getFocusedCell()
    {
        return Collections.unmodifiableSet(focusedcells);
    }
    
    //--------------------
    //ALTRI METODI
    //--------------------
    
    /**
     * Aggiunge un listener di tipo {@link BoardListener} a quest'oggetto,
     * ogni volta che un evento tra quelli considerati dall'interfaccia del listener accade,
     * questo oggetto chiama quell'evento su tutti i listener aggiunti da questa funzione.
     * Il listener non può essere rimosso. 
     * 
     * @param bl
     */
    public void addNewListener(BoardListener bl)
    {
        listeners.add(bl);
    }
    
    /**
     * Passata un oggetto di tipo {@link Board},
     * viene disegnata sulla board pezzo per pezzo, 
     * resettando i cambiamenti precedentemente effettuati sulla board grafica.
     * 
     * @param board
     * 
     * @throws NullPointerException se la board è nulla
     */
    public void loadBoard(Board<PieceModel<Species>> board)
    {
        Objects.requireNonNull(board);
        
        defocusAllCell();
        removeAllPieces();
        
        for(Pos p : board.get())
            putPiece(board.get(p).species.name(), p.b ,p.t, board.get(p).color);            
    }
    
    /**
     * Disegna una mossa sulla board,
     * L'operazione è istantanea quindi sarà visibile solo il risultato finale.
     * Se la mossa è nulla non accade niente, inoltre
     * la mossa viene disegna sempre al meglio, ma può fallire,
     * se gli accessi alla board sono troppo rapidi da una parita giocata solo da giocatori non umani,
     * in tutti i casi gli errori vengono gestiti internamente,
     * e non restano visibili.
     * 
     * @param m mossa da eseguire
     */
    public void execMove(Move<PieceModel<Species>> m)
    {
        if(m==null) return;
                   
        // Disegno la mossa sulla board
        try
        { 
            for(Action<PieceModel<Species>> action : m.actions)
            {
                if(action==null) continue;
                
                switch(action.kind)
                {
                    case ADD:    
                        putPiece(action.piece.species.name(), action.pos.get(0).b, action.pos.get(0).t, action.piece.color);
                    break;
                    
                    case REMOVE:                    
                        for(Pos p : action.pos) removePiece(p.b, p.t);
                    break;
                    
                    case MOVE:
                        {
                            for(Pos p : action.pos)
                            {
                                int mv_b = p.b;
                                int mv_t = p.t;
                                
                                for(int i=0 ; i<action.steps ; i++)
                                {
                                    switch(action.dir)
                                    {
                                        case UP: mv_t++;
                                            break;
                                        case DOWN: mv_t--;
                                            break;
                                        case LEFT: mv_b--;
                                            break;
                                        case RIGHT: mv_b++;
                                            break;
                                        case UP_L: {mv_b--; mv_t++;}
                                            break;
                                        case UP_R: {mv_b++; mv_t++;}
                                            break;
                                        case DOWN_L: {mv_b--; mv_t--;}
                                            break;
                                        case DOWN_R: {mv_b++; mv_t--;}
                                            break;
                                    }
                                }
                                
                                putPiece(board_map.get(action.pos.get(0)).species.name(), mv_b, mv_t, board_map.get(action.pos.get(0)).color);
                                removePiece(p.b, p.t);
                            }
                        }
                    break;
                    
                    case JUMP:
                        {
                            putPiece(board_map.get(action.pos.get(0)).species.name(), action.pos.get(1).b, action.pos.get(1).t, board_map.get(action.pos.get(0)).color);
                            removePiece(action.pos.get(0).b, action.pos.get(0).t);
                        }
                    break;
                    
                    case SWAP:                    
                        for(Pos p : action.pos) putPiece(action.piece.species.name(), p.b, p.t, action.piece.color);                    
                    break;
                }
            }
        }
        catch(NullPointerException npe){}
    }
}
