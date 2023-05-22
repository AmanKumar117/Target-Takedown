import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.*;
import java.util.Random;
import java.sql.*;

class ConnectionToDB {
    private final String DB_URL = "jdbc:mysql://localhost:3306/aim_practice";
    private final String DB_USERNAME = "root";
    private final String DB_PASSWORD = "Mysql123";
    private Connection conn;
    public ConnectionToDB()throws SQLException, ClassNotFoundException{
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    public void seeHighScore(JComboBox<String> difficultyComboBox){
        String selectedDifficulty = (String) difficultyComboBox.getSelectedItem();
        try {
            Statement stmt = conn.createStatement();
            String query = "SELECT name, score FROM " + selectedDifficulty + "_scores ORDER BY score DESC LIMIT 100";
            ResultSet rs = stmt.executeQuery(query);
            // Create a JTable to display the high scores
            String[] columnNames = {"Name", "Score"};
            DefaultTableModel model = new DefaultTableModel(columnNames, 0);
            while (rs.next()) {
                String name = rs.getString("name");
                int score = rs.getInt("score");
                model.addRow(new Object[]{name, score});
            }
            JTable table = new JTable(model);
            table.setPreferredScrollableViewportSize(new Dimension(500, 70));
            table.setFillsViewportHeight(true);
            // Add the JTable to a scroll pane and a new JFrame
            JScrollPane scrollPane = new JScrollPane(table);
            JFrame frame = new JFrame("High Scores");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.add(scrollPane);
            frame.pack();
            frame.setVisible(true);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    public void setDataToDB(int score, String difficulty){
        try {
            String playerName = null;
            do {
                playerName = JOptionPane.showInputDialog(null, "Enter your name:");
                if (playerName == null || playerName.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "Please enter your name.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            } while (playerName == null || playerName.isEmpty());
            PreparedStatement stmt;
            switch (difficulty) {
                case "easy":
                stmt = conn.prepareStatement("INSERT INTO easy_scores (name, score) VALUES (?, ?)");
                break;
                case "medium":
                stmt = conn.prepareStatement("INSERT INTO medium_scores (name, score) VALUES (?, ?)");
                break;
                case "hard":
                stmt = conn.prepareStatement("INSERT INTO hard_scores (name, score) VALUES (?, ?)");
                break;
                default:
                return;
            }
            stmt.setString(1, playerName);
            stmt.setInt(2, score);
            stmt.executeUpdate();
        } catch (SQLException e1) {
            e1.printStackTrace();
        }
    }
}
public class Aim extends JFrame {
    private final JLabel targetLabel;
    private final int targetSize = 50;
    private final int numTargets = 10;
    private int[] hitTarget = new int[numTargets];
    private int currentTarget = -1;
    private int lastTarget = -1;
    private int score = 0;
    private final JLabel scoreLabel;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton highScore;
    private final Random random;
    private final JComboBox<String> difficultyComboBox;
    private Timer timer;
    private ConnectionToDB c;
    public Aim() throws SQLException, ClassNotFoundException {
        super("Aim Practice Game");
        c = new ConnectionToDB();
        random = new Random();
        JPanel upperPanel = new JPanel();
        upperPanel.setLayout(new FlowLayout());
        JLabel difficultyLabel = new JLabel("Difficulty:");
        upperPanel.add(difficultyLabel);
        highScore = new JButton("Show High Score");
        String[] difficulties = {"easy", "medium", "hard"};
        difficultyComboBox = new JComboBox<>(difficulties);
        upperPanel.add(difficultyComboBox);
        scoreLabel = new JLabel("Score: 0");
        upperPanel.add(scoreLabel);
        startButton = new JButton("Start");
        upperPanel.add(startButton);
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        upperPanel.add(stopButton);
        upperPanel.add(highScore);
        highScore.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e){
                c.seeHighScore(difficultyComboBox);
            }
        });
        // Adding the upper panel to the frame
        add(upperPanel, BorderLayout.NORTH);
        // Creating the lower panel
        JPanel lowerPanel = new JPanel();
        lowerPanel.setBackground(Color.WHITE);
        lowerPanel.setPreferredSize(new Dimension(500, 500));
        lowerPanel.setLayout(null);
        // Creating the target label
        targetLabel = new JLabel();
        targetLabel.setSize(targetSize, targetSize);
        targetLabel.setOpaque(true);
        targetLabel.setBackground(Color.RED);
        targetLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentTarget == lastTarget) {
                    return;
                }
                hitTarget[currentTarget] = 1;
                score += 10;
                scoreLabel.setText("Score: " + score);
                lastTarget = currentTarget;
                targetLabel.setVisible(false);
                timer.stop();
                createNewTarget();
            }
        });
        lowerPanel.add(targetLabel);
        // Adding the lower panel to the frame
        add(lowerPanel, BorderLayout.CENTER);
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                numTargetsDisplayed = 0;
                score = 0;
                scoreLabel.setText("Score: " + score);
                currentTarget = -1;
                hitTarget = new int[numTargets];
                for (int i = 0; i < numTargets; i++) {
                    hitTarget[i] = 0;
                }
                createNewTarget();
            }
        });    stopButton.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            timer.stop();
            targetLabel.setVisible(false);
            }
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(600, 600);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    private int numTargetsDisplayed = 0;
    private void createNewTarget() {
        currentTarget = -1;
        for (int i = 0; i < numTargets; i++) {
            if (hitTarget[i] == 0) {
                currentTarget = i;
                break;
            }
        }
        if (currentTarget == -1 || numTargetsDisplayed == numTargets) {// All targets have been generated/hit
            String difficulty = (String) ((JComboBox<?>) ((JPanel) getContentPane().getComponent(0)).getComponent(1)).getSelectedItem();
            c.setDataToDB(score, difficulty);
            JOptionPane.showMessageDialog(this, "Game over! Final score: " + score);
            startButton.setEnabled(true);
            stopButton.setEnabled(false);
            return;
        }
        numTargetsDisplayed++;
        targetLabel.setLocation(getRandomX(), getRandomY());
        targetLabel.setVisible(true);
        timer = new Timer(getRandomSpeed(), new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                targetLabel.setVisible(false);
                timer.stop();
                createNewTarget();
            }
        });
        timer.setRepeats(false);
        timer.start();
    }
    private int getRandomX() {
        return random.nextInt(450);
    }

    private int getRandomY() {
        return random.nextInt(450);
    }
    private int getRandomSpeed() {
        String difficulty = (String) ((JComboBox<?>) ((JPanel) getContentPane().getComponent(0)).getComponent(1)).getSelectedItem();
        switch (difficulty) {
            case "easy":
                return random.nextInt(2000) + 1000;
            case "medium":
                return random.nextInt(1500) + 500;
            case "hard":
                return random.nextInt(1150) + 500;
            default:
                return 1000;
        }
    }
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        new Aim();
    }
}