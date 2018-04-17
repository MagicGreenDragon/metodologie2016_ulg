package gapp.gui.board;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import javafx.geometry.Point2D;

/**
 * Questa classe mantiene in modo statico le funzioni di disegno dei pezzi,
 * qui possono essere aggiunte nuove funzioni di disegno o sostituite quelle già esistenti,
 * questo sistema permette alla schacchiera di riconoscere nuove funzioni di disegno se introdotti nuovi pezzi,
 * permette anche di cambiare le funzioni già esistenti se si voglia cambiare lo stile di disegno.
 * Questo oggetto chiama setDefaultDrawFunctions() staticamente aggiungendo un set di funzioni per disegnare tutti
 * i pezzi della classe {@link PieceModel}, la funzione può essere richiamata nuovamente per risostiutire le funzioni
 * se sono state sostituite con altre.
 * 
 * Ultima Modifica: Sera - 03/08
 * @author Gabriele Cavallaro
 *
 */
public class DrawingFunctions 
{
    private static final ConcurrentMap<String, Consumer<DrawingInfo>> drawingfunctions;
    
    static
    {
        //inizializzo mappa statica
        drawingfunctions = new ConcurrentHashMap<String, Consumer<DrawingInfo>>();
        
        //inizializzo funzioni di disegno pezzi
        setDefaultDrawFunctions();
    }
    
    public static Consumer<DrawingInfo> getDrawPieceFuncion(String name)
    {
        Objects.requireNonNull(name);
        
        synchronized(drawingfunctions)
        {
            return drawingfunctions.get(name);
        }
    }
    
    public static Set<String> getDrawPieceFuncionList()
    {
        synchronized(drawingfunctions)
        {
            return drawingfunctions.keySet();
        }
    }
    
    public static int getDrawPieceFuncionCount()
    {
        synchronized(drawingfunctions)
        {
            return drawingfunctions.keySet().size();
        }
    }
    
    /**
     * Aggiunge una nuova o sostituisce un esiste,
     * funzione di scrittura di un pezzo,
     * il nome del pezzo deve essere lo stesso utilizzato dalla lista dei modelli che utilizza il gioco,
     * la funzione di disegno deve disegnare utilizzando l'oggetto DrawingInfo
     * @param name (nome del pezzo)
     * @param consumer (funzione consumatore che disegna il pezzo)
     */
    public static void putDrawPieceFuncion(String name, Consumer<DrawingInfo> consumer)
    {
        Objects.requireNonNull(name);
        Objects.requireNonNull(consumer);
        
        synchronized(drawingfunctions)
        {
            drawingfunctions.put(name, consumer);
        }
    }
    
