package com.cloudread.storage;

import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * 使用书名与作者自动生成默认文字排版封面。
 */
@Component
public class CoverGenerator {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 800;

    public byte[] generate(String title, String author) {
        try {
            BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int seed = (title == null ? "unknown" : title).hashCode();
            Color c1 = hsla((seed & 0x7FFFFFFF) % 360, 0.55f, 0.32f);
            Color c2 = hsla(((seed & 0x7FFFFFFF) / 3 + 40) % 360, 0.60f, 0.20f);
            g.setPaint(new GradientPaint(0, 0, c1, WIDTH, HEIGHT, c2));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            g.setColor(new Color(255, 255, 255, 70));
            g.setStroke(new java.awt.BasicStroke(3f));
            g.draw(new RoundRectangle2D.Double(28, 28, WIDTH - 56, HEIGHT - 56, 18, 18));

            g.setFont(pickFont(Font.BOLD, 52));
            g.setColor(Color.WHITE);
            List<String> lines = wrapText(g, title == null || title.isBlank() ? "未命名书籍" : title, WIDTH - 120);
            int lineHeight = 64;
            int startY = HEIGHT / 2 - ((lines.size() - 1) * lineHeight) / 2;
            for (int i = 0; i < lines.size(); i++) {
                drawCentered(g, lines.get(i), startY + i * lineHeight);
            }

            if (author != null && !author.isBlank()) {
                g.setFont(pickFont(Font.PLAIN, 28));
                g.setColor(new Color(255, 255, 255, 220));
                drawCentered(g, "—— " + author + " ——", HEIGHT - 170);
            }

            g.setFont(pickFont(Font.PLAIN, 22));
            g.setColor(new Color(255, 255, 255, 150));
            drawCentered(g, "云阅 CloudRead", HEIGHT - 78);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("生成默认封面失败", e);
        }
    }

    private Font pickFont(int style, int size) {
        try {
            Font f = new Font("Microsoft YaHei", style, size);
            if (!"Dialog".equals(f.getFamily())) {
                return f;
            }
        } catch (Exception ignored) {
        }
        return new Font(Font.SANS_SERIF, style, size);
    }

    private List<String> wrapText(Graphics2D g, String text, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        String[] raw = text.split("\n");
        for (String line : raw) {
            if (fm.stringWidth(line) <= maxWidth) {
                lines.add(line);
                continue;
            }
            StringBuilder current = new StringBuilder();
            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);
                if (fm.stringWidth(current.toString() + c) > maxWidth && current.length() > 0) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                current.append(c);
            }
            if (current.length() > 0) {
                lines.add(current.toString());
            }
        }
        return lines.size() > 6 ? lines.subList(0, 6) : lines;
    }

    private void drawCentered(Graphics2D g, String text, int y) {
        FontMetrics fm = g.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        g.drawString(text, x, y);
    }

    private Color hsla(int hue, float saturation, float lightness) {
        return Color.getHSBColor(hue / 360f, saturation, lightness);
    }
}
