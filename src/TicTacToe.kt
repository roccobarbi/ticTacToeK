package ticTacToeK

import java.util.Scanner

/**
 * Implements a program that plays tic tac toe against a human player.
 * The program represents the board as a single array, row-wise from the top left.
 * 0 means that the slot is empty, 1 that the first player moved there, 2 that the second player moved there.
 *
 * Accepts a --verbose argument to show more information about how the computer calculated a move.
 *
 * @author Rocco Barbini (roccobarbi@gmail.com)
 */
class TicTacToe {

    val BOARD_SIZE = 3 // Lato del tabellone
    val DEBUG = true // Debug mode

    // Private fields
    private var board: IntArray? = null
    var computer: Int = 0
        private set
    var human: Int = 0
        private set // Each holds the number that identifies each player (1 or 2)
    private var currentMove: Int = 0
    var winner: Int = 0
        private set // Identify who should move now and who won the game
    var timeStart: Long = 0
        private set
    var timeEnd: Long = 0
        private set
    var duration: Long = 0
        private set
    var iterations: Long = 0
        private set

    // Constructors
    init {
        val boardLength = Math.pow(BOARD_SIZE.toDouble(), 2.0) as Int
        board = IntArray(boardLength)
        for (i in 0 until boardLength) {
            board!![i] = 0
        }
        // Choose randomly who plays first.
        computer = Math.round(Math.random()) as Int + 1
        human = if (computer == 1) 2 else 1
        winner = 0
        // Zeroes the counters
        iterations = 0
        duration = iterations
        timeEnd = duration
        timeStart = timeEnd
        currentMove = 1
    }

    // Accessors

    fun getBoard(): IntArray {
        val tempBoard = IntArray(board!!.size)
        for (i in board!!.indices) {
            tempBoard[i] = board!![i]
        }
        return tempBoard
    }

    private fun setDuration() {
        if (timeEnd > timeStart)
            duration = timeEnd - timeStart
        else
            duration = 0
    }

    // Private methods
    private fun chooseMove2(): Int { // To record the iterations and duration for debugging purposes
        var move = board!!.size / 2 // Default move
        var currentScore = 0
        var maxScore = -100

        // Local copy of the board
        val tempBoard = IntArray(board!!.size)
        for (i in board!!.indices) {
            tempBoard[i] = board!![i]
        }

        iterations = 0
        timeStart = System.currentTimeMillis() // Log the start time of the evaluation phase

        for (i in 0..8) {
            if (DEBUG) println("validateMove for " + i + " returns " + validateMove(i))
            if (validateMove(i)) { // The move is playable
                tempBoard[i] = computer
                currentScore = evaluateTree2(tempBoard, 0)
                if (currentScore > maxScore) {
                    maxScore = currentScore
                    move = i
                }
                tempBoard[i] = 0
                if (DEBUG) println("$i -> $currentScore")
            }
        }

        timeEnd = System.currentTimeMillis() // Log the end time of the evaluation phase
        setDuration()
        if (DEBUG) println("move -> $move")
        return move
    }

    private fun evaluateTree2(tempBoard: IntArray, depth: Int): Int { // A better, self-contained version
        var score = 0
        var currentScore = 0
        var noMovesAvailable = true // Default: the game is a draw at this stage
        var scoreChanged = false
        iterations++

        /*// Local copy of the board
		int tempBoard[] = new int[board.length];
		for(int i = 0; i < board.length; i++){
			tempBoard[i] = board[i];
		}*/

        if (checkVictory(tempBoard) == human) { // Caso base: vince l'avversario
            return depth - 11
        }
        if (checkVictory(tempBoard) == computer) { // Caso base: vince il computer
            return 21 - depth
        }
        if (checkVictory(tempBoard) == -1) { // Caso base: il gioco finisce in patta
            return 10 - depth
        }

        for (i in tempBoard.indices) {
            if (tempBoard[i] == 0) { // The move is playable
                noMovesAvailable = false

                if (depth % 2 == 0)
                    tempBoard[i] = human // The next move is human
                else
                    tempBoard[i] = computer // The next move is the computer's

                currentScore = evaluateTree2(tempBoard, depth + 1)

                if (!scoreChanged)
                    score = currentScore // Default
                else {
                    if (depth % 2 == 0 && currentScore < score)
                        score = currentScore // This is the computer's move
                    else if (depth % 2 != 0 && currentScore > score) score = currentScore // This is the player's move
                }

                scoreChanged = true

                tempBoard[i] = 0
            }
        }

        return if (noMovesAvailable) { // Caso base di riserva: non ci sono mosse disponibili
            10 - depth
        } else score
    }

