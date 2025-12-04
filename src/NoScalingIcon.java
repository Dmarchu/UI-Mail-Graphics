import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.io.IOException;

public class NoScalingIcon implements Icon {
    private final Icon icon;

    public NoScalingIcon(Icon icon)
    {
        this.icon = icon;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2d = (Graphics2D)g.create();

        AffineTransform at = g2d.getTransform();

        int scaleX = (int)(x * at.getScaleX());
        int scaleY = (int)(y * at.getScaleY());

        int offsetX = (int)(icon.getIconWidth() * ((at.getScaleX() - 1) / 2));
        int offsetY = (int)(icon.getIconHeight() * ((at.getScaleY() - 1) / 2));

        int locationX = scaleX + offsetX;
        int locationY = scaleY + offsetY;

        AffineTransform scaled = AffineTransform.getScaleInstance(1.0 / at.getScaleX(), 1.0 / at.getScaleY());
        at.concatenate( scaled );
        g2d.setTransform( at );

        icon.paintIcon(c, g2d, locationX, locationY);

        g2d.dispose();
    }

    public int getIconWidth()
    {
        return icon.getIconWidth();
    }

    public int getIconHeight()
    {
        return icon.getIconHeight();
    }

    public static Image scaledImage(String name, int width, int height) {
        try {
            return ImageIO.read(UIMain.class.getResource("/images/" + name)).getScaledInstance(width,height,Image.SCALE_SMOOTH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static NoScalingIcon scaledIcon(String name, int width, int height) {
        return new NoScalingIcon(new ImageIcon(scaledImage(name, width, height)));
    }

    public static Image scaledComponent(String name, int width, int height) {
        try {
            return ImageIO.read(UIMain.class.getResource("/images/" + name)).getScaledInstance(width,height,Image.SCALE_SMOOTH);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static NoScalingIcon scaledComponentIcon(String name, int width, int height) {
        return new NoScalingIcon(new ImageIcon(scaledComponent(name, width, height)));
    }
}
