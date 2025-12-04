import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicScrollBarUI; import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets; import java.nio.file.Files;
import java.sql.Connection; import java.sql.DriverManager; import java.sql.PreparedStatement;
import java.sql.ResultSet; import java.sql.SQLException; import java.sql.Statement; import java.sql.Blob;
import java.text.Normalizer; import java.time.LocalDateTime; import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

@SuppressWarnings({"ClassEscapesDefinedScope", "FieldCanBeLocal", "ResultOfMethodCallIgnored", "SqlNoDataSourceInspection", "BusyWait"})

public class UIMain {
    private static final int BORDER_THICKNESS = 10;
    private static final String dbuser = "root", dbpass = "RfedDWK3KHnnk3zSomot0Q==:OLeLvF7ifez49gr4GFv1MrG3dUXOGMFczx8F1PtyFvM=",
                                dbdir = "jdbc:mysql://localhost:3306/bd_umail?useSSL=false&serverTimezone=UTC";
    public static UIMail frame;

    public static File filecache;
    public static File logdir;
    public static File logfile;
    public static PrintWriter logfilewriter;

    public static File loginfile;
    public static Properties loginproperties;
    public static File configfile;
    public static Properties configproperties;
    public static File statefile;
    public static Properties stateproperties;

    private static Socket connection;
    private static Scanner in;
    private static PrintWriter out;
    private static boolean connected = false;
    private static JScrollPane consoleScrollPane;

    private static JTextField senderfield, receiverfield, subjectfield;
    private static JTextArea bodyfield;
    private static JButton clipButton;
    private static UIMail.RoundedButton send;

    private static boolean showing = false;
    private static final ArrayList<File> files = new ArrayList<>();
    private static final ArrayList<File> filesmem = new ArrayList<>();
    private static String sendermem = "", receivermem = "", subjectmem = "", bodymem = "";
    private static JPanel filesPanel;

    private static final Color mainblue = new Color(6,186,199);
    private static final Color maindark = new Color(33,33,33);
    private static final Color mainlight = new Color(45,45,45);

    public static void main(String[] args) {
        try (Connection conexion = DriverManager.getConnection(dbdir,
                dbuser, dbpass)) {
            createAndLoadAll();
            if (Boolean.parseBoolean(loginproperties.getProperty("login.bool"))) {
                String query = "SELECT * FROM users WHERE ID='" + loginproperties.getProperty("login.id") + "'";
                PreparedStatement ps = conexion.prepareStatement(query);
                ResultSet rs = ps.executeQuery();
                boolean isadmin = false; String name = ""; int goodID = -1;
                while (rs.next()) {
                    goodID = rs.getInt("ID");
                    isadmin = rs.getBoolean("admin");
                    name = rs.getString("name");
                }
                if (goodID == Integer.parseInt(loginproperties.getProperty("login.id")) && Encryptor.verify(loginproperties.getProperty("login.hash"), name)) {
                    frame = new UIMail(Integer.parseInt(loginproperties.getProperty("login.id")), name, isadmin);
                } else {
                    loginproperties.setProperty("login.bool", "false");
                    saveProperties(loginproperties, loginfile);
                    JOptionPane.showMessageDialog(null, "Credentials not valid. You should know\n that trying to break into someone's account without\n permission is illegal and could potentially get\n you in trouble. Please, re-login.", "Login Failed", JOptionPane.WARNING_MESSAGE);
                    new LoginUI("UI-Mail login");
                }
            } else {
                new LoginUI("UI-Mail login");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error connecting to database, please check your connection.\n If you keep having trouble please get in touch with the administrator.", "UI-Mail Error", JOptionPane.ERROR_MESSAGE);
        }
        //delCache();
    }

    private static void createAndLoadAll() {
        try {
            filecache = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "filecache" + File.separator);
            filecache.mkdirs();

            logdir = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "logs" + File.separator);
            logdir.mkdirs();
            logfile = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "logs" + File.separator + "sessionlog_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
            System.out.println("sessionlog_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + ".txt");
            logfile.createNewFile();
            logfilewriter = new PrintWriter(logfile);

            loginfile = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "cache" + File.separator + "login.properties");
            statefile = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "cache" + File.separator + "state.properties");
            loginfile.getParentFile().mkdirs();
            loginfile.createNewFile();
            statefile.createNewFile();
            loginproperties = loadProperties(loginfile);
            stateproperties = loadProperties(statefile);

