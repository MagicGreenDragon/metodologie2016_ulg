package gapp.ulg.test;

import gapp.ulg.game.board.GameRuler;
import gapp.ulg.game.board.PieceModel;
import gapp.ulg.game.board.Pos;
import gapp.ulg.game.util.Probe;
import gapp.ulg.games.MNKgame;
import gapp.ulg.games.Othello;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class TestNS
{
	public static void main(String[] args) {
        test(false, ()->new MNKgame(-1,3,3,3,"a","b"), 10);
    }

    public static void test(boolean parallel, Supplier<GameRuler<PieceModel<PieceModel.Species>>> gR, int depth) {
        GameRuler<PieceModel<PieceModel.Species>> game = gR.get();
        Function<Probe.EncS<PieceModel<PieceModel.Species>>,GameRuler.Situation<PieceModel<PieceModel.Species>>>
                dec = e -> e.decode(game.mechanics());

        Function<GameRuler.Situation<PieceModel<PieceModel.Species>>, Probe.EncS<PieceModel<PieceModel.Species>>>
                enc = s -> new Probe.EncS<>(game.mechanics(), s);

        Set<Probe.EncS<PieceModel<PieceModel.Species>>> encSet = new HashSet<>();
        encSet.add(enc.apply(game.mechanics().start));
        for (int i = 0; i < depth; i++) {
            Probe.NSResult<Probe.EncS<PieceModel<PieceModel.Species>>> nS = Probe.nextSituations(parallel, game.mechanics().next,dec,enc,encSet);
            encSet = nS.next;
            int finali = 0;
            int patte = 0;
            int nonFinite = 0;
            for (Probe.EncS<PieceModel<PieceModel.Species>> e : encSet) {
                GameRuler.Situation<PieceModel<PieceModel.Species>> s = dec.apply(e);
                if (s.turn < 0) finali++;
                else if (s.turn == 0) patte++;
                else nonFinite++;
                printBoardOneRow(s);
            }
            System.out.println("Depth " + (i+1) + " : Size = " + encSet.size() + " | Finite con vittoria di un giocatore: " + finali + " | Patte: " + patte + " | Non finite: " + nonFinite);
        }
        System.out.println();
    }


    public static void printBoardOneRow(GameRuler.Situation<PieceModel<PieceModel.Species>> s) {
        Pos pp[] = new Pos[9];
        for (int y = 0; y < 3; y++) {
            for (int x = 0; x < 3; x++) {
                pp[y*3+x] = new Pos(x,y);
            }
        }
        Map<Pos, PieceModel<PieceModel.Species>> mp = s.newMap();
        String r = "";
        for (Pos pos : pp) {
            if (mp.get(pos) == null) r += ".";
            else r += mp.get(pos).color.equals("nero") ? "X" : "O";
        }
        System.out.println(r + " " + s.turn);
    }
	
	/*
    TEST SEMPLICE OTHELLO + MNK
	public static void main(String[] args)
    {
        //test(false, ()->new MNKgame(-1,3,3,3,"a","b"), 10);
        //test(true, ()->new MNKgame(-1,3,3,3,"a","b"), 10);
    	long st = System.currentTimeMillis();
        test(true, ()->new Othello(-1,6,"a","b"), 9);
        System.out.println("\nTempo: " + (System.currentTimeMillis()-st));
    }

    public static void test(boolean parallel, Supplier<GameRuler<PieceModel<PieceModel.Species>>> gR, int depth)
    {
        GameRuler<PieceModel<PieceModel.Species>> game = gR.get();
        Function<Probe.EncS<PieceModel<PieceModel.Species>>,GameRuler.Situation<PieceModel<PieceModel.Species>>>
                dec = e -> e.decode(game.mechanics());

        Function<GameRuler.Situation<PieceModel<PieceModel.Species>>, Probe.EncS<PieceModel<PieceModel.Species>>>
                enc = s -> new Probe.EncS<>(game.mechanics(), s);

        Set<Probe.EncS<PieceModel<PieceModel.Species>>> encSet = new HashSet<>();
        encSet.add(enc.apply(game.mechanics().start));
        for (int i = 0; i < depth; i++) {
            encSet = Probe.nextSituations(parallel, game.mechanics().next,dec,enc,encSet).next;
            System.out.println("Depth " + (i+1) + " : Size = " + encSet.size());
        }
        System.out.println();
    }
    */
}