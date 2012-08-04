/**
 * 
 */
package com.agical.golddigger.server;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.agical.golddigger.model.Digger;
import com.agical.golddigger.model.Diggers;
import com.agical.golddigger.model.Position;
import com.agical.jambda.Functions;

public class PathExecutor {

	private final Diggers diggers;
	private final Writer log;
	Timer timer, gameTimer, endingTimer;
	GameTask gameTask;
	EndingTask endingTask;
	int joinTime = 30;
	int gameTime = 60;
	int endTime = 10; // Seconds, as above
	int totalTime;
	boolean ending = false;

	/** ANUS-24 experimental code **/
	// The maximum number of commands an individual digger can send before
	// the next commands are dropped/ignored
	private final int MAX_PREGAME_COMMANDS = 10;
	// The queue that holds the incoming pre-game commands from all diggers
//	private String[] commandQueue;
	// Real queues and linked lists were hard to use (clone to be specific)
	private int queueIndex;
	 private Queue<String> commandQueueB;
	// Keeps count of the number of commands sent by each digger
	private Map<String, Integer> commandCounter;

	public PathExecutor(Diggers diggers, Writer log) {
		super();
		this.diggers = diggers;
		this.log = log;

		totalTime = joinTime + gameTime;

		timer = new Timer();
		JoinTask t = new JoinTask();
		timer.schedule(t, 0, 1000);

		endingTimer = new Timer();
		endingTask = new EndingTask();

		gameTimer = new Timer();
		gameTask = new GameTask();
		gameTimer.schedule(gameTask, (totalTime) * 1000L);

		/** ANUS-24 experimental code **/
		commandCounter = new HashMap<String, Integer>();

//		System.out.println(diggers.getDiggers().toString());
//		
//		for (Digger d : diggers.getDiggers()) { // Initialise the counters
//			commandCounter.put(d.getName(), 0);
//		}
//		// Checking the contents
//		for (String key : commandCounter.keySet()) {
//			System.out.println("commandCounter contents: " + key + ", " + commandCounter.get(key));
//		}

		commandQueueB = new LinkedList<String>();
		// Make enough space in the array
		queueIndex = 0;
		/** x **/

	}

	class JoinTask extends TimerTask {
		public void run() {
			joinTime--;
			System.out.println(joinTime);
			if (joinTime <= 0) {
				// Start processing the queued commands because game has started
				if (!commandQueueB.isEmpty()) {
					restoreFromQueue();
				}
				this.cancel();
			}
		}
	}

	class GameTask extends TimerTask {
		public void run() {
			joinTime = 1;
		}
	}

	class EndingTask extends TimerTask {
		public void run() {
			// Insert some action performed after the time has expired
			joinTime = 1; // Anything non zero
			System.out.println("\"End of game\"");
		}
	}

	public void executePath(String pathInfo, PrintWriter writer) {

		String[] splitPath = pathInfo.split("/");
		String actor = splitPath[0];
		
		if (actor.equals("digger")) {

			/** ANUS-24 experimental code **/
			String secretName = splitPath[1];
			int noQueuedCommands = 0;
			
			if (joinTime > 0) { // Before game begins

				if (commandCounter.containsKey(secretName)){
					noQueuedCommands = commandCounter.get(secretName);
				}
				if (noQueuedCommands < MAX_PREGAME_COMMANDS) {
					commandQueueB.offer(pathInfo);
					commandCounter.put(secretName, noQueuedCommands + 1);
					System.out.println("Now queuing \"" + pathInfo + "\", commands queued by this digger: " + (noQueuedCommands + 1));
				}
			} else {
				handleDigger(writer, splitPath);
			}
		} else if (actor.equals("admin")) {
			String secretName = splitPath[1];
			if (!secretName.equals("ccret")) {
				writer.write("bad command\n");
				return;
			}
			String action = splitPath[2];
			if (action.equals("listdiggers")) {
				for (Digger digger : diggers.getDiggers()) {
					writer.write(digger.getName() + " " + digger.getSecretName() + "\n");
				}
			} else if (action.equals("add")) {
				String newName = splitPath[3];
				String newSecretName = splitPath[4];
				Digger digger = diggers.createDigger(newName, newSecretName);
				diggers.newGame(digger);

			}
		}

	}

