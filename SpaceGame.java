/** Project: Solo Lab 7 Assignment
 * Purpose Details: Space game with background stars, random colors, ship image,
 * sprite sheet obstacles, score color, audio, reset, shield, health, timer, and levels.
 * Course:
 * Author: Kenneth Klos
 * Date Developed:
 * Last Date Changed:
 * Rev:
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.sound.sampled.*;

public class SpaceGame extends JFrame implements KeyListener {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 500;

    private static final int PLAYER_WIDTH = 50;
    private static final int PLAYER_HEIGHT = 50;

    private static final int OBSTACLE_WIDTH = 20;
    private static final int OBSTACLE_HEIGHT = 20;

    private static final int PROJECTILE_WIDTH = 5;
    private static final int PROJECTILE_HEIGHT = 10;

    private static final int PLAYER_SPEED = 5;
    private static final int OBSTACLE_SPEED = 3;
    private static final int PROJECTILE_SPEED = 10;

    private int obstacleSpeed = OBSTACLE_SPEED;
    private int score = 0;
    private int playerHealth = 100;
    private int timeLeft = 60;
    private int level = 1;

    private List<Point> stars;

    private JPanel gamePanel;
    private JLabel scoreLabel;
    private JLabel healthLabel;
    private JLabel timerLabel;
    private JLabel levelLabel;

    private Timer timer;
    private Timer countdownTimer;

    private int playerX, playerY;
    private int projectileX, projectileY;

    private boolean isProjectileVisible;
    private boolean isFiring;
    private boolean isGameOver;

    private boolean shieldActive = false;
    private int shieldDuration = 5000; // Shield duration in milliseconds
    private long shieldStartTime;

    private java.util.List<Point> obstacles;
    private java.util.List<Integer> obstacleSprites;
    private java.util.List<Point> healthPowerUps;

    private BufferedImage shipImage;
    private BufferedImage spriteSheet;
    private int spriteWidth;
    private int spriteHeight;

    private Clip clip;

    private List<Point> generateStars(int numStars) {
        List<Point> starsList = new ArrayList<>();
        Random random = new Random();

        for (int i = 0; i < numStars; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            starsList.add(new Point(x, y));
        }

        return starsList;
    }

    public static Color generateRandomColor() {
        Random rand = new Random();
        int r = rand.nextInt(256); // Red component (0-255)
        int g = rand.nextInt(256); // Green component (0-255)
        int b = rand.nextInt(256); // Blue component (0-255)
        return new Color(r, g, b);
    }

    public SpaceGame() {
        setTitle("Space Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        try {
            // Load the ship image from file
            shipImage = ImageIO.read(new File("ship.png"));

            // Load the sprite sheet from file
            spriteSheet = ImageIO.read(new File("astro.png"));
            spriteWidth = spriteSheet.getWidth() / 4;
            spriteHeight = spriteSheet.getHeight();

            // Load the fire sound from file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File("fire.wav").getAbsoluteFile());
            clip = AudioSystem.getClip();
            clip.open(audioInputStream);

        } catch (LineUnavailableException ex) {
            ex.printStackTrace();
        } catch (UnsupportedAudioFileException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        stars = generateStars(200);

        gamePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                draw(g);
            }
        };

        scoreLabel = new JLabel("Score: 0");
        scoreLabel.setForeground(Color.GREEN);
        scoreLabel.setBounds(10, 10, 90, 20);
        gamePanel.add(scoreLabel);

        healthLabel = new JLabel("Health: 100");
        healthLabel.setBounds(100, 10, 110, 20);
        healthLabel.setForeground(Color.WHITE);
        gamePanel.add(healthLabel);

        timerLabel = new JLabel("Time: 60");
        timerLabel.setBounds(220, 10, 90, 20);
        timerLabel.setForeground(Color.WHITE);
        gamePanel.add(timerLabel);

        levelLabel = new JLabel("Level: 1");
        levelLabel.setBounds(320, 10, 90, 20);
        levelLabel.setForeground(Color.WHITE);
        gamePanel.add(levelLabel);

        add(gamePanel);
        gamePanel.setFocusable(true);
        gamePanel.addKeyListener(this);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 50;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        shieldActive = false;

        obstacles = new java.util.ArrayList<>();
        obstacleSprites = new java.util.ArrayList<>();
        healthPowerUps = new java.util.ArrayList<>();

        timer = new Timer(20, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    update();
                    gamePanel.repaint();
                }
            }
        });
        timer.start();

        countdownTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!isGameOver) {
                    timeLeft--;
                    timerLabel.setText("Time: " + timeLeft);

                    if (timeLeft <= 0) {
                        isGameOver = true;
                    }
                }
            }
        });
        countdownTimer.start();
    }

    private void draw(Graphics g) {
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        // Draw stars
        g.setColor(generateRandomColor());
        // g.setColor(Color.WHITE);
        for (Point star : stars) {
            g.fillOval(star.x, star.y, 2, 2);
        }

        if (shipImage != null) {
            g.drawImage(shipImage, playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT, null);
        } else {
            g.setColor(Color.BLUE);
            g.fillRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        if (isShieldActive()) {
            g.setColor(new Color(0, 255, 255, 100));
            g.fillOval(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
        }

        if (isProjectileVisible) {
            g.setColor(Color.GREEN);
            g.fillRect(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);
        }

        for (int i = 0; i < obstacles.size(); i++) {
            Point obstacle = obstacles.get(i);

            if (spriteSheet != null) {
                int spriteIndex = obstacleSprites.get(i);

                int spriteX = spriteIndex * spriteWidth;
                int spriteY = 0;

                g.drawImage(
                        spriteSheet.getSubimage(spriteX, spriteY, spriteWidth, spriteHeight),
                        obstacle.x,
                        obstacle.y,
                        OBSTACLE_WIDTH,
                        OBSTACLE_HEIGHT,
                        null
                );
            } else {
                g.setColor(Color.RED);
                g.fillRect(obstacle.x, obstacle.y, OBSTACLE_WIDTH, OBSTACLE_HEIGHT);
            }
        }

        g.setColor(Color.PINK);
        for (Point healthPowerUp : healthPowerUps) {
            g.fillOval(healthPowerUp.x, healthPowerUp.y, 20, 20);
            g.setColor(Color.WHITE);
            g.drawString("+", healthPowerUp.x + 6, healthPowerUp.y + 15);
            g.setColor(Color.PINK);
        }

        if (isGameOver) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Game Over!", WIDTH / 2 - 80, HEIGHT / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("Press ESC to Reset", WIDTH / 2 - 80, HEIGHT / 2 + 30);
        }
    }

    private void update() {
        if (!isShieldActive()) {
            deactivateShield();
        }

        // Move obstacles
        for (int i = 0; i < obstacles.size(); i++) {
            obstacles.get(i).y += obstacleSpeed;

            if (obstacles.get(i).y > HEIGHT) {
                obstacles.remove(i);
                obstacleSprites.remove(i);
                i--;
            }
        }

        // Generate new obstacles and pick one sprite for each one
        if (Math.random() < 0.02 + (level * 0.005)) {
            int obstacleX = (int) (Math.random() * (WIDTH - OBSTACLE_WIDTH));
            obstacles.add(new Point(obstacleX, 0));
            obstacleSprites.add(new Random().nextInt(4));
        }

        // Generate new star field for the background so that it changes
        if (Math.random() < 0.1) {
            stars = generateStars(200);
        }

        // Generate random player health power-ups less often
        if (Math.random() < 0.001) {
            int healthX = (int) (Math.random() * (WIDTH - 20));
            healthPowerUps.add(new Point(healthX, 0));
        }

        // Move health power-ups
        for (int i = 0; i < healthPowerUps.size(); i++) {
            healthPowerUps.get(i).y += 2;

            if (healthPowerUps.get(i).y > HEIGHT) {
                healthPowerUps.remove(i);
                i--;
            }
        }

        // Move projectile
        if (isProjectileVisible) {
            projectileY -= PROJECTILE_SPEED;

            if (projectileY < 0) {
                isProjectileVisible = false;
            }
        }

        // Check projectile collision with obstacle first, so laser stops the astro
        if (isProjectileVisible) {
            Rectangle projectileRect = new Rectangle(projectileX, projectileY, PROJECTILE_WIDTH, PROJECTILE_HEIGHT);

            for (int i = 0; i < obstacles.size(); i++) {
                Rectangle obstacleRect = new Rectangle(
                        obstacles.get(i).x,
                        obstacles.get(i).y,
                        OBSTACLE_WIDTH,
                        OBSTACLE_HEIGHT
                );

                if (projectileRect.intersects(obstacleRect)) {
                    obstacles.remove(i);
                    obstacleSprites.remove(i);
                    score += 10;
                    isProjectileVisible = false;
                    break;
                }
            }
        }

        // Check collision with player
        Rectangle playerRect = new Rectangle(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);

        for (int i = 0; i < obstacles.size(); i++) {
            Rectangle obstacleRect = new Rectangle(
                    obstacles.get(i).x,
                    obstacles.get(i).y,
                    OBSTACLE_WIDTH,
                    OBSTACLE_HEIGHT
            );

            if (playerRect.intersects(obstacleRect)) {
                if (!isShieldActive()) {
                    playerHealth -= 20;
                }

                obstacles.remove(i);
                obstacleSprites.remove(i);
                i--;

                if (playerHealth <= 0) {
                    playerHealth = 0;
                    isGameOver = true;
                }
            }
        }

        // Check collision with health power-up
        for (int i = 0; i < healthPowerUps.size(); i++) {
            Rectangle healthRect = new Rectangle(
                    healthPowerUps.get(i).x,
                    healthPowerUps.get(i).y,
                    20,
                    20
            );

            if (playerRect.intersects(healthRect)) {
                playerHealth += 20;

                if (playerHealth > 100) {
                    playerHealth = 100;
                }

                healthPowerUps.remove(i);
                i--;
            }
        }

        if (score >= 50 && level == 1) {
            level = 2;
            obstacleSpeed = 5;
        }

        if (score >= 100 && level == 2) {
            level = 3;
            obstacleSpeed = 7;
        }

        scoreLabel.setText("Score: " + score);
        healthLabel.setText("Health: " + playerHealth);
        levelLabel.setText("Level: " + level);
    }

    public void playSound() {
        if (clip != null) {
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start(); // Start playing the sound
        }
    }

    private void activateShield() {
        shieldActive = true;
        shieldStartTime = System.currentTimeMillis();
    }

    private void deactivateShield() {
        shieldActive = false;
    }

    private boolean isShieldActive() {
        return shieldActive && (System.currentTimeMillis() - shieldStartTime) < shieldDuration;
    }

    private void resetGame() {
        score = 0;
        playerHealth = 100;
        level = 1;
        timeLeft = 60;
        obstacleSpeed = OBSTACLE_SPEED;

        obstacles.clear();
        obstacleSprites.clear();
        healthPowerUps.clear();

        stars = generateStars(200);

        playerX = WIDTH / 2 - PLAYER_WIDTH / 2;
        playerY = HEIGHT - PLAYER_HEIGHT - 50;
        projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
        projectileY = playerY;

        isProjectileVisible = false;
        isGameOver = false;
        isFiring = false;
        shieldActive = false;

        scoreLabel.setText("Score: 0");
        healthLabel.setText("Health: 100");
        timerLabel.setText("Time: 60");
        levelLabel.setText("Level: 1");

        gamePanel.repaint();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_LEFT && playerX > 0) {
            playerX -= PLAYER_SPEED;

        } else if (keyCode == KeyEvent.VK_RIGHT && playerX < WIDTH - PLAYER_WIDTH) {
            playerX += PLAYER_SPEED;

        } else if (keyCode == KeyEvent.VK_SPACE && !isFiring && !isGameOver) {
            playSound();

            isFiring = true;
            projectileX = playerX + PLAYER_WIDTH / 2 - PROJECTILE_WIDTH / 2;
            projectileY = playerY;
            isProjectileVisible = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500); // Limit firing rate
                        isFiring = false;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();

        } else if (keyCode == KeyEvent.VK_CONTROL && !isGameOver) {
            activateShield();

        } else if (keyCode == KeyEvent.VK_ESCAPE) {
            resetGame();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyReleased(KeyEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new SpaceGame().setVisible(true);
            }
        });
    }
}