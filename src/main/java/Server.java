import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

public class Server {

	int count = 1;
	ArrayList<ClientThread> clients = new ArrayList<>();
	TheServer server;
	private Consumer<Serializable> callback;

	ArrayList<ClientThread> waitingForGame = new ArrayList<>();
	ArrayList<BattleShipsGame> currentGames = new ArrayList<>();
	int battleShipGameIndex = 0;

	// Add AI player instance
	ServerAIPlayer aiPlayer;

	public Server(Consumer<Serializable> call) {
		callback = call;
		server = new TheServer();
		server.start();
		// Initialize the AI player
		aiPlayer = new ServerAIPlayer();
	}

	public class TheServer extends Thread {

		public void run() {
			try (ServerSocket mysocket = new ServerSocket(5555);) {
				System.out.println("Server is waiting for a client!");

				while (true) {
					Socket clientSocket = mysocket.accept();
					callback.accept("client has connected to server: " + "client #" + count);
					ClientThread c = new ClientThread(clientSocket, count);
					clients.add(c);
					c.start();
					count++;
				}
			} catch (IOException e) {
				callback.accept("Server socket did not launch");
			}
		}
	}

	class ClientThread extends Thread {

		Socket connection;
		int count;
		ObjectInputStream in;
		ObjectOutputStream out;

		ClientThread opponent;
		int gameIndex;
		int playerNumber;

		ClientThread(Socket s, int count) {
			this.connection = s;
			this.count = count;
		}

