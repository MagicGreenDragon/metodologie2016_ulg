package gapp.ulg.game.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import gapp.ulg.game.board.Action;
import gapp.ulg.game.board.Board;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Board.Dir;
import gapp.ulg.game.board.Move;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.util.PlayerGUI.MoveChooser;

/** Classe che implementa l'ainterfaccia MoveChooser<P>.
 * @param <P> tipo di modello dei pezzi
 * 
 * @author Daniele Giudice
 */
public class PlayerMoveChooser<P> implements MoveChooser<P> 
{
	/** Classe che implementa un nodo dell'albero delle mosse.
	 * @param <P> tipo di modello dei pezzi
	 */
	private static class MTNode<P>
	{
		/** Contenuto del nodo */
		public final List<Action<P>> value;
		/** Nodo padre */
		public volatile MTNode<P> father;
		/** Insieme dei figli */
		public final Set<MTNode<P>> childrens;
		/** Flag che indica se il nodo è o meno finale */
		public volatile boolean isFinal;
		
		/** Crea un nuovo nodo.
		 * @param value contenuto del nodo (può anche essere vuoto)
		 * @param father nodo padre (nel caso della radice, è null)
		 * @param isFinal flag per indicare se il nodo è finale o no
		 */
		public MTNode(List<Action<P>> value, MTNode<P> father, boolean isFinal)
		{
			this.value = value==null ? new ArrayList<>() : value;
			this.father = father;
			this.childrens = new HashSet<>();
			this.isFinal = isFinal;
		}
		
		/** Aggiunge il nodo dato nell'insieme dei figli.
		 * @param n nodo da inserire
		 */
		public void addChild(MTNode<P> n)
		{
			this.childrens.add(n);
		}
		
		/** Crea un nuovo nodo e lo aggiunge nell'insieme dei figli del nodo corrente.
		 * @param value contenuto del nodo
		 * @param isFinal flag per indicare se il nodo è finale o no
		 */
		public void addChild(List<Action<P>> value, boolean isFinal)
		{
			this.childrens.add( new MTNode<>(value, this, isFinal) );
		}
		
		/** Rimuove il nodo dato dall'insieme dei figli (se presente).
		 * @param n nodo da rimuovere
		 */
		public void removeChild(MTNode<P> n)
		{
			this.childrens.remove(n);
		}
		
		/** Ritorna la sotto-mossa di questo nodo.
		 * @return un {@link Optional} con la sotto-mossa di questo nodo o un
         * {@link Optional} vuoto se questo nodo è la radice con prefisso vuoto
		 */
		public Optional<Move<P>> getSubMove()
		{
			if( this.isRoot() )
				return this.isEmpty() ? Optional.empty() : Optional.of(new Move<>(this.value));
			
			return Optional.of( new Move<>(this.value.subList(this.father.value.size(), this.value.size())) );
		}
		
		/** Ritorna la lista con le sotto-mosse di tutti i nodi figli di questo nodo. 
         * Se questo nodo è una foglia, ritorna la lista vuota. 
         * La lista ritornata è sempre creata ex novo.
         * @return la lista con le sotto-mosse di tutti i nodi figli di questo nodo
         */
		public List<Move<P>> getChildrenSubMoves()
		{
			List<Move<P>> subMoves = new ArrayList<>();
			
			if( this.isLeaf() )
				return subMoves;
			
			int value_size = this.value.size();
			
			for( MTNode<P> child : this.childrens )
				subMoves.add( new Move<>(child.value.subList(value_size, child.value.size())) );
			
			return subMoves;
		}
		
		/** Verifica se il nodo è vuoto.
		 * @return true se il nodo è vuoto, false altrimenti
		 */
		public boolean isEmpty() { return this.value.isEmpty(); }
		
		/** Verifica se il nodo è una foglia.
		 * @return true se il nodo è una foglia, false altrimenti
		 */
		public boolean isLeaf() { return this.childrens.isEmpty(); }
		
		/** Verifica se il nodo è la radice.
		 * @return true se il nodo è la radice dell'albero, false altrimenti
		 */
		public boolean isRoot() { return this.father==null; }
	}
	
	/** Mossa per passare il turno */
	private final Move<P> passMove = new Move<P>(Move.Kind.PASS);
	/** Mossa per arrendersi */
	private final Move<P> resignMove = new Move<P>(Move.Kind.RESIGN);
	
	/** Lista delle posizioni della board */
	private final List<Pos> boardPositions;
	
	/** History delle board interne del MoveChooser */
	private final Deque<Map<Pos,P>> boardHistory;
	
	/** Radice dell'albero delle mosse */
	private final MTNode<P> treeRoot;
	/** Nodo corrente */
	private volatile MTNode<P> currentNode;
	
	/** Insieme delle posizioni della selezione corrente */
	private volatile Set<Pos> currentSelection;
	/** Mossa scelta dal MoveChooser */
	private volatile Move<P> moveChoosed;
	
	/** Flag che indica se è possibile usare una mossa di tipo PASS */
	private final AtomicBoolean canPass;
	/** Flag che indica se questo MoveChooser è ancora utilizzabile */
	private final AtomicBoolean canUse;
	
	/** Crea un oggetto che implementa {@link MoveChooser} 
	 * @param gR {@link GameRuler} dello stato attuale del gioco
	 */
	public PlayerMoveChooser(GameRuler<P> gR)
	{
		// Ottenimento insieme delle mosse valide e relativa lista (senza le mosse PASS e RESIGN)
		Set<Move<P>> vm = gR.validMoves();
		List<Move<P>> lst_vm = new ArrayList<>(vm);
		lst_vm.remove(this.passMove);
		lst_vm.remove(this.resignMove);
		
		// Costruzione albero delle mosse
		if( lst_vm.isEmpty() )
		{
			// Se non ci sono mosse valide, l'albero è vuoto
			this.treeRoot = null;
		}
		else if( lst_vm.size() == 1  )
		{
			// Con una sola mossa valida, l'albero è composto solo dalla radice
			this.treeRoot = new MTNode<>( lst_vm.get(0).actions, null, true );
		}
		else
		{
			// Ottiene il prefisso comune a tutte le mosse
			List<Action<P>> prefix = this.getCommonPrefix(lst_vm);
			
			// La radice è finale solo se non è vuota o se il suo prefisso corrisponde ad una mossa valida
			boolean isFinal = prefix.size()==0 ? false : lst_vm.contains( new Move<>(prefix) );
			
			// Creo la radice dell'albero
			// Se il prefisso corrisponde esattamente ad una mossa, la radice sarà finale, altrimenti no
			this.treeRoot = new MTNode<>( prefix, null, isFinal );
			
			// Riempio l'albero
			for( Move<P> m : lst_vm )
				this.addNode(this.treeRoot, m.actions);
		}
		
		// Creazione prima board interna
		Map<Pos,P> board = new HashMap<>();
    	Board<P> board_view = gR.getBoard();
        for( Pos p : board_view.positions() )
            if( board_view.get(p) != null )
            	board.put(p, board_view.get(p));
        
        // Creazione history board
        this.boardHistory = new ConcurrentLinkedDeque<>();
        this.boardHistory.push(board);
        
        // Impostazione variabili interne
        this.boardPositions = board_view.positions();
		this.currentNode = this.treeRoot;
		this.currentSelection = new HashSet<>();
		this.moveChoosed = null;
		
		// Impostazione flag
		this.canPass = new AtomicBoolean( vm.contains(this.passMove) );
		this.canUse = new AtomicBoolean( true );
		
		// Esegue la sotto-mossa della radice (se presente)
		if( this.treeRoot!=null )
		{
			Optional<Move<P>> op_sub_move = this.treeRoot.getSubMove();
			if( op_sub_move.isPresent() )
				this.execActions(op_sub_move.get().actions);
		}
	}
	
	// ------------------------ Metodi ereditati da MoveChooser ------------------------
	
	@Override
	public synchronized Optional<Move<P>> subMove()
	{
		if( !this.canUse.get() )
			throw new IllegalStateException();
		
		if( this.treeRoot == null )
			return null;
		
		return this.currentNode.getSubMove();
	}

	@Override
	public synchronized List<Move<P>> childrenSubMoves()
	{
		if( !this.canUse.get() )
			throw new IllegalStateException();
		
		if( this.treeRoot == null )
			return null;
		
		return this.currentNode.getChildrenSubMoves();
	}

    @Override
    public synchronized List<Move<P>> select(Pos... pp)
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	Objects.requireNonNull(pp);
    	
    	// Controlla che le posizioni siano non nulle e che siano presenti nella board
    	for( Pos p : pp )
        {
        	if( p == null )
            	throw new NullPointerException();
        	
        	if( !this.boardPositions.contains(p) )
        		throw new IllegalArgumentException();
        }
    	
    	if( pp.length==0 || Utils.duplicates(pp) )
    		throw new IllegalArgumentException();
    	
    	// Non fa nulla se l'albero è vuoto
    	if( this.treeRoot==null )
			return null;
    	
    	// Posizioni della selezione corrente
    	this.currentSelection = new HashSet<Pos>(Arrays.asList(pp));
    	
    	return this.getSubMovesNodesSelected();
	}
    
    @Override
    public synchronized List<Move<P>> quasiSelected()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	// Non fa nulla se l'albero è vuoto
    	if( this.treeRoot==null )
			return null;
		
    	// Lista sottomosse dei nodi quasi-selezionati dalla selezione corrente
    	List<Move<P>> sub_moves_quasi_selected = new ArrayList<>();
    	
    	// Se la selezione corrente è vuota ritorna la lista vuota (l'insieme vuoto è sempre sottoinsieme improprio)
    	if( this.currentSelection.isEmpty() )
    		return sub_moves_quasi_selected;
    	
    	Move<P> child_sub_move;
    	Action<P> child_first_action;
    	Set<Pos> child_first_action_pos;
    	for( MTNode<P> child : this.currentNode.childrens )
    	{
    		child_sub_move = child.getSubMove().get();
    		child_first_action = child_sub_move.actions.get(0);
    		
    		// Se è una JUMP, controlla che non sia già selezionata
    		if( child_first_action.kind==Action.Kind.JUMP && this.currentSelection.size()==1 && this.currentSelection.contains(child_first_action.pos.get(0)) )
    			continue;
    		
    		// Se non è una JUMP, allora controlla l'insieme delle pos
    		child_first_action_pos = new HashSet<Pos>(child_first_action.pos);
    		if( !this.currentSelection.equals(child_first_action_pos) && child_first_action_pos.containsAll(this.currentSelection) )
    			sub_moves_quasi_selected.add( child_sub_move );
    	}
		
		return sub_moves_quasi_selected;
	}

    @Override
    public synchronized List<P> selectionPieces()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	if( this.treeRoot==null )
			return null;
		
    	// Lista delle sotto-mosse dei nodi selezionati
    	List<Move<P>> subMovesSelected = this.getSubMovesNodesSelected();
    	
    	// Lista dei pezzi delle prime azioni delle sottomosse selezionate della selezione corrente
    	List<P> piecesSelected = new ArrayList<>();
    	
    	// Se non ci sono sotto-mosse selezionate, ritorna la lista vuota
    	if( subMovesSelected.isEmpty() )
    		return piecesSelected;
    	
    	// Ottengo il tipo della prima action della prima sotto-mossa selezionata
		Action.Kind first_action_kind = subMovesSelected.get(0).actions.get(0).kind;
		
		if( first_action_kind == Action.Kind.ADD || first_action_kind == Action.Kind.SWAP )
		{
			// Uso un insieme per evitare le ripetizioni
			Set<P> temp_pieces = new HashSet<>();
			
			// Controlla che tutte le prime azioni delle sotto-mosse siano o ADD o SWAP
			boolean equal_flag = true;
			for( Move<P> sub_move : subMovesSelected )
			{
				if( sub_move.actions.get(0).kind != first_action_kind )
				{
					equal_flag = false;
					break;
				}
				
				temp_pieces.add(sub_move.actions.get(0).piece);
			}
			
			// Se lo sono, inserisci nella lista i tipi di pezzi coinvolti in tali azioni (senza ripetizioni)
			if( equal_flag )
				piecesSelected.addAll(temp_pieces);
		}
		else if( first_action_kind == Action.Kind.REMOVE && subMovesSelected.size() == 1 )
		{
			piecesSelected.add(null);
		}
		
		return piecesSelected;
	}

    @Override
    public synchronized void clearSelection()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
		
		this.currentSelection = new HashSet<>();
	}

    @Override
    public synchronized Move<P> doSelection(P pm)
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	List<MTNode<P>> nodesSelected = this.getNodesSelected();
    	
    	// Se non ci sono nodi selezionati, non fa nulla e ritorna null
    	if( nodesSelected.isEmpty() )
    		return null;
    	
		Action.Kind first_action_kind = nodesSelected.get(0).getSubMove().get().actions.get(0).kind;
		
		if( first_action_kind == Action.Kind.ADD || first_action_kind == Action.Kind.SWAP )
		{
			Map<P,MTNode<P>> piecesMap = new HashMap<>();
			
			boolean equal_flag = true;
			Move<P> sub_move;
			for( MTNode<P> nodeSelected : nodesSelected )
			{
				sub_move = nodeSelected.getSubMove().get();
				
				if( sub_move.actions.get(0).kind != first_action_kind )
				{
					equal_flag = false;
					break;
				}
				
				piecesMap.put(sub_move.actions.get(0).piece, nodeSelected);
			}
			
			if( equal_flag && piecesMap.containsKey(pm) )
			{
				this.currentNodeUpdate(piecesMap.get(pm));
    			
    			return this.currentNode.getSubMove().get();
			}
		}
		else if( nodesSelected.size() == 1 && first_action_kind == Action.Kind.REMOVE && pm == null )
		{
			this.currentNodeUpdate(nodesSelected.get(0));
			
			return this.currentNode.getSubMove().get();
		}
		
		return null;
	}

    @Override
    public synchronized Move<P> jumpSelection(Pos p)
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	Move<P> sub_move;
    	Action<P> first_action;
		for( MTNode<P> nodeSelected : this.getNodesSelected() )
		{
			sub_move = nodeSelected.getSubMove().get();
			first_action = sub_move.actions.get(0);
			
			if( first_action.kind==Action.Kind.JUMP && first_action.pos.get(1).equals(p) )
			{
				this.currentNodeUpdate(nodeSelected);
				return sub_move;
			}
		}
		
		return null;
	}

    @Override
    public synchronized Move<P> moveSelection(Board.Dir d, int ns)
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	Move<P> sub_move;
    	Action<P> first_action;
		for( MTNode<P> nodeSelected : this.getNodesSelected() )
		{
			sub_move = nodeSelected.getSubMove().get();
			first_action = sub_move.actions.get(0);
			
			if( first_action.kind==Action.Kind.MOVE && first_action.dir==d && first_action.steps==ns )
			{
				this.currentNodeUpdate(nodeSelected);
				return sub_move;
			}
		}
		
		return null;
	}

    @Override
    public synchronized Move<P> back()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
    	
    	// Se l'albero è vuoto o il nodo corrente è la radice non fa nulla
    	if( this.treeRoot==null || this.currentNode.isRoot() )
    		return null;
    	
    	// Ottengo la lista delle azioni della sotto-mossa e ne inverto l'ordine
    	List<Action<P>> current_sub_move_actions = this.currentNode.getSubMove().get().actions;
    	Collections.reverse(current_sub_move_actions);
    	
    	// Lista delle azioni della sotto-mossa inversa
    	List<Action<P>> reverse_actions = new ArrayList<>();
    	
    	// Scorro all'inverso la lista delle azioni per creare la sotto-mossa inversa
    	Map<Pos,P> previousBoard;
    	for(Action<P> attAction : current_sub_move_actions)
    	{
    		/*
			 * Ogni volta che annullo una action, rimuovo una board dallo stack.
			 * Per invertire le REMOVE e le SWAP uso però la board ancora precedente 
			 * (quella che dopo la pop() si trova in cima), poiché occorre lo stato della board prima delle suddette mosse.
			 */
    		this.boardHistory.pop();
    		
    		// In base al tipo di mossa creo la sotto-mossa inversa
    		switch(attAction.kind)
    		{
    			case ADD:
	    			{
	    				reverse_actions.add( new Action<>(attAction.pos.get(0)) );
	    			}
    				break;
    			
    			case REMOVE:
	    			{
	    				// Copio la board in cima alla pila
	    				previousBoard = new HashMap<>();
	    				previousBoard.putAll(this.boardHistory.peek());
	    				
	    				for( Pos p : attAction.pos )
	    					reverse_actions.add( new Action<>(p, previousBoard.get(p)) );
	    			}
	    			break;
    			
    			case MOVE:
	    			{
	    				// Trovo la direzione inversa
	    				Board.Dir dir_inverse = null;
	    				switch(attAction.dir)
	    		    	{
	    		    		case UP: dir_inverse = Dir.DOWN;
	    		    			break;
	    		    		case DOWN: dir_inverse = Dir.UP;
    		    				break;
	    		    		case LEFT: dir_inverse = Dir.RIGHT;
    		    				break;
	    		    		case RIGHT: dir_inverse = Dir.LEFT;
    		    				break;
	    		    		case UP_L: dir_inverse = Dir.DOWN_R;
    		    				break;
	    		    		case UP_R: dir_inverse = Dir.DOWN_L;
    		    				break;
	    		    		case DOWN_L: dir_inverse = Dir.UP_R;
    		    				break;
	    		    		case DOWN_R: dir_inverse = Dir.UP_L;
    		    				break;
	    		    	}
	    		    	
	    				// Calcolo l'array con le posizioni di arrivo
	    				Pos[] pp = new Pos[attAction.pos.size()];
	    				
	    				int i = 0;
	    				for(Pos p : attAction.pos)
	    				{
	    					int b = p.b;
	    			    	int t = p.t;
	    			    	
	    			    	for( int j=0 ; j<attAction.steps ; j++)
	    			    	{
	    			    		switch(attAction.dir)
		    			    	{
		    			    		case UP: t++;
		    			    			break;
		    			    		case DOWN: t--;
	    			    				break;
		    			    		case LEFT: b--;
	    			    				break;
		    			    		case RIGHT: b++;
	    			    				break;
		    			    		case UP_L: {b--; t++;}
	    			    				break;
		    			    		case UP_R: {b++; t++;}
	    			    				break;
		    			    		case DOWN_L: {b--; t--;}
	    			    				break;
		    			    		case DOWN_R: {b++; t--;}
	    			    				break;
		    			    	}
	    			    	}
	    			    	
	    			    	pp[i] = new Pos(b, t);
	    			    	i++;
	    				}
	    				
	    				reverse_actions.add( new Action<>(dir_inverse, attAction.steps, pp) );
	    			}
					break;
    			
    			case JUMP:
	    			{
	    				reverse_actions.add( new Action<>(attAction.pos.get(1), attAction.pos.get(0)) );
	    			}
					break;
    			
    			case SWAP:
	    			{
	    				// Copio la board in cima alla pila
	    				previousBoard = new HashMap<>();
    					previousBoard.putAll(this.boardHistory.peek());
	    				
	    				for( Pos p : attAction.pos )
	    					reverse_actions.add( new Action<>(previousBoard.get(p), p) );
	    			}
					break;
    		}
    	}
    	
    	// Setta come nuovo nodo corrente il padre del nodo corrente precedente ed annulla la selezione corrente
    	this.currentNode = this.currentNode.father;
    	this.clearSelection();
    	
		return new Move<>(reverse_actions);
	}
    
    @Override
    public synchronized boolean isFinal()
	{
    	if( !this.canUse.get() || this.treeRoot==null )
			throw new IllegalStateException();
		
		return this.currentNode.isFinal;
	}

    @Override
    public synchronized void move()
	{
    	if( !this.canUse.get() || this.treeRoot==null || !this.isFinal() )
			throw new IllegalStateException();
		
    	this.moveChoosed = new Move<>(this.currentNode.value);
		
    	// Rende inutilizzabile il MoveChooser
    	this.destroy();
		
    	// Risveglia tutti i threads in attesa
		this.notifyAll();
	}

    @Override
    public synchronized boolean mayPass()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
		
		return this.canPass.get();
	}

    @Override
    public synchronized void pass()
	{
    	if( !this.canUse.get() || !this.mayPass() )
			throw new IllegalStateException();
		
		this.moveChoosed = this.passMove;
		
		// Rende inutilizzabile il MoveChooser
    	this.destroy();
		
    	// Risveglia tutti i threads in attesa
		this.notifyAll();
	}

    @Override
    public synchronized void resign()
	{
    	if( !this.canUse.get() )
			throw new IllegalStateException();
		
    	this.moveChoosed = this.resignMove;
		
    	// Rende inutilizzabile il MoveChooser
    	this.destroy();
		
    	// Risveglia tutti i threads in attesa
		this.notifyAll();
	}
    
    // ------------------------ Metodi di comunicazione con l'esterno ------------------------
    
    /** Se il moveChooser ha scelto una mossa, ritorna tale mossa, altrimenti ritorna null.
     * @return la mossa scelta, o null se non è ancora stata scelta
     */
    public synchronized Move<P> getMoveChoosed()
    {
    	return this.moveChoosed;
    }
    
    /** Ritorna lo stato attuale del MoveChooser.
     * @return true se il MoveChooser è ancora utilizzabile, false altrimenti
     */
    public synchronized boolean isUsable()
    {
    	return this.canUse.get();
    }
    
    /** Rende inutilizzabile il MoveChooser. */
    public synchronized void destroy()
    {
    	this.canUse.set(false);
    }
    
    /** Rende inutilizzabile il MoveChooser e sceglie la resa. 
     * Identico al metodo resign(), ma non invoca this.notifyAll(), poiché è chiamato in caso di errore, 
     * quando nessun threads è più in attesa su quest'oggetto.
     * */
    public synchronized void timeoutResign()
    {
    	// Rende inutilizzabile il MoveChooser
    	this.destroy();
    	
    	this.moveChoosed = this.resignMove;
    }
    
	// ------------------------ Metodi di costruzione dell'albero ------------------------
	
	/** Aggiunge un nodo all'albero delle mosse (assume che la radice sia già stata inserita).
	 * 
	 * @param node radice dell'albero dove aggiungere il nodo
	 * @param moveActions lista di azioni che dovrà contenere il nodo inserito
	 */
	private void addNode(MTNode<P> node, List<Action<P>> moveActions)
	{
		/* 
		 * - 'Preesistenza' di una mossa nell'albero: 
		 * In alcuni casi un nodo potrebbe già contenere l'intera lista di azioni di una mossa che deve essere inserita,
		 * e in questi casi tale nodo (anche se non foglia) viene segnato come finale (e ciò termina l'inserimento).
		 * Queste situazioni accadono in Camelot, in cui è possibile in uno stesso insieme di mosse valide che 
		 * la lista di azioni di una mossa sia la sottolista della lista di azioni di un'altra mossa più lunga 
		 * (es: un cavallo in 'Camelot' può combinare mosse 'Canter' e mosse 'Jump', ma non è obbligato a farlo).
		 */
		
		// Controlla la 'Preesistenza' nel nodo attuale
		if( node.value.equals(moveActions) )
		{
			node.isFinal = true;
			return;
		}
		
		// Controlla la 'Preesistenza' nei figli del nodo attuale
		for( MTNode<P> child : node.childrens )
		{
			if( child.value.equals(moveActions) )
			{
				child.isFinal = true;
				return;
			}
		}
		
		// Se il nodo attuale è una foglia, inserisci direttamente la mossa come come figlio finale di tale nodo
		if( node.isLeaf() )
		{
			node.addChild(moveActions, true);
			return;
		}
		
		// Verfico se una delle sottomosse dei nodi dei figli è prefisso (parziale o completo) della mossa da inserire.
		// Alla mossa da inserire viene rimosso l'eventuale prefisso presente nella radice del sottoalbero dove si sta inserendo,
		// poiché altrimenti non potrebbe mai coincidere con le sottomosse.
		MTNode<P> childSelected = null;
		List<Action<P>> childSubMove;
		int prefixLenght = 0;
		
		// Lunghezza delle azioni del nodo padre (cioè la radice del sottoalbero dove inserire)
		int fatherLength = node.value.size();
		
		// Differenza tra la lista delle azioni da inserire e la lista di azioni del nodo genitore
		List<Action<P>> subMoveActions = fatherLength==0 ? moveActions : moveActions.subList(fatherLength, moveActions.size());
		
		// Cerco un nodo figlio che sia prefisso della mossa da inserire, e la lunghezza di tale prefisso
		for( MTNode<P> child : node.childrens )
		{
			childSubMove = child.getSubMove().get().actions;
			prefixLenght = childSubMove.size();
			
			while( prefixLenght>0 )
			{
				if( Collections.indexOfSubList(subMoveActions, childSubMove.subList(0, prefixLenght)) == 0 )
				{
					childSelected = child;
					break;
				}
				
				prefixLenght--;
			}
			
			if( childSelected != null )
				break;
		}
		
		// Compenso il troncamento della mossa da inserire aggiungendo la lunghezza del valore del nodo padre
		prefixLenght += fatherLength;
		
		if( childSelected==null )
		{
			// Se nessuno dei figli è prefisso (nè completo nè parziale) della mossa, 
			// la inserisco come figlio finale del nodo attuale
			
			node.addChild(moveActions, true);
		}
		else if( prefixLenght == childSelected.value.size() )
		{
			// Se il figlio scelto è prefisso completo della mossa da inserire, 
			// l'inserimento avviene nel sottoalbero del nodo scelto
			
			addNode(childSelected, moveActions);
		}
		else
		{
			// Il figlio scelto è prefisso parziale della mossa da inserire.
			// Bisogna quindi creare un nuovo livello (inserendo tra il figlio scelto e il nodo attuale un nuovo nodo).
			
			// Creo un nuovo nodo (non finale), lo setto come padre del figlio scelto, e inserisco quest'ultimo come suo figlio
			// Come valore ha il prefisso comune al figlio selezionato e al nodo da inserire (di lunghezza inferiore a quello del figlio scelto)
			MTNode<P> new_node = new MTNode<>(childSelected.value.subList(0, prefixLenght), childSelected.father, false);
			childSelected.father = new_node;
			new_node.addChild(childSelected);
			
			// Tolgo il figlio scelto dalla lista dei figli del nodo attuale e vi inserisco il nuovo nodo
			node.removeChild(childSelected);
			node.addChild(new_node);
			
			// Controlla la 'Preesistenza' nel nuovo nodo intermedio
			if( new_node.value.equals(moveActions) )
			{
				// Se è verificata, segno 'new_node' come finale
				new_node.isFinal = true;
			}
			else
			{
				// Se non è verificata, inserisco la mossa come figlio finale di 'new_node'
				new_node.addChild(moveActions, true);
			}
		}
	}
	
	/** Ottiene il prefisso comune a tutte le mosse date (se presente).
	 * Usato per cercare un prefisso comune a tutte le mosse valide.
	 * @param moves lista delle mosse valide
	 * @return il prefisso comune fra le mosse valide, o unalista vuota se esso non c'è
	 */
	private List<Action<P>> getCommonPrefix(List<Move<P>> moves)
	{
		List<Action<P>> prefix = new ArrayList<>();
		
		// Ricavo la lunghezza massima del prefisso comune, 
		// che è uguale alla lunghezza della mossa più corta
		int min = moves.get(0).actions.size();
		for( Move<P> m : moves )
			if( m.actions.size() < min )
				min = m.actions.size();
		
		// Calcolo il prefisso comune
		Action<P> a;
		for( int i=0 ; i<min ; i++ )
		{
			a = moves.get(0).actions.get(i);
			
			for( int j=1 ; j<moves.size() ; j++ )
			{
				if( !moves.get(j).actions.get(i).equals(a) )
					return prefix;
			}
			
			prefix.add(a);
		}
		
		return prefix;
	}
	
	// ------------------------ Metodi di navigazione nell'albero ------------------------
	
	/** Pone il nodo corrente uguale al figlio dato, ed esegue la sotto-mossa di quest'ultimo.
	 * Esegue la sotto-mossa del nodo corrente, che viene poi sostituito con il figlio dato.
	 * Se il nodo dato è null o non è figlio del nodo corrente, non fa nulla.
	 * 
	 * @param n nodo figlio del nodo corrente
	 */
	private void currentNodeUpdate(MTNode<P> n)
	{
		if( n==null || !this.currentNode.childrens.contains(n) )
			return;
		
		// Aggiorna il nodo corrente e annulla la selezione corrente
		this.currentNode = n;
		this.clearSelection();
		
		// Esegui la sotto-mossa dell'attuale nodo corrente nella board interna
		Optional<Move<P>> op_sub_move = this.currentNode.getSubMove();
		if( op_sub_move.isPresent() )
			this.execActions(op_sub_move.get().actions);
	}
	
	/** Esegue la lista di azioni date.
	 * Per ogni action della lista, applica i cambiamenti nell'ultima board disponibile 
	 * (quella iniziale o quella con i cambiamenti efettuati alla prima action), che poi inserirà nella history.
	 * Si assume che TUTTE le azioni siano valide per il gioco corrente.
	 * Se la lista è null o è vuota, non fa nulla. Eventuali null presenti nella lista sono ignorati.
	 * 
	 * @param actions lista di azioni da eseguire
	 */
	private void execActions(List<Action<P>> actions)
	{
		if( actions==null || actions.isEmpty() )
			return;
		
    	// Eseguo la lista di azioni sulla copia della board
		Map<Pos,P> board;
		for( Action<P> action : actions )
		{
			if( action==null )
				continue;
			
			// Copio la board in cima alla pila
			board = new HashMap<>();
	    	board.putAll(this.boardHistory.peek());
			
	    	// Eseguo l'azione in base al tipo
			switch(action.kind)
			{
				case ADD:
	    			{
	    				board.put(action.pos.get(0), action.piece);
	    			}
					break;
				
				case REMOVE:
	    			{
	    				for( Pos p : action.pos )
	    					board.remove(p);
	    			}
	    			break;
				
				case MOVE:
	    			{
	    				for(Pos p : action.pos)
	    				{
	    					int b = p.b;
	    			    	int t = p.t;
	    			    	
	    			    	for( int i=0 ; i<action.steps ; i++)
	    			    	{
	    			    		switch(action.dir)
		    			    	{
		    			    		case UP: t++;
		    			    			break;
		    			    		case DOWN: t--;
	    			    				break;
		    			    		case LEFT: b--;
	    			    				break;
		    			    		case RIGHT: b++;
	    			    				break;
		    			    		case UP_L: {b--; t++;}
	    			    				break;
		    			    		case UP_R: {b++; t++;}
	    			    				break;
		    			    		case DOWN_L: {b--; t--;}
	    			    				break;
		    			    		case DOWN_R: {b++; t--;}
	    			    				break;
		    			    	}
	    			    	}
	    			    	
	    			    	board.put(new Pos(b,t), board.get(p));
	    			    	board.remove(p);
	    				}
	    			}
					break;
				
				case JUMP:
	    			{
	    				board.put(action.pos.get(1), board.get(action.pos.get(0)));
	    				board.remove(action.pos.get(0));
	    			}
					break;
				
				case SWAP:
	    			{
	    				for(Pos p : action.pos)
	    					board.put(p, action.piece);
	    			}
					break;
			}
			
			// Aggiungo la board modificata dalla action nella hostory
			this.boardHistory.push(board);
		}
	}
	
	// ------------------------ Metodi di selezione nodi e sottomosse ------------------------
	
	/** Ritorna la lista dei nodi figli del nodo corrente selezionati dalla selezione corrente. 
     * Se la selezione è vuota o non ci sono nodi figli da essa selezionati, ritorna la lista vuota.
     * La lista è sempre creata ex-novo.
     * 
     * @return lista con i nodi figli del nodo corrente selezionati dalla selezione corrente
     */
    private List<MTNode<P>> getNodesSelected()
    {
    	// Lista sottomosse dei nodi selezionati dalla selezione corrente
    	List<MTNode<P>> nodes_selected = new ArrayList<>();
    	
    	if( this.currentSelection.isEmpty() )
    		return nodes_selected;
    	
    	Action<P> child_first_action;
    	for( MTNode<P> child : this.currentNode.childrens )
    	{
    		child_first_action = child.getSubMove().get().actions.get(0);
    		
    		if( child_first_action.kind == Action.Kind.JUMP )
    		{
    			if( this.currentSelection.size()==1 && this.currentSelection.contains(child_first_action.pos.get(0)) )
    				nodes_selected.add( child );
    		}
    		else
    		{
    			if( this.currentSelection.equals(new HashSet<Pos>(child_first_action.pos)) )
    				nodes_selected.add( child );
    		}
    	}
		
		return nodes_selected;
    }
    
    /** Ritorna le sotto-mosse dei nodi figli del nodo corrente selezionati dalla selezione corrente. 
     * Se la selezione è vuota o non ci sono nodi figli da essa selezionati, ritorna la lista vuota.
     * La lista è sempre creata ex-novo.
     * 
     * @return le sotto-mosse dei nodi figli del nodo corrente selezionati dalla selezione corrente
     */
    private List<Move<P>> getSubMovesNodesSelected()
    {
    	// Lista sottomosse dei nodi selezionati dalla selezione corrente
    	List<Move<P>> moves_selected = new ArrayList<>();
    	
    	if( this.currentSelection.isEmpty() )
    		return moves_selected;
    	
    	Action<P> first_action;
    	
    	for( Move<P> child_sub_move : this.currentNode.getChildrenSubMoves() )
    	{
    		first_action = child_sub_move.actions.get(0);
    		
    		if( first_action.kind == Action.Kind.JUMP )
    		{
    			if( this.currentSelection.size()==1 && this.currentSelection.contains(first_action.pos.get(0)) )
    				moves_selected.add( child_sub_move );
    		}
    		else
    		{
    			if( this.currentSelection.equals(new HashSet<Pos>(first_action.pos)) )
    				moves_selected.add( child_sub_move );
    		}
    	}
		
		return moves_selected;
    }
}
