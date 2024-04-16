

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MyTest {

	@Test
	void testShipBasic() {
		Ship s1 = new Ship(4);
		Ship s2 = new Ship(5);
		assertEquals(4, s1.size);
		assertEquals(5, s2.size);
		for (int i = 0; i < s1.size; i++) {
			assertEquals(0, s1.coordinatesR[i]);
			assertEquals(0, s1.coordinatesC[i]);
			assertFalse(s1.hit[i]);
		}
		for (int i = 0; i < s2.size; i++) {
			assertEquals(0, s2.coordinatesR[i]);
			assertEquals(0, s2.coordinatesC[i]);
			assertFalse(s2.hit[i]);
		}
		assertEquals(4, s1.hitpoints);
		assertEquals(5, s2.hitpoints);
		assertFalse(s1.isDestroyed);
		assertFalse(s2.isDestroyed);
	}

	@Test
	void testShipBoardConstructor() {
		ShipBoard b = new ShipBoard();

		assertEquals(10, b.board.length);
		for (int i = 0; i < 10; i++) {
			assertEquals(10, b.board[i].length);
			for (int j = 0; j < 10; j++) {
				assertNull(b.board[i][j]);
			}
		}
	}

	@Test
	void testPlaceShip() {
		ShipBoard b = new ShipBoard();
		Ship s1 = new Ship(5);
		//Testing all out of bounds
		b.placeShip(s1,-1, 0, 4, 0);
		assertNull(b.board[4][0]);
		b.placeShip(s1,12, 0, 8, 0);
		assertNull(b.board[8][0]);
		b.placeShip(s1,0, -1, 0, 4);
		assertNull(b.board[0][4]);
		b.placeShip(s1,0, 12, 0, 8);
		assertNull(b.board[0][8]);
		b.placeShip(s1,4, 0, -1, 0);
		assertNull(b.board[4][0]);
		b.placeShip(s1,8, 0, 12, 0);
		assertNull(b.board[8][0]);
		b.placeShip(s1,0, 4, 0, -1);
		assertNull(b.board[0][4]);
		b.placeShip(s1,0, 8, 0, 12);
		assertNull(b.board[0][8]);

		//valid placement
		b.placeShip(s1, 5, 4, 5, 4); //point
		assertNull(b.board[5][4]);
		b.placeShip(s1, 1, 2, 3, 4); //diagonal
		assertNull(b.board[1][2]);
		assertNull(b.board[3][4]);
		//size doesnt fit
		b.placeShip(s1, 1, 2, 4, 2);
		assertNull(b.board[1][2]);
		b.placeShip(s1, 1, 2, 6, 2);
		assertNull(b.board[1][2]);
		b.placeShip(s1, 3, 2, 3, 5);
		assertNull(b.board[3][2]);
		b.placeShip(s1, 3, 2, 3, 7);
		assertNull(b.board[3][2]);

		//ship placed correctly
		b.placeShip(s1, 2, 3, 2, 7);
		//test placement
		assertEquals(s1, b.board[2][3]);
		assertEquals(s1, b.board[2][4]);
		assertEquals(s1, b.board[2][5]);
		assertEquals(s1, b.board[2][6]);
		assertEquals(s1, b.board[2][7]);
		//test the ship data members
		for (int i = 0; i < s1.size; i++) {
			assertEquals(2,s1.coordinatesR[i]);
			assertEquals(i+3, s1.coordinatesC[i]);
		}
		//Test collision
		Ship s2 = new Ship(4);
		b.placeShip(s2, 2, 0, 2, 3);
		assertNull(b.board[2][0]);
		assertNull(b.board[2][2]);
		assertEquals(s1, b.board[2][3]);
		b.placeShip(s2, 0, 4, 3, 4);
		assertNull(b.board[0][4]);
		assertNull(b.board[1][4]);
		assertNull(b.board[3][4]);
		assertEquals(s1, b.board[2][4]);
		b.placeShip(s2, 2, 9, 2, 6);
		assertNull(b.board[2][9]);
		assertNull(b.board[2][8]);
		assertEquals(s1, b.board[2][7]);
		assertEquals(s1, b.board[2][6]);
		b.placeShip(s2, 5, 5, 2, 5);
		assertNull(b.board[5][5]);
		assertNull(b.board[4][5]);
		assertNull(b.board[3][5]);
		assertEquals(s1, b.board[2][5]);
		b.placeShip(s2, 8, 1, 5, 1);
		assertEquals(s2, b.board[5][1]);
		assertEquals(s2, b.board[6][1]);
		assertEquals(s2, b.board[7][1]);
		assertEquals(s2, b.board[8][1]);
		assertNull(b.board[9][1]);
		for (int i = 0; i < s2.size; i++) {
			assertEquals(i + 5, s2.coordinatesR[i]);
			assertEquals(1, s2.coordinatesC[i]);
		}
		b.printBoard();
	}

}
