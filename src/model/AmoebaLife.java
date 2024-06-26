package model;

import java.util.ArrayList;
import java.util.Arrays;

import edu.princeton.cs.algs4.BreadthFirstPaths;
import edu.princeton.cs.algs4.Graph;
import edu.princeton.cs.algs4.Queue;

/**
 * A graph-based amoeba simulation. Includes growth, mitosis, and hunger-based
 * population control.
 * 
 * @author Tommy Collier
 * @author Paul Nguyen
 */
public class AmoebaLife implements ILife {
	private Graph world;
	private CellState[] cells; // row-col indexed
	private int[][] amoebaInfo; // row-col indexed
	private int nrows;
	private int ncols;
	private Queue<Cell> queue = new Queue<>(); // Stores cell updates
	private ArrayList<Integer> alreadyMoved; // Stores already moved cells
	private static final int GROWTH_STAGE_1 = 10; // 5 wide stage
	private static final int GROWTH_STAGE_2 = 20; // 7 wide stage
	private static final int GROWTH_STAGE_3 = 30; // Cell splitting stage
	private static final CellState FOOD = CellState.BLUE; // Food cells
	private static final CellState BODY = CellState.GREEN; // Body cells
	private static final CellState NUCLEUS = CellState.RED; // Nucleus cells

	@Override
	public void resize(int nrows, int ncols) {
		this.world = new Graph(nrows * ncols);
		this.cells = new CellState[nrows * ncols];
		this.amoebaInfo = new int[nrows * ncols][2];
		this.nrows = nrows;
		this.ncols = ncols;

		clear();

		// Initialize edges/neighbors
		for (int current = 0; current < cells.length; current++)
			initializeNeighbors(current);
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

		// Initialize amoeba growth count and hunger
		for (int i = 0; i < amoebaInfo.length; i++) {
			amoebaInfo[i][0] = 1; // Growth count
			amoebaInfo[i][1] = 1; // Hunger/steps without food
		}
	}

	@Override
	public void randomize() {
		clear();

		// 3 Nuclei
		for (int i = 0; i < 3; i++) {
			int randomInt = RANDOM.nextInt(nrows * ncols);
			cells[randomInt] = NUCLEUS;
			setGrowthStage(convertToRow(randomInt), convertToCol(randomInt));
		}
		// 1 Food
		cells[RANDOM.nextInt(nrows * ncols)] = FOOD;
	}

	/**
	 * Moves cell's growth and hunger count to next cell.
	 * 
	 * @param current Current cell position
	 * @param next    Next cell position
	 */
	private void moveAmoebaInfo(int current, int next) {
		amoebaInfo[next][0] = amoebaInfo[current][0];
		amoebaInfo[next][1] = amoebaInfo[current][1];
	}