	private void handleDigger(PrintWriter writer, String[] splitPath) {
		String secretName = splitPath[1];
		String action = splitPath[2];
		Digger digger = diggers.getDigger(secretName);
		int numberOfSides = digger.getGoldField().getNumberOfSides();
		try {
			if (joinTime > 0) { // The game has not begun
				// They get queued above but here just in case
			} else {
				if (action.equals("view")) {
					writer.write(digger.getView());
				}
				if (action.equals("score")) {
					int goldInTheBank = digger.getGoldInTheBank();
					writer.write(goldInTheBank + "\n");
				}
				if (action.equals("grab")) {
					int carriedBefore = digger.getCarriedGold();
					digger.grab();
					int carriedAfter = digger.getCarriedGold();
					writer.write((carriedAfter - carriedBefore) + "\n");

					// Players should bank
					if (!digger.getGoldField().hasGold() && endTime <= gameTime) {
						System.out.println("Last piece of gold collected, x seconds remaining to bank");
						ending = true;
						endingTimer.schedule(endingTask, endTime * 1000L);
					}
				}
				if (action.equals("drop")) {
					int carriedBefore = digger.getCarriedGold();
					digger.drop();
					int carriedAfter = digger.getCarriedGold();
					writer.write((carriedBefore - carriedAfter) + "\n");

					if (ending) {
						/*
						 * If a digger banks before the ending timer runs out
						 * just immediately stop the game
						 */
						endingTimer.cancel();
						joinTime = 1;
					}
				}
				if (action.equals("carrying")) {
					writer.write(digger.getCarriedGold() + "\n");
				}
				if (action.equals("next") && (joinTime <= 0)) {
					if (digger.getGoldField().hasGold()) {
						writer.write("FAILED\n");
					} else {
						writer.write("OK\n");
						diggers.newGame(digger);
					}
				}
				if (action.equals("move")) {
					String direction = splitPath[3].toLowerCase();
					String ok = "OK\n";
					String failed = "FAILED\n";

					if (numberOfSides == 4) {
						if (direction.equals("north")) {
							writer.write(digger.move(Position.NORTH).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("east")) {
							writer.write(digger.move(Position.EAST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("west")) {
							writer.write(digger.move(Position.WEST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("south")) {
							writer.write(digger.move(Position.SOUTH).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else {
							writer.write("Cannot Move in that direction");
						}
					} else if (numberOfSides == 6) {

						if (direction.equals("north_east")) {
							writer.write(digger.move(Position.NORTH_EAST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("south_east")) {
							writer.write(digger.move(Position.SOUTH_EAST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("north_west")) {
							writer.write(digger.move(Position.NORTH_WEST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("south_west")) {
							writer.write(digger.move(Position.SOUTH_WEST).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("south")) {
							writer.write(digger.move(Position.SOUTH).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else if (direction.equals("north")) {
							writer.write(digger.move(Position.NORTH).map(Functions.<Position, String> constantly(ok), Functions.<String> constantly(failed)));
						} else {
							writer.write("Cannot Move in that direction");
						}
					} else {
						writer.write("Cannot Move in that direction");
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(digger.toString(), e);
		}
	}

	public void restoreFromLog(Reader reader) {
		try {
			BufferedReader bufferedReader = new BufferedReader(reader);
			String logRow = null;
			PrintWriter writer = new PrintWriter(new VoidOutputStream());
			while ((logRow = bufferedReader.readLine()) != null) {
				String pathInfo = logRow.substring(logRow.indexOf(" ") + 1);
				executePath(pathInfo, writer);
			}
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	/**
	 * Will read from the reader until it is closed. If the reader is blocked,
	 * this method blocks.
	 * 
	 * @param reader
	 * @param delay
	 */
	public void restoreFromLogWithDelay(Reader reader, long delay) {
		String logRow = null;
		try {
			BufferedReader bufferedReader = new BufferedReader(reader);
			PrintWriter writer = new PrintWriter(new VoidOutputStream());
			while ((logRow = bufferedReader.readLine()) != null) {
				long timestamp = Long.parseLong(logRow.substring(0, logRow.indexOf(" ")));
				long timeSinceLog = System.currentTimeMillis() - timestamp;
				if (timeSinceLog < delay) {
					Thread.sleep(delay - timeSinceLog);
				}
				String pathInfo = logRow.substring(logRow.indexOf(" ") + 1);
				executePath(pathInfo, writer);
			}
		} catch (Exception e) {
			throw new RuntimeException("logRow is:" + logRow, e);
		}
	}

	private void restoreFromQueue() {
			System.out.println(Arrays.toString(commandQueueB.toArray()));
			Set<String> queuedDiggers = commandCounter.keySet();
			String[] commandArray = new String[commandQueueB.size()];
			
			for(int i = 0; commandQueueB.iterator().hasNext(); i++){ // Try alternate for loop form later
				commandArray[i] = commandQueueB.poll();
			}
			System.out.println(Arrays.toString(commandArray));
			
			RestoreFromQueueThread[] threads = new RestoreFromQueueThread[queuedDiggers.size()];
			int i = 0;
			for (String d : queuedDiggers) { // d is the secretName
				threads[i] = new RestoreFromQueueThread(d, commandArray);
				threads[i].start();
			}
	}

	class RestoreFromQueueThread extends Thread {
		String secretName;
		String commandArray[]; //Working on using a queue instead of an array
		PrintWriter writer = new PrintWriter(new VoidOutputStream());

		RestoreFromQueueThread(String secretName, String[] queue) {
			this.secretName = secretName;
			this.commandArray = queue;
		}

		public void run() {
			System.out.println("New thread for " + secretName + " starting");
			for (int i = 0; i < commandArray.length; i++) {
				if ((commandArray[i].split("/"))[1].equals(secretName)) {
					System.out.println("The next command for me is: " + commandArray[i]);
					executePath(commandArray[i], writer);
				}
			}
			System.out.println("The thread for " + secretName + " has reached the end of its command queue");
		}
	}

}
