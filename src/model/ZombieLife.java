package model;

import java.util.ArrayList;
import java.util.Arrays;

import edu.princeton.cs.algs4.BreadthFirstPaths;
import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

/**
 * Using a graph-based implementation of Conway's Game of Life (classic ruleset
 * B3/S23), 'Zombie' cells use BFS to attack all life.
 * 
 * @author Tommy Collier
 * @author Paul Nguyen
 */
public class ZombieLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int nrows;
	private int ncols;
	private int[][] zombieTargets; // row-col indexed
	private int zombieCount = 0;
	private static final CellState COLOR_1 = CellState.GREEN;

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.zombieTargets = new int[nrows * ncols][2];
		this.nrows = nrows;
		this.ncols = ncols;

		clear();

		// Initialize edges/neighbors
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current);

		// Initialize zombie targets
		for (int i = 0; i < zombieTargets.length; i++) {
			zombieTargets[i][0] = -1; // Target
			zombieTargets[i][1] = 0; // Amount of steps spent chasing target.
		}
	}

	/**
	 * Returns index of cell based on row and col
	 *
	 * @param row
	 * @param col
	 * @return int cell index
	 */
	private int convertToIndex(int row, int col) {
		return row * ncols + col;
	}

	/**
	 * Returns cell's row based on index
	 *
	 * @param index
	 * @return int cell's row
	 */
	private int convertToRow(int index) {
		return index / ncols;
	}

	/**
	 * Returns cell's col based on index
	 *
	 * @param index
	 * @return int cell's col
	 */
	private int convertToCol(int index) {
		return index % ncols;
	}

	/**
	 * Adds neighbor edges to given cell.
	 *
	 * @param index
	 */
	private void initializeNeighbors(int index) {
		int row = convertToRow(index);
		int col = convertToCol(index);

		int[] rowOffsets = { (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows };
		int[] colOffsets = { (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols };

		for (int r : rowOffsets)
			for (int c : colOffsets)
				if (r != row || c != col) { // Disclude current cell
					int neighbor = convertToIndex(r, c);
					if (hasEdge(index, neighbor) != true)
						world.addEdge(index, neighbor);
				}
	}

	/**
	 * Checks if vertex already linked to neighbor.
	 *
	 * @param index
	 * @param neighbor
	 * @return boolean true/false linked to neighbor
	 */
	private boolean hasEdge(int index, int neighbor) {
		for (int n : world.adj(index))
			if (n == neighbor)
				return true;
		return false;
	}

	@Override
	public void clear() {
		Arrays.fill(cells, CellState.DEAD);
		zombieTargets = new int[nrows * ncols][2];
	}

	@Override
	public void randomize() {
		for (int current = 0; current < cells.length; current++)
			if (RANDOM.nextBoolean())
				cells[current] = CellState.ALIVE;
			else
				cells[current] = CellState.DEAD;

		// Generate 1 zombie.
		cells[RANDOM.nextInt(cells.length)] = COLOR_1;
	}

	@Override
	public CellState get(int row, int col) {
		return cells[convertToIndex(row, col)];
	}

	@Override
	public void set(int row, int col, CellState state) {
		cells[convertToIndex(row, col)] = state;
	}

	@Override
	public boolean step(Callback action) {
		Queue<Cell> queue = new Queue<>();

		// Calculate needed updates
		for (int current = 0; current < cells.length; current++) {
			// If cell is ZOMBIE
			if ((cells[current] == COLOR_1)) {
				// Find alive cells
				ArrayList<Integer> aliveCells = new ArrayList<>();
				for (int cell = 0; cell < cells.length; cell++)
					if (cells[cell] == CellState.ALIVE)
						aliveCells.add(cell);

				int oldRow = convertToRow(current);
				int oldCol = convertToCol(current);

				// if zombie count gets too high, they randomly (50-50) starve.
				if ((double) zombieCount / aliveCells.size() > 1.5 && RANDOM.nextBoolean()) {
					queue.enqueue(new Cell(oldRow, oldCol, CellState.DEAD));
					zombieTargets[current][0] = -1; // Reset target
					zombieTargets[current][1] = 0; // Reset target step count
					zombieCount--;
				}
				// Else if there's alive cells to infect, get em.
				else if (aliveCells.size() > 0) {
					BreadthFirstPaths bfs = new BreadthFirstPaths(world, current);

					// If current target has been chased too long or it hasn't been found yet, get
					// new one.
					if (zombieTargets[current][1] > 5 || zombieTargets[current][0] == -1) {
						zombieTargets[current][1] = 0; // Reset target step count
						zombieTargets[current][0] = aliveCells.get(0);
						int closestDistance = bfs.distTo(zombieTargets[current][0]);

						for (int i = 1; i < aliveCells.size(); i++) {
							int currentCell = aliveCells.get(i);
							int currentDistance = bfs.distTo(aliveCells.get(i));
							if (currentDistance < closestDistance)
								zombieTargets[current][0] = currentCell;
						}
					}
					// Else keep current target and record that its been pursued while dead again.
					else {
						if (cells[zombieTargets[current][0]] == CellState.DEAD)
							zombieTargets[current][1]++;
					}

					// Get the next position.
					var path = bfs.pathTo(zombieTargets[current][0]).iterator();
					path.next();
					int nextPosition = current;
					if (path.hasNext())
						nextPosition = path.next();

					// Ensure zombie only moves into empty/dead space.
					// if next position not empty, pick a random empty position or stay in place.
					if (cells[nextPosition] != CellState.DEAD) {
						// Get all possible positions
						ArrayList<Integer> availablePositions = new ArrayList<Integer>();
						availablePositions.add(current);

						var neighbors = world.adj(current);
						for (int neighbor : neighbors) {
							if (cells[neighbor] == CellState.DEAD)
								availablePositions.add(neighbor);
						}

						// Pick a random position for next position
						nextPosition = availablePositions
						    .get(RANDOM.nextInt(availablePositions.size()));
					}

					// If moving, move.
					if (current != nextPosition) {
						int newRow = convertToRow(nextPosition);
						int newCol = convertToCol(nextPosition);

						queue.enqueue(new Cell(oldRow, oldCol, CellState.DEAD));
						queue.enqueue(new Cell(newRow, newCol, COLOR_1));

						// Set new zombie target values
						zombieTargets[convertToIndex(newRow,
						    newCol)][0] = zombieTargets[current][0];
						zombieTargets[convertToIndex(newRow,
						    newCol)][1] = zombieTargets[current][1];
						zombieTargets[current][0] = -1;
						zombieTargets[current][1] = 0;
					}
					// If not moving, stay in place
					else {
						queue.enqueue(new Cell(oldRow, oldCol, COLOR_1));
					}
				}
				// Else zombie starves.
				else {
					queue.enqueue(new Cell(oldRow, oldCol, CellState.DEAD));
				}
			}
			// else cell is ALIVE or DEAD
			else {
				// Count amount of alive neighbors
				int aliveNeighbors = 0;
				for (int neighbor : world.adj(current)) {
					if (cells[neighbor] == CellState.ALIVE)
						aliveNeighbors++;

					if (cells[neighbor] == COLOR_1)
						;
				}

				// Check if there's a zombie neighbor
				boolean zombieNeighbor = false;
				for (int neighbor : world.adj(current)) {
					if (cells[neighbor] == COLOR_1)
						zombieNeighbor = true;
				}

				// Record needed updates
				int row = convertToRow(current);
				int col = convertToCol(current);

				if (cells[current] == CellState.ALIVE) {
					if (zombieNeighbor) {// If cell has a zombie neighbor, cell becomes a zombie.
						queue.enqueue(new Cell(row, col, COLOR_1));
						zombieTargets[current][0] = -1; // Reset target
						zombieTargets[current][1] = 0; // Reset target step count
					}
					else if (aliveNeighbors < 2 || aliveNeighbors > 3) // Alive cells only stay
					                                                   // alive if between 2-3
					                                                   // neighbors.
						queue.enqueue(new Cell(row, col, CellState.DEAD));
				}
				else { // if (cells[i].state() == CellState.DEAD)
					if (aliveNeighbors == 3) // Dead cell with 3 neighbors becomes alive.
						queue.enqueue(new Cell(row, col, CellState.ALIVE));
				}

			}
		}

		boolean worldChanged = false;

		// Make needed updates (done afterwards to prevent invalid updates)
		int newZombieCount = 0;
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();

			// Invoke callback if a new state differs from old state
			if (cell.state() != get(cell.row(), cell.col())) {
				action.invoke(cell.row(), cell.col(), cell.state());
				worldChanged = true;
			}
			set(cell.row(), cell.col(), cell.state());

			if (cell.state() == COLOR_1)
				newZombieCount++;
		}
		zombieCount = newZombieCount;

		return worldChanged;
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			if (cells[current] == CellState.ALIVE || cells[current] == COLOR_1)
				action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}

	@Override
	public long populationCount() {
		long count = 0;

		for (var state : cells)
			if (state == CellState.ALIVE || state == COLOR_1)
				count++;

		return count;
	}

	/**
	 * @return Description of this model
	 */
	public String description() {
		return "Zombies vs The Game of Life.\nZombies use BFS to attack all life.";
	}
}
