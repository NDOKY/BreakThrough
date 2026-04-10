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
- Opponent moves validated before applying; invalid moves are ignored (logged), except placeholder moves such as `A8-A8` used when red opens.
- Configurable time limit per move (default 5 s; CLI or server timer, clamped 1–60 seconds).
- Optional CLI **preferred side** (`rouge` / `noir`); mismatch with server assignment → disconnect after notifying.
- **Server address** configurable without editing code: `--host` / `--hote` / `-H`, `--port` / `-p`, or `--gui-hote` for a Swing dialog (non-headless). Default remains `localhost:8888`.
- Recovery on invalid move (command 4): restore board snapshot and send an alternative legal move.
- **Console output is in French** for consistency with the course materials.

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
| `GameAI.java` | Minimax + alpha–beta + iterative deepening; returns best move within a caller-supplied time limit. |
| `Client.java` | Entry point: TCP connection, protocol loop (commands 1–5), board state, AI move selection, retry on rejection. |

**Dependencies:** `Client` uses `Board`, `Mark`, and `GameAI`. `GameAI` uses `Board`, `Mark`, and `PositionEvaluator`.

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
| `evaluate` | `static int evaluate(Board board)` | If Red wins → `+WIN_SCORE`; if Black wins → `-WIN_SCORE`. Otherwise: piece count × `PIECE_VALUE` plus advance × `ADVANCE_BONUS`. Red pieces add to score; Black pieces subtract. |

The class is `final` with a private constructor; no instances are created.

---

### 4. `GameAI`

Computes the best move using **minimax with alpha–beta pruning** and **iterative deepening**, within a time limit passed by the caller (e.g. `Client`).

#### Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `getBestMove` | `static String getBestMove(Board board, Mark sideToMove, long limitMs)` | Returns the best move for `sideToMove` within `limitMs`. Uses iterative deepening (depth 1, 2, …) until time runs out; returns the best move from the last fully completed depth. Returns `null` if game over or no moves; returns the single move if only one is legal. |
| `alphaBeta` | *(private)* | Recursive alpha–beta: uses `PositionEvaluator.evaluate(board)` at leaves and terminals. Red maximizes; Black minimizes. Honors `deadline` and `timedOut[]`. |
| `opposite` | *(private)* | Returns the opposite side (`rouge` ↔ `noir`). |

The class is `final` with a private constructor; all public API is static `getBestMove` with explicit `limitMs`.

---

### 5. `Client`

TCP client and game loop: connect to the server, handle commands 1–5, maintain board state, get moves from the AI, send moves, and handle invalid-move retry (command 4).

#### Constants & Fields

| Name | Type | Description |
|------|------|-------------|
| `SERVER_RANK_1_IS_TOP` | `boolean` | If true, ranks are converted with `9 - rank` for server orientation; default `false`. |
| `DEFAULT_SERVER_PORT` | `int` | Default TCP port when not overridden (8888). |
| `timeLimitMs` | `static long` | Per-move AI budget in ms (default 5_000; overridden by CLI numeric arg or optional 65th server field; clamped 1–60 s). |

#### Main Variables (in `main`)

| Variable | Type | Description |
|----------|------|-------------|
| `myClient` | `Socket` | Connection to `launch.serverHost`:`launch.serverPort` (defaults `localhost:8888`). |
| `input` / `output` | `BufferedInputStream` / `BufferedOutputStream` | Streams for reading/writing. |
| `board` | `int[8][8]` | Raw board data from server (column-major cells in nested array layout used by `Board` constructor). |
| `gameBoard` | `Board` | Board used for play. |
| `boardBeforeOurMove` | `Board` | Snapshot before sending our move; restored on command 4. |
| `lastSentMove` | `String` | Last move we sent; used to exclude a retry on command 4. |
| `ourSide` | `Mark` | Our color (`rouge` or `noir`). |
| `preferredSide` | `Mark` | Optional CLI preference; if it disagrees with message `1`/`2`, client disconnects. |

#### Methods

| Method | Description |
|--------|-------------|
| `main` | Parses CLI (host, port, GUI prompt, time, side), connects, runs command loop. |
| `LaunchConfig` | Holds `serverHost`, `serverPort`, `preferredSide`, `guiHostPrompt`. |
| `parseLaunchArguments` | Parses `--host` / `--hote` / `-H`, `--port` / `-p`, `--gui-hote`, seconds, colour; unknown tokens → usage + exit. |
| `printUsage` | French usage on `--help` / `-?` or errors. |
| `promptServerAddressFromDialog` | Swing dialog for host/port when `--gui-hote` is set (fails if headless). |
| `parsePort` | Validates TCP port 1–65535. |
| `parseAndFillBoardFromPayload` | Splits payload and fills `int[][] board` with 64 cell values; returns token array (for optional timer). |
| `applyServerTimerIfPresent` | If a 65th token exists, parses seconds and updates `timeLimitMs`. |
| `getMoveFromAI` | Calls `GameAI.getBestMove` with `max(200, timeLimitMs - 200)`; fallback first legal or `A2A3`. |
| `getValidMoveForServer` | Builds a legal move; if `excludeMove` and several legals exist, prefers a different normalized move; formats via `formatMoveForServer`. |
| `normalizeMove` | Strip `-`/spaces, uppercase. |
| `isInvalidMovePlaceholder` | True for degenerate moves such as `A8A8` (same from/to). |
| `normalizeOpponentMove` | Strip brackets/dashes/spaces; optional rank flip. |
| `formatMoveForServer` | Compact `A2A3`; optional rank flip. |

---

## Data Flow

1. **Server sends board (command 1 or 2)**  
   `parseAndFillBoardFromPayload` + `applyServerTimerIfPresent`, then `new Board(board)`. Preferred-side check. On command 1, first move is computed, sent, and applied if the game is not already over.

2. **Our turn (command 1 first move, or command 3)**  
   Opponent move (command 3 only): normalized, validated with `isValidMove` for the opponent side; placeholders skipped; invalid moves logged and not applied. Then `getValidMoveForServer` → `getMoveFromAI` → `GameAI.getBestMove`. Search uses `PositionEvaluator.evaluate` and `Board.applyMove` / `generateAllMoves`.

3. **Invalid move (command 4)**  
   Restore `gameBoard` from `boardBeforeOurMove`, pick a move with `excludeMove = lastSentMove`, send and apply.

4. **Game end (command 5)**  
   Read final message, send `"0"`, drain input briefly, exit loop and close.

---

## Server Protocol

| Command | Meaning | Client action |
|---------|---------|----------------|
| `1` | New game; we play red (blanc). | Parse board (+ optional timer), create `Board`, optional side check, send first move if needed. |
| `2` | New game; we play black (noir). | Parse board (+ optional timer), create `Board`, optional side check; no move yet. |
| `3` | Opponent move. | Read move, validate for opponent, apply if legal; then send our move if game continues. |
| `4` | Our move rejected. | Restore snapshot, send a different valid move when possible. |
| `5` | Game end. | Read trailer, send `"0"`, short wait, exit. |

Board data: 64 space-separated integers (0 / 2 / 4) in column-major order; optional 65th value = timer in seconds.

---

## Build & Run

### Compile from sources

- **Compile:**  
  `javac -encoding UTF-8 *.java`

### Runnable JAR (recommended for deployment / tournament)

All command-line options (host, port, GUI dialog, seconds, colour) work the same with **`java -jar`** as with **`java Client`**: arguments after the JAR name are passed to `main`.

1. **Build the JAR** (requires a **JDK** with `javac` and `jar` on PATH, or set **`JAVA_HOME`**):
   - **Windows:** run `build-jar.bat` from the project folder.
   - **Linux / macOS:** `chmod +x build-jar.sh` then `./build-jar.sh`

2. **Output:** `BreakThrough.jar` (manifest `Main-Class: Client` in `META-INF/MANIFEST.MF`).

3. **Run examples:**

| Goal | Command |
|------|---------|
| Help | `java -jar BreakThrough.jar --help` |
| Default (localhost:8888, 5 s) | `java -jar BreakThrough.jar` |
| Remote host + port + time + side | `java -jar BreakThrough.jar --host 192.168.0.15 -p 8888 5 rouge` |
| Host only | `java -jar BreakThrough.jar -H 10.0.0.3` |
| GUI for host/port | `java -jar BreakThrough.jar --gui-hote` |
| Black + 5 s | `java -jar BreakThrough.jar 5 noir` |

### Run without JAR (classpath = current directory)

- **Run (default 5 s per move, localhost:8888):**  
  `java Client`

- **Remote server (tournament / LAN):**  
  `java Client --host 192.168.0.15`  
  `java Client -H 192.168.0.15 --port 8888 5 rouge`  
  (Use your PC’s LAN IP instead of `localhost` for a first test against a server on the same machine.)

- **Graphical host/port prompt:**  
  `java Client --gui-hote`  
  (Requires a display; use `--host` on headless systems.)

- **Time limit in seconds (1–60):**  
  `java Client 5`

- **Preferred side (any order with other options):**  
  `java Client rouge 5` · accepts `red`, `black`, `r`, `b` as aliases.

- **Help:**  
  `java Client --help`

Ensure the game server is listening on the chosen host and port before starting the client.

---

*This document reflects the BreakThrough project as implemented in the current source tree.*
