import java.io.Serializable;

public class GameInfo implements Serializable {
    static final long serialVersionUID = 42L;
    boolean lookingForGame = false;
    boolean gameFound = false;
    boolean placeShip = false;
    int r1;
    int c1;
    int r2;
    int c2;
    boolean validPlacement = false;
    boolean allShipsPlaced = false;
}
