import javax.swing.*;
import java.awt.*;

public class MyFrame extends JFrame {
    private static int mouseX, mouseY;
    private JLayeredPane mypanel;
    private JPanel background, topPanel;
    private JButton close, minimize;
    private JLabel titlelabel;
    private final Color maindark = new Color(33,33,33);

    public MyFrame(String title, Dimension size) {
        setUndecorated(true); setResizable(false); setLayout(new FlowLayout(FlowLayout.LEADING));
        setSize(size); setVisible(true); setLocationRelativeTo(null);
        setIconImage(NoScalingIcon.scaledImage("logo.png", 200,200));

        mypanel = new JLayeredPane();
        mypanel.setLayout(null); mypanel.setBounds(0, 0, getWidth(), getHeight()); mypanel.setBackground(maindark);

        background = new JPanel();
        background.setLayout(null); background.setBounds(0, 0, getWidth(), getHeight());
        background.setBackground(maindark); background.setBorder(BorderFactory.createLineBorder(Color.darkGray));

        topPanel = new JPanel();
        topPanel.setBounds(0, 0, getWidth(), 30); topPanel.setBackground(maindark);
        topPanel.setLayout(null); topPanel.setBorder(BorderFactory.createLineBorder(Color.darkGray));
        topPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
                mouseX = evt.getX();
                mouseY = evt.getY();
            }

            public void mouseReleased(java.awt.event.MouseEvent evt) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        });

        topPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                setCursor(new Cursor(Cursor.MOVE_CURSOR));
                int x = getX() + evt.getX() - mouseX;
                int y = getY() + evt.getY() - mouseY;
                setLocation(x, y);
            }
        });

        close = new JButton();
        close.setBounds(getWidth() - 30, 0, 30, 30); close.setBackground(maindark); close.setLayout(null);
        close.setIcon(NoScalingIcon.scaledIcon("cross.png", 20, 20)); close.setFocusable(false);
        close.setBorder(BorderFactory.createLineBorder(Color.darkGray)); close.addActionListener(e -> dispose());

        minimize = new JButton();
        minimize.setBounds(getWidth() - 60, 0, 30, 30); minimize.setBackground(maindark); minimize.setLayout(null);
        minimize.setIcon(NoScalingIcon.scaledIcon("minB.png", 20, 20)); minimize.setFocusable(false);
        minimize.setBorder(BorderFactory.createLineBorder(Color.darkGray)); minimize.addActionListener(e -> setState(Frame.ICONIFIED));

        titlelabel = new JLabel();
        titlelabel.setBounds(5, 0, 200, 30); titlelabel.setBackground(maindark); titlelabel.setLayout(null);
        titlelabel.setIcon(NoScalingIcon.scaledIcon("logo.png", 30, 30)); titlelabel.setForeground(Color.white);
        titlelabel.setText(title); titlelabel.setHorizontalTextPosition(SwingConstants.RIGHT);

        add(mypanel);

        mypanel.add(background);
        mypanel.setLayer(background, 0);
        mypanel.add(topPanel);
        mypanel.setLayer(topPanel, 200);

        topPanel.add(close);
        topPanel.add(minimize);
        topPanel.add(titlelabel);
    }

    public void panelAdd(Component component, int layer) {
        mypanel.add(component);
        mypanel.setLayer(component, layer);
    }
}