		public void updateClients(String message) {
			for (int i = 0; i < clients.size(); i++) {
				ClientThread t = clients.get(i);
				try {
					t.out.writeObject(message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		public void run() {
			try {
				out = new ObjectOutputStream(connection.getOutputStream());
				in = new ObjectInputStream(connection.getInputStream());
				connection.setTcpNoDelay(true);

				updateClients("new client on server: client #" + count);

				while (true) {
					try {
						GameInfo data = (GameInfo) in.readObject();
						callback.accept("client: " + count + " sent: " + data);

						if (data.lookingForGame) {
							waitingForGame.add(this); // Add client thread to arraylist
							if (waitingForGame.size() == 2) {
								// Each client thread has the opponent's thread for easier communication
								waitingForGame.get(0).opponent = waitingForGame.get(1);
								waitingForGame.get(1).opponent = waitingForGame.get(0);
								GameInfo newGame = new GameInfo();
								newGame.gameFound = true;
								BattleShipsGame game = new BattleShipsGame();
								currentGames.add(game);
								// New game was created and added to list of games
								System.out.println("New game started index: " + battleShipGameIndex +
										" between user number: " + waitingForGame.get(0).count +
										" and player number: " + waitingForGame.get(1).count);
								// Give each waiting player the index of the current game
								waitingForGame.get(0).gameIndex = battleShipGameIndex;
								waitingForGame.get(1).gameIndex = battleShipGameIndex;
								battleShipGameIndex++;
								// Give them player numbers
								waitingForGame.get(0).playerNumber = 1;
								waitingForGame.get(1).playerNumber = 2;
								// Send to them that game was found and clear the list
								waitingForGame.get(0).out.writeObject(newGame);
								waitingForGame.get(1).out.writeObject(newGame);
								waitingForGame.clear();
							}
						} else if (data.placeShip) {
							ShipBoard temp;
							BattleShipsGame game = currentGames.get(gameIndex);
							//get the board from given player
							if (playerNumber == 1) {
								temp = game.player1Board;
							} else {
								temp = game.player2Board;
							}
							int r1 = data.r1;
							int c1 = data.c1;
							int r2 = data.r2;
							int c2 = data.c2;
							//try to place ship at given location
							int val = temp.placeShip(temp.ships[temp.currentShip], r1, c1, r2, c2);
							GameInfo placementMessage = new GameInfo();
							placementMessage.placeShip = true;
							if (val == 1) {
								temp.currentShip++;
								placementMessage.validPlacement = true;
								//cords for the gui
								placementMessage.r1 = r1;
								placementMessage.c1 = c1;
								placementMessage.r2 = r2;
								placementMessage.c2 = c2;
								//if all ships are placed add the flag to disable the placement gui
								if (temp.currentShip == 5) {
									placementMessage.allShipsPlaced = true;
									System.out.println("All ships placed");
								}
							}
							//send the message
							//if placement was invalid then valid placement is false by default
							out.writeObject(placementMessage);
							//server/debugging purposes
							System.out.println("Ship placement for game index " + gameIndex);
							System.out.println("P1 board");
							game.player1Board.printBoard();
							System.out.println("P2 board");
							game.player2Board.printBoard();

							//if both players placed all ships start the game
							if (game.player1Board.currentShip == 5 && game.player2Board.currentShip == 5) {
								System.out.println("All ships placed for both players in game " + gameIndex);
								GameInfo startGame = new GameInfo();
								startGame.allShipsPlacedBothPlayers = true;
								out.writeObject(startGame);
								startGame.yourTurn = true; //whoever placed first, its their turn
								opponent.out.writeObject(startGame);
							}
						} else if (data.hitShip) {
							// Check if the player is AI or human
							if (opponent instanceof ClientThread) {
								// Process the hit ship from the human player
								int hitRow = data.hitShipRow;
								int hitCol = data.hitShipCol;
								int result;
								int shipsLeft;
								// A message that ship was hit
								BattleShipsGame game = currentGames.get(gameIndex);
								// Check from who the message came and hit the ship on the enemy's board
								if (playerNumber == 1) {
									result = game.player2Board.hitShip(hitRow, hitCol);
									shipsLeft = game.player2Board.shipsLeft;
								} else {
									result = game.player1Board.hitShip(hitRow, hitCol);
									shipsLeft = game.player1Board.shipsLeft;
								}
								// The one who shot needs to know the cords they shot, result, how many ships opponent has left
								GameInfo hitResult = new GameInfo();
								hitResult.shipHitResult = result;
								hitResult.shipsLeftAfterHit = shipsLeft;
								hitResult.hitShipRow = hitRow;
								hitResult.hitShipCol = hitCol;
								hitResult.youHitShip = true;
								out.writeObject(hitResult);
								// The one who got hit needs to know which ship, cords, and how many ships they have left
								// Why, for the auto lose and win screen
								GameInfo opponentHitResult = new GameInfo();
								opponentHitResult.yourShipWasHit = true;
								opponentHitResult.hitShipRow = hitRow;
								opponentHitResult.hitShipCol = hitCol;
								opponentHitResult.shipHitResult = result;
								opponentHitResult.shipsLeftAfterHit = shipsLeft;
								opponent.out.writeObject(opponentHitResult);
							} else {
								// If the player is AI, make an AI move
								GameInfo aiMove = aiPlayer.makeAIMove();
								out.writeObject(aiMove);
							}
						} else if (data.lookingForGame && data.gameFound) {
							// Start an offline game
							startOfflineGame(this);
						}
					} catch (IOException e) {
						callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
						clients.remove(this);
						break;
					} catch (ClassNotFoundException e) {
						callback.accept("OOOOPPs...Class not found!");
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	// Method to start an offline game
	public void startOfflineGame(ClientThread client) {
		// Create an instance of ServerAIPlayer
		ServerAIPlayer aiPlayer = new ServerAIPlayer();

		// Start a game between the client and the AI
		GameInfo newGame = new GameInfo();
		newGame.gameFound = true;
		currentGames.add(new BattleShipsGame());

		// Provide the client with the game information
		client.gameIndex = battleShipGameIndex;
		client.playerNumber = 1;
		client.opponent = null; // No opponent in offline mode
		try {
			client.out.writeObject(newGame);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Generate AI move
		GameInfo aiMove = aiPlayer.makeAIMove();
		// Send AI move to the client
		try {
			client.out.writeObject(aiMove);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
