package core.entities;

import core.adapters.IWaifuAdapter;
import core.utils.Util;
import core.audio.AudioManager;
import core.settings.Settings;
import core.utils.WaifuUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import static java.awt.Image.SCALE_AREA_AVERAGING;

public class Secretary extends JFrame implements MouseListener, MouseMotionListener,
        MouseWheelListener, KeyListener, WindowListener {

    private static final String MAX_HEIGHT               = "waifu.height";
    private static final String MIRRORED                 = "waifu.mirrored";
    private static final String WELCOME_ENABLED          = "waifu.welcome.enabled";
    private static final String WELCOME_DELAY            = "waifu.welcome.delay";
    private static final String FLOAT_ENABLED            = "floating.enabled";
    private static final String VOICE_ENABLED            = "voice.enabled";
    private static final String VOICE_VOLUME             = "voice.volume";
    private static final String DIALOGS_ENABLED          = "dialogs.enabled";
    private static final String DIALOGS_ON_CLICK_ENABLED = "dialogs.onClick";
    private static final String DIALOGS_ON_IDLE_ENABLED  = "dialogs.onIdle";
    private static final String BALOON_DURATION_NO_VOICE = "dialogs.baloon.noVoiceDuration";
    private static final String ALWAYS_ON_TOP            = "waifu.alwaysOnTop";
    private static final String BALOON_TEXT_FORMAT_HTML  = "baloon.formatString";
    // private static final String OFFLINE               = "ship.offline";
    // private static final String CACHE_SAVE_LOCAL      = "dialogs.enabled";
    // private static final String CACHE_AUDIOS          = "dialogs.onClick";
    // private static final String CACHE_IMAGES          = "dialogs.onIdle";

    private final AudioManager audioManager = new AudioManager();

    private final IWaifuAdapter waifuInterface;

    private double startPoint = 0;
    private int    xClickPosition;

    private boolean running;
    private boolean alwaysOnTop = Settings.get(ALWAYS_ON_TOP, false);

    private int skinIndex = 0;

    private Baloon         baloon;
    private SecretaryLabel secretaryLabel;

    /**
     * Application start point
     *
     * @param waifu Your initialized waifu
     * @throws Exception Something went wrong while starting the ship
     */
    public Secretary(IWaifuAdapter waifu) throws Exception {

        waifuInterface = waifu;

        swingSetup();

        if (Settings.get(FLOAT_ENABLED, true)) {
            secretaryLabel.startFloating();
        }

        if (Settings.get(DIALOGS_ON_IDLE_ENABLED, true)) {
            idle();
        }

        if (Settings.get(WELCOME_ENABLED, true)) {
            onLogin(); // Say Hi!
        }

    }

    private void idle() {
        new Thread(() -> {
            Util.sleep(5000); // Wait 5 seconds before starting idle loop
            while (running) {
                secretaryLabel.waitIdle();
                speak(waifuInterface.getDialogs().get(waifuInterface.onIdleEventKey()));
                secretaryLabel.waitSpeak();
            }
        }).start();
    }

    private void onLogin() {
        new Thread(() -> {
            Util.sleep(Settings.get(WELCOME_DELAY, 5000));
            speak(waifuInterface.getDialogs().get(waifuInterface.onLoginEventKey()), false);
        }).start();
    }


    private Image loadSkin(int index) throws IOException {

        if (index < 0) {
            index = waifuInterface.getSkinCount() - 1;
            skinIndex = index;
        } else if (index == waifuInterface.getSkinCount()) {
            skinIndex = 0;
            index = 0;
        }

        byte[] imgData = WaifuUtils.getShipImage(waifuInterface, index);

        BufferedImage buffImage = ImageIO.read(new ByteArrayInputStream(imgData));
        if (Settings.get(MIRRORED, false)) {
            buffImage = Util.flipImage(buffImage);
        }

        Image i = buffImage.getScaledInstance(-1, Settings.get(MAX_HEIGHT, 800), SCALE_AREA_AVERAGING);

        setSize(i.getWidth(null), (int) Util.getScreenSize().getHeight());
        setLocation(0, Util.getYStartPosition(getHeight()));
        return i;
    }

    private void swingSetup() throws IOException {
        setTitle(waifuInterface.getName());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Listeners
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        addWindowListener(this);

        setLayout(null);
        setUndecorated(true);
        setBackground(new Color(0, 0, 0, 0));

        ImageIcon icn = new ImageIcon(loadSkin(Settings.get("waifu.skinIndex", 0)));
        secretaryLabel = new SecretaryLabel(icn);
        secretaryLabel.setBounds(secretaryLabel.getDesiredBounds(icn.getIconWidth(), Util.getScreenSize().height));

        baloon = new Baloon(getWidth(), getHeight());
        baloon.setBounds(baloon.getDesiredSize(icn.getIconWidth(), Util.getScreenSize().height));

        add(baloon);
        add(secretaryLabel);

        setAlwaysOnTop(alwaysOnTop);
        setVisible(true);
        secretaryLabel.onVisible();

        System.out.println("Swing setup done");

        running = true;
    }

    public void toggleAlwaysOnTop() {
        alwaysOnTop = !alwaysOnTop;
        setAlwaysOnTop(alwaysOnTop);
        System.out.println("Always on top: " + alwaysOnTop);
    }

    public void reloadSkin() throws IOException {
        secretaryLabel.setIcon(new ImageIcon(loadSkin(skinIndex)));
    }

    public void speak(List<Dialog> dialogs) {
        speak(dialogs, true);
    }

    public void speak(List<Dialog> dialogs, boolean withJump) {
        if (secretaryLabel.isSpeaking() || !Settings.get(DIALOGS_ENABLED, true) || !running) {
            return;
        }
        Random rand = new Random();

        Dialog dialog = dialogs.get(rand.nextInt(dialogs.size()));

        SwingUtilities.invokeLater(() -> {
            secretaryLabel.speak(true);

            baloon.setText(
                    Settings.get(BALOON_TEXT_FORMAT_HTML, "[[text]]")
                            .replace("[[text]]", dialog.getDialog() + "<br>&zwnj;")
            );

            // Activate baloon only if there is text to show
            baloon.toggle(!dialog.getDialog().equals(""));
            new Thread(() -> {
                if (Settings.get(VOICE_ENABLED, true) && dialog.getAudio() != null) {

                    audioManager.play(dialog.getAudio(), Settings.get(VOICE_VOLUME, 50));
                } else {
                    Util.sleep(Settings.get(BALOON_DURATION_NO_VOICE, 3000));
                }

                baloon.toggle(false);
                secretaryLabel.speak(false);
            }).start();
        });
    }


    public void close() {
        this.running = false;
        this.dispose();
    }

    private void onClick() {
        if (Settings.get(DIALOGS_ON_CLICK_ENABLED, true)) {
            speak(waifuInterface.getDialogs().get(waifuInterface.onTouchEventKey()));
        }
    }

    // Region: Swing Mouse Events
    @Override
    public void mousePressed(MouseEvent e) {
        startPoint = e.getXOnScreen();
        xClickPosition = e.getXOnScreen();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        startPoint = e.getXOnScreen();
        if (xClickPosition == e.getXOnScreen()) {
            secretaryLabel.speakJump();
            onClick();
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        setLocation((int) getLocation().getX() + e.getXOnScreen() - (int) startPoint, getY());
        startPoint = e.getXOnScreen();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        try {
            switch (e.getKeyChar()) {
                case 'k':
                    skinIndex++;
                    reloadSkin();
                    break;
                case 'j':
                    skinIndex--;
                    reloadSkin();
                    break;
                case 't':
                    toggleAlwaysOnTop();
                    break;
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    // Endregion
}