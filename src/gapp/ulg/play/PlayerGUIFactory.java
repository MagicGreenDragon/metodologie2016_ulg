package gapp.ulg.play;

import gapp.ulg.game.GameFactory;
import gapp.ulg.game.Param;
import gapp.ulg.game.PlayerFactory;
import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.Player;
import gapp.ulg.game.util.PlayerGUI;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Una PlayerGUIFactory è una fabbrica di {@link PlayerGUI}
 *
 * @param <P> tipo del modello dei pezzi
 */
public class PlayerGUIFactory<P> implements PlayerFactory<Player<P>, GameRuler<P>>
{
    static public final String NAME = "GUI player";
    
    private final List<Param<?>> PARAMS = Collections.unmodifiableList(new ArrayList<>());
    private Consumer<PlayerGUI.MoveChooser<P>> master;

    public PlayerGUIFactory(Consumer<PlayerGUI.MoveChooser<P>> master)
    {
        this.master = master;
    }

    @Override
    public String name()
    {
        return NAME;
    }

    @Override
    public void setDir(Path dir) { }

    @Override
    public List<Param<?>> params() { return PARAMS; }

    @Override
    public Play canPlay(GameFactory<? extends GameRuler<P>> gF)
    {
        return Play.YES;
    }

    @Override
    public String tryCompute(GameFactory<? extends GameRuler<P>> gF, boolean parallel, Supplier<Boolean> interrupt)
    {
        return null;
    }

    @Override
    public Player<P> newPlayer(GameFactory<? extends GameRuler<P>> gF, String name)
    {
        PlayerGUI<P> result = new PlayerGUI<>(name, this.master);
        return result;
    }
}
