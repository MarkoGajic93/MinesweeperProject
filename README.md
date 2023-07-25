# MinesweeperProject
There are two goals of this project (oldschool minsesweeper game):
- to implement game logic (with help of java swing),
- to demonstrate the use of relational databases with JDBC (possibility to create user accounts and save/view/delete results).
## Getting started
Database used in this project is MySQL database. Database dump (empty tables) is included in this repository (.idea/resources/minesweeper.sql). 
You can reload that file using a "mysql -u [name] -p < minesweeper.sql" command ([name] is your MySQL server username, and later you will be promped to enter your password).
After recreating a database from a dump, you can start the application. 
The input dialog will appear, where you need to enter the necessary details of your MySQL connection, so JDBC driver could establish a connection with a database (server name, port number, username and password).
After connection was successfully established, new input dialog will appear, where you can choose to login, create new account or play as guest (NOTE: if you login as guest, your results would not be saved in DB).
## Implementation
The project consists three classes. 
- Minesweeper.java is the one with the main method and is used to establish a connecton with a database and then to create an instance of GameFrame class.
- GameFrame.java is used to create all necessary swing components for game GUI; also all of the game logic is implemented here.
- ResultsFrame.java is used to display and delete results (user scores) in new frame. Instance of this class is created (new window opens) if user choose 'view result' option from game frame.

Game logic is implemented using 2D arrays of JButton and JLabel components. Arrays are placed one over another in a JLayeredPane. JLabel components are marked with randomly placed mines and warnings around them.
When user clicks on any JButton component, that component will be removed from JLayeredPane and later checks will be done (is JLabel under clicked button maybe marked and with what (mine - game over; warning - remove nothing else and check for win). if not marked - remove all neighbouring buttons until 'warning' label is reached and check for win).
If user click on button that has a mine under it, game over message will be displayed. If user wins (all buttons are removed except those that have 'mine' label under them), score will be automatically added into a database.
Score is measured in seconds, and for this purpose javax.swing.Timer is used.

As mentioned before, MySQL database is used to store information about users and results; and JDBC API is used to access and manipulate data. Database consists of two tables: minesweeper.users and minesweeper.results.
Users table has the following columns: user_id (PK), username and password; and results table has: result_id (PK), user_id (FK), score, level, date.