    private fun chooseMove(): Int {
        var move = board!!.size / 2 // Default move
        var currentValue = 0.0
        var maxValue = 0.0 // the current and maximum tree evaluation up to now
        val tempBoard = IntArray(board!!.size)
        var boardSum = 0 // Checks if all cells are empty and moves to center.
        iterations = 0
        timeStart = System.currentTimeMillis() // Log the start time of the evaluation phase
        for (i in board!!.indices) { // This will be used by evaluateTree
            tempBoard[i] = board!![i]
            boardSum += tempBoard[i]
        }
        for (i in board!!.indices) {
            if (validateMove(i)) {
                tempBoard[i] = human // Check if a move here would get a win to the opponent
                if (checkVictory(tempBoard) == human) {
                    currentValue = 0.75 // Start from a higher current value.
                    if (DEBUG) println("Cell " + (i + 1) + " instant loss found! Current value: " + currentValue)
                } else {
                    currentValue = 0.0 // Zero the current value.
                    if (DEBUG) println("Cell " + (i + 1) + " instant loss not found! Current value: " + currentValue)
                }
                tempBoard[i] = computer // Assign the move to the temporary board
                currentValue += evaluateTree(i, 0, tempBoard)
                if (boardSum == 0 && i == board!!.size / 2) {
                    currentValue += 0.1 // Bias towards a first move at the center
                }
                if (DEBUG) println("Cell " + (i + 1) + " = " + currentValue)
                if (currentValue > maxValue) {
                    move = i
                    maxValue = currentValue
                } else if (maxValue == 0.0 && !validateMove(move)) {
                    move = i
                }
                tempBoard[i] = 0 // Zero it
            }
        }
        timeEnd = System.currentTimeMillis() // Log the end time of the evaluation phase
        setDuration()
        return move
    }

    /*
     * Evaluate tree: takes a move and returns the score for the subsequent decision tree
     * At each step:
     * - an invalid move returns 0;
     * - a move that puts the opponent in a winning position returns 0;
     * - an opponent's winning move returns -1;
     * - a winning move returns 1;
     * - anything else returns the sum of what the donwstream moves returned;
     * - exception: the opponent's move returns that divided by 2 to account for distance.
     */
    private fun evaluateTree(move: Int, depth: Int, innerBoard: IntArray): Double {
        // depth 0 = computer's first move move, 1 = opponent's first move, 2...
        var value = 0.0 // tree starting value is always 0
        var solution = 0.0
        var noMoreMoves = true // Default: there are no available moves
        iterations++
        if (depth % 2 == 0) { // The computer is playing
            if (checkVictory(innerBoard) == computer) {
                return 1.0
            }
            for (i in innerBoard.indices) {
                if (innerBoard[i] == 0) { // the move is playable
                    noMoreMoves = false // There are available moves
                    innerBoard[i] = human
                    solution = evaluateTree(i, depth + 1, innerBoard)
                    innerBoard[i] = 0 // Reset for the next iterations
                    if (value == 0.0 || solution < value) value = solution
                }
            }
        } else { // The opponent is playing
            if (checkVictory(innerBoard) == human) {
                return -1.0
            }
            for (i in innerBoard.indices) {
                if (innerBoard[i] == 0) { // the move is playable
                    noMoreMoves = false // There are available moves
                    innerBoard[i] = computer
                    solution = evaluateTree(i, depth + 1, innerBoard)
                    innerBoard[i] = 0 // Reset for the next iterations
                    if (solution > value) value = solution
                }
            }
        }
        if (noMoreMoves) value = 0.5 // A tie is better than a loss, but worse than a win
        return value
    }

    private fun askMove(): Int { // Asks the move to the player and validates it
        var move = 0
        var isValidMove = false
        val keyboard = Scanner(System.`in`)
        while (!isValidMove) {
            println()
            println("Inserisci la tua mossa e premi invio:")
            System.out.print(">: ")
            if (keyboard.hasNextInt()) {
                move = keyboard.nextInt() - 1
                isValidMove = validateMove(move)
                if (!isValidMove) println("Mossa non valida.")
            } else {
                println("ERRORE: input non valido. Devi inserire il numero di una casella da 1 a 9")
            }
            keyboard.nextLine() // Flushes the line.
        }
        return move
    }

    private fun validateMove(move: Int): Boolean { // Returns true if the move is legal, false otherwise
        var isLegal = false
        if (move >= 0 && move < board!!.size && board!![move] == 0)
            isLegal = true
        return isLegal
    }

