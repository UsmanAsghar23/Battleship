public class board {
    private final int size = 10;  // Standard size for Battleship
    private int[][] grid;
    public static final int EMPTY = 0;
    public static final int SHIP = 1;
    public static final int HIT = 2;
    public static final int MISS = 3;




    public board() {
        grid = new int[size][size];
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                grid[i][j] = EMPTY;  // Initialize all cells to be empty
            }
        }
    }

    public int getCell(int x, int y) {
        return grid[x][y];
    }


    public void setCell(int x, int y, int value) {
        grid[x][y] = value;
    }



    // Place a ship at a specified location and orientation
    public boolean placeShip(int x, int y, int length, boolean horizontal) {
        if (horizontal) {
            if (y + length > size) return false;  // Check if the ship fits horizontally
            for (int i = 0; i < length; i++) {
                if (grid[x][y + i] != EMPTY) return false;  // Check if the area is free
                grid[x][y + i] = SHIP;
            }
        } else {
            if (x + length > size) return false;  // Check if the ship fits vertically
            for (int i = 0; i < length; i++) {
                if (grid[x + i][y] != EMPTY) return false;  // Check if the area is free
                grid[x + i][y] = SHIP;
            }
        }
        return true;
    }




    // Record an attack at a specified location
    public boolean attack(int x, int y) {
        if (grid[x][y] == SHIP) {
            grid[x][y] = HIT;
            return true;
        } else {
            grid[x][y] = MISS;
            return false;
        }
    }




    // Display the board (for debugging or CLI version)
    public void printBoard() {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                System.out.print(grid[i][j] + " ");
                switch (grid[i][j]) {
                    case EMPTY:
                        //System.out.print("- ");
                        break;
                    case SHIP:
                        //System.out.print("S ");
                        break;
                    case HIT:
                        //System.out.print("X ");
                        break;
                    case MISS:
                        //System.out.print("O ");
                        break;
                }
            }
            System.out.println();
        }
    }
}