    /**
     * Imposta nella mappa delle funzioni di disegno dei pezzi le funzioni base di disegno,
     * questa funzione è chiamata nell'inizializzazione dell'oggetto,
     * ma se è stata sostituita una funzione di disegno tra quelle di default,
     * richiamando questa funzione, può essere ripristinata.
     */
    public static void setDefaultDrawFunctions()
    {        
        synchronized(drawingfunctions)
        {
            //Funzione di disegno disco
            Consumer<DrawingInfo> drawDisk = di ->
            {
                //disco                 
                di.gc.setLineWidth(1);
                di.gc.setFill (di.firstcolor);
                di.gc.setStroke   (di.secondcolor);  
                          
                //bordo disco       
                di.gc.fillOval(di.x+di.cell_width/4, di.y+di.cell_height/4, di.cell_width/2, di.cell_height/2);   
                di.gc.strokeOval(di.x+di.cell_width/4, di.y+di.cell_height/4, di.cell_width/2, di.cell_height/2); 
                
                //disco interno
                //di.gc.strokeOval(di.x+di.cell_width*28/100, di.y+di.cell_height*28/100, di.cell_width*44/100, di.cell_height*44/100);
                di.gc.strokeOval(di.x+di.cell_width*30/100, di.y+di.cell_height*30/100, di.cell_width*40/100, di.cell_height*40/100);
            };
            
            putDrawPieceFuncion("DISC", drawDisk);
            
            Consumer<DrawingInfo> drawDama = di ->
            {
                //disco                 
                di.gc.setLineWidth(1);
                di.gc.setFill (di.firstcolor);
                di.gc.setStroke   (di.secondcolor);  
                          
                //bordo disco       
                di.gc.fillOval(di.x+di.cell_width/4, di.y+di.cell_height/4, di.cell_width/2, di.cell_height/2);   
                di.gc.strokeOval(di.x+di.cell_width/4, di.y+di.cell_height/4, di.cell_width/2, di.cell_height/2); 
                
                //disco interno
                di.gc.strokeOval(di.x+di.cell_width*30/100, di.y+di.cell_height*30/100, di.cell_width*40/100, di.cell_height*40/100);
                
                //disco interno interno
                di.gc.strokeOval(di.x+di.cell_width*40/100, di.y+di.cell_height*40/100, di.cell_width*20/100, di.cell_height*20/100);
            };
            
            //ho dato alla dama la funzione del disco per farla poi
            putDrawPieceFuncion("DAMA", drawDama);
    
            //Funzione di disegno pedone 
            Consumer<DrawingInfo> drawPawn = di ->
            {        
                di.gc.setFill(di.firstcolor); 
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor);  
                
                //triangolo
                di.gc.fillPolygon
                (
                    new double[]{di.x+(di.cell_width/2),di.x+(di.cell_width/5),di.x+(di.cell_width*4/5)},
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height*4/5),di.y+(di.cell_height*4/5)},
                    3
                );
                