    private fun checkVictory(innerBoard: IntArray): Int { // 0 = nobody yet, -1 = tied game, or 1, or 2.
        var victor = 0
        if (innerBoard[0] != 0 && // check first diagonal

            innerBoard[0] == innerBoard[4] &&
            innerBoard[0] == innerBoard[8]
        ) {
            victor = innerBoard[0]
        }
        if (victor == 0 && innerBoard[2] != 0 && // check second diagonal

            innerBoard[2] == innerBoard[4] &&
            innerBoard[2] == innerBoard[6]
        ) {
            victor = innerBoard[2]
        }
        if (victor == 0) { // Check rows
            var i = 0
            while (i < 7) {
                if (innerBoard[i] != 0 &&
                    innerBoard[i] == innerBoard[i + 1] &&
                    innerBoard[i] == innerBoard[i + 2]
                ) {
                    victor = innerBoard[i]
                }
                i += 3
            }
        }
        if (victor == 0) { // check columns
            for (i in 0..2) {
                if (innerBoard[i] != 0 &&
                    innerBoard[i] == innerBoard[i + 3] &&
                    innerBoard[i] == innerBoard[i + 6]
                ) {
                    victor = innerBoard[i]
                }
            }
        }
        if (victor == 0) {
            victor = -1 // Assume a tie
            for (position in innerBoard) {
                if (position == 0) victor = 0 // Correct it if it is not a tie
            }
        }
        return victor
    }

    // Public methods
    /**
     * Prints the current board to the screen
     */
    fun printBoard() {
        println()
        println(board!![0].toString() + " | " + board!![1] + " | " + board!![2])
        println("---------")
        println(board!![3].toString() + " | " + board!![4] + " | " + board!![5])
        println("---------")
        println(board!![6].toString() + " | " + board!![7] + " | " + board!![8])
        println()
    }

    /**
     * Resets the game.
     */
    fun reset() {
        val boardLength = Math.pow(BOARD_SIZE.toDouble(), 2.0) as Int
        board = IntArray(boardLength)
        for (i in 0 until boardLength) {
            board!![i] = 0
        }
        // Choose randomly who plays first.
        computer = Math.round(Math.random()) as Int + 1
        human = if (computer == 1) 2 else 1
        winner = 0
        // Zeroes the counters
        iterations = 0
        duration = iterations
        timeEnd = duration
        timeStart = timeEnd
        currentMove = 1
    }

    /**
     * Plays the game.
     */
    fun play() {
        if (computer < human) {
            println("Il computer gioca per primo con il numero $computer")
            println("Tu giochi per secondo con il numero $human")
        } else {
            println("Tu giochi per primo con il numero $human")
            println("Il computer gioca per secondo con il numero $computer")
        }
        while (winner == 0) { // Continua a giocare finché non emerge un vincitore
            if (currentMove == human) {
                board?.set(askMove(), human)
            } else {
                board?.set(chooseMove2(), computer)
                if (DEBUG) println("Duration: $duration ms")
                if (DEBUG) println("Iterations: $iterations")
            }
            winner = checkVictory(board!!)
            printBoard() // show current board
            currentMove = if (currentMove == 1) 2 else 1 // New turn, new player.
        }
        if (winner == -1) { // Tied game
            println()
            println("The game is tied: nobody won.")
            println()
        } else { // Somebody won the game
            println()
            println((if (winner == human) "You" else "The computer") + " won the game.")
            println()
        }
    }

    // main method
    fun main(args: Array<String>) {
        val keyboard = Scanner(System.`in`)
        var play = true
        var repeatInput = true
        var input: String
        val game = TicTacToe()

        println("This program is going to play tic tac toe with you.")
        println("Each slot on the board is represented by a number, like this:")
        println("1 | 2 | 3")
        println("---------")
        println("4 | 5 | 6")
        println("---------")
        println("7 | 8 | 9")
        println("When your turn is called, you will have to enter your move and press ENTER.")

        while (play) {
            game.play()
            // Check if the player wants to play again. Default: yes.
            repeatInput = true
            while (repeatInput) {
                println("Do you want to play again? [y|n]")
                System.out.print(">:")
                input = keyboard.nextLine()
                if (input.length > 0) {
                    when (input.toLowerCase()[0]) {
                        'n' -> {
                            play =
                                false // No need to break, case 'y' has only one condition and it's good for 'n' too
                            repeatInput = false
                        }
                        'y' -> repeatInput = false
                        else -> {
                            println("ERROR: invalid input.")
                            repeatInput = true
                        }
                    }
                } else {
                    println("ERROR: invalid input.")
                    repeatInput = true
                }
            }
            if (play) game.reset() // Only if the user plays again, reset the board for the next iteration.
        }
    }

}