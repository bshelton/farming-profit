package com.farmingprofit;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;

/** Shared native controls; hover and focus repaint only on input events. */
final class SidebarStyle
{
    static final Color GOLD = new Color(242, 190, 101);
    static final Color GREEN = new Color(169, 219, 131);
    static final Color MUTED = new Color(185, 185, 185);
    static final Color LINE = new Color(65, 65, 62);
    static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    static JPanel column()
    {
        JPanel panel = new JPanel()
        {
            @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR); panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        return panel;
    }
    static <T extends JComponent> T fit(T component)
    {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }
    static JLabel label(String value)
    { JLabel label = new JLabel(value); label.setFont(BODY); return label; }
    static JLabel copy(String value)
    {
        JLabel label = label("<html><div style='width:145px'>" + value + "</div></html>");
        label.setForeground(MUTED); label.setBorder(new EmptyBorder(0, 0, 6, 0)); return fit(label);
    }
    static JButton button(String label)
    {
        JButton button = new JButton(label)
        {
            @Override protected void paintComponent(Graphics graphics)
            {
                super.paintComponent(graphics);
                if (isFocusOwner()) { graphics.setColor(GOLD); graphics.drawRect(1, 1, getWidth() - 3, getHeight() - 3); }
            }
        };
        button.setFont(BODY); button.setFocusPainted(false); button.setOpaque(true); button.setContentAreaFilled(true);
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(e -> refresh(button));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), new EmptyBorder(7, 7, 7, 7)));
        refresh(button); return button;
    }
    static void selected(JButton button, boolean selected)
    {
        button.putClientProperty("selectedChoice", selected);
        button.setForeground(selected ? GOLD : Color.WHITE);
        button.setFont(BODY.deriveFont(selected ? Font.BOLD : Font.PLAIN, 11f));
        button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(selected ? new Color(130, 105, 55) : LINE), new EmptyBorder(7, 3, 7, 3)));
        refresh(button);
    }
    private static void refresh(JButton button)
    {
        boolean selected = Boolean.TRUE.equals(button.getClientProperty("selectedChoice"));
        button.setBackground(button.isEnabled() && button.getModel().isRollover() ? new Color(55, 52, 44)
            : selected ? new Color(57, 50, 35) : new Color(32, 32, 32));
    }
    static void divider(JPanel parent)
    {
        JSeparator line = new JSeparator(); line.setForeground(LINE); line.setBackground(LINE);
        parent.add(Box.createVerticalStrut(8)); parent.add(fit(line)); parent.add(Box.createVerticalStrut(8));
    }
    private SidebarStyle() { }
}
