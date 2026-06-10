package com.heater.analysis;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Concept diagrams: single Pickaso-style cell and Colossus-scale fan orchestra. */
public final class FanOrchestraDiagramGenerator {

    private static final int W = 1200;
    private static final int H = 520;

    private FanOrchestraDiagramGenerator() {}

    public static String writeCellDiagram(Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ChartStyle.BG);
        g.fillRect(0, 0, W, H);

        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("One fan-orchestra cell (Pickaso Rotary Bow → GPU cooling fan)", 40, 42);

        drawCell(g, 120, 120, "Fan shaft", ChartStyle.SLATE);
        drawArrow(g, 250, 200, 320, 200);
        drawBowWheel(g, 380, 155, "Rotary bow wheel");
        drawArrow(g, 500, 200, 560, 200);
        drawStringResonator(g, 620, 130);
        drawArrow(g, 780, 200, 840, 200);
        drawSpeaker(g, 880, 150, "Fence-line SPL");

        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.setColor(ChartStyle.SLATE);
        g.drawString("BPF amplitude modulation (~490 Hz) pulses bow pressure — hall-wide tremolo rhythm", 40, 460);
        g.drawString("Interchangeable covers: Tremobow (butterfly) · Vase · Curved — timbre varies by rack row", 40, 485);

        g.dispose();
        Path path = outputDir.resolve("fan_orchestra_cell.png");
        ImageIO.write(img, "png", path.toFile());
        return "docs/figures/fan_orchestra_cell.png";
    }

    public static String writeScaleDiagram(Path outputDir, int racks, int fansPerRack, int gpuCount) throws IOException {
        Files.createDirectories(outputDir);
        int instruments = racks * fansPerRack;
        BufferedImage img = new BufferedImage(W, H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(ChartStyle.BG);
        g.fillRect(0, 0, W, H);

        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        g.drawString("Colossus-class hall: one mechanical instrument per cooling fan", 40, 42);

        int barTop = 100;
        int barH = 48;
        int maxBarW = 900;
        drawScaleBar(g, 80, barTop, maxBarW, barH, 1, "1 Pickaso cell (lab)", ChartStyle.INDIGO);
        drawScaleBar(g, 80, barTop + 90, maxBarW, barH, instruments / 15000.0,
                String.format("%,d fan-orchestra cells (this sim)", instruments), ChartStyle.TEAL);
        drawScaleBar(g, 80, barTop + 180, maxBarW, barH, gpuCount / 25000.0,
                String.format("%,d B200 GPUs (reference hall heat load)", gpuCount), ChartStyle.AMBER);

        g.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g.setColor(ChartStyle.TEXT);
        g.drawString(String.format("%d racks × %d fans/rack = %,d sustained bow voices", racks, fansPerRack, instruments),
                80, 360);
        g.drawString("Blade-passing clock: 4200 RPM × 7 blades ≈ 490 Hz tremolo across the hall", 80, 390);
        g.drawString("Aggregate physics: statistical 128-voice waveform + incoherent 1/3-octave SPL sum", 80, 420);

        g.dispose();
        Path path = outputDir.resolve("fan_orchestra_at_scale.png");
        ImageIO.write(img, "png", path.toFile());
        return "docs/figures/fan_orchestra_at_scale.png";
    }

    private static void drawScaleBar(Graphics2D g, int x, int y, int maxW, int h, double fraction, String label, Color color) {
        int w = Math.max(24, (int) (maxW * Math.min(1.0, fraction)));
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, w, h, 12, 12));
        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString(label, x + w + 16, y + h / 2 + 5);
    }

    private static void drawCell(Graphics2D g, int x, int y, String label, Color color) {
        g.setColor(color);
        g.fill(new RoundRectangle2D.Double(x, y, 100, 100, 16, 16));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        g.drawString("CRAC", x + 28, y + 45);
        g.drawString("fan", x + 36, y + 62);
        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(label, x + 10, y + 125);
    }

    private static void drawBowWheel(Graphics2D g, int x, int y, String label) {
        g.setColor(ChartStyle.TEAL);
        g.fill(new Ellipse2D.Double(x, y, 90, 90));
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 11));
        g.drawString("Bow", x + 30, y + 42);
        g.drawString("wheel", x + 26, y + 58);
        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(label, x - 4, y + 115);
    }

    private static void drawStringResonator(Graphics2D g, int x, int y) {
        g.setColor(ChartStyle.AMBER);
        g.setStroke(new BasicStroke(3f));
        for (int i = 0; i < 4; i++) {
            g.draw(new Line2D.Double(x, y + i * 18, x + 120, y + i * 18));
        }
        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString("Bowed string / resonator", x, y + 95);
    }

    private static void drawSpeaker(Graphics2D g, int x, int y, String label) {
        g.setColor(ChartStyle.INDIGO);
        g.fill(new RoundRectangle2D.Double(x, y, 80, 80, 8, 8));
        g.setColor(Color.WHITE);
        g.fillPolygon(new int[]{x + 80, x + 110, x + 80}, new int[]{y + 20, y + 40, y + 60}, 3);
        g.setColor(ChartStyle.TEXT);
        g.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g.drawString(label, x - 10, y + 105);
    }

    private static void drawArrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.setColor(ChartStyle.SLATE);
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
        g.fillPolygon(new int[]{x2, x2 - 10, x2 - 10}, new int[]{y1, y1 - 6, y1 + 6}, 3);
    }
}
