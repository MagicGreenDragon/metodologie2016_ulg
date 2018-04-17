package gapp.ulg.test;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.Action;
import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.Board.Dir;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Move.Kind;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Player;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.board.PieceModel.Species;
import gapp.ulg.game.util.BoardOct;
import gapp.ulg.game.util.PlayerMoveChooser;
import gapp.ulg.game.util.Utils;
import gapp.ulg.games.MNKgameFactory;
import gapp.ulg.games.OthelloFactory;
import gapp.ulg.play.MCTSPlayerFactory;
import gapp.ulg.play.OptimalPlayerFactory;
import gapp.ulg.play.RandPlayerFactory;

/**
 * Classe per test vari.
 * 
 * @author Daniele Giudice
 */
public class Test
{
	// Flag debug
	private static boolean debug;
	private static boolean debug_game;
	private static boolean debug_move_chooser;
	
	// Parametri timeout
	private static long move_timeout_margin;
	private static String move_timeout_str;
	private static long move_timeout;
	
	// Nomi giocatori
	private static String p1;
	private static String p2;
	
	// File di log
	private static String log_file;
	
	// Directory PlayerFactory
	private static Path dir_player_factory;
	
	// Fork-Join Pool
	private static ForkJoinPool fj;
	
	// Test main
	public static void main(String...args)
	{
		// Flag debug
		debug = true;
		debug_game = true;
		debug_move_chooser = true;
		
		// Parametri timeout
		move_timeout_margin = 20;
		move_timeout_str = "1s";
		
		// Nomi giocatori
		p1 = "A";
		p2 = "B";
		
		long t = Long.parseLong(move_timeout_str.substring(0, move_timeout_str.length()-1));
		move_timeout = (move_timeout_str.substring(move_timeout_str.length()-1).equals("s") ? t : t*60)*1000;
		
		// File di log
		log_file = "C:\\Users\\daniele\\Documents\\log.txt";
		
		// Directory PlayerFactory
		dir_player_factory = Paths.get("C:\\Users\\daniele\\Documents", "a");
		
		// Redirect output to file (non funziona con output lunghi)
		//setOutputToLogFile();
		
		// Funzioni di test
		//main_testGamePlayers();
		main_testMoveChooser();
	}
	
	// -------------------------------- TEST MOVE CHOOSER ---------------------------------------------------
	
	// Testa il MoveChooser
	public static void main_testMoveChooser()
	{
		// Test con GameRuler reali
		//test_RealGameRuler();
	    
	    // Test con insieme delle mosse della dama
		//dama_ValidMoves();
	    
	    // Test con insieme delle mosse scelto ad-hoc
		hoc_ValidMoves();
	}
	
	public static void test_RealGameRuler()
	{
		System.out.println("###### Test con GameRuler reali ######\n");
		
		// Variabili gameFactory
		GameFactory<GameRuler<PieceModel<Species>>> gF;
	    
	    // Test integrità con gameruler MNK
	    gF = getMNKFactory(20, 20, 20);
	    playGame( gF, getRandomPlayer(p1, gF, null), getRandomPlayer(p2, gF, null) );
	    
	    // Test integrità con gameruler Othello
	    gF = getOthelloFactory("12x12");
	    playGame( gF, getRandomPlayer(p1, gF, null), getRandomPlayer(p2, gF, null) );
	}
	
	public static void dama_ValidMoves()
	{
		System.out.println("###### Test con insieme delle mosse della dama ######\n");
		
		PieceModel<Species> pn = new PieceModel<>(Species.DISC, "nero");
		PieceModel<Species> pb = new PieceModel<>(Species.DISC, "bianco");
		
		Set<Move<PieceModel<Species>>> vm = new HashSet<>();
	    
	    Action<PieceModel<Species>> a1, a2, a3, a4;
	    Pos[] remove_list;
	    
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(6,4));
	    a3 = new Action<>(new Pos(6,4), new Pos(4,6));
	    remove_list = new Pos[] {new Pos(3,1), new Pos(5,3), new Pos(5,5)};
	    a4 = new Action<>(remove_list);
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(2,4), new Pos(0,6));
	    remove_list = new Pos[] {new Pos(3,1), new Pos(3,3), new Pos(1,5)};
	    a4 = new Action<>(remove_list);
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(2,4), new Pos(0,2));
	    remove_list = new Pos[] {new Pos(3,1), new Pos(3,3), new Pos(1,3)};
	    a4 = new Action<>(remove_list);
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    vm.add( new Move<>( Kind.RESIGN ) );
	    
	    // Creo una board fittizia (compatibile con l'insieme delle mosse)
	    Board<PieceModel<Species>> board = new BoardOct<>(8,8);
	    board.put(pn, new Pos(2,0));
	    board.put(pb, new Pos(3,1));
	    board.put(pb, new Pos(3,3));
	    board.put(pb, new Pos(1,3));
	    board.put(pb, new Pos(5,3));
	    board.put(pb, new Pos(5,5));
	    board.put(pb, new Pos(1,5));
	    
	    PlayerMoveChooser<PieceModel<Species>> mc = null; //new PlayerMoveChooser<>(board, vm);
	    
	    System.out.println("\n#### Test metodi albero ####");
	    
	    // Nodo radice
	    printCurrentNodeInfo(mc);
	    
	    // Selezione ramo JUMP
	    updateCurrentSelection(mc, new Pos(4,2));
	    System.out.println("# Jump selection -> "+mc.jumpSelection(new Pos(2,4))); //Inserisco la posizione di arrivo, ricavabile dalla mossa selezionata
	    
	    printCurrentNodeInfo(mc);
	}
	
	public static void hoc_ValidMoves()
	{
		/*
		
		- ALBERO DELLE MOSSE CORRETTO
		
		             1  
		     2       2      2  
		  4  3  3 | 3  4 | 3  3  
		   |5 4| |    | |   |4 5  
		     |5 5|            |6 6
		
		*/
		
		System.out.println("###### Test con insieme delle mosse scelto ad-hoc ######\n");
		
		PieceModel<Species> pn = new PieceModel<>(Species.DISC, "nero");
		PieceModel<Species> pb = new PieceModel<>(Species.DISC, "bianco");
		
		Set<Move<PieceModel<Species>>> vm = new HashSet<>();
	    
		Action<PieceModel<Species>> a1, a2, a3, a4, a5, a6;
	    
	    // Mossa 1
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(2,4), new Pos(7,7));
	    a4 = new Action<>(new Pos(6,7), pb);
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    // Mossa 2
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(0,0), pn);
	    a4 = new Action<>(pb, new Pos(2,2));
	    a5 = new Action<>(new Pos(6,5), pn);
	    vm.add( new Move<>( a1, a2, a3, a4, a5 ) );
	    
	    // Mossa 3
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(0,0), pn);
	    a4 = new Action<>(new Pos(0,0), new Pos(1,1));
	    a5 = new Action<>(new Pos(1,1));
	    vm.add( new Move<>( a1, a2, a3, a4, a5 ) );
	    
	    // Mossa 4
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(0,0), pn);
	    a4 = new Action<>(new Pos(0,0), new Pos(1,1));
	    a5 = new Action<>(Dir.UP, 3, new Pos(1,1));
	    vm.add( new Move<>( a1, a2, a3, a4, a5 ) );
	    
	    // Mossa 5
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(pn, new Pos(3,2));
	    vm.add( new Move<>( a1, a2, a3 ) );
	    
	    // Mossa 6
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(2,2));
	    a3 = new Action<>(new Pos(3,2), pb);
	    vm.add( new Move<>( a1, a2, a3 ) );
	    
	    // Mossa 7
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(2,2));
	    a3 = new Action<>(pb, new Pos(4,5));
	    a4 = new Action<>(new Pos(7,7), new Pos(0,0));
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    // Mossa 8
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,4), pn);
	    a3 = new Action<>(new Pos(5,5));
	    vm.add( new Move<>( a1, a2, a3 ) );
	    
	    // Mossa 9
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,4), pn);
	    a3 = new Action<>(Dir.UP, 2, new Pos(4,4));
	    a4 = new Action<>(Dir.DOWN, 7, new Pos(7,7));
	    vm.add( new Move<>( a1, a2, a3, a4 ) );
	    
	    // Mossa 10
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,4), pn);
	    a3 = new Action<>(Dir.UP, 2, new Pos(4,4));
	    a4 = new Action<>(new Pos(6,6), pn);
	    a5 = new Action<>(pn, new Pos(7,7));
	    a6 = new Action<>(Dir.DOWN, 2, new Pos(5,5));
	    vm.add( new Move<>( a1, a2, a3, a4, a5, a6 ) );
	    
	    // Mossa 11
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,4), pn);
	    a3 = new Action<>(Dir.UP, 2, new Pos(4,4));
	    a4 = new Action<>(new Pos(6,6), pn);
	    a5 = new Action<>(pn, new Pos(7,7));
	    a6 = new Action<>(new Pos(2,2), new Pos(3,3));
	    vm.add( new Move<>( a1, a2, a3, a4, a5, a6 ) );
	    
	    // Mossa 12 (finale non foglia)
	    a1 = new Action<>(new Pos(2,0), new Pos(4,2));
	    a2 = new Action<>(new Pos(4,2), new Pos(2,4));
	    a3 = new Action<>(new Pos(0,0), pn);
	    a4 = new Action<>(new Pos(0,0), new Pos(1,1));
	    vm.add( new Move<>( a1, a2, a3, a4) );
	    
	    vm.add( new Move<>( Kind.RESIGN ) );
	    
	    // Creo una board fittizia (compatibile con l'insieme delle mosse)
	    Board<PieceModel<Species>> board = new BoardOct<>(8,8);
	    board.put(pn, new Pos(2,0));
	    board.put(pn, new Pos(2,2));
	    board.put(pb, new Pos(3,2));
	    board.put(pb, new Pos(7,7));
	    board.put(pn, new Pos(5,5));
	    
	    PlayerMoveChooser<PieceModel<Species>> mc = null; //new PlayerMoveChooser<>(board, vm);
	    
	    System.out.println("\n#### Test metodi albero ####");
	    
	    // Nodo radice
	    printCurrentNodeInfo(mc);
	    
	    // --- Test ramo JUMP ---
	    
	    // Selezione ramo JUMP
	    updateCurrentSelection(mc, new Pos(4,2));
	    System.out.println("# Jump selection -> "+mc.jumpSelection(new Pos(2,4))); //Inserisco la posizione di arrivo, ricavabile dalla mossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    // Selezione ramo JUMP
	    updateCurrentSelection(mc, new Pos(2,4));
	    System.out.println("# Jump selection -> "+mc.jumpSelection(new Pos(7,7))); //Inserisco la posizione di arrivo, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    doBack(mc);
	    
	    // Selezione ramo swap
	    updateCurrentSelection(mc, new Pos(3,2));
	    System.out.println("# Do selection -> "+mc.doSelection(pn)); //Inserico il pezzo della mossa swap, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    doBack(mc);
	    doBack(mc); //Con questo sono alla radice
	    
	    // --- Test ramo REMOVE ---
	    
	    // Selezione ramo remove
	    updateCurrentSelection(mc, new Pos(2,2));
	    System.out.println("# Do selection -> "+mc.doSelection(null)); //Inserico il pezzo della mossa swap, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    // Selezione ramo add
	    updateCurrentSelection(mc, new Pos(3,2));
	    //System.out.println("# selectionPieces: "+mc.selectionPieces()); // TEST selectionPieces
	    //System.out.println("# quasiSelected: "+mc.quasiSelected()); // TEST quasiSelected	
	    System.out.println("# Do selection -> "+mc.doSelection(pb)); //Inserico il pezzo della mossa add, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    doBack(mc);
	    doBack(mc); //Con questo sono alla radice
	    
	    // --- Test ramo ADD ---
	    
	    // Selezione ramo add
	    updateCurrentSelection(mc, new Pos(4,4));
	    System.out.println("# Do selection -> "+mc.doSelection(pn)); //Inserico il pezzo della mossa swap, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    // Selezione ramo move
	    updateCurrentSelection(mc, new Pos(4,4));
	    System.out.println("# Move selection -> "+mc.moveSelection(Board.Dir.UP, 2)); //Inserico il pezzo della mossa swap, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    // Selezione ramo move
	    updateCurrentSelection(mc, new Pos(7,7));
	    System.out.println("# Move selection -> "+mc.moveSelection(Board.Dir.DOWN, 7)); //Inserico il pezzo della mossa swap, ricavabile dalla sottomossa selezionata
	    
	    printCurrentNodeInfo(mc);
	    
	    doBack(mc);
	    
	    /*
	    // Selezione mossa
	    mc.move();
	    System.out.println("\n# Mossa finale: "+mc.getMoveChoosed());
	    */
	    /*
	    // Controllo distruzione MoveChooser
	    mc.back();
	    mc.childrenSubMoves();
	    mc.clearSelection();
	    mc.doSelection(null);
	    mc.isFinal();
	    mc.jumpSelection(null);
	    mc.mayPass();
	    mc.move();
	    mc.moveSelection(null, 2);
	    mc.pass();
	    mc.quasiSelected();
	    mc.resign();
	    */
	}
	
	private static void printCurrentNodeInfo(PlayerMoveChooser<PieceModel<Species>> mc)
	{
		System.out.println("\n# Nodo attuale " + (mc.isFinal() ? "(FINALE)" : "(NON FINALE)") + ":");
		System.out.println("Sottomossa: "+(mc.subMove().isPresent() ? mc.subMove().get() : "NONE"));
		
		if( !mc.isFinal() )
		{
			System.out.println("Sottomosse nodi figli:");
			for( Move<PieceModel<Species>> m : mc.childrenSubMoves() )
				System.out.println("- " + m);
		}
	}
	
	private static void doBack(PlayerMoveChooser<PieceModel<Species>> mc)
	{
		System.out.println("\n# Back:");
	    System.out.println("# Sotto-mossa inversa: "+mc.back());
	    printCurrentNodeInfo(mc);
	}
	
	private static void updateCurrentSelection(PlayerMoveChooser<PieceModel<Species>> mc, Pos... pp)
	{
		System.out.println("# Mosse selezionate da "+Arrays.asList(pp)+":");
	    
		List<Move<PieceModel<Species>>> sub_moves_selected = mc.select(pp);
	    
	    if(sub_moves_selected.isEmpty())
	    	System.out.println("- Nessuna");
	    else
	    {
	    	for(Move<PieceModel<Species>> m : sub_moves_selected )
		    	System.out.println("- "+m);
	    }
	}
	
	// -------------------------------- TEST PLAYERS ---------------------------------------------------
	
	// Esegue una partita
	public static void main_testGamePlayers()
	{
		/*
		 * - ATTENZIONE:
		 * NON UTILIZZARE IL 'SequentialPlayer' PER GIOCARE A 'Camelot' (VA IN LOOP INFINITO)
		 */
		
		// - Variabili gameFactory
		GameFactory<GameRuler<PieceModel<Species>>> gF;
		
	    // - Limiti threads
		int maxTh = 2;
	    fj = new ForkJoinPool(2);
	    ExecutorService ex = Executors.newFixedThreadPool(4);
		
	    threadLimits tl = new threadLimits(true, maxTh, fj, ex);
	    
	    // - Selezione gioco
	    int game_index = 1;
	    
	    switch(game_index)
	    {
	    	// Giochi reali
	    	case 1: gF = getOthelloFactory("8x8");
	    		break;
	    	case 2: gF = getMNKFactory(20, 20, 2);
	    		break;
	    	case 3: gF = getBreakthroughFactory(8, 8);
	    		break;
	    	case 4: gF = getCamelotFactory();
	    		break;
	    	
	    	// Giochi di test
	    	case 5: gF = getSwapGameFactory();
	    		break;
	    	case 6: gF = getAddGameFactory();
	    		break;
	    	
	    	// Gioco di default
	    	default: gF = getOthelloFactory("12x12");
	    }
	    
	    // - Selezione giocatori
	    int player_index = 3;
	    
	    switch(player_index)
	    {
	    	// Coppie giocatori uguali
	    	case 1: playGame( gF, 
		    			getRandomPlayer(p1, gF, tl), 
		    			getRandomPlayer(p2, gF, tl) 
		    		);
	    		break;
	    	case 2: playGame( gF, 
			    		getSequentialPlayer(p1, gF, tl), 
			    		getSequentialPlayer(p2, gF, tl) 
		    		);
	    		break;
	    	case 3: playGame( gF, 
		    			getMontecarloPlayer(p1, gF, 100, "Parallel", tl), 
		    			getMontecarloPlayer(p2, gF, 100, "Parallel", tl) 
		    		);
	    		break;
	    	case 4: playGame( gF, 
		    			getOptimalPlayer(p1, gF, "Sequential", true, false, tl), 
		    			getOptimalPlayer(p2, gF, "Sequential", true, false, tl) 
		    		);
	    		break;
	    	
	    	// Coppie giocatori misti (SequentialPlayer escluso)
	    	case 5: playGame( gF, 
		    			getMontecarloPlayer(p1, gF, 100, "Sequential", tl), 
		    			getRandomPlayer(p2, gF, tl) 
		    		);
	    		break;
	    	case 6: playGame( gF, 
		    			getOptimalPlayer(p1, gF, "Sequential", true, false, tl), 
		    			getRandomPlayer(p2, gF, tl) 
		    		);
	    		break;
	    	case 7: playGame( gF, 
		    			getOptimalPlayer(p1, gF, "Sequential", true, false, tl), 
		    			getMontecarloPlayer(p2, gF, 100, "Parallel", tl) 
		    		);
    			break;
	    	
    		// Gioco di default
	    	default: playGame( gF, 
		    			getRandomPlayer(p1, gF, tl), 
		    			getRandomPlayer(p2, gF, tl) 
		    		);
	    }
	    
	    // - Spegne tutti gli esecutori
	    if( fj != null )
	    	fj.shutdownNow();
	    if( ex != null )
	    	ex.shutdownNow();
	    
	    try
	    {
	    	fj.awaitTermination(40, TimeUnit.MILLISECONDS);
	    	ex.awaitTermination(40, TimeUnit.MILLISECONDS);
		}
	    catch (InterruptedException e)
	    {
			e.printStackTrace();
		}
	    
	    System.out.println("\nStato esecutori:");
	    System.out.println("- Fork Join Pool: " + fj);
	    System.out.println("- BG Executor service: " + ex);
	    
	    System.out.println("\nThreads attivi: ");
	    for( Thread t : Thread.getAllStackTraces().keySet() )
	    {
	    	System.out.println("- "+t.getName());
	    }
	}
	
	// Classe limiti threds
 	public static class threadLimits
 	{
 		private boolean useLimits;
 		private int maxTh;
 		private ForkJoinPool fjp;
 		private ExecutorService bgExec;
 		
 		public threadLimits(boolean useLimits, int maxTh, ForkJoinPool fjp, ExecutorService bgExec)
 		{
 			this.useLimits = useLimits;
 			this.maxTh = maxTh;
 			this.fjp = fjp;
 			this.bgExec = bgExec;
 		}
 		
 		public void applyLimits(Player<PieceModel<Species>> p)
 		{
 			if( p!=null && this.useLimits )
 				p.threads(this.maxTh, this.fjp, this.bgExec);
 		}
 	}
	
	// Esecutore gioco
	
	public static void playGame(GameFactory<GameRuler<PieceModel<Species>>> gF, Player<PieceModel<Species>> p1, Player<PieceModel<Species>> p2)
	{
		if( p1==null || p2==null )
		{
			System.out.println("\n!!!! Partita annullata !!!!");
			return;
		}
		
		String name1 = p1.name();
		String name2 = p2.name();
		
		System.out.println("\n# '"+p1.getClass().getSimpleName()+"'("+name1+") VS '"+p2.getClass().getSimpleName()+"'("+name2+")");
		
		long st = System.currentTimeMillis();
		
		System.out.println("#### Inizio Partita ####");
		
		GameRuler<PieceModel<Species>> gr = debug ? playDebug(gF, p1, p2) : Utils.play(gF, p1, p2);
		
		System.out.println("#### Fine Partita ####");
		
		// Se previsti dal gioco, visualizza i punteggi
		try
		{
			double score_a = gr.score(1);
			double score_b = gr.score(2);
			
			System.out.println("- Score "+name1+": "+score_a);
			System.out.println("- Score "+name2+": "+score_b);
			
		}
		catch(UnsupportedOperationException e){}
		
		// Mostra il risultato della partita
		switch( gr.result() )
		{
			case 0: System.out.println("- Vincitore: Nessuno");
				break;
			case 1: System.out.println("- Vincitore: "+name1);
				break;
			case 2: System.out.println("- Vincitore: "+name2);
				break;
			default: System.out.println("- FATAL ERROR!! ");
		}
		
		System.out.println("- Tempo partita: " + (System.currentTimeMillis()-st) + " ms");
	}
	
	// Metodo play con debug
	
    @SuppressWarnings("unchecked")
	@SafeVarargs
	public static <P> GameRuler<P> playDebug(GameFactory<? extends GameRuler<P>> gf, Player<P>...pp )
    {
    	Objects.requireNonNull(gf);
    	Objects.requireNonNull(pp);
    	
        for( Player<P> player : pp )
        {
        	if( player == null )
        		throw new NullPointerException();
        }
        
        if( pp.length < gf.minPlayers() || pp.length > gf.maxPlayers() )
        	throw new IllegalArgumentException();
        
        // Ottengo i nomi dei giocatori (in ordine)
        String[] names = new String[pp.length];
        for( int i=0 ; i<pp.length ; i++ )
        	names[i] = pp[i].name();
        
        // Li setto nel GameFactory
        gf.setPlayerNames(names);
        
        // Ottengo il GameRuler
        GameRuler<P> gr = gf.newGame();
        
        // Passo a tutti i giocatori una copia del GameRuler
        for( Player<P> player : pp )
        	player.setGame( gr.copy() );
        
        // Ottengo i colori dei giocatori (in ordine)
        String[] player_colors = new String[names.length+1];
        for( int i=1 ; i<=names.length ; i++ )
        	player_colors[i] = gr.color(names[i-1]);
        
        // Indice di turnazione del giocatore
        int player_id;
        
        Move<P> m;
        
        Function<Board<PieceModel<PieceModel.Species>>,String> boardToStr = Utils.BoardToString(Utils.PieceModelToString());
        
        int turno = 0;
        
        boolean timeout_warning = false, fj_warning = false;
        
        /* Gioca la partita */
        while( gr.result() == -1 )
        {
        	// Aggiorna l'indice di turnazione
        	player_id = gr.turn();
        	
        	turno++;
        	
        	if( debug_game )
        	{
        		java.lang.System.out.println("- Turno " + turno + " (" + player_colors[gr.turn()] + "):");
        	}
        	
        	long st = System.currentTimeMillis();
        	// Ottieni la mossa del giocatore attuale (con tempo di esecuzione)
        	m = pp[player_id-1].getMove();
        	long ft = System.currentTimeMillis()-st;
        	
        	// Avvisa se rimangono esecuzioni residue nel Fork-Join Pool
        	if( fj!=null && !fj.isQuiescent() )
        	{
        		System.err.println("ERROR: FORK-JOIN POOL IS STILL RUNNING!!!!");
        		fj_warning = true;
        	}
        	
        	if( debug_game )
        	{
        		if( !move_timeout_str.equals("No limit") && ft > move_timeout-move_timeout_margin )
            	{
            		timeout_warning = true;
            		System.err.println("Calcolato in: "+ ft + " ms");
            	}
            	else
            		System.out.println("Calcolato in: "+ ft + " ms");
        	}
        	
        	if( debug_game )
        	{
        		System.out.println(boardToStr.apply((Board<PieceModel<Species>>) gr.getBoard()));
                System.out.println("Mosse valide:");
                for( Move<P> mossa : gr.validMoves() )
                	System.out.println( mossa + (mossa.equals(m) ? " (Selected)" : "") );
                
                System.out.println();
        	}
        	
        	if( debug_move_chooser )
        	{
        		// Test integrità conversione ValidMoves -> MovesTree
        		new PlayerMoveChooser<>(gr);
        	}
        	
            // Esegui la mossa
        	gr.move(m);
            
			// Aggiorna tutti i GameRuler
        	for( Player<P> player : pp )
        		player.moved(player_id, m);
        }
        
        if( debug_game )
    	{
        	java.lang.System.out.println("- Board finale:");
            java.lang.System.out.println(boardToStr.apply((Board<PieceModel<Species>>) gr.getBoard()));
            
            if( timeout_warning )
            	System.err.println("TIMEOUT WARNING DETECTED!!!!");
            
            if( fj_warning )
            	System.err.println("FORK-JOIN ERROR DETECTED!!!!");
    	}
        
    	return gr;
    }
	
	// Giochi
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getOthelloFactory(String Size)
	{
		System.out.println("# Game: Othello");
		
		OthelloFactory gf = new OthelloFactory();
		
		gf.params().get(0).set(move_timeout_str);
	    gf.params().get(1).set(Size);
	    
	    // Test getParams GameRuler
	    gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time = "+gr.getParam("Time", String.class));
	    System.out.println("Board = "+gr.getParam("Board", String.class));
	    System.out.println();
	    
	    return gf;
	}
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getMNKFactory(int M, int N, int K)
	{
		System.out.println("# Game: MNK");
		
		MNKgameFactory gf = new MNKgameFactory();
		
	    gf.params().get(0).set(move_timeout_str);
	    gf.params().get(1).set(M);
	    gf.params().get(2).set(N);
	    gf.params().get(3).set(K);
	    
	    // Test getParams GameRuler
	    gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time = "+gr.getParam("Time", String.class));
	    System.out.println("M    = "+gr.getParam("M", Integer.class));
	    System.out.println("N    = "+gr.getParam("N", Integer.class));
	    System.out.println("K    = "+gr.getParam("K", Integer.class));
	    System.out.println();
	    
	    return gf;
	}
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getBreakthroughFactory(int width, int height)
	{
		System.out.println("# Game: Breakthrough");
		
		BreakthroughFactory gf = new BreakthroughFactory();
		
	    gf.params().get(0).set(move_timeout_str);
	    gf.params().get(1).set(width);
	    gf.params().get(2).set(height);
	    
	    // Test getParams GameRuler
	    gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time   = "+gr.getParam("Time", String.class));
	    System.out.println("Width  = "+gr.getParam("Width", Integer.class));
	    System.out.println("Height = "+gr.getParam("Height", Integer.class));
	    System.out.println();
	    
	    return gf;
	}
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getCamelotFactory()
	{
		System.out.println("# Game: Camelot");
		
		CamelotFactory gf = new CamelotFactory();
		
		gf.params().get(0).set(move_timeout_str);
		
	    // Test getParams GameRuler
		gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time = "+gr.getParam("Time", String.class));
	    System.out.println();
	    
	    return gf;
	}
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getSwapGameFactory()
	{
		System.out.println("# Game: Swap Game");
		
		SwapGameFactory gf = new SwapGameFactory();
		
		gf.params().get(0).set(move_timeout_str);
		
	    // Test getParams GameRuler
		gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time = "+gr.getParam("Time", String.class));
	    System.out.println();
	    
	    return gf;
	}
	
	public static GameFactory<GameRuler<PieceModel<Species>>> getAddGameFactory()
	{
		System.out.println("# Game: Add Game");
		
		AddGameFactory gf = new AddGameFactory();
		
		gf.params().get(0).set(move_timeout_str);
		
	    // Test getParams GameRuler
		gf.setPlayerNames(p1,p2);
	    GameRuler<PieceModel<Species>> gr = gf.newGame();
	    
	    System.out.println("- Parametri:");
	    System.out.println("Time = "+gr.getParam("Time", String.class));
	    System.out.println();
	    
	    return gf;
	}
	
	// Giocatori
	
	public static Player<PieceModel<Species>> getSequentialPlayer(String name, GameFactory<GameRuler<PieceModel<Species>>> gF, threadLimits tl)
	{
		SequentialPlayerFactory<PieceModel<Species>> pF = new SequentialPlayerFactory<>();
		
		Player<PieceModel<Species>> p = pF.newPlayer(gF, name);
		
		if( tl!=null )
			tl.applyLimits(p);
		
		return p;
	}
	
	public static Player<PieceModel<Species>> getRandomPlayer(String name, GameFactory<GameRuler<PieceModel<Species>>> gF, threadLimits tl)
	{
		RandPlayerFactory<PieceModel<Species>> pF = new RandPlayerFactory<>();
		
		Player<PieceModel<Species>> p = pF.newPlayer(gF, name);
		
		if( tl!=null )
			tl.applyLimits(p);
		
		return p;
	}
	
	public static Player<PieceModel<Species>> getMontecarloPlayer(
			String name, 
			GameFactory<GameRuler<PieceModel<Species>>> gF, 
			int rollout, 
			String execution, 
			threadLimits tl)
	{
		System.out.println("\n# Player '"+name+"': Montecarlo");
		
		MCTSPlayerFactory<PieceModel<Species>> pF = new MCTSPlayerFactory<>();
		
		pF.params().get(0).set(rollout);
	    pF.params().get(1).set(execution);
	    
	    System.out.println("- Parametri Player:");
	    System.out.println("Rollout   = "+pF.params().get(0).get());
	    System.out.println("Execution = "+pF.params().get(1).get());
	    System.out.println();
	    
	    Player<PieceModel<Species>> p = pF.newPlayer(gF, name);
	    
	    if( tl!=null )
			tl.applyLimits(p);
	    
	    return p;
	}
	
	public static Player<PieceModel<Species>> getOptimalPlayer(
			String name, 
			GameFactory<GameRuler<PieceModel<Species>>> gF, 
			String execution, 
			boolean dir, 
			boolean parallel, 
			threadLimits tl)
	{
		System.out.println("\n# Player '"+name+"': Optimal");
		
		OptimalPlayerFactory<PieceModel<Species>> opF = new OptimalPlayerFactory<>();
		
		opF.params().get(0).set(execution);
		
		System.out.println("- Parametri Player:");
	    System.out.println("Execution = "+opF.params().get(0).get());
	    System.out.println();
		
		// Setta la directory
		if( dir )
			opF.setDir(dir_player_factory);
		
		long st = System.currentTimeMillis();
		PlayerFactory.Play py = opF.canPlay(gF);
		System.out.println("canPlay() = "+py);
        if( py == PlayerFactory.Play.TRY_COMPUTE )
        {
        	String res = opF.tryCompute(gF, parallel, null);
        	if (res != null)
        	{
        		System.out.println("\ntryCompute fallisce, e ritorna: "+res);
        		System.out.println("\nTempo ottenimento strategia: " + (System.currentTimeMillis()-st) + " ms\n");
        		return null;
        	}
        	else
        	{
        		Player<PieceModel<Species>> p = opF.newPlayer(gF, name);
                
                System.out.println("\nTempo ottenimento strategia: " + (System.currentTimeMillis()-st) + " ms\n");
                
                if( tl!=null )
        			tl.applyLimits(p);
                
        	    return p;
        	}
        }
        else if( py == PlayerFactory.Play.YES )
        {
        	Player<PieceModel<Species>> p = opF.newPlayer(gF, name);
            
            System.out.println("\nTempo ottenimento strategia: " + (System.currentTimeMillis()-st) + " ms\n");
            
            if( tl!=null )
    			tl.applyLimits(p);
            
    	    return p;
        }
        
        System.out.println();
        
	    return null;
	}
	
	// -------------------------------- METODI DI UTILITA' ---------------------------------------------------
	
	protected static void printToFile(String text)
	{
		try
		{
			PrintWriter f = new PrintWriter(new FileWriter(log_file,true));
			f.println(text);
			f.flush();
			f.close();
		}
		catch(IOException e){}
	}
	
	protected static void setOutputToLogFile()
	{
		PrintStream log_file_stream;
		try
		{
			log_file_stream = new PrintStream(new BufferedOutputStream(new FileOutputStream(log_file, true)));
			System.setOut(log_file_stream);
			System.setErr(log_file_stream);
		}
		catch (IOException e) {}
	}
}
