import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.scene.control.ListView;
/*
 * Clicker: A: I really get it    B: No idea what you are talking about
 * C: kind of following
 */

public class Server{

	int count = 1;	
	ArrayList<ClientThread> clients = new ArrayList<ClientThread>();
	TheServer server;
	private Consumer<Serializable> callback;

	ArrayList<ClientThread> waitingForGame = new ArrayList<>();  //users that want to play online game
	//will be added here once there is 2 a new game is created for them. This takes place in ClientThread
	//on the server side once the thread receives GameInfo
	ArrayList<BattleShipsGame> currentGames = new ArrayList<>();
	//This will be list of current games being played out
	int battleShipGameIndex = 0;
	//Used to track the games. Each client thread has a game index so that server knows which game they
	//are playing



	
	
	Server(Consumer<Serializable> call){
	
		callback = call;
		server = new TheServer();
		server.start();
	}
	
	
	public class TheServer extends Thread{
		
		public void run() {
		
			try(ServerSocket mysocket = new ServerSocket(5555);){
		    System.out.println("Server is waiting for a client!");
		  
			
		    while(true) {
		
				ClientThread c = new ClientThread(mysocket.accept(), count);
				callback.accept("client has connected to server: " + "client #" + count);
				clients.add(c);
				c.start();
				
				count++;
				
			    }
			}//end of try
				catch(Exception e) {
					callback.accept("Server socket did not launch");
				}
			}//end of while
		}
	

		class ClientThread extends Thread{
			
		
			Socket connection;
			int count;
			ObjectInputStream in;
			ObjectOutputStream out;

			ClientThread opponent;
			int gameIndex;
			int playerNumber;

			
			ClientThread(Socket s, int count){
				this.connection = s;
				this.count = count;	
			}
			
			public void updateClients(String message) {
				for(int i = 0; i < clients.size(); i++) {
					ClientThread t = clients.get(i);
					try {
					 t.out.writeObject(message);
					}
					catch(Exception e) {}
				}
			}
			
			public void run(){
					
				try {
					in = new ObjectInputStream(connection.getInputStream());
					out = new ObjectOutputStream(connection.getOutputStream());
					connection.setTcpNoDelay(true);	
				}
				catch(Exception e) {
					System.out.println("Streams not open");
				}
				
				updateClients("new client on server: client #"+count);
					
				 while(true) {
					    try {
					    	GameInfo data = (GameInfo)in.readObject();
					    	callback.accept("client: " + count + " sent: " + data);

							if (data.lookingForGame) {
								waitingForGame.add(this);
								if (waitingForGame.size() == 2) {
									waitingForGame.get(0).opponent = waitingForGame.get(1);
									waitingForGame.get(1).opponent = waitingForGame.get(0);
									GameInfo newGame = new GameInfo();
									newGame.gameFound = true;
									BattleShipsGame game = new BattleShipsGame();
									currentGames.add(game);
									System.out.println("New game started index: " + battleShipGameIndex +
											" between user number: " + waitingForGame.get(0).count +
											" and player number: " + waitingForGame.get(1).count);
									waitingForGame.get(0).gameIndex = battleShipGameIndex;
									waitingForGame.get(1).gameIndex = battleShipGameIndex;
									battleShipGameIndex++;
									waitingForGame.get(0).playerNumber = 1;
									waitingForGame.get(1).playerNumber = 2;
									waitingForGame.get(0).out.writeObject(newGame);
									waitingForGame.get(1).out.writeObject(newGame);
									waitingForGame.clear();
								}
							}
							//Not finished at all
							else if (data.placeShip) {
								ShipBoard temp;
								if (playerNumber == 1) {
									temp = currentGames.get(gameIndex).player1Board;
								}
								else {
									temp = currentGames.get(gameIndex).player2Board;
								}
								int r1 = data.r1;
								int c1 = data.c1;
								int r2 = data.r2;
								int c2 = data.c2;
								int val = temp.placeShip(temp.ships[temp.currentShip], r1, c1, r2, c2);
								GameInfo placementMessage = new GameInfo();
								placementMessage.placeShip = true;
								if (val == 1) {
									temp.currentShip++;
									placementMessage.validPlacement = true;
									placementMessage.r1 = r1;
									placementMessage.c1 = c1;
									placementMessage.r2 = r2;
									placementMessage.c2 = c2;
									if (temp.currentShip == 5) {
										placementMessage.allShipsPlaced = true;
										System.out.println("All ships placed");
									}
								}
								out.writeObject(placementMessage);
								System.out.println("Ship placement for game index " + gameIndex);
								System.out.println("P1 board");
								currentGames.get(gameIndex).player1Board.printBoard();
								System.out.println("P2 board");
								currentGames.get(gameIndex).player2Board.printBoard();
							}

							//next step
							//if its not want to game then its during game
							//go back to menu will be made later
							//first place ship
							//we will get sets of cords from user here
							//get index from thread to get game and het player index
							//then check then call place ship in given cords for correct board
							//await return
							//message user with reuslt
							//repeat untill all ships are placed


//							updateClients("client #"+count+" said: "+data);


					    	}
					    catch(Exception e) {
					    	callback.accept("OOOOPPs...Something wrong with the socket from client: " + count + "....closing down!");
					    	updateClients("Client #"+count+" has left the server!");
					    	clients.remove(this);
					    	break;
					    }
					}
				}//end of run
			
			
		}//end of client thread
}


	
	

	