            configfile = new File(System.getenv("APPDATA") + File.separator + "UI-Mail" + File.separator + "config.properties");
            configfile.getParentFile().mkdirs();
            configfile.createNewFile();
            configproperties = loadProperties(configfile);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void delCache() {
        File[] files = filecache.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.delete()) {
                    UIMail.console.println("INTERNAL ERROR WHILE REMOVING FILES IN UI");
                }
            }
        }
    }

    private static Properties loadProperties(File file) {
        Properties properties = new Properties();

        if (!file.exists()) {
            try (InputStream defaultConfig = UIMail.class.getClassLoader().getResourceAsStream(file.getName())) {
                if (defaultConfig != null) {
                    properties.load(defaultConfig);
                    saveProperties(properties, file);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    private static void saveProperties(Properties properties, File file) {
        try (FileOutputStream fos = new FileOutputStream(file.getAbsolutePath())) {
            properties.store(fos, "Archivo actualizado");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class LoginUI extends MyFrame {
        public LoginUI(String title) {
            super(title, new Dimension(220, 225));

            FocusListener focusListener = new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (e.getComponent().getForeground() == Color.red) {
                        e.getComponent().setForeground(Color.WHITE);
                    }
                }
            };

            TitledBorder titledBorder = new TitledBorder(BorderFactory.createLineBorder(Color.white));
            titledBorder.setTitleColor(Color.white); titledBorder.setTitle("Usuario o E-Mail");

            JTextField user = new JTextField();
            user.setBounds(10, 35, 200, 40);
            user.setBackground(maindark); user.setForeground(Color.white);
            user.setBorder(titledBorder);
            user.addFocusListener(focusListener);

            titledBorder = new TitledBorder(BorderFactory.createLineBorder(Color.white));
            titledBorder.setTitleColor(Color.white); titledBorder.setTitle("Contraseña");

            JPasswordField pswd = new JPasswordField();
            pswd.setBounds(10, 85, 200, 40);
            pswd.setEditable(true); pswd.setBackground(maindark); pswd.setForeground(Color.white);
            pswd.setBorder(titledBorder);
            pswd.addFocusListener(focusListener);

            JCheckBox session = new JCheckBox();
            session.setBounds(10, 125, 200, 40);
            session.setBackground(maindark); session.setForeground(Color.white);
            session.setFocusable(false); session.setFocusPainted(false);
            session.setText("Keep me logged in");

            JButton login = new JButton();
            login.setBounds(70, 170, 80, 30);
            login.setBackground(maindark); login.setForeground(Color.white);
            login.setFocusable(false); login.setFocusPainted(false);
            login.setBorder(BorderFactory.createLineBorder(Color.white));
            login.setText("Log in");
            login.addActionListener(e -> {
                String query = "SELECT * FROM users WHERE name='" + user.getText().trim() + "'";
                try (Connection conexion = DriverManager.getConnection(dbdir,
                        dbuser, dbpass)) {
                    PreparedStatement ps = conexion.prepareStatement(query);
                    ResultSet rs = ps.executeQuery();
                    boolean correctCredentials = false, admin = false, found = false;
                    int ID = -1;
                    String username = "";
                    while (rs.next()) {
                        found = true;
                        if (Encryptor.verify(rs.getString("password"), pswd.getText())) {
                            correctCredentials = true;
                            ID = rs.getInt("ID");
                            username = rs.getString("name");
                            admin = rs.getBoolean("admin");
                        } else pswd.setForeground(Color.red);
                    }
                    if (!found) {
                        user.setForeground(Color.red);
                    } else if (correctCredentials) {
                        dispose();
                        if (session.isSelected()) {
                            loginproperties.setProperty("login.bool", "true");
                            loginproperties.setProperty("login.id", String.valueOf(ID));
                            loginproperties.setProperty("login.hash", Encryptor.encrypt(username));
                            saveProperties(loginproperties, loginfile);
                        }
                        frame = new UIMail(ID, username, admin);
                    }
                } catch (SQLException ex) {
                    System.out.println("ERROR al cargar correos desde la base de datos: " + ex.getMessage());
                    throw new RuntimeException(ex);
                }
            });

            panelAdd(user, 200);
            panelAdd(pswd, 200);
            panelAdd(session, 200);
            panelAdd(login, 200);
        }
    }

    private static class UIMail extends JFrame {
        private Point initialClick;
        private boolean resizing = false;
        private int cursor = Cursor.DEFAULT_CURSOR;

        private ArrayList<infoButton> buttons = new ArrayList<>();

        private double sval = 5, cval = 6;

        private final JPanel main;
        private final JPanel side;
        private static console console;
        private final JLabel background;
        private final JLayeredPane top;
        private final JButton closeButton;
        private final JButton minimizeButton;
        private final JButton fullscreenButton;
        private final JScrollPane scrollPane;

        private final int ID;
        private final String user;
        private final boolean admin;

        public UIMail(int ID, String user, boolean admin) {
            System.setProperty("sun.java2d.uiScale", "1.0");

            this.ID = ID;
            this.user = user;
            this.admin = admin;

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            double width = screenSize.width / 1.35;
            double height = screenSize.height / 1.30;

            setSize((int) width, (int) height);
            setMinimumSize(new Dimension((int) (width / 2), (int) height));
            setUndecorated(true);
            setLocationRelativeTo(null);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setIconImage(NoScalingIcon.scaledImage("logo.png", 500,500));
            setTitle("UI-Mail Tool");
            setLayout(null);

            top = new JLayeredPane();
            top.setLayout(new BorderLayout());
            top.setBackground(maindark);
            top.setBounds(0,0, getWidth(), getHeight() / 10);
            add(top);

            background = new JLabel();
            background.setBounds(-250, 0, getWidth() * 2, 100);
            background.setIcon(NoScalingIcon.scaledIcon("back.png", getWidth() * 2, getHeight()));
            top.add(background, BorderLayout.LINE_START);
            top.setLayer(background, 0);

            side = new JPanel();
            side.setBackground(mainlight);
            side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));

            scrollPane = new JScrollPane(side);
            scrollBarSet(scrollPane);
            scrollPane.getVerticalScrollBar().setUnitIncrement(10);
            scrollPane.setPreferredSize(new Dimension(200, 400));
            scrollPane.setBounds(0,top.getHeight(), (int) (getWidth() / sval),  getHeight() - top.getHeight());
            scrollPane.setBorder(null);
            scrollPane.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    super.mousePressed(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    super.mouseReleased(e);
                }
            });
            add(scrollPane);

            sideButtons();

            main = new JPanel();
            main.setBackground(maindark);
            main.setLayout(null);
            main.setBounds(scrollPane.getWidth(), top.getHeight(), getWidth() - side.getWidth(), getHeight() - top.getHeight() - (int) (getHeight() / cval));
            add(main);

            console = new console();
            console.setLineWrap(true);
            console.setWrapStyleWord(true);

            consoleScrollPane = new JScrollPane(console);
            scrollBarSet(consoleScrollPane);
            consoleScrollPane.setBounds(scrollPane.getWidth(), top.getHeight() + main.getHeight(), main.getWidth(), (int) (getHeight() / cval));
            consoleScrollPane.setBorder(null);
            add(consoleScrollPane);

            JLabel senderinfo = new JLabel("From: ");
            senderinfo.setForeground(Color.WHITE);
            senderinfo.setBackground(new Color(0,0,0,100));
            senderinfo.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,50)));
            senderinfo.setIcon(NoScalingIcon.scaledIcon("from.png", 25,25));
            senderinfo.setBounds(25, 20, 75, 30);
            main.add(senderinfo);

            senderfield = new JTextField();
            senderfield.setBackground(new Color(0,0,0,100));
            senderfield.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,50)));
            senderfield.setForeground(Color.WHITE);
            senderfield.setCaretColor(Color.WHITE);
            senderfield.setOpaque(false);
            senderfield.setBounds(100, 20, 250, 30);
            main.add(senderfield);

            JLabel receiverinfo = new JLabel("To: ");
            receiverinfo.setForeground(Color.WHITE);
            receiverinfo.setBackground(new Color(0,0,0,100));
            receiverinfo.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,50)));
            receiverinfo.setIcon(NoScalingIcon.scaledIcon("to.png", 25,25));
            receiverinfo.setBounds(25, 50, 75, 30);
            main.add(receiverinfo);

            receiverfield = new JTextField();
            receiverfield.setBackground(new Color(0,0,0,100));
            receiverfield.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,50)));
            receiverfield.setForeground(Color.WHITE);
            receiverfield.setCaretColor(Color.WHITE);
            receiverfield.setOpaque(false);
            receiverfield.setBounds(100, 50, 250, 30);
            main.add(receiverfield);

            subjectfield = new JTextField("Write your subject here");
            subjectfield.setBackground(new Color(30,30,30,50));
            subjectfield.setBorder(BorderFactory.createLineBorder(new Color(255,255,255,50)));
            subjectfield.setForeground(mainlight.brighter().brighter());
            subjectfield.setCaretColor(Color.WHITE);
            subjectfield.setOpaque(false);
            subjectfield.setBounds(25, 80, 325, 45);
            subjectfield.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (subjectfield.isEnabled() && subjectfield.getText().equals("Write your subject here")) {
                        subjectfield.setText("");
                        subjectfield.setForeground(Color.white);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (subjectfield.isEnabled() && subjectfield.getText().isEmpty()) {
                        subjectfield.setText("Write your subject here");
                        subjectfield.setForeground(mainlight.brighter().brighter());
                    }
                }
            });
            main.add(subjectfield);

            bodyfield = new JTextArea("Write the text of your body here");
            bodyfield.setBackground(new Color(0, 0, 0, 100));
            bodyfield.setForeground(mainlight.brighter().brighter());
            bodyfield.setCaretColor(Color.WHITE);
            bodyfield.setOpaque(false);
            bodyfield.setLineWrap(true);
            bodyfield.setWrapStyleWord(true);
            bodyfield.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (bodyfield.isEnabled() && bodyfield.getText().equals("Write the text of your body here")) {
                        bodyfield.setText("");
                        bodyfield.setForeground(Color.white);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (bodyfield.isEnabled() && bodyfield.getText().isEmpty()) {
                        bodyfield.setText("Write the text of your body here");
                        bodyfield.setForeground(mainlight.brighter().brighter());
                    }
                }
            });

            JScrollPane scrollBody = new JScrollPane(bodyfield);
            scrollBody.setBounds(25, 125, 325, 275);
            scrollBody.setOpaque(false);
            scrollBody.getViewport().setOpaque(false);
            scrollBody.setBorder(BorderFactory.createLineBorder(new Color(255, 255, 255, 50)));
            scrollBarSet(scrollBody);
            main.add(scrollBody);

            send = new RoundedButton("Send");
            send.setBackground(new Color(45,45,45,100));
            send.setForeground(Color.WHITE);
            send.setCornerRadius(25);
            send.setBorderColor(new Color(0,0,0,100));
            send.setBorderThickness(2);
            send.setBounds(275, 415, 75,35);
            send.setFocusable(false);
            send.addActionListener(e -> {
                if (connected) {
                    boolean valid = true;
                    if (senderfield.getText().isEmpty() || senderfield.getText().isBlank() || !senderfield.getText().contains("@")) {
                        senderfield.setForeground(Color.red);
                        valid = false;
                    }
                    if (receiverfield.getText().isEmpty() || receiverfield.getText().isBlank() || !receiverfield.getText().contains("@")) {
                        receiverfield.setForeground(Color.red);
                        valid = false;
                    }
                    if (subjectfield.getText().isEmpty() || subjectfield.getText().isBlank() ||
                            subjectfield.getText().equals("Write your subject here")) {
                        subjectfield.setText("");
                    }
                    if (bodyfield.getText().isEmpty() || bodyfield.getText().isBlank() ||
                            bodyfield.getText().equals("Write the text of your body here")) {
                        bodyfield.setText("");
                    }
                    if (valid) {
                        if (showing) {
                            ArrayList<File> aux = new ArrayList<>();
                            String inipath = filecache.getAbsolutePath() + File.separator;
                            for (File f : files) {
                                aux.add(new File(inipath + f.getName()));
                            }
                            files.clear();
                            files.addAll(aux);
                        }
                        sendMail(senderfield.getText().trim(), receiverfield.getText().trim(),
                                subjectfield.getText().trim(), bodyfield.getText());
                        sideButtons();
                    } else {
                        console.println("Invalid parameters");
                    }
                } else {
                    console.println("Not connected");
                }
            });
            main.add(send);

            FocusListener focusListener = new FocusAdapter() {
                public void focusGained(FocusEvent e) {
                    if (e.getComponent().getForeground() == Color.red) {
                        e.getComponent().setForeground(Color.WHITE);
                    }
                }
            };
            senderfield.addFocusListener(focusListener);
            receiverfield.addFocusListener(focusListener);
            subjectfield.addFocusListener(focusListener);
            bodyfield.addFocusListener(focusListener);

            filesPanel = new JPanel();
            filesPanel.setBounds(25,400,send.getX() - 50,50);
            filesPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            filesPanel.setBackground(new Color(0,0,0, 0));
            filesPanel.setOpaque(false);
            main.add(filesPanel);

            JPanel lefttop = new JPanel();
            lefttop.setBackground(new Color(0,0,0,0));
            lefttop.setPreferredSize(new Dimension(getMinimumSize().width - 200, top.getHeight()));
            lefttop.setLayout(null);
            lefttop.setOpaque(false);

            JLabel logo = new JLabel();
            logo.setIcon(NoScalingIcon.scaledIcon("logo.png", 50, 44));
            logo.setHorizontalTextPosition(SwingConstants.RIGHT);
            logo.setForeground(Color.WHITE);
            logo.setFont(new Font("Verdana", Font.PLAIN, 12));
            logo.setBounds(0, 5, 300, 44);
            lefttop.add(logo);

            RoundedButton newMail = getNewMailButton();
            lefttop.add(newMail);

            JButton reloadButton = new JButton();
            reloadButton.setBounds(150, 14, 30,30);
            reloadButton.setIcon(NoScalingIcon.scaledIcon("reload.png", 18, 18));
            reloadButton.setBackground(new Color(0,0,0,0));
            reloadButton.setBorder(null);
            reloadButton.setFocusable(false);
            reloadButton.setFocusPainted(false);
            reloadButton.setOpaque(false);
            reloadButton.addActionListener(e -> sideButtons());
            lefttop.add(reloadButton);

            clipButton = new JButton("Attach");
            clipButton.setBounds(180, 14, 75,30);
            clipButton.setBackground(new Color(0,0,0,0));
            clipButton.setForeground(Color.WHITE);
            clipButton.setFont(new Font("Verdana", Font.PLAIN, 10));
            clipButton.setIcon(NoScalingIcon.scaledIcon("clip.png", 18, 18));
            clipButton.setBorder(null);
            clipButton.setFocusable(false);
            clipButton.setFocusPainted(false);
            clipButton.setOpaque(false);
            clipButton.addActionListener(e -> {
               JFileChooser chooser = new JFileChooser();
               chooser.setBackground(maindark);
               chooser.setForeground(Color.white);
               chooser.setMultiSelectionEnabled(true);
               chooser.setCurrentDirectory(new File(System.getProperty("user.home") + File.separator + "Desktop"));
               if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                   files.addAll(Arrays.asList(chooser.getSelectedFiles()));
                   fileButtons();
               }
            });
            lefttop.add(clipButton);

            MyFrame configFrame;
            MyFrame profileFrame;

            JButton configButton = new JButton();
            configButton.setBounds(255, 14, 30,30);
            configButton.setBackground(new Color(0,0,0,0));
            configButton.setIcon(NoScalingIcon.scaledIcon("config.png", 18, 18));
            configButton.setBorder(null);
            configButton.setFocusable(false);
            configButton.setFocusPainted(false);
            configButton.setOpaque(false);
            configButton.addActionListener(e -> {

            });
            lefttop.add(configButton);

            JButton profileButton = new JButton();
            profileButton.setBounds(285, 14, 30,30);
            profileButton.setBackground(new Color(0,0,0,0));
            profileButton.setIcon(NoScalingIcon.scaledIcon("from.png", 20, 20));
            profileButton.setBorder(null);
            profileButton.setFocusable(false);
            profileButton.setFocusPainted(false);
            profileButton.setOpaque(false);
            profileButton.addActionListener(e -> {

            });
            lefttop.add(profileButton);

            JButton adminButton = null;
            if (admin) {
                adminButton = new JButton();
                adminButton.setBounds(315, 14, 30,30);
                adminButton.setBackground(new Color(0,0,0,0));
                adminButton.setIcon(NoScalingIcon.scaledIcon("admin.png", 18, 18));
                adminButton.setBorder(null);
                adminButton.setFocusable(false);
                adminButton.setFocusPainted(false);
                adminButton.setOpaque(false);
                adminButton.addActionListener(e -> {
                    MyFrame adminFrame = new MyFrame("Admin Panel", new Dimension(600,600));
                    adminFrame.repaint();
                });
                lefttop.add(adminButton);
            }

            top.add(lefttop, BorderLayout.LINE_START);
            top.setLayer(lefttop, 100);

            JPanel righttop = new JPanel();
            righttop.setBackground(new Color(0,0,0,0));
            righttop.setPreferredSize(new Dimension(200, 50));
            righttop.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 5));
            righttop.setOpaque(false);
            top.add(righttop, BorderLayout.LINE_END);
            top.setLayer(righttop, 100);

            minimizeButton = new JButton();
            minimizeButton.setIcon(NoScalingIcon.scaledIcon("minB.png", 20, 20));
            minimizeButton.setBackground(new Color(0,0,0,0));
            minimizeButton.setBorderPainted(false);
            minimizeButton.setFocusable(false);
            minimizeButton.setOpaque(false);
            minimizeButton.addActionListener(e -> setState(JFrame.ICONIFIED));

            fullscreenButton = new JButton();
            fullscreenButton.setIcon(NoScalingIcon.scaledIcon("maxB2.png", 20, 20));
            fullscreenButton.setBackground(new Color(0,0,0,0));
            fullscreenButton.setBorderPainted(false);
            fullscreenButton.setFocusable(false);
            fullscreenButton.setOpaque(false);
            fullscreenButton.addActionListener(e -> {
                setExtendedState(getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH);
                if (getExtendedState() == JFrame.MAXIMIZED_BOTH) {
                    fullscreenButton.setIcon(NoScalingIcon.scaledIcon("maxB.png", 20, 20));
                    dispatchEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
                } else {
                    fullscreenButton.setIcon(NoScalingIcon.scaledIcon("maxB2.png", 20, 20));
                    setSize(getWidth(), getHeight() + 5);
                    dispatchEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
                    setSize(getWidth(), getHeight() - 5);
                }
            });

            closeButton = new JButton();
            closeButton.setIcon(NoScalingIcon.scaledIcon("cross.png", 20, 20));
            closeButton.setBackground(new Color(0,0,0,0));
            closeButton.setBorderPainted(false);
            closeButton.setFocusable(false);
            closeButton.setOpaque(false);
            closeButton.addActionListener(e -> {
                try {
                    Scanner prover = new Scanner(logfile);
                    logfilewriter.close();
                    int counter = 0;
                    while (prover.hasNextLine()) {
                        if (counter > 1) break;
                        prover.nextLine();
                        counter++;
                    }
                    System.out.println(counter);
                    if (counter == 1) {
                        System.out.println("Deleting " + logfile.getAbsolutePath());
                        logfile.delete();
                    }
                } catch (FileNotFoundException ignored) {
                } finally {
                    System.exit(0);
                }
            });

            righttop.add(minimizeButton);
            righttop.add(fullscreenButton);
            righttop.add(closeButton);

            addDragFunctionality(top);
            addResizeFunctionality();

            addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    top.setBounds(0,0, getWidth(), getHeight() / 10);
                    scrollPane.setBounds(0,top.getHeight(), (int) (getWidth() / sval),  getHeight() - top.getHeight());
                    consoleScrollPane.setBounds(scrollPane.getWidth(), (int) (getHeight() - (getHeight() / cval)), getWidth() - scrollPane.getWidth(), getHeight() - (int) (getHeight() - (getHeight() / cval)));
                    main.setBounds(scrollPane.getWidth(), top.getHeight(), consoleScrollPane.getWidth(), getHeight() - top.getHeight() - consoleScrollPane.getHeight());

                    senderfield.setSize(main.getWidth() - 125, senderfield.getHeight());
                    receiverfield.setSize(main.getWidth() - 125, receiverfield.getHeight());
                    subjectfield.setSize(main.getWidth() - 50, subjectfield.getHeight());
                    scrollBody.setSize(main.getWidth() - 50, scrollBody.getHeight());

                    if (getHeight() > getMinimumSize().height) {
                        main.setSize(main.getWidth(), getHeight() - consoleScrollPane.getHeight() - top.getHeight());
                        scrollBody.setSize(scrollBody.getWidth(), main.getHeight() - scrollBody.getY() - send.getHeight() - 40);
                    }

                    bodyfield.setSize(scrollBody.getSize());
                    send.setLocation(25 + scrollBody.getWidth() - send.getWidth(), scrollBody.getY() + scrollBody.getHeight() + 15);
                    filesPanel.setBounds(25,scrollBody.getY() + scrollBody.getHeight(),send.getX() - 50,30 + send.getHeight());
                }

            });

            setVisible(true);
            console.println("Welcome Again, " + user);
            setSize(getWidth(), getHeight() + 5);
            dispatchEvent(new ComponentEvent(this, ComponentEvent.COMPONENT_RESIZED));
            setSize(getWidth(), getHeight() - 5);
        }

        private RoundedButton getNewMailButton() {
            RoundedButton newMail = new RoundedButton("New Mail");
            newMail.setBackground(new Color(6,186,199,75));
            newMail.setForeground(Color.WHITE);
            newMail.setBounds(50, 15, 80, 28);
            newMail.setFont(new Font("Verdana", Font.PLAIN, 10));
            newMail.setBorderColor(new Color(45,45,45,100));
            newMail.setBorderThickness(2);
            newMail.setCornerRadius(20);
            newMail.setOpaque(false);
            newMail.setFocusable(false);
            newMail.setFocusPainted(false);
            newMail.addActionListener(e -> {
                if (showing) {
                    clipButton.setEnabled(true);
                    senderfield.setEnabled(true);
                    receiverfield.setEnabled(true);
                    subjectfield.setEnabled(true);
                    bodyfield.setEnabled(true);
                    showing = false;
                    if (!sendermem.isEmpty() || !receivermem.isEmpty() || !subjectmem.equals("Write your subject here") || !bodymem.equals("Write the text of your body here")) {
                        senderfield.setText(sendermem);
                        receiverfield.setText(receivermem);
                        subjectfield.setText(subjectmem);
                        bodyfield.setText(bodymem);
                    } else {
                        senderfield.setText("");
                        receiverfield.setText("");
                        subjectfield.setText("Write your subject here");
                        subjectfield.setForeground(mainlight.brighter().brighter());
                        bodyfield.setText("Write the text of your body here");
                        bodyfield.setForeground(mainlight.brighter().brighter());
                    }
                    sendermem = ""; receivermem = ""; subjectmem = ""; bodymem = "";
                    files.clear();
                    if (!filesmem.isEmpty()) {
                        files.addAll(filesmem);
                        filesmem.clear();
                    }
                    fileButtons();
                    for (Component component : filesPanel.getComponents()) {
                        component.setEnabled(true);
                    }
                }
                newMail.repaint();
                newMail.getParent().repaint();
                newMail.getParent().getParent().repaint();
            });
            return newMail;
        }

        public void scrollBarSet(JScrollPane s) {
            s.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = mainlight;
                    this.trackColor = maindark;
                }

                @Override
                protected JButton createDecreaseButton(int orientation) {
                    return new JButton() {{
                        setPreferredSize(new Dimension(0, 0));
                    }};
                }

                @Override
                protected JButton createIncreaseButton(int orientation) {
                    return new JButton() {{
                        setPreferredSize(new Dimension(0, 0));
                    }};
                }
            });
            s.getVerticalScrollBar().addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                    super.mouseMoved(e);
                }
            });
            s.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            s.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        }

        public void fileButtons() {
            filesPanel.removeAll();

            if (!files.isEmpty()) {
                for (File file : files) {
                    if (!file.getName().isBlank()) {
                        JButton rb = new JButton(file.getName());
                        String nameof = file.getName().trim();
                        Icon icon;

                        rb.setOpaque(true);
                        if (nameof.endsWith(".zip") || nameof.endsWith(".7z") || nameof.endsWith(".rar") || nameof.endsWith(".gz")) {
                            icon = NoScalingIcon.scaledIcon("files.png", 25, 25);
                        } else if (nameof.endsWith(".png") || nameof.endsWith(".gif") || nameof.endsWith(".jpg")
                                || nameof.endsWith(".webp") || nameof.endsWith(".jpeg") || nameof.endsWith(".ico")) {
                            try {
                                File imgFile = new File(filecache.getAbsolutePath() + File.separator + nameof);
                                if (imgFile.exists() && imgFile.canRead()) {
                                    file = imgFile;
                                }
                                Image img = ImageIO.read(file);
                                if (img != null) {
                                    icon = new ImageIcon(img.getScaledInstance(25, 25, Image.SCALE_SMOOTH));
                                } else {
                                    icon = NoScalingIcon.scaledIcon("image.png", 25, 25);
                                }
                            } catch (IOException e) {
                                icon = NoScalingIcon.scaledIcon("image.png", 25, 25);
                            }
                        } else if (nameof.endsWith(".java") || nameof.endsWith(".jar")) {
                            icon = NoScalingIcon.scaledIcon("java.png", 25, 25);
                        } else if (nameof.endsWith(".bat") || nameof.endsWith(".exe") || nameof.endsWith(".sh")
                                || nameof.endsWith(".dll") || nameof.endsWith(".com")) {
                            icon = NoScalingIcon.scaledIcon("executable.png", 25, 25);
                        } else {
                            icon = NoScalingIcon.scaledIcon("textfile.png", 25, 25);
                        }

                        rb.setIcon(icon);
                        rb.setBackground(maindark);
                        rb.setBorder(null);
                        rb.setForeground(Color.white);
                        rb.setFont(new Font("Verdana", Font.PLAIN, 10));
                        rb.setIconTextGap(5);

                        FontMetrics fm = rb.getFontMetrics(rb.getFont());
                        int textWidth = fm.stringWidth(rb.getText());
                        int buttonWidth = textWidth + icon.getIconWidth() + rb.getIconTextGap() + 10;
                        int buttonHeight = Math.max(icon.getIconHeight(), fm.getHeight()) + 6;

                        rb.setPreferredSize(new Dimension(buttonWidth, buttonHeight));

                        rb.setFocusable(false);
                        rb.setFocusPainted(false);

                        rb.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseEntered(MouseEvent e) {
                                if (showing) {
                                    e.getComponent().setForeground(mainblue);
                                } else {
                                    e.getComponent().setForeground(Color.red);
                                }
                            }

                            @Override
                            public void mouseExited(MouseEvent e) {
                                e.getComponent().setForeground(Color.white);
                            }
                        });

                        File finalFile = file;
                        rb.addActionListener(e -> {
                            if (showing) {
                                try {
                                    Desktop.getDesktop().open(finalFile);
                                } catch (Exception ex) {
                                    console.println("ERROR: " + ex.getMessage());
                                }
                            } else {
                                String name = e.getSource().toString().split("text=")[1].split(",")[0];
                                int i;
                                for (i = 0; i < files.size(); i++) {
                                    if (name.equals(files.get(i).getName())) {
                                        break;
                                    }
                                }
                                if (i != files.size() - 1 || name.equals(files.get(i).getName())) {
                                    files.remove(files.get(i));
                                    fileButtons();
                                }
                            }
                        });

                        filesPanel.add(rb);
                    }

                }
            }

            filesPanel.setPreferredSize(filesPanel.getPreferredSize());
            filesPanel.revalidate();
            filesPanel.repaint();
        }

        private void createMailButtons() {
            String query = (admin) ? "SELECT * FROM mails ORDER BY timestamp DESC" : "SELECT * FROM mails WHERE sentby='" + user + "' ORDER BY timestamp DESC";

            try (Connection conexion = DriverManager.getConnection(dbdir,
                    dbuser, dbpass);
                 PreparedStatement ps = conexion.prepareStatement(query);
                 ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    int ID = rs.getInt("ID");
                    String mode = rs.getString("server");
                    String sender = rs.getString("sender").split("@")[0];
                    String receiver = rs.getString("receiver");
                    String subject = rs.getString("subject");
                    String body = rs.getString("body");
                    String timestamp = rs.getString("timestamp");
                    String sentby = rs.getString("sentby");

                    String[] dateTimeParts = timestamp.split(" ");
                    String date = dateTimeParts[0];
                    String time = dateTimeParts[1];

                    StringBuilder receiverFormatted = new StringBuilder();
                    if (receiver.contains(",")) {
                        String[] directions = receiver.split(",");
                        for (String direction : directions) {
                            receiverFormatted.append(direction.trim().split("@")[0]).append(", ");
                        }
                        receiverFormatted = new StringBuilder(receiverFormatted.substring(0, receiverFormatted.length() - 2));
                    } else {
                        receiverFormatted = new StringBuilder(receiver.split("@")[0]);
                    }

                    String htmlText = "<html>"
                            + "<div style='text-align:left; font-family:Trebuchet MS;'>"
                            + "<h3 style='margin:2.5px;'>" + sentby + " in " +  mode + "</h3>"
                            + "<p style='margin:2.5px;'> " + sender + " → " + receiverFormatted + "</p>"
                            + "<div style='text-align:left; font-size:10px; margin-top:2.5px; color:gray; margin:2.5px;'>"
                            + date + " " + time
                            + "</div>"
                            + "</div></html>";

                    infoButton button = new infoButton(ID ,mode, rs.getString("sender"), rs.getString("receiver"),
                            subject, body, date, time);
                    button.setText(htmlText);
                    button.setBackground(mainlight);
                    button.setBorder(BorderFactory.createLineBorder(maindark));
                    button.setForeground(Color.white);
                    button.setPreferredSize(new Dimension(side.getWidth() - 4, 75));
                    button.setHorizontalAlignment(SwingConstants.LEFT);
                    button.setAlignmentX(Component.CENTER_ALIGNMENT);
                    button.setFocusable(false);

                    buttons.add(button);
                }
            } catch (SQLException e) {
                System.out.println("ERROR al cargar correos desde la base de datos: " + e.getMessage());
            }
        }

        private void sideButtons() {
            buttons = new ArrayList<>();
            side.removeAll();

            createMailButtons();

            if (buttons.isEmpty()) {
                infoButton emptySpace = getEmptyButton();
                side.add(emptySpace);
            } else {
                for (infoButton b : buttons) {
                    side.add(b);
                    side.add(Box.createVerticalStrut(10));
                }
            }

            side.setPreferredSize(new Dimension(side.getWidth(), buttons.size() * 85));
            side.revalidate();
            side.repaint();
        }

        private infoButton getEmptyButton() {
            infoButton emptySpace = new infoButton();
            emptySpace.setText("Oops! Nothing here yet");
            emptySpace.setBackground(mainlight);
            emptySpace.setBorder(null);
            emptySpace.setForeground(Color.white);
            emptySpace.setPreferredSize(new Dimension(side.getWidth() - 4, 75));
            emptySpace.setAlignmentX(Component.CENTER_ALIGNMENT);
            emptySpace.setAlignmentY(Component.CENTER_ALIGNMENT);
            emptySpace.setFocusable(false);
            emptySpace.setEnabled(false);
            return emptySpace;
        }

        private void saveLog(String from, String to, String subject, String bodytext) {
            String mailQuery = "INSERT INTO mails (sentby, server, sender, receiver, subject, body, timestamp) VALUES (?, ?, ?, ?, ?, ?, ?)";
            String fileQuery = "INSERT INTO attachments (mail_id, filename, file_data) VALUES (?, ?, ?)";

            try (Connection conexion = DriverManager.getConnection(dbdir,
                    dbuser, dbpass);
                 PreparedStatement mailPs = conexion.prepareStatement(mailQuery, Statement.RETURN_GENERATED_KEYS)) {

                String server = connection.getInetAddress().toString().split("/")[0];
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                mailPs.setString(1, user);
                mailPs.setString(2, server);
                mailPs.setString(3, from);
                mailPs.setString(4, to);
                mailPs.setString(5, subject);
                mailPs.setString(6, bodytext);
                mailPs.setString(7, timestamp);
                mailPs.executeUpdate();

                ResultSet generatedKeys = mailPs.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int mailId = generatedKeys.getInt(1);

                    try (PreparedStatement filePs = conexion.prepareStatement(fileQuery)) {
                        for (File file : files) {
                            try (FileInputStream fis = new FileInputStream(file)) {
                                filePs.setInt(1, mailId);
                                filePs.setString(2, file.getName());
                                filePs.setBlob(3, fis);
                                filePs.executeUpdate();
                            }
                        }
                    }
                }

            } catch (SQLException | IOException e) {
                console.println("ERROR in data base: " + e.getMessage());
            }
        }

        private void sendMail(String from, String to, String subject, String bodytext) {
            try {
                bodytext = Normalizer.normalize(bodytext, Normalizer.Form.NFC).replace("\n", "\r\n");
                String encodedBody = Base64.getEncoder().encodeToString(bodytext.getBytes(StandardCharsets.UTF_8));
                String boundary = "----=_Part_" + new Random().nextInt(999999);

                out.print("HELO\r\n");
                out.flush();

                out.print("MAIL FROM:<" + from + ">\r\n");
                out.flush();

                if (to.contains(",")) {
                    String[] tos = to.split(",");
                    for (String s : tos) {
                        out.print("RCPT TO:<" + s.trim() + ">\r\n");
                        out.flush();
                    }
                } else {
                    out.print("RCPT TO:<" + to + ">\r\n");
                    out.flush();
                }

                out.print("DATA\r\n");
                out.flush();

                out.print("From: " + from + "\r\n");
                out.print("To: " + to + "\r\n");
                out.print("Subject: " + subject + "\r\n");
                out.print("MIME-Version: 1.0\r\n");
                out.print("Content-Type: multipart/mixed; boundary=\"" + boundary + "\"\r\n\r\n");

                out.print("--" + boundary + "\r\n");
                out.print("Content-Type: text/plain; charset=UTF-8\r\n");
                out.print("Content-Transfer-Encoding: base64\r\n\r\n");
                out.print(encodedBody + "\r\n");

                if (!files.isEmpty()) {
                    for (File file : files) {
                        if (file.exists()) {
                            String fileName = file.getName();
                            byte[] fileContent = Files.readAllBytes(file.toPath());
                            String encodedFile = Base64.getEncoder().encodeToString(fileContent);

                            out.print("--" + boundary + "\r\n");
                            out.print("Content-Type: application/octet-stream; name=\"" + fileName + "\"\r\n");
                            out.print("Content-Transfer-Encoding: base64\r\n");
                            out.print("Content-Disposition: attachment; filename=\"" + fileName + "\"\r\n\r\n");
                            out.print(encodedFile + "\r\n\r\n");
                        }
                    }
                }

                out.print("--" + boundary + "--\r\n");
                out.print(".\r\n");
                out.flush();

                if (!console.lastLine().startsWith("-> 5")) {
                    saveLog(from, to, subject, bodytext);
                } else {
                    console.println("ERROR: Something went wrong");
                }

                senderfield.setText("");
                receiverfield.setText("");
                subjectfield.setText("Write your subject here");
                subjectfield.setForeground(mainlight.brighter().brighter());
                bodyfield.setText("Write the text of your body here");
                bodyfield.setForeground(mainlight.brighter().brighter());

                files.clear();
                fileButtons();
            } catch (IOException e) {
                console.println("ERROR in monitoring: " + e.getMessage());
            }
        }

        private void addDragFunctionality(JLayeredPane top) {
            top.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    initialClick = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                public void mouseReleased(MouseEvent e) {
                    setCursor(Cursor.getDefaultCursor());
                }
            });

            top.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (!resizing) {
                        int thisX = getLocation().x;
                        int thisY = getLocation().y;
                        int xMoved = e.getX() - initialClick.x;
                        int yMoved = e.getY() - initialClick.y;
                        setLocation(thisX + xMoved, thisY + yMoved);
                    }
                }
            });
        }

        private void addResizeFunctionality() {
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    initialClick = e.getPoint();
                    resizing = true;
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    resizing = false;
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    int width = getWidth();
                    int height = getHeight();

                    boolean left = x < BORDER_THICKNESS;
                    boolean right = x > width - BORDER_THICKNESS;
                    boolean top = y < BORDER_THICKNESS;
                    boolean bottom = y > height - BORDER_THICKNESS;

                    if (left && top) cursor = Cursor.NW_RESIZE_CURSOR;
                    else if (right && top) cursor = Cursor.NE_RESIZE_CURSOR;
                    else if (left && bottom) cursor = Cursor.SW_RESIZE_CURSOR;
                    else if (right && bottom) cursor = Cursor.SE_RESIZE_CURSOR;
                    else if (left) cursor = Cursor.W_RESIZE_CURSOR;
                    else if (right) cursor = Cursor.E_RESIZE_CURSOR;
                    else if (top) cursor = Cursor.N_RESIZE_CURSOR;
                    else if (bottom) cursor = Cursor.S_RESIZE_CURSOR;
                    else cursor = Cursor.DEFAULT_CURSOR;

                    setCursor(Cursor.getPredefinedCursor(cursor));
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!resizing) return;

                    int x = e.getX();
                    int y = e.getY();
                    int width = getWidth();
                    int height = getHeight();
                    int newWidth = width;
                    int newHeight = height;
                    int newX = getX();
                    int newY = getY();

                    switch (cursor) {
                        case Cursor.E_RESIZE_CURSOR:
                            newWidth = x;
                            break;
                        case Cursor.W_RESIZE_CURSOR:
                            newWidth = width - (x - initialClick.x);
                            newX = getX() + (x - initialClick.x);
                            break;
                        case Cursor.S_RESIZE_CURSOR:
                            newHeight = y;
                            break;
                        case Cursor.N_RESIZE_CURSOR:
                            newHeight = height - (y - initialClick.y);
                            newY = getY() + (y - initialClick.y);
                            break;
                        case Cursor.NW_RESIZE_CURSOR:
                            newWidth = width - (x - initialClick.x);
                            newHeight = height - (y - initialClick.y);
                            newX = getX() + (x - initialClick.x);
                            newY = getY() + (y - initialClick.y);
                            break;
                        case Cursor.NE_RESIZE_CURSOR:
                            newWidth = x;
                            newHeight = height - (y - initialClick.y);
                            newY = getY() + (y - initialClick.y);
                            break;
                        case Cursor.SW_RESIZE_CURSOR:
                            newWidth = width - (x - initialClick.x);
                            newHeight = y;
                            newX = getX() + (x - initialClick.x);
                            break;
                        case Cursor.SE_RESIZE_CURSOR:
                            newWidth = x;
                            newHeight = y;
                            break;
                    }

                    setBounds(newX, newY, Math.max(300, newWidth), Math.max(200, newHeight));
                }
            });
        }

        private static class infoButton extends JButton {
            public int ID;
            public String mode, sender, receiver, subject, body, date, time;

            public infoButton(int ID, String mode, String sender, String receiver, String subject, String body, String date, String time) {
                super();
                this.ID = ID;
                this.mode = mode;
                this.sender = sender;
                this.receiver = receiver;
                this.subject = subject;
                this.body = body;
                this.date = date;
                this.time = time;
                addActionListener(e -> {
                    if (!showing) {
                        sendermem = senderfield.getText();
                        receivermem = receiverfield.getText();
                        subjectmem = subjectfield.getText();
                        bodymem = bodyfield.getText();
                        if (filesmem.isEmpty()) {
                            filesmem.addAll(files);
                        }
                    }
                    showing = true;
                    senderfield.setEnabled(false);
                    receiverfield.setEnabled(false);
                    subjectfield.setEnabled(false);
                    bodyfield.setEnabled(false);
                    senderfield.setText(sender);
                    receiverfield.setText(receiver);
                    subjectfield.setText(subject);
                    bodyfield.setText(body);
                    files.clear();
                    downloadAttachments(ID);
                    for (Component component : filesPanel.getComponents()) {
                        component.setEnabled(false);
                    }
                    clipButton.setEnabled(false);
                    frame.fileButtons();
                });
                addMouseListener(new HandCursorListener());
            }

            public infoButton() {
                super();
                this.mode = "";
                this.sender = "";
                this.receiver = "";
                this.subject = "";
                this.body = "";
                this.date = "";
                this.time = "";
            }

            private void downloadAttachments(int mailId) {
                String query = "SELECT filename, file_data FROM attachments WHERE mail_id = ?";

                if (!filecache.exists()) {
                    filecache.mkdirs();
                }

                try (Connection conexion = DriverManager.getConnection(dbdir,
                        dbuser, dbpass);
                     PreparedStatement ps = conexion.prepareStatement(query)) {

                    ps.setInt(1, mailId);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        String filename = rs.getString("filename");
                        Blob blob = rs.getBlob("file_data");

                        if (blob != null) {
                            File outputFile = new File(filecache, filename);
                            try (InputStream inputStream = blob.getBinaryStream();
                                 FileOutputStream outputStream = new FileOutputStream(outputFile)) {

                                byte[] buffer = new byte[4096];
                                int bytesRead;
                                while ((bytesRead = inputStream.read(buffer)) != -1) {
                                    outputStream.write(buffer, 0, bytesRead);
                                }
                                files.add(outputFile);
                            }
                        }
                    }
                } catch (SQLException | IOException ex) {
                    console.println("ERROR retrieving files: " + ex.getMessage());
                }
            }
        }

        public class console extends JTextArea {
            private int lastPromptIndex = 0;
            private final ArrayList<String> commands = new ArrayList<>();

            public console() {
                super();
                commands.add("");
                setEditable(true);
                setBackground(mainlight);
                setForeground(Color.WHITE);
                setCaretColor(Color.WHITE);
                setMargin(new Insets(25, 25, 25, 25));

                addKeyListener(new KeyAdapter() {
                    public void keyPressed(KeyEvent e) {
                        int caretPos = getCaretPosition();
                        if (caretPos < lastPromptIndex) {
                            setCaretPosition(getDocument().getLength());
                        }

                        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                            println("");
                            e.consume();
                            String[] lines = getText().split("\n");
                            if (lines.length > 0) {
                                command(lines[lines.length - 1]);
                            }
                        }

                        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE && caretPos <= lastPromptIndex) {
                            e.consume();
                        }
                    }
                });

                addCaretListener(e -> manageCaret());
            }

            public String lastLine() {
                String[] lines = console.getText().split("\n");
                return lines[lines.length - 1];
            }

            public void print(String text) {
                append(text);
                updatePromptIndex();
                consoleScrollPane.getVerticalScrollBar().setValue(consoleScrollPane.getVerticalScrollBar().getMaximum());
                logfilewriter.print(text);
            }

            public void println(String text) {
                print(text + "\n");
            }

            private void manageCaret() {
                if (getCaretPosition() < lastPromptIndex) {
                    setCaretPosition(getDocument().getLength());
                }
            }

            private void updatePromptIndex() {
                lastPromptIndex = getDocument().getLength();
                setCaretPosition(lastPromptIndex);
            }

            public void command(String command) {
                command = command.trim();
                commands.add(command);
                if (command.startsWith("connect")) {
                    if (!connected) {
                        if (command.split(" ").length > 1) {
                            String ip = command.split(" ")[1];
                            int port = command.split(" ").length > 2 ? Integer.parseInt(command.split(" ")[2]) : 25;
                            setEditable(false);
                            new Thread(() -> {
                                try {
                                    connection = new Socket(ip, port);
                                    in = new Scanner(connection.getInputStream());
                                    out = new PrintWriter(connection.getOutputStream(), false);
                                    connected = true;
                                    new Thread(() -> {
                                        try {
                                            while (connection.isConnected()) {
                                                if (toPrint(in)) {
                                                    println("-> " + printLine(in));
                                                    Thread.sleep(10);
                                                }
                                            }
                                        } catch (NoSuchElementException e) {
                                            println("Closing connection from " + ip + ":" + port);
                                            connected = false;
                                        } catch (InterruptedException e) {
                                            println("ERROR (Interrupted): " + e.getMessage());
                                            connected = false;
                                        }
                                    }).start();
                                    String fakeEHLO = "EHLO [" + (new Random().nextInt(255) + 1) + "." + new Random().nextInt(256) + "." + new Random().nextInt(256) + "." + new Random().nextInt(256) + "]";
                                    out.print(fakeEHLO);
                                    out.flush();
                                } catch (IOException e) {
                                    println("ERROR: Cannot connect to: " + ip + ":" + port + " (" + e.getMessage() + ")");
                                }
                                setEditable(true);
                                consoleScrollPane.getVerticalScrollBar().setValue(consoleScrollPane.getVerticalScrollBar().getMaximum());
                            }).start();
                        } else {
                            println("ERROR: You must enter the command following this pattern: connect smtp.gmail.com 25");
                        }
                    } else {
                        println("ERROR: You're already connected to: " + connection.getInetAddress().toString() + ":" + connection.getPort());
                    }
                } else if (command.startsWith("query")) {
                    if (command.split(" ").length > 1) {
                        out.write(command.split("query ")[1] + "\r\n");
                        out.flush();
                    } else {
                        println("ERROR: You should enter a query");
                    }
                } else if (command.equals("disconnect")) {
                    if (connected) {
                        try {
                            connection.close();
                            println("Closing connection from " + connection.getInetAddress().toString() + ":" + connection.getPort());
                        } catch (IOException e) {
                            println("ERROR: Error during disconnect: " + e.getMessage());
                        }
                        connected = false;
                    } else {
                        println("ERROR: There is no connection to close");
                    }
                } else {
                    if (command.isEmpty() || command.isBlank()) {
                        println("ERROR: You should try entering a command");
                    } else {
                        println("ERROR: Command not recognized: " + command);
                    }
                }
                consoleScrollPane.getVerticalScrollBar().setValue(consoleScrollPane.getVerticalScrollBar().getMaximum());
            }

            private synchronized boolean toPrint(Scanner scanner) {
                return scanner.hasNextLine();
            }

            private synchronized String printLine(Scanner scanner) {
                return scanner.nextLine();
            }
        }

        private static class RoundedButton extends JButton {
            private Color borderColor = Color.BLACK;
            private int borderThickness = 2;
            private int cornerRadius = 30;

            public RoundedButton(String text) {
                super(text);
                setContentAreaFilled(false);
                setFocusPainted(false);
                setBorderPainted(false);
            }

            public void setBorderColor(Color color) {
                this.borderColor = color;
                repaint();
            }

            public void setBorderThickness(int thickness) {
                this.borderThickness = thickness;
                repaint();
            }

            public void setCornerRadius(int radius) {
                this.cornerRadius = radius;
                repaint();
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                g2.setColor(getModel().isPressed() ? Color.LIGHT_GRAY : getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderThickness));
                g2.drawRoundRect(borderThickness / 2, borderThickness / 2,
                        getWidth() - borderThickness, getHeight() - borderThickness,
                        cornerRadius, cornerRadius);

                g2.setColor(getForeground());
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2.drawString(getText(), x, y);

                g2.dispose();
                super.paintComponent(g);
            }
        }

        private static class HandCursorListener extends MouseAdapter {
            @Override
            public void mouseEntered(MouseEvent e) {
                e.getComponent().setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                e.getComponent().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        }
    }
}