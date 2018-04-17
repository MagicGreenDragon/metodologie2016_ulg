package gapp.gui;

import gapp.gui.util.ScenesManager;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 *  Classe principale dell'applicazione
 * 
 *  Ultima modifica: Sera - 31/08/2016
 */
public class Main extends Application
{       
	/** Metodo main dell'applicazione */
	public static void main(String[] args)
	{
	    launch(args);	    
	}

	@Override
	public void start(Stage primaryStage)
	{
	    //inizializzo gestore scene dell'interfaccia
	    ScenesManager scenesmanager = new ScenesManager(primaryStage);     
	 	 
	    //imposto prima scena
	    scenesmanager.changescenetoMainMenu();
	    
	    //mostro la prima schermata
	    primaryStage.show(); 
	}
}