                //bordo triangolo
                di.gc.strokePolygon
                (
                    new double[]{di.x+(di.cell_width/2),di.x+(di.cell_width/5),di.x+(di.cell_width*4/5)},
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height*4/5),di.y+(di.cell_height*4/5)},
                    3
                );
                
                //pallino grande
                di.gc.fillOval(di.x+(di.cell_width-di.cell_width/3)/2, di.y+(di.cell_width-di.cell_height/3)/5, di.cell_width/3, di.cell_height/3);
                //bordo pallino grande
                di.gc.strokeOval(di.x+(di.cell_width-di.cell_width/3)/2, di.y+(di.cell_width-di.cell_height/3)/5, di.cell_width/3, di.cell_height/3);
            };
            
            putDrawPieceFuncion("PAWN", drawPawn);
    
            //Funzione di disegno torre        
            Consumer<DrawingInfo> drawRook = di ->
            {        
                di.gc.setFill(di.firstcolor); 
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor);   
                
                //base
                di.gc.fillPolygon
                (
                    new double[]{di.x+(di.cell_width/5),di.x+(di.cell_width/5),di.x+(di.cell_width*4/5),di.x+(di.cell_width*4/5)}, 
                    new double[]{di.y+(di.cell_height*7/10),di.y+(di.cell_height*4/5),di.y+(di.cell_height*4/5),di.y+(di.cell_height*7/10)}, 
                    4
                );
                
                //bordo base
                di.gc.strokePolygon
                (
                    new double[]{di.x+(di.cell_width/5),di.x+(di.cell_width/5),di.x+(di.cell_width*4/5),di.x+(di.cell_width*4/5)}, 
                    new double[]{di.y+(di.cell_height*7/10),di.y+(di.cell_height*4/5),di.y+(di.cell_height*4/5),di.y+(di.cell_height*7/10)}, 
                    4
                );
                
                //tronco
                di.gc.fillPolygon
                (
                    new double[]{di.x+(di.cell_width*2/5),di.x+(di.cell_width*3/5),di.x+(di.cell_width*3/5),di.x+(di.cell_width*2/5)}, 
                    new double[]{di.y+(di.cell_height*7/10),di.y+(di.cell_height*7/10),di.y+(di.cell_height/5),di.y+(di.cell_height/5)},
                    4
                );
                
                //bordo tronco
                di.gc.strokePolygon
                (
                    new double[]{di.x+(di.cell_width*2/5),di.x+(di.cell_width*3/5),di.x+(di.cell_width*3/5),di.x+(di.cell_width*2/5)}, 
                    new double[]{di.y+(di.cell_height*7/10),di.y+(di.cell_height*7/10),di.y+(di.cell_height/5),di.y+(di.cell_height/5)},
                    4
                );
                
                
                //cubo alto 1
                di.gc.fillPolygon
                (
                    new double[]{di.x+(di.cell_width*2/5),di.x+(di.cell_width*2/5),di.x+(di.cell_width*3/10),di.x+(di.cell_width*3/10)}, 
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height/10),di.y+(di.cell_height/10),di.y+(di.cell_height/5)}, 
                    4
                );
                
                //bordo cubo alto 1
                di.gc.strokePolygon
                (
                    new double[]{di.x+(di.cell_width*2/5),di.x+(di.cell_width*2/5),di.x+(di.cell_width*3/10),di.x+(di.cell_width*3/10)}, 
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height/10),di.y+(di.cell_height/10),di.y+(di.cell_height/5)}, 
                    4
                );
                        
                //cubo alto 2
                di.gc.fillPolygon
                (
                    new double[]{di.x+(di.cell_width*3/5),di.x+(di.cell_width*7/10),di.x+(di.cell_width*7/10),di.x+(di.cell_width*3/5)}, 
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height/5),di.y+(di.cell_height/10),di.y+(di.cell_height/10)}, 
                    4
                );
                
                //bordo cubo alto 2
                di.gc.strokePolygon
                (
                    new double[]{di.x+(di.cell_width*3/5),di.x+(di.cell_width*7/10),di.x+(di.cell_width*7/10),di.x+(di.cell_width*3/5)}, 
                    new double[]{di.y+(di.cell_height/5),di.y+(di.cell_height/5),di.y+(di.cell_height/10),di.y+(di.cell_height/10)}, 
                    4
                );
            };
            
            putDrawPieceFuncion("ROOK", drawRook);   
    
            //Funzione di disegno cavallo        
            Consumer<DrawingInfo> drawKnight = di ->
            {           
                Point2D x1 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*4/5));
                Point2D x2 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height*4/5));
                Point2D x3 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height*7/10));
                Point2D x4 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*7/10));
               
                Point2D x5 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height*7/10));
                Point2D x6 = new Point2D(di.x+(di.cell_width*7/10),di.y+(di.cell_height*7/10));
                Point2D x7 = new Point2D(di.x+(di.cell_width*7/10),di.y+(di.cell_height/2));
               
                Point2D x8 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*3/10));
                Point2D x9 = new Point2D(di.x+(di.cell_width*7/10),di.y+(di.cell_height/8));
               
                Point2D x10 = new Point2D(di.x+(di.cell_width*9/10),di.y+(di.cell_height/2));
               
                di.gc.setFill(di.firstcolor); 
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor); 
                
                //base
                di.gc.fillPolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX(),x4.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY(),x4.getY()},
                    4
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX(),x4.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY(),x4.getY()},
                    4
                );
               
                //triangolo basso
                di.gc.fillPolygon
                (
                    new double[]{x5.getX(),x6.getX(),x7.getX()},
                    new double[]{x5.getY(),x6.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x5.getX(),x6.getX(),x7.getX()},
                    new double[]{x5.getY(),x6.getY(),x7.getY()},
                    3
                );
               
                //triangolo alto
                di.gc.fillPolygon
                (
                    new double[]{x7.getX(),x8.getX(),x9.getX()},
                    new double[]{x7.getY(),x8.getY(),x9.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x7.getX(),x8.getX(),x9.getX()},
                    new double[]{x7.getY(),x8.getY(),x9.getY()},
                    3
                );     
               
                //poligono laterale
                di.gc.fillPolygon
                (
                    new double[]{x2.getX(),x6.getX(),x9.getX(),x10.getX()},
                    new double[]{x2.getY(),x6.getY(),x9.getY(),x10.getY()},
                    4
                );     
               
                di.gc.strokePolygon
                (
                    new double[]{x2.getX(),x6.getX(),x9.getX(),x10.getX()},
                    new double[]{x2.getY(),x6.getY(),x9.getY(),x10.getY()},
                    4
                );     
            };
            
            putDrawPieceFuncion("KNIGHT", drawKnight);   
     
            //Funzione di disegno dell'alfiere        
            Consumer<DrawingInfo> drawBishop = di ->
            {
                Point2D x1 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height/5));
                Point2D x2 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*4/5));
                Point2D x3 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height*4/5));
               
                Point2D x4 = new Point2D(di.x+(di.cell_width*3/10),di.y+(di.cell_height/2));
                Point2D x5 = new Point2D(di.x+(di.cell_width*7/10),di.y+(di.cell_height/2));
                Point2D x6 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height/10));
               
                Point2D x7 = new Point2D(di.x+(di.cell_width*4/10),di.y+(di.cell_height*32/100));
                Point2D x8 = new Point2D(di.x+(di.cell_width*53/100),di.y+(di.cell_height*38/100));
               
                di.gc.setFill(di.firstcolor);
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor);
               
                //triangolo base
                di.gc.fillPolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3
                );
               
                //bordo triangolo base
                di.gc.strokePolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3
                );
               
                //triangolo testa
                di.gc.fillPolygon
                (
                    new double[]{x4.getX(),x5.getX(),x6.getX()},
                    new double[]{x4.getY(),x5.getY(),x6.getY()},
                    3
                );
               
                //bordo triangolo testa
                di.gc.strokePolygon
                (
                    new double[]{x4.getX(),x5.getX(),x6.getX()},
                    new double[]{x4.getY(),x5.getY(),x6.getY()},
                    3
                );
               
                //triangolo bocca
                di.gc.fillPolygon
                (
                    new double[]{x7.getX(),x8.getX(),x6.getX()},
                    new double[]{x7.getY(),x8.getY(),x6.getY()},
                    3
                );
               
                //bordo triangolo bocca
                di.gc.strokePolygon
                (
                    new double[]{x7.getX(),x8.getX(),x6.getX()},
                    new double[]{x7.getY(),x8.getY(),x6.getY()},
                    3
                );
            };
           
            putDrawPieceFuncion("BISHOP", drawBishop);
            
            //Funzione di disegno regina        
            Consumer<DrawingInfo> drawQueen = di ->
            {            
                Point2D x1 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*4/5));
                Point2D x2 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height*4/5));
                Point2D x3 = new Point2D(di.x+(di.cell_width*34/100),di.y+(di.cell_height/2));
               
                Point2D x4 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height/5));
                Point2D x5 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height/5));
               
                Point2D x6 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height/5));
                Point2D x7 = new Point2D(di.x+(di.cell_width*66/100),di.y+(di.cell_height/2));
                Point2D x8 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height*4/5));
               
                di.gc.setFill(di.firstcolor); 
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor);
                
                //triangolo in basso sinistra
                di.gc.fillPolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3          
                );     
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo centrale giu
                di.gc.fillPolygon
                (
                    new double[]{x2.getX(),x3.getX(),x7.getX()},
                    new double[]{x2.getY(),x3.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x2.getX(),x3.getX(),x7.getX()},
                    new double[]{x2.getY(),x3.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
         
                //triangolo in basso destra
                di.gc.fillPolygon
                (
                    new double[]{x2.getX(),x7.getX(),x8.getX()},
                    new double[]{x2.getY(),x7.getY(),x8.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x2.getX(),x7.getX(),x8.getX()},
                    new double[]{x2.getY(),x7.getY(),x8.getY()},
                    3          
                );
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo in alto sinistra
                di.gc.fillPolygon
                (
                    new double[]{x3.getX(),x4.getX(),x5.getX()},
                    new double[]{x3.getY(),x4.getY(),x5.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x3.getX(),x4.getX(),x5.getX()},
                    new double[]{x3.getY(),x4.getY(),x5.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
                
                //triangolo centrale su
                di.gc.fillPolygon
                (
                    new double[]{x4.getX(),x3.getX(),x7.getX()},
                    new double[]{x4.getY(),x3.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x4.getX(),x3.getX(),x7.getX()},
                    new double[]{x4.getY(),x3.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo in alto destra
                di.gc.fillPolygon
                (
                    new double[]{x4.getX(),x6.getX(),x7.getX()},
                    new double[]{x4.getY(),x6.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x4.getX(),x6.getX(),x7.getX()},
                    new double[]{x4.getY(),x6.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
                
                //testa
               
                //corona
                di.gc.fillPolygon
                (
                    new double[]{(x4.getX()-di.cell_width/10),(x4.getX()+di.cell_width/10),(x4.getX()-di.cell_width/10),(x4.getX()+di.cell_width/10)},
                    new double[]{(x4.getY()-di.cell_width/10),(x4.getY()-di.cell_width/10),(x4.getY()+di.cell_width/10),(x4.getY()+di.cell_width/10)},
                    4
                );
                
                di.gc.strokePolygon
                (
                    new double[]{(x4.getX()-di.cell_width/10),(x4.getX()+di.cell_width/10),(x4.getX()-di.cell_width/10),(x4.getX()+di.cell_width/10)},
                    new double[]{(x4.getY()-di.cell_width/10),(x4.getY()-di.cell_width/10),(x4.getY()+di.cell_width/10),(x4.getY()+di.cell_width/10)},
                    4       
                );
                
                //pallino grande
                di.gc.fillOval(di.x+(di.cell_width-di.cell_width/4)/2, di.y+(di.cell_width-di.cell_height/4)/5, di.cell_width/4, di.cell_height/4);
                //bordo pallino grande
                di.gc.strokeOval(di.x+(di.cell_width-di.cell_width/4)/2, di.y+(di.cell_width-di.cell_height/4)/5, di.cell_width/4, di.cell_height/4);
            };
            
            putDrawPieceFuncion("QUEEN", drawQueen);
        
            //Funzione di disegno re        
            Consumer<DrawingInfo> drawKing = di ->
            {            
                Point2D x1 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height*4/5));
                Point2D x2 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height*4/5));
                Point2D x3 = new Point2D(di.x+(di.cell_width*34/100),di.y+(di.cell_height/2));
               
                Point2D x4 = new Point2D(di.x+(di.cell_width/2),di.y+(di.cell_height/5));
                Point2D x5 = new Point2D(di.x+(di.cell_width/5),di.y+(di.cell_height/5));
               
                Point2D x6 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height/5));
                Point2D x7 = new Point2D(di.x+(di.cell_width*66/100),di.y+(di.cell_height/2));
                Point2D x8 = new Point2D(di.x+(di.cell_width*4/5),di.y+(di.cell_height*4/5));
                
                //croce 1
                Point2D x9 = new Point2D(di.x+(di.cell_width*4/10),di.y+(di.cell_height*17/100));
                Point2D x10 = new Point2D(di.x+(di.cell_width*4/10),di.y+(di.cell_height*23/100));
                
                Point2D x11 = new Point2D(di.x+(di.cell_width*6/10),di.y+(di.cell_height*17/100));
                Point2D x12 = new Point2D(di.x+(di.cell_width*6/10),di.y+(di.cell_height*23/100));
                
                //croce 2
                                
                Point2D x13 = new Point2D(di.x+(di.cell_width*47/100),di.y+(di.cell_height/10));
                Point2D x14 = new Point2D(di.x+(di.cell_width*47/100),di.y+(di.cell_height*3/10));
                
                Point2D x15 = new Point2D(di.x+(di.cell_width*53/100),di.y+(di.cell_height/10));
                Point2D x16 = new Point2D(di.x+(di.cell_width*53/100),di.y+(di.cell_height*3/10));
                 
                di.gc.setFill(di.firstcolor); 
                di.gc.setLineWidth(1);
                di.gc.setStroke(di.secondcolor);
                
                //triangolo in basso sinistra
                di.gc.fillPolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x1.getX(),x2.getX(),x3.getX()},
                    new double[]{x1.getY(),x2.getY(),x3.getY()},
                    3          
                );     
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo centrale giu
                di.gc.fillPolygon
                (
                    new double[]{x2.getX(),x3.getX(),x7.getX()},
                    new double[]{x2.getY(),x3.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x2.getX(),x3.getX(),x7.getX()},
                    new double[]{x2.getY(),x3.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
         
                //triangolo in basso destra
                di.gc.fillPolygon
                (
                    new double[]{x2.getX(),x7.getX(),x8.getX()},
                    new double[]{x2.getY(),x7.getY(),x8.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x2.getX(),x7.getX(),x8.getX()},
                    new double[]{x2.getY(),x7.getY(),x8.getY()},
                    3          
                );
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo in alto sinistra
                di.gc.fillPolygon
                (
                    new double[]{x3.getX(),x4.getX(),x5.getX()},
                    new double[]{x3.getY(),x4.getY(),x5.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x3.getX(),x4.getX(),x5.getX()},
                    new double[]{x3.getY(),x4.getY(),x5.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
                
                //triangolo centrale su
                di.gc.fillPolygon
                (
                    new double[]{x4.getX(),x3.getX(),x7.getX()},
                    new double[]{x4.getY(),x3.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x4.getX(),x3.getX(),x7.getX()},
                    new double[]{x4.getY(),x3.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.secondcolor); 
                di.gc.setStroke(di.firstcolor);
                
                //triangolo in alto destra
                di.gc.fillPolygon
                (
                    new double[]{x4.getX(),x6.getX(),x7.getX()},
                    new double[]{x4.getY(),x6.getY(),x7.getY()},
                    3
                );
               
                di.gc.strokePolygon
                (
                    new double[]{x4.getX(),x6.getX(),x7.getX()},
                    new double[]{x4.getY(),x6.getY(),x7.getY()},
                    3          
                );
                
                di.gc.setFill(di.firstcolor); 
                di.gc.setStroke(di.secondcolor);
                
                //testa
                
                di.gc.fillPolygon
                (
                    new double[]{x9.getX(),x10.getX(),x12.getX(),x11.getX()},
                    new double[]{x9.getY(),x10.getY(),x12.getY(),x11.getY()},
                    4
                );
                
                di.gc.strokePolygon
                (
                    new double[]{x9.getX(),x10.getX(),x12.getX(),x11.getX()},
                    new double[]{x9.getY(),x10.getY(),x12.getY(),x11.getY()},
                    4          
                );
                
                di.gc.fillPolygon
                (
                    new double[]{x13.getX(),x14.getX(),x16.getX(),x15.getX()},
                    new double[]{x13.getY(),x14.getY(),x16.getY(),x15.getY()},
                    4
                );
                
                di.gc.strokePolygon
                (
                    new double[]{x13.getX(),x14.getX(),x16.getX(),x15.getX()},
                    new double[]{x13.getY(),x14.getY(),x16.getY(),x15.getY()},
                    4          
                );
            };
            
            putDrawPieceFuncion("KING", drawKing);
        }
    }
}
