/*
    CuckooChess - A java chess program.
    Copyright (C) 2011  Peter Österlund, peterosterlund2@gmail.com

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package chess;

import android.content.Context;
import android.util.Log;
import chess.Search.Listener;
import chess.TranspositionTable.TTEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import org.jason.lxcoff.lib.ExecutionController;
import org.jason.lxcoff.lib.Remoteable;

import com.google.gson.Gson;


/**
 *
 * @author petero
 */
public class OffSearch extends Remoteable {
//public class OffSearch {
	private Position pos;
	private long[] posHashList;
	private int posHashListSize;
	private byte generation;
	transient TranspositionTable tt = null;
	transient History ht;
	transient private static String TAG = "OffSearch";
	transient private ExecutionController controller;
    transient Listener listener;
    private static String logFileName = "/sdcard/DefaultGame.txt";
	private static FileWriter logFileWriter;
	
	public OffSearch(ExecutionController controller, Position pos, long[] posHashList, int posHashListSize, byte generation, History ht){
		this.controller = controller;
        this.pos = new Position(pos);
        this.posHashList = posHashList;
        this.posHashListSize = posHashListSize;
        this.generation = generation;
        this.tt = new TranspositionTable(15);
        this.tt.setGeneration(this.generation);
        this.ht = new History();
	}
	
	public static void startNewLog(){
		logFileName = "/sdcard/GameRecord/game" + System.currentTimeMillis() + ".txt"; 
		if (logFileWriter != null){
			try {
				logFileWriter.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		try {
			File logFile = new File(logFileName);
			logFile.createNewFile(); // Try creating new, if doesn't exist
			logFileWriter = new FileWriter(logFile, true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public Search getSearch(){
		Search sc = new Search(this.pos, this.posHashList, this.posHashListSize, this.tt, this.ht);
		return sc;
	}
	
	public Move iterativeDeepening(MoveGen.MoveList scMovesIn, int maxDepth, long initialMaxNodes, boolean verbose) {
		Method toExecute;
		Class<?>[] paramTypes = {MoveGen.MoveList.class, int.class, long.class, boolean.class};
		Object[] paramValues = {scMovesIn, maxDepth, initialMaxNodes, verbose};

		Move result = null;
		long starttime = System.nanoTime();
		try {
			toExecute = this.getClass().getDeclaredMethod("localIterativeDeepening", paramTypes);
			result = (Move) controller.execute(toExecute, paramValues, this);
		} catch (SecurityException e) {
			// Should never get here
			e.printStackTrace();
			throw e;
		} catch (NoSuchMethodException e) {
			// Should never get here
			e.printStackTrace();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		long dura = System.nanoTime()-starttime;
		if (logFileWriter != null) {
			try {
				logFileWriter.append(dura + "\n");
				logFileWriter.flush();

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		this.listener.notifyTimeCost("search "+maxDepth+" depth cost "+ dura/1000000 + " ms");
		return result;
		
	}    
	
	public Move localIterativeDeepening(MoveGen.MoveList scMovesIn, int maxDepth, long initialMaxNodes, boolean verbose) {
		if(this.tt == null){
			this.tt = new TranspositionTable(15);
	        this.tt.setGeneration(this.generation);
		}
		
		if(this.ht == null){
			this.ht = new History();
		}
		
		Move bestM;
		Search sc = new Search(this.pos, this.posHashList, this.posHashListSize, this.tt, this.ht);
		MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
/*        for (int i = 0; i < moves.size; i++) {
            Log.d(TAG, "origin move score " + moves.m[i].score);
        }*/
        
        sc.scoreMoveList(moves, 0);
        
/*        for (int i = 0; i < moves.size; i++) {
            Log.d(TAG, "after scorefunc first move score " + moves.m[i].score);

        }*/
        // Test for "game over"
        if (moves.size == 0) {
            // Switch sides so that the human can decide what to do next.
            return null;
        }
		bestM = sc.iterativeDeepening(scMovesIn, maxDepth, initialMaxNodes, verbose);
		return bestM;
	} 
	
	public MoveGen.MoveList getMoves(Position pos){
		Search sc = new Search(this.pos, this.posHashList, this.posHashListSize, this.tt, this.ht);
		MoveGen.MoveList moves = new MoveGen().pseudoLegalMoves(pos);
        MoveGen.removeIllegal(pos, moves);
        sc.scoreMoveList(moves, 0);
        return moves;
	}

    public void setListener(Listener listener) {
        this.listener = listener;
    }
    
	@Override
	public void copyState(Remoteable state) {

	}
}