	/**
	 * Sets the body cells in the current world and in the update based on the
	 * current growth stage.
	 * 
	 * @param row Nucleus next position row
	 * @param col Nucleus next position col
	 */
	private void setGrowthStage(int row, int col) {
		int nucleus = convertToIndex(row, col);

		// Default growth stage:
		if (amoebaInfo[nucleus][0] >= 0) {
			int[] rowOffsets = { (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows };
			int[] colOffsets = { (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols };

			// Fill neighbors
			for (int r : rowOffsets)
				for (int c : colOffsets) {
					fillBody(r, c);
				}
		}
		// 1st growth stage:
		if (amoebaInfo[nucleus][0] > GROWTH_STAGE_1) {
			int[] rowOffsets = { (row - 2 + nrows) % nrows, (row - 1 + nrows) % nrows, row,
			    (row + 1 + nrows) % nrows, (row + 2 + nrows) % nrows };
			int[] colOffsets = { (col - 2 + ncols) % ncols, (col - 1 + ncols) % ncols, col,
			    (col + 1 + ncols) % ncols, (col + 2 + ncols) % ncols };

			// Fill outer sides
			for (int i = 1; i < 4; i++) {
				fillBody(rowOffsets[0], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[0]);
				fillBody(rowOffsets[4], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[4]);
			}
		}
		// 2nd growth stage:
		if (amoebaInfo[nucleus][0] > GROWTH_STAGE_2) {
			int[] rowOffsets = { (row - 3 + nrows) % nrows, (row - 2 + nrows) % nrows,
			    (row - 1 + nrows) % nrows, row, (row + 1 + nrows) % nrows,
			    (row + 2 + nrows) % nrows, (row + 3 + nrows) % nrows };
			int[] colOffsets = { (col - 3 + ncols) % ncols, (col - 2 + ncols) % ncols,
			    (col - 1 + ncols) % ncols, col, (col + 1 + ncols) % ncols,
			    (col + 2 + ncols) % ncols, (col + 3 + ncols) % ncols };

			// Fill outer sides
			for (int i = 1; i < 6; i++) {
				fillBody(rowOffsets[0], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[0]);
				fillBody(rowOffsets[6], colOffsets[i]);
				fillBody(rowOffsets[i], colOffsets[6]);
			}
			// Fill middle corners
			fillBody(rowOffsets[1], colOffsets[1]);
			fillBody(rowOffsets[1], colOffsets[5]);
			fillBody(rowOffsets[5], colOffsets[1]);
			fillBody(rowOffsets[5], colOffsets[5]);
		}
	}

	/**
	 * Adds body cell so long as it doesn't cover a nucleus cell.
	 * 
	 * @param row
	 * @param col
	 */
	private void fillBody(int row, int col) {
		int cell = convertToIndex(row, col);
		if (cells[cell] != NUCLEUS) {
			cells[cell] = BODY;
			queue.enqueue(new Cell(row, col, BODY));
		}
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
		ArrayList<Integer> foodIndexes = new ArrayList<>();
		alreadyMoved = new ArrayList<>(); // Stores movements to prevent multiple movements in one
		                                  // step.
		int nucleusCount = 0;
		int deadCellsSkipped = 0; // Counts dead cells skipped each step.
		int noMoreFood = 0; // Counts steps without food for population control.

		for (int i = 0; i < cells.length; i++) {
			if (cells[i] == NUCLEUS)
				nucleusCount++;
			if (cells[i] == FOOD)
				foodIndexes.add(i);
		}

		// Calculate needed updates:
		for (int current = 0; current < cells.length; current++) {
			// Skip cells that have already been moved/dealt with.
			if (!alreadyMoved.contains(current)) {
				int row = convertToRow(current);
				int col = convertToCol(current);

				// If cell food, stay food and float around.
				if (cells[current] == FOOD) {
					// Chance to float around.
					if (RANDOM.nextInt(4) == 0) {
						// Get all possible positions
						ArrayList<Integer> availablePositions = getPossiblePositions(current);

						// Pick a random position for next position.
						int nextPosition = availablePositions
						    .get(RANDOM.nextInt(availablePositions.size()));

						queue.enqueue(
						    new Cell(convertToRow(nextPosition), convertToCol(nextPosition), FOOD));
					}
					// Else stay in place.
					else
						queue.enqueue(new Cell(convertToRow(current), convertToCol(current), FOOD));
				}
				// If cell dead/empty or body and alone, chance to become food if all dead.
				else if (cells[current] != NUCLEUS)
					// Skip most cells (helps with performance & random spawn chance).
					if (deadCellsSkipped < nrows * ncols)
						if (cells[current] != BODY) {
							deadCellsSkipped = 0;

							// Find whether cell is alone.
							boolean alone = true;
							for (Integer neighbor : world.adj(current))
								if (cells[neighbor] != CellState.DEAD)
									alone = false;

							// Chance of becoming food.
							if (RANDOM.nextInt(ncols * nrows * 6) == 0 && alone)
								queue.enqueue(new Cell(row, col, FOOD));
						}
						else
							deadCellsSkipped++;
					else
						deadCellsSkipped++;
				// Else cell is nucleus.
				else {
					// If there's food to get, target/eat it.
					if (foodIndexes.size() > 0) {
						// Find closest food.
						BreadthFirstPaths bfs = new BreadthFirstPaths(world, current);
						int target = foodIndexes.get(0);
						int targetDistance = bfs.distTo(target);

						for (int food : foodIndexes)
							if (bfs.distTo(food) < targetDistance) {
								target = food;
								targetDistance = bfs.distTo(food);
							}

						// If too big, split into 2 nucleus cells.
						if (amoebaInfo[current][0] > GROWTH_STAGE_3) {
							// Get all possible positions
							ArrayList<Integer> availablePositions = getPossiblePositions(current);

							// Pick a random position for next position.
							int twinPosition = availablePositions
							    .get(RANDOM.nextInt(availablePositions.size()));

							// Keep original cell with the default growth stage and hunger.
							queue.enqueue(new Cell(row, col, NUCLEUS));
							setGrowthStage(row, col);
							amoebaInfo[current][0] = 1;
							amoebaInfo[current][1] = 1;

							// Add twin cell with the default growth stage and hunger.
							int twinRow = convertToRow(twinPosition);
							int twinCol = convertToCol(twinPosition);

							cells[twinPosition] = NUCLEUS;
							queue.enqueue(new Cell(twinRow, twinCol, NUCLEUS));
							setGrowthStage(twinRow, twinCol);
							amoebaInfo[twinPosition][0] = 1;
							amoebaInfo[twinPosition][1] = 1;
						}
						// Growth Stage default eating.
						else if (targetDistance <= 2) {
							eat(current, target);
						}
						// Growth Stage 1 eating.
						else if (targetDistance <= 3 && amoebaInfo[current][0] > GROWTH_STAGE_1) {
							eat(current, target);
						}
						// Growth Stage 2 eating.
						else if (targetDistance <= 4 && amoebaInfo[current][0] > GROWTH_STAGE_2) {
							eat(current, target);
						}
						// Continue targeting.
						else {
							amoebaInfo[current][1]++; // Add 1 to hunger.
							int bestPosition = current;
							int alternativePosition = current;

							// Get all possible positions
							ArrayList<Integer> availablePositions = getPossiblePositions(current);

							// Pick the closest available position to the target.
							int currentDistance = targetDistance;
							for (int neighbor : availablePositions) {
								BreadthFirstPaths neighborPath = new BreadthFirstPaths(world,
								    neighbor);
								if (currentDistance > neighborPath.distTo(target)) {
									currentDistance = neighborPath.distTo(target);
									if (bestPosition != current)
										alternativePosition = bestPosition;
									bestPosition = neighbor;
								}
							}

							// If best position gets/stays too close to another nucleus, move to the
							// alternative position.
							if (getPossiblePositions(bestPosition)
							    .size() >= getPossiblePositions(alternativePosition).size() - 1)
								move(current, bestPosition);
							else
								move(current, alternativePosition);
						}
					}
					// Else no food to eat, baby dies or move randomly.
					else {
						// If population too high and food is running out too frequently, kill the
						// babies randomly until level.
						if ((nucleusCount > (nrows * ncols) / 50 && amoebaInfo[current][0] == 1)
						    || (noMoreFood > 50 && amoebaInfo[current][0] == 1)) {
							cells[current] = CellState.DEAD;
							queue.enqueue(new Cell(row, col, CellState.DEAD));
							nucleusCount--;
							noMoreFood = 0;
						}
						else {
							// Get all possible random positions
							ArrayList<Integer> availablePositions = getPossiblePositions(current);

							// Pick a random position for next position.
							int randomPosition = availablePositions
							    .get(RANDOM.nextInt(availablePositions.size()));
							move(current, randomPosition);
						}

						noMoreFood++;
					}
					setGrowthStage(row, col);
				}
			}
		}

		// Make needed updates (done afterwards to prevent invalid updates)
		Arrays.fill(cells, CellState.DEAD);
		while (!queue.isEmpty()) {
			Cell cell = queue.dequeue();
			// Invoke callback if a new state differs from old state
			if (cell.state() != get(cell.row(), cell.col())) {
				action.invoke(cell.row(), cell.col(), cell.state());
			}
			set(cell.row(), cell.col(), cell.state());
		}
		for (int current = 0; current < cells.length; current++)
			action.invoke(convertToRow(current), convertToCol(current), cells[current]);

		return populationCount() > 0; // Game designed to go on as long as Amoeba still alive.
	}

	/**
	 * Allows nucleus to eat the target and stay in the world.
	 * 
	 * @param current Current nucleus cell
	 * @param target  Target food cell
	 */
	private void eat(int current, int target) {
		int row = convertToRow(current);
		int col = convertToCol(current);

		amoebaInfo[current][0]++; // Add to size.
		// Kill the food.
		cells[target] = CellState.DEAD;
		queue.enqueue(new Cell(convertToRow(target), convertToCol(target), CellState.DEAD));
		// Keep the nucleus.
		queue.enqueue(new Cell(row, col, NUCLEUS));
		setGrowthStage(row, col);
	}

	/**
	 * Returns a list of all neighbor positions that aren't already taken by a
	 * nucleus. If no positions are available, current position is put in the list.
	 * 
	 * @param current Current cell
	 * @return availablePositions List of possible positions
	 */
	private ArrayList<Integer> getPossiblePositions(int current) {
		ArrayList<Integer> availablePositions = new ArrayList<Integer>();

		var neighbors = world.adj(current);
		for (int neighbor : neighbors) {
			if (cells[neighbor] != NUCLEUS)
				availablePositions.add(neighbor);
		}
		if (availablePositions.size() == 0)
			availablePositions.add(current);

		return availablePositions;
	}

	/**
	 * Moves the nucleus to next position. Works if staying in place as well.
	 * 
	 * @param current      Current nucleus cell
	 * @param nextPosition Index to move current to
	 */
	private void move(int current, int nextPosition) {
		int newRow = convertToRow(nextPosition);
		int newCol = convertToCol(nextPosition);

		// Move nucleus cell.
		cells[current] = BODY;
		queue.enqueue(new Cell(newRow, newCol, BODY));
		cells[nextPosition] = NUCLEUS;
		queue.enqueue(new Cell(newRow, newCol, NUCLEUS));
		// Set body cells.
		setGrowthStage(newRow, newCol);
		// Transfer info and mark that cell has already been moved.
		moveAmoebaInfo(current, nextPosition);
		alreadyMoved.add(nextPosition);
	}

	@Override
	public void forAllLife(Callback action) {
		for (int current = 0; current < cells.length; current++)
			action.invoke(convertToRow(current), convertToCol(current), cells[current]);
	}

	@Override
	public long populationCount() {
		long count = 0;
		for (var state : cells)
			if (state == NUCLEUS)
				count++;
		return count;
	}

	/**
	 * @return Description of this model
	 */
	public String description() {
		return "Amoeba simulation.\nIncludes growth, mitosis, and hunger-based population control.";
	}
}