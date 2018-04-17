package gapp.gui.board;

import java.util.Objects;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Il compito di questa classe è istruire una funzione di disegno della board,
 * fornisce quindi i dati necessari per disegnare un determinato pezzo,
 * non che il al {@link GraphicsContext} dell {@link Canvas} ove il pezzo va disegnato. 
 * Non viene fornito alcun metodo di appoggio per il corretto disegno del pezzo,
 * si suppone che la funzione di disegno sia corretta e precedentemente testata.
 *
 * @author Gabriele Cavallaro
 *
 */
public class DrawingInfo
{
    public final GraphicsContext gc;
    public final double clm, row, x, y, cell_width, cell_height;
    public final Color firstcolor, secondcolor;
    
    /**
     * Questo metodo costruttore richiede tutti i seguenti parametri da passare alla funzione di disegno,
     * si richiede che nessun parametro sia nullo.
     * I valori numerici non posso assumere valori negativi.
     * 
     * @param in_gc
     * @param in_clm
     * @param in_row
     * @param in_x
     * @param in_y
     * @param in_cell_width
     * @param in_cell_height
     * @param in_firstcolor
     * @param in_secondcolor
     * 
     * @throw NullPointerException se uno qualunque dei parametri è nullo.
     * @throw IllegalArgumentException se un valore non rispetta le condizioni nella descrizione del costruttore. 
     */
    public DrawingInfo(GraphicsContext in_gc, double in_clm, double in_row, double in_x, double in_y, double in_cell_width, double in_cell_height, Color in_firstcolor, Color in_secondcolor)
    {
        Objects.requireNonNull(in_gc);
        Objects.requireNonNull(in_clm);
        Objects.requireNonNull(in_row);
        Objects.requireNonNull(in_x);
        Objects.requireNonNull(in_y);
        Objects.requireNonNull(in_cell_width);
        Objects.requireNonNull(in_cell_height);
        Objects.requireNonNull(in_firstcolor);
        Objects.requireNonNull(in_secondcolor);
        
        if(in_clm < 0 || in_row < 0 || in_x < 0 || in_y < 0 || in_cell_width < 0 || in_cell_height < 0) throw new IllegalArgumentException();
        
        gc  =         in_gc;        
        clm =         in_clm;
        row =         in_row;
        x   =         in_x;
        y   =         in_y;
        cell_width  = in_cell_width;
        cell_height = in_cell_height;
        firstcolor  = in_firstcolor;
        secondcolor = in_secondcolor;
    }
}
