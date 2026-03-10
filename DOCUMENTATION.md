# BreakThrough — Project Documentation

A **Breakthrough** board game client implemented in Java. The program connects to a game server over TCP, maintains an internal 8×8 board, and uses **minimax with alpha–beta pruning** and **iterative deepening** to choose moves within a configurable time limit.

---

## Table of Contents

1. [Project Overview](#project-overview)
2. [Game Rules](#game-rules)
3. [Architecture & File Structure](#architecture--file-structure)
4. [Class Reference](#class-reference)
5. [Data Flow](#data-flow)
6. [Server Protocol](#server-protocol)
7. [Build & Run](#build--run)

---

## Project Overview

### Purpose

- **Play Breakthrough** against a remote opponent via a TCP game server (default: `localhost:8888`).
- **Keep board state** in sync with the server using the protocol codes `0` (empty), `2` (black/noir), `4` (red/rouge).
- **Choose moves** with an AI that uses minimax, alpha–beta pruning, and iterative deepening, respecting a per-move time limit.

### Main Features

- TCP client that handles server commands 1–5 (new game, opponent move, invalid move retry, game end).
- Full Breakthrough rules: move validation, legal move generation, win detection (piece on goal row).
- Configurable time limit per move (CLI or server; clamped 1–60 seconds).
- Recovery on invalid move (command 4): restore board snapshot and send an alternative legal move.

---

## Game Rules

- **Board:** 8×8. Red (rouge) starts on rows 6–7 (ranks 7–8); Black (noir) on rows 0–1 (ranks 1–2).
- **Moves:** One square forward (same file) or one square diagonally forward. No backward or sideways moves.
- **Capture:** Diagonal move can land on an enemy piece (capture). Cannot land on a friendly piece.
- **Win:** First side to get any piece to the opposite back rank (Red → row 0, Black → row 7).

---

## Architecture & File Structure

| File | Role |
|------|------|
| `Mark.java` | Enum for cell/side: empty (vide), black (noir), red (rouge); maps to server codes 0, 2, 4. |
| `Board.java` | 8×8 game state: apply moves, validate, generate legal moves, detect winner, convert to/from server format. |
| `PositionEvaluator.java` | Static evaluation for AI: win score, piece count, advance bonus (red = positive, black = negative). |
| `GameAI.java` | Minimax + alpha–beta + iterative deepening; returns best move within time limit. |
| `MoveGenerator.java` | Optional layer: set board from server, apply opponent move, return next AI move (50 ms limit). |
| `Client.java` | Entry point: TCP connection, protocol loop (commands 1–5), board state, AI move selection, retry on rejection. |

**Dependencies:** `Board` and `GameAI` are used by `Client`; `Client` does not use `MoveGenerator`. `PositionEvaluator` is used only by `GameAI`.

---

## Class Reference

### 1. `Mark` (enum)

Represents the content of a cell (or the side to move). Aligns with the server protocol.

#### Constants / Enum Values

| Value | Meaning | Server Code |
|-------|---------|-------------|
| `vide` | Empty cell | 0 |
| `noir` | Black piece | 2 |
| `rouge` | Red piece | 4 |

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `serverCode` | `int` (final) | Numeric value sent/received on the wire (0, 2, or 4). |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `toServerCode` | `int toServerCode()` | Returns the server code for this mark. |
| `fromServerCode` | `static Mark fromServerCode(int serverCode)` | Maps `2` → `noir`, `4` → `rouge`, anything else → `vide`. |

---

### 2. `Board`

8×8 Breakthrough board: state, move application, validation, move generation, win detection, and conversion to/from the server’s `int[][]` format.

#### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `GRID_SIZE` | 8 | Board dimension. |
| `RED_GOAL_ROW` | 0 | Row index where a red piece wins. |
| `BLACK_GOAL_ROW` | 7 | Row index where a black piece wins. |

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `grid` | `Mark[][]` | Row-major grid; `grid[rowIndex][colIndex]`. Row 0 = rank 1 (black back rank), row 7 = rank 8 (red back rank). |

#### Constructors

| Constructor | Description |
|-------------|-------------|
| `Board()` | Default starting position: red on rows 6–7, black on rows 0–1. |
| `Board(int[][] serverBoard)` | Builds board from server state; expects `serverBoard[column][row]` with values 0/2/4. |
| `Board(Board other)` | Copy constructor for search (e.g. minimax). |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `initializeDefaultPosition` | `private void initializeDefaultPosition()` | Fills the grid with the standard starting setup. |
| `makeMove` | `void makeMove(String move)` | Applies a move in server format (`"D6-D5"` or `"D6D5"`). Does not validate; no-op if move is null or too short. |
| `isValidMove` | `boolean isValidMove(String move, Mark sideToMove)` | Returns true if the move is legal for the given side (correct piece, forward/diagonal, no friendly fire). |
| `generateAllMoves` | `List<String> generateAllMoves(Mark sideToMove)` | Returns all legal moves for the side; each move is in format `"A2A3"` (no dash). |
| `moveString` | `private static String moveString(...)` | Builds a move string from from/to column and rank. |
| `applyMove` | `Board applyMove(String move)` | Returns a new board with the move applied; current board is unchanged. Used during search. |
| `getCell` | `Mark getCell(int rowIndex, int colIndex)` | Returns the mark at the given indices; returns `vide` if out of bounds. |
| `getWinner` | `Mark getWinner()` | Returns `rouge` or `noir` if a piece has reached the goal row; otherwise `null`. |
| `isGameOver` | `boolean isGameOver()` | True if `getWinner() != null`. |
| `getGridSize` | `static int getGridSize()` | Returns 8. |
| `toServerBoard` | `int[][] toServerBoard()` | Exports to `int[column][row]` using `Mark.toServerCode()` for each cell. |

---

### 3. `PositionEvaluator`

Static evaluation for the minimax/alpha–beta search. Score is from Red’s perspective: **positive = good for Red**, **negative = good for Black**.

#### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `WIN_SCORE` | 100_000 | Score for a winning position. |
| `PIECE_VALUE` | 100 | Score per piece. |
| `ADVANCE_BONUS` | 10 | Bonus per row advanced toward the goal (Red: higher row index is better; Black: lower row index is better). |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `evaluate` | `static int evaluate(Board board, Mark sideToMove)` | If Red wins → `+WIN_SCORE`; if Black wins → `-WIN_SCORE`. Otherwise: piece count × `PIECE_VALUE` plus advance × `ADVANCE_BONUS`. Red pieces add to score; Black pieces subtract. |

The class is `final` with a private constructor; no instances are created.

---

### 4. `GameAI`

Computes the best move using **minimax with alpha–beta pruning** and **iterative deepening**, within a given time limit.

#### Constants

| Constant | Value | Description |
|----------|-------|-------------|
| `DEFAULT_TIME_LIMIT_MS` | 4_900 | Default time limit in milliseconds when the overload without `limitMs` is used. |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `getBestMove` | `static String getBestMove(Board board, Mark sideToMove, long limitMs)` | Returns the best move for `sideToMove` within `limitMs`. Uses iterative deepening (depth 1, 2, …) until time runs out; returns the best move from the last fully completed depth. Returns `null` if game over or no moves; returns the single move if only one is legal. |
| `getBestMove` | `static String getBestMove(Board board, Mark sideToMove)` | Overload that uses `DEFAULT_TIME_LIMIT_MS`. |
| `alphaBeta` | `private static int alphaBeta(Board, int depth, int alpha, int beta, Mark currentPlayer, Mark maximizingPlayer, long deadline, boolean[] timedOut)` | Recursive alpha–beta search. Stops at depth 0 or terminal (winner or no moves). Uses `PositionEvaluator.evaluate` at leaves and terminals. Sets `timedOut[0]` and returns when past `deadline`. Red maximizes; Black minimizes. |
| `opposite` | `private static Mark opposite(Mark mark)` | Returns the opposite side (`rouge` ↔ `noir`). |

The class is `final` with a private constructor; all methods are static.

---

### 5. `MoveGenerator`

Keeps a board in sync with the server and produces the next AI move: set board from server (messages 1 or 2), apply opponent move (message 3), then compute and return our move.

#### Fields

| Field | Type | Description |
|-------|------|-------------|
| `board` | `Board` | Current game state. |
| `playingRed` | `boolean` | True if we play red; false if we play black. |
| `TIME_LIMIT_MS` | `long` (static) | Time limit for AI per move (50 ms) when calling `GameAI.getBestMove`. |

#### Constructors

| Constructor | Description |
|-------------|-------------|
| `MoveGenerator(boolean playingRed)` | Initializes with an empty default board. |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `setBoard` | `void setBoard(int[][] serverBoard)` | Replaces `board` with `new Board(serverBoard)`. Call when receiving message "1" or "2" with board data. |
| `nextMove` | `String nextMove(String lastOpponentMove)` | If `lastOpponentMove` is non-empty and not an invalid-move placeholder, validates and applies it for the opponent. If game over, returns `null`. Otherwise gets our move via `GameAI.getBestMove(board, ourSide, TIME_LIMIT_MS)`, applies it on `board`, and returns the move string. |
| `isInvalidMovePlaceholder` | `private static boolean isInvalidMovePlaceholder(String move)` | Detects "from equals to" placeholder (e.g. `"A2A2"`). |

**Note:** `Client` does not use `MoveGenerator`; it uses `Board` and `GameAI` directly.

---

### 6. `Client`

TCP client and game loop: connect to the server, handle commands 1–5, maintain board state, get moves from the AI, send moves, and handle invalid-move retry (command 4).

#### Constants

| Constant | Type | Description |
|----------|------|-------------|
| `SERVER_RANK_1_IS_TOP` | `boolean` | If true, rank is sent as `9 - rank` so server’s rank 1 is top; currently `false`. |
| `timeLimitMs` | `long` | Per-move time limit in ms (default 50; overridden by CLI or server, clamped 1–60 s). |

#### Main Variables (in `main`)

| Variable | Type | Description |
|----------|------|-------------|
| `myClient` | `Socket` | Connection to `localhost:8888`. |
| `input` / `output` | `BufferedInputStream` / `BufferedOutputStream` | Streams for reading/writing. |
| `board` | `int[8][8]` | Raw board data from server. |
| `gameBoard` | `Board` | Board used for play. |
| `boardBeforeOurMove` | `Board` | Snapshot before sending our move; restored on command 4 (invalid move). |
| `lastSentMove` | `String` | Last move we sent; excluded when retrying on command 4. |
| `ourSide` | `Mark` | Our color (`rouge` or `noir`). |

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `main` | `static void main(String[] args)` | Entry point. Optional `args[0]` = seconds for `timeLimitMs`. Connects to server and runs the command loop. |
| `getMoveFromAI` | `private static String getMoveFromAI(Board board, Mark sideToMove)` | Calls `GameAI.getBestMove` with `timeLimitMs - 200` (minimum 200 ms). Fallback: `"A2A3"` or first legal move if AI returns null. |
| `getValidMoveForServer` | `private static String getValidMoveForServer(Board board, Mark sideToMove, String excludeMove)` | Returns a valid move to send. If `excludeMove` is set and there are multiple moves, picks one different from it (for cmd 4 retry). Formats the move for the server. |
| `normalizeMove` | `private static String normalizeMove(String move)` | Removes dashes and spaces, converts to uppercase. |
| `normalizeOpponentMove` | `private static String normalizeOpponentMove(String move)` | Strips brackets, dashes, spaces; optionally flips ranks if `SERVER_RANK_1_IS_TOP`. |
| `formatMoveForServer` | `private static String formatMoveForServer(String move)` | Compact format `"A2A3"` (no dash); optionally flips ranks for server. |

---

## Data Flow

1. **Server sends board (command 1 or 2)**  
   Client parses the board (and optional timer), builds a `Board`, sets `ourSide`. On command 1, client also gets and sends the first move and applies it locally.

2. **Our turn (command 1 first move, or command 3)**  
   Client calls `getValidMoveForServer` → `getMoveFromAI` → `GameAI.getBestMove`. Inside the search, `PositionEvaluator.evaluate` and `Board.applyMove` / `generateAllMoves` are used. Client sends the chosen move and applies it to `gameBoard`.

3. **Invalid move (command 4)**  
   Client restores `gameBoard` from `boardBeforeOurMove`, then gets a different move with `excludeMove = lastSentMove`, sends it, and applies it.

4. **Game end (command 5)**  
   Client reads the final message, sends `"0"`, waits briefly, then exits the loop and closes the connection.

---

## Server Protocol

| Command | Meaning | Client action |
|---------|---------|----------------|
| `1` | New game; we play red (blanc). | Parse board (+ optional timer), create `Board`, get and send first move, apply locally. |
| `2` | New game; we play black (noir). | Parse board (+ optional timer), create `Board`; no move sent yet. |
| `3` | Opponent move. | Read move, normalize, apply on `gameBoard`. If not game over, get our move via `getValidMoveForServer`, send it, apply locally. |
| `4` | Our move rejected. | Restore `gameBoard` from `boardBeforeOurMove`, get alternative move (excluding `lastSentMove`), send and apply. |
| `5` | Game end. | Read final message, send `"0"`, wait briefly, exit loop. |

Board data: 64 space-separated integers (0 / 2 / 4) in column-major order; optional 65th value = timer in seconds.

---

## Build & Run

- **Compile:**  
  `javac *.java`

- **Run (default time limit):**  
  `java Client`

- **Run with time limit (e.g. 5 seconds per move):**  
  `java Client 5`

Ensure the game server is listening on `localhost:8888` before starting the client.

---

*This document describes the BreakThrough project structure, classes, methods, variables, and behavior as implemented in the source code.*
