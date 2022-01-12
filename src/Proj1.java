import java.util.Scanner;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Starter code for Project 1: wireless router signal strength.
 * This program determines the minimum signal strength and location
 * of the lowest signal cell of a grid of cells which contains a router
 * at 23dB.
 * Author: Duncan Parke
 * jhed: dparke2
 * 1/11/2022
 * Version 1.0: Created program with all functionality as described
 *              by the confines of the assignment.
 */
public class Proj1 {

   public static void main(String[] args) throws IOException {
      // THIS METHOD IS COMPLETE - DO NOT CHANGE IT

      final double EPS = .0001;
      final String PROMPT1 = "Enter name of grid data file: ";
      final String ERROR = "ERROR: invalid input file";

      Scanner kb = new Scanner(System.in);
      System.out.print(PROMPT1);
      String name = kb.nextLine();

      FileInputStream infile = new FileInputStream(name);
      Scanner scanFile = new Scanner(infile);
      double size = scanFile.nextDouble();
      int rows = scanFile.nextInt();
      int cols = scanFile.nextInt();
      scanFile.nextLine(); // skip past end of line character 

      FileOutputStream outstream = new FileOutputStream("signals.txt");
      PrintWriter outfile = new PrintWriter(outstream);

      Cell[][] grid = new Cell[rows][cols];
      Cell[][] old = new Cell[rows][cols];
      initialize(grid);
      initialize(old);

      int routerRow;
      int routerCol;
      final double ROUTER = 23;


      read(grid, scanFile);
      if (!isValid(grid)) {
         System.out.println(ERROR);
      } else {
         // keep processing
         System.out.print("Enter router row and column: ");
         routerRow = kb.nextInt();
         routerCol = kb.nextInt();
         grid[routerRow][routerCol].setSignal(ROUTER);

         setAllDirections(grid, routerRow, routerCol);
         setAllDistances(grid, routerRow, routerCol, size);

         while (!equivalent(grid, old, EPS)) {
            copy(grid, old);
            iterate(grid, old, routerRow, routerCol);
            printAll(grid, outfile);
            outfile.println();  // blank link separator
         }
      }

      double minSignal = findMinSignal(grid);
      System.out.println("minimum signal strength: " + minSignal +
         " occurs in these cells: ");
      printMinCellCoordinates(grid, minSignal);

      outstream.flush();
      outfile.close();
   }

   /**
    * Set the direction from the router position to every other cell
    * in the grid. (Do not change the direction of the router cell.)
    *
    * @param grid      the grid of cells to manage
    * @param routerRow the row position of the router cell in the grid
    * @param routerCol the column position of the router cell in the grid
    */
   public static void setAllDirections(Cell[][] grid, int routerRow,
                                       int routerCol) {
      for (int i = 0; i < grid.length; i++) {
         for (int j = 0; j < grid[i].length; j++) {
            if (i == routerRow && j == routerCol) {
               grid[i][j].setDirection("--");
            } else {
               grid[i][j].setDirection(direction(routerRow, routerCol, i, j));
            }
         }
      }

   }

   /**
    * Set the distance from the router position to every other cell
    * in the grid. (Do not change the distance of the router cell.)
    *
    * @param grid      the grid of cells to manage
    * @param routerRow the row position of the router cell in the grid
    * @param routerCol the column position of the router cell in the grid
    * @param size      the size of each cell
    */
   public static void setAllDistances(Cell[][] grid, int routerRow,
                                      int routerCol, double size) {
      //THIS METHOD IS COMPLETE -DO NOT CHANGE IT
      for (int i = 0; i < grid.length; i++) {
         for (int j = 0; j < grid[0].length; j++) {
            if (!(i == routerRow && j == routerCol)) {
               grid[i][j].setDistance(size * Math.sqrt(Math.pow(routerRow - i,
                  2) + Math.pow(routerCol - j, 2)));
            }
         }
      }
   }


   /**
    * Iterate over the grid, updating the signal strength and
    * attenuation rate of each cell based on the old values of
    * the relevant neighbor cells.
    *
    * @param current   the updated values of each cell
    * @param previous  the old values of each cell
    * @param routerRow the row position of the router's cell
    * @param routerCol the column position of the router's cell
    */
   public static void iterate(Cell[][] current, Cell[][] previous,
                              int routerRow, int routerCol) {
      for (int i = 0; i < current.length; i++) {
         for (int j = 0; j < current[0].length; j++) {
            if (i == routerRow && j == routerCol) {
               current[i][j].setSignal(23); // this is redundant
            } else {
               current[i][j].setRate(attenRate(previous, i, j));
               current[i][j].setSignal(23 -
                  (current[i][j].getRate() + (fspl(previous[i][j].getDistance(),
                     5.0))));

            }
         }
      }
   }

   /**
    * Calculate the signal transmission free space path loss (FSPL).
    *
    * @param distance  the distance from the source to the receiver
    * @param frequency the frequency of the transmission
    * @return the fspl ratio
    */
   public static double fspl(double distance, double frequency) {
      return ((20 * Math.log10(distance)) + (20 * Math.log10(frequency)) +
         92.45);
   }

   /**
    * Calculate the attenuation rate of a cell based on the
    * attenuation of its relevant neighbor(s).
    *
    * @param prev the grid of cells from prior iteration
    * @param row  the row of the current cell
    * @param col  the column of the current cell
    * @return the new attenuation rate of that cell
    */
   public static int attenRate(Cell[][] prev, int row, int col) {
      String tempString;
      tempString = prev[row][col].getDirection();
      switch (tempString) {
         // if the cell is north of the router cell
         case "N": return prev[row + 1][col].getRate() +
            attenuation(prev[row][col].getSouth());
         // if the cell is south of the router cell
         case "S": return prev[row - 1][col].getRate() +
            attenuation(prev[row][col].getNorth());
         // if the cell is east of the router cell
         case "E": return prev[row][col - 1].getRate() +
            attenuation(prev[row][col].getWest());
         // if the cell is west of the router cell
         case "W":  return prev[row][col + 1].getRate() +
            attenuation(prev[row][col].getEast());
         // if the cell is north-east of the router cell
         case "NE": return Math.max(prev[row + 1][col].getRate() +
            attenuation(prev[row][col].getSouth()),
            prev[row][col - 1].getRate() +
            attenuation(prev[row][col].getWest()));
         // if the cell is north-west of the router cell
         case "NW": return Math.max(prev[row + 1][col].getRate() +
            attenuation(prev[row][col].getSouth()),
            prev[row][col + 1].getRate() +
            attenuation(prev[row][col].getEast()));
         // if the cell is south-east of the router cell
         case "SE": return Math.max(prev[row - 1][col].getRate() +
               attenuation(prev[row][col].getNorth()),
               prev[row][col - 1].getRate() +
               attenuation(prev[row][col].getWest()));
         // if the cell is south-west of the router cell
         case "SW": return Math.max(prev[row - 1][col].getRate() +
               attenuation(prev[row][col].getNorth()),
               prev[row][col + 1].getRate() +
               attenuation(prev[row][col].getEast()));
         // if the cell is the router cell
         default: return 0;
      }

   }


   /**
    * Find the direction between the router cell and the current cell.
    *
    * @param r0 the router row
    * @param c0 the router column
    * @param r1 the current cell row
    * @param c1 the current cell column
    * @return a string direction heading (N, E, S, W, NE, SE, SW, NW)
    */
   public static String direction(int r0, int c0, int r1, int c1) {
      //THIS METHOD IS COMPLETE -DO NOT CHANGE IT
      int rDelta = Math.abs(r0 - r1);
      int cDelta = Math.abs(c0 - c1);

      if (rDelta > cDelta) {
         if (r1 < r0) {
            return "N";
         } else if (r1 > r0) {
            return "S";
         }
      }
      if (rDelta < cDelta) {
         if (c1 > c0) {
            return "E";
         } else if (c1 < c0) {
            return "W";
         }
      }

      // rDelta == cDelta -> on a diagonal
      if (r1 < r0 && c1 > c0) {
         return "NE";
      } else if (r1 < r0 && c1 < c0) {
         return "NW";
      } else if (r1 > r0 && c1 < c0) {
         return "SW";
      } else {
         return "SE";
      }
   }


   /**
    * Determine if the corresponding cells in the two grids of the same size
    * have the same signal value, to a specified precision.
    *
    * @param grid1   the first grid
    * @param grid2   the second grid
    * @param epsilon the difference cutoff that makes two values "equivalent"
    * @return true if the grids are the same sizes, and the signal values
    * are all within (<=) epsilon of each other; false otherwise
    */

   public static boolean equivalent(Cell[][] grid1, Cell[][] grid2,
                                    double epsilon) {
      if (grid1.length == grid2.length && grid1[0].length == grid2[0].length) {
         for (int i = 0; i < grid1.length; i++) {
            for (int j = 0; j < grid1[0].length; j++) {
               if (Math.abs(grid2[i][j].getSignal() - grid1[i][j].getSignal()) >
                  epsilon) { //abs(grid 2 sig
                  // - grid 1 sig) > epsilon
                  return false;
               }
            }
         }
         return true;
      }
      return false;
   }


   /**
    * Read a grid from a plain text file using a Scanner that has
    * already advanced past the first line. This method assumes the
    * specified file exists. Each subsequent line provides the wall
    * information for the cells in a single row, using a 4-character
    * string in NESW (north-east-south-west) order for each cell.
    *
    * @param grid is the grid whose Cells must be updated with the input data
    * @param scnr is the Scanner to use to read the rest of the file
    * @throws IOException if file can not be read
    */
   public static void read(Cell[][] grid, Scanner scnr) throws IOException {
      for (Cell[] cells : grid) {
         for (int j = 0; j < grid[0].length; j++) {
            cells[j].setWalls(scnr.next());
         }
      }
   }


   /**
    * Validate the cells of a maze as being consistent with respect
    * to neighboring internal walls. For example, suppose some cell
    * C has an east wall with material 'b' for brick. Then for the
    * maze to be valid, the cell to C's east must have a west wall
    * that is also 'b' for brick. (This method does not need to check
    * external walls.)
    *
    * @param grid the grid to check
    * @return true if valid (consistent), false otherwise
    */
   public static boolean isValid(Cell[][] grid) {
      for (int i = 0; i < grid.length; i++) { //for each row
         for (int j = 0; j < grid[0].length; j++) { //for each column
            // if there is a wall to the north
            if (i > 0) {
               if (grid[i][j].getNorth() != grid[i - 1][j].getSouth()) {
                  return false;
               }
            }
            //if there is a wall to the east
            if (j < grid[0].length - 1) {
               if (grid[i][j].getEast() != grid[i][j + 1].getWest()) {
                  return false;
               }
            }
            // if there is a wall to the south
            if (i < grid.length - 1) {
               if (grid[i][j].getSouth() != grid[i + 1][j].getNorth()) {
                  return false;
               }
            }
            //if there is a wall to the west
            if (j > 0) {
               if (grid[i][j].getWest() != grid[i][j - 1].getEast()) {
                  return false;
               }
            }
         }
      }
      return true; //if the method finds no discrepancies, return true
   }

   /**
    * Find the minimum cell signal strength.
    *
    * @param grid the grid of cells to search
    * @return the minimum signal value
    */
   public static double findMinSignal(Cell[][] grid) {
      double minSignal;
      minSignal = grid[0][0].getSignal();
      for (Cell[] cells : grid) {
         for (int j = 0; j < grid[0].length; j++) {
            if (cells[j].getSignal() < minSignal) {
               minSignal = cells[j].getSignal();
            }
         }
      }
      return minSignal;
   }

   /**
    * Print the coordinates of cells with <= the minimum signal strength,
    * one per line in (i, j) format, in row-column order.
    *
    * @param grid      the collection of cells
    * @param minSignal the minimum signal strength
    */
   public static void printMinCellCoordinates(Cell[][] grid, double minSignal) {
      for (int i = 0; i < grid.length; i++) {
         for (int j = 0; j < grid[0].length; j++) {
            if (grid[i][j].getSignal() == minSignal) {
               System.out.println(i + ", " + j);
            }
         }
      }

   }

   /**
    * Get the attenuation rate of a wall material.
    *
    * @param wall the material type
    * @return the attenuation rating
    */
   public static int attenuation(char wall) {
      // THIS METHOD IS COMPLETE - DO NOT CHANGE IT
      switch (wall) {
         case 'b':
            return 22;
         case 'c':
            return 6;
         case 'd':
            return 4;
         case 'g':
            return 20;
         case 'w':
            return 6;
         case 'n':
            return 0;
         default:
            System.out.println("ERROR: invalid wall type");
      }
      return -1;
   }


   /**
    * Create a copy of a grid by copying the contents of each
    * Cell in an original grid to a copy grid. Note that we use the
    * makeCopy method in the Cell class for this to work correctly.
    *
    * @param from the original grid
    * @param to   the copy grid
    */
   public static void copy(Cell[][] from, Cell[][] to) {
      // THIS METHOD IS COMPLETE - DO NOT CHANGE IT
      for (int i = 0; i < from.length; i++) {
         for (int j = 0; j < from[0].length; j++) {
            to[i][j] = from[i][j].makeCopy();
         }
      }
   }

   /**
    * Initialize a grid to contain a bunch of new Cell objects.
    *
    * @param grid the array to initialize
    */
   public static void initialize(Cell[][] grid) {
      // THIS METHOD IS COMPLETE - DO NOT CHANGE IT
      for (int i = 0; i < grid.length; i++) {
         for (int j = 0; j < grid[0].length; j++) {
            grid[i][j] = new Cell();
         }
      }
   }

   /**
    * Display the computed values of a grid (signal strength, direction,
    * attenuation rate, and distance) to the provided output destination,
    * using the format provided by the toString method in the Cell class.
    *
    * @param grid the signal grid to display
    * @param pout the output location
    */
   public static void printAll(Cell[][] grid, PrintWriter pout) {
      // THIS METHOD IS COMPLETE - DO NOT CHANGE IT
      for (int i = 0; i < grid.length; i++) {
         for (int j = 0; j < grid[0].length; j++) {
            pout.print(grid[i][j].toString() + " ");
         }
         pout.println();
      }
   }
}
