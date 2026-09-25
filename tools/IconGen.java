import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.TextLayout;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders the TickCount launcher foreground: black text on transparent.
 *
 * Vector XML cannot draw CJK glyphs, so the label is rasterised here with the
 * JDK's text engine and checked in as a PNG.
 *
 * Two deliberate details:
 *  - The text is written as unicode escapes (backslash-u plus four hex digits).
 *    JDK 17's single-file source launcher reads the file with the *platform*
 *    charset (GBK on this machine), so a literal would arrive mangled.
 *  - The label is sized to fit a 63dp box inside the 108dp adaptive-icon
 *    canvas, which is what keeps the first and last glyph inside every mask
 *    shape, including a full circle.
 */
public class IconGen {

    private static final String TEXT = "\u5012\u6570\u65e5"; // 倒数日
    private static final int SIZE = 432;                      // 108dp at xxxhdpi
    private static final double SAFE_WIDTH_RATIO = 63.0 / 108.0;

    private static final String[] BOLD_FONT_FILES = {
            "Fonts/msyhbd.ttc",  // Microsoft YaHei Bold
            "Fonts/simhei.ttf",  // SimHei
            "Fonts/Dengb.ttf",   // DengXian Bold
            "Fonts/msyh.ttc",
            "Fonts/simsun.ttc",
    };

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File outFile = new File(args[0]);
        outFile.getParentFile().mkdirs();
        File previewFile = args.length > 1 ? new File(args[1]) : null;

        BufferedImage icon = renderIcon();
        ImageIO.write(icon, "png", outFile);
        System.out.println("wrote " + outFile.getAbsolutePath() + " (" + outFile.length() + " bytes)");

        if (previewFile != null) {
            previewFile.getParentFile().mkdirs();
            ImageIO.write(renderPreview(icon), "png", previewFile);
            System.out.println("wrote " + previewFile.getAbsolutePath());
        }
    }

    private static BufferedImage renderIcon() throws Exception {
        BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        applyQuality(g);
        g.setColor(new Color(0, 0, 0, 0));
        g.fillRect(0, 0, SIZE, SIZE);
        g.setColor(Color.BLACK);

        Font base = loadFont(g);
        double targetWidth = SIZE * SAFE_WIDTH_RATIO;

        // Measure at a reference size, then scale so the ink box exactly fills
        // the safe width. Centring uses the ink box, not the font metrics line
        // box, because CJK line boxes carry a lot of leading.
        TextLayout reference = new TextLayout(TEXT, base.deriveFont(100f), g.getFontRenderContext());
        Font sized = base.deriveFont((float) (100.0 * targetWidth / reference.getBounds().getWidth()));

        TextLayout layout = new TextLayout(TEXT, sized, g.getFontRenderContext());
        Rectangle2D ink = layout.getBounds();
        layout.draw(
                g,
                (float) ((SIZE - ink.getWidth()) / 2.0 - ink.getX()),
                (float) ((SIZE - ink.getHeight()) / 2.0 - ink.getY())
        );
        g.dispose();

        System.out.println("font=" + base.getFontName() + " size=" + Math.round(sized.getSize2D()));
        System.out.println("ink=" + Math.round(ink.getWidth()) + "x" + Math.round(ink.getHeight())
                + "px of " + SIZE + " (safe width " + Math.round(targetWidth) + ")");
        return image;
    }

    /** Loads the first font file that can actually draw the label. */
    private static Font loadFont(Graphics2D g) {
        String windir = System.getenv("windir");
        List<File> candidates = new ArrayList<>();
        for (String relative : BOLD_FONT_FILES) {
            candidates.add(new File(windir, relative));
        }

        for (File file : candidates) {
            if (!file.isFile()) continue;
            try {
                Font font = Font.createFont(Font.TRUETYPE_FONT, file);
                if (font.canDisplayUpTo(TEXT) == -1) {
                    System.out.println("using font file " + file.getPath());
                    return font;
                }
                System.out.println("no CJK coverage: " + file.getPath());
            } catch (Exception e) {
                System.out.println("could not load " + file.getPath() + ": " + e);
            }
        }

        for (String family : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            Font font = new Font(family, Font.BOLD, 100);
            if (font.canDisplayUpTo(TEXT) == -1) {
                System.out.println("using installed family " + family);
                return font;
            }
        }

        throw new IllegalStateException("No CJK-capable font found; cannot render the icon.");
    }

    private static void applyQuality(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    /**
     * Builds a contact sheet showing the icon under the two mask shapes Android
     * actually uses, plus an unmasked copy, at a realistic launcher size.
     */
    private static BufferedImage renderPreview(BufferedImage icon) {
        int cell = 200;
        int label = 26;
        int gap = 24;
        int width = gap + 3 * (cell + gap);
        int height = gap + cell + label + gap;

        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        applyQuality(g);
        g.setColor(new Color(0xECECEC));
        g.fillRect(0, 0, width, height);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 15));

        String[] captions = {"squircle mask", "circle mask", "unmasked"};
        for (int i = 0; i < 3; i++) {
            int x = gap + i * (cell + gap);
            int y = gap;

            BufferedImage masked = new BufferedImage(cell, cell, BufferedImage.TYPE_INT_RGB);
            Graphics2D mg = masked.createGraphics();
            applyQuality(mg);
            mg.setColor(Color.WHITE);
            mg.fillRect(0, 0, cell, cell);

            Shape clip = switch (i) {
                case 0 -> new RoundRectangle2D.Float(0, 0, cell, cell, cell * 0.42f, cell * 0.42f);
                case 1 -> new Ellipse2D.Float(0, 0, cell, cell);
                default -> new Rectangle2D.Float(0, 0, cell, cell);
            };
            mg.setClip(clip);
            mg.drawImage(icon, 0, 0, cell, cell, null);
            mg.setClip(null);
            mg.setColor(new Color(0x33000000, true));
            mg.setStroke(new BasicStroke(1f));
            mg.draw(clip);
            mg.dispose();

            g.drawImage(masked, x, y, null);
            g.setColor(new Color(0x333333));
            g.drawString(captions[i], x, y + cell + 18);
        }
        g.dispose();
        return out;
    }
}
