package com.farmingprofit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import net.runelite.api.gameval.ItemID;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/** Swing implementation sized for RuneLite's actual narrow sidebar. */
final class FarmingProfitPanel extends PluginPanel
{
    private static final Color GOLD = new Color(242, 190, 101);
    private static final Color GREEN = new Color(169, 219, 131);
    private static final Color MUTED = new Color(185, 185, 185);
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private final FarmingProfitPlugin plugin;
    private final JPanel current = column();
    private final JPanel history = column();
    private final JPanel actions = column();
    private final JLabel status = text("");
    private final JButton start = button("Start run");
    private final JButton finish = button("Finish run");
    private final JButton pause = button("Pause run");
    private final JButton delete = button("Delete run");
    private final JButton adjust = button("Add / correct entry");
    private final NumberFormat numbers = NumberFormat.getIntegerInstance();
    private final JTabbedPane tabs = new JTabbedPane()
    {
        @Override public Dimension getMaximumSize()
        { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    };
    private boolean ready;
    private boolean active;
    private boolean paused;
    private String activeId;

    FarmingProfitPanel(FarmingProfitPlugin plugin)
    {
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 10, 12, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel content = column();
        JPanel heading = new JPanel(new BorderLayout(4, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("Farming Profit");
        title.setIcon(new ImageIcon(BrandAssets.MARK)); title.setIconTextGap(6);
        title.setFont(BODY.deriveFont(Font.BOLD, 14)); title.setForeground(GOLD);
        heading.add(title, BorderLayout.CENTER);
        JButton settings = button("Settings");
        settings.setFont(BODY.deriveFont(10f));
        settings.setBorder(new EmptyBorder(6, 5, 6, 5));
        settings.addActionListener(e -> settings());
        heading.add(settings, BorderLayout.EAST);
        content.add(fit(heading)); content.add(Box.createVerticalStrut(12));
        tabs.setFont(BODY); tabs.setAlignmentX(LEFT_ALIGNMENT);
        current.setBorder(new EmptyBorder(12, 0, 4, 0));
        history.setBorder(new EmptyBorder(12, 0, 4, 0));
        tabs.addTab("Current run", current); tabs.addTab("History", history);
        content.add(tabs); content.add(Box.createVerticalStrut(12));
        content.add(actions); content.add(Box.createVerticalStrut(10));
        status.setForeground(MUTED); content.add(status);
        JButton help = button("How profit is calculated");
        help.setFont(BODY.deriveFont(11f));
        help.addActionListener(e -> showHelp()); content.add(fit(help));
        add(content, BorderLayout.NORTH);
        start.addActionListener(e -> plugin.startManual());
        finish.addActionListener(e -> plugin.finishManual());
        pause.addActionListener(e -> plugin.pauseManual(activeId, !paused));
        delete.setForeground(new Color(244, 151, 145));
        delete.addActionListener(e -> deleteCurrentRun());
        adjust.addActionListener(e -> correction());
        tabs.addChangeListener(e -> updateActions());
        render(new FarmingProfitPlugin.Data(), "Log in to start tracking.", false);
    }

    private static JPanel column()
    {
        JPanel panel = new JPanel()
        {
            @Override public Dimension getMaximumSize()
            { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
        };
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR); panel.setAlignmentX(LEFT_ALIGNMENT); return panel;
    }
    private static <T extends JComponent> T fit(T component)
    {
        component.setAlignmentX(LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, component.getPreferredSize().height));
        return component;
    }
    private static JButton button(String label)
    {
        JButton button = new JButton(label); button.setFont(BODY);
        button.setBorder(new EmptyBorder(8, 8, 8, 8)); return button;
    }
    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static JLabel text(String value)
    {
        JLabel label = new JLabel(); label.setFont(BODY);
        setText(label, value); return label;
    }
    private static void setText(JLabel label, String value)
    {
        label.setText("<html><div style='width:145px'>" + escape(value) + "</div></html>");
        label.setBorder(new EmptyBorder(0, 0, 6, 0)); fit(label);
    }
    private void row(JPanel parent, String label, long value)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0)); row.setOpaque(false);
        JLabel name = new JLabel(label); name.setFont(BODY); name.setForeground(MUTED);
        JLabel amount = new JLabel(numbers.format(value) + " gp"); amount.setFont(BODY);
        row.add(name, BorderLayout.WEST); row.add(amount, BorderLayout.EAST);
        row.setBorder(new EmptyBorder(4, 0, 4, 0)); parent.add(fit(row));
    }
    private void updateActions()
    {
        actions.removeAll();
        if (ready && tabs.getSelectedIndex() == 0)
        {
            if (active)
            {
                pause.setText(paused ? "Resume run" : "Pause run");
                JPanel controls = new JPanel(new GridLayout(1, 2, 6, 0)); controls.setOpaque(false);
                controls.add(pause); controls.add(finish); actions.add(fit(controls));
                actions.add(Box.createVerticalStrut(6)); adjust.setEnabled(ready); actions.add(fit(adjust));
                actions.add(Box.createVerticalStrut(10)); actions.add(fit(delete));
            }
            else { actions.add(fit(start)); }
        }
        actions.revalidate(); actions.repaint();
    }

    void render(FarmingProfitPlugin.Data data, String message, boolean ready)
    {
        current.removeAll(); history.removeAll();
        this.ready = ready; active = data.active != null;
        activeId = active ? data.active.id : null; paused = active && data.active.paused;
        String concise = message.startsWith("Saved locally") ? "Saved locally | GE estimates" : message;
        setText(status, concise); status.setToolTipText(message);
        status.setVisible(ready);
        if (data.active == null)
        {
            JLabel illustration = new JLabel(BrandAssets.HARVEST, SwingConstants.CENTER);
            illustration.getAccessibleContext().setAccessibleName("Herbs and gold coins in a harvest crate");
            current.add(fit(illustration)); current.add(Box.createVerticalStrut(12));
            JLabel title = text(ready ? "Ready for your next run" : "Waiting for your account");
            title.setFont(BODY.deriveFont(Font.BOLD, 13)); current.add(fit(title));
            JLabel hint = text(ready ? "Harvest or plant a patch to begin automatically." : message);
            hint.setForeground(MUTED); current.add(hint);
            if (ready)
            {
                current.add(Box.createVerticalStrut(6));
                JLabel manual = text("Or start a run manually below."); manual.setForeground(MUTED); current.add(manual);
            }
        }
        else { summary(current, data.active); }
        if (data.history.isEmpty())
        {
            JLabel title = text(ready ? "No completed runs yet" : "Waiting for your account"); title.setFont(BODY.deriveFont(Font.BOLD, 13)); history.add(fit(title));
            JLabel hint = text(ready ? "Finish a run to see its harvest, costs and profit here." : message); hint.setForeground(MUTED); history.add(hint);
        }
        else
        {
            row(history, "Total profit", data.history.stream().mapToLong(FarmRun::profit).sum());
            history.add(Box.createVerticalStrut(8));
            for (FarmRun run : data.history)
            {
                JButton button = button("<html>" + DATE.format(Instant.ofEpochMilli(run.startedAt)) + "<br>" + numbers.format(run.profit()) + " gp</html>");
                button.setHorizontalAlignment(SwingConstants.LEFT);
                button.addActionListener(e -> showLedger(run)); history.add(fit(button)); history.add(Box.createVerticalStrut(6));
            }
        }
        updateActions();
        current.invalidate(); history.invalidate(); actions.invalidate();
        for (java.awt.Container parent = tabs; parent != null; parent = parent.getParent())
        { parent.invalidate(); }
        revalidate(); repaint();
    }

    private void summary(JPanel parent, FarmRun run)
    {
        if (run.paused)
        {
            JLabel notice = text("Paused — automatic tracking is off.");
            notice.setForeground(GOLD); parent.add(notice);
        }
        JLabel since = text("Started " + DATE.format(Instant.ofEpochMilli(run.startedAt))); since.setForeground(MUTED); parent.add(since);
        parent.add(text("Estimated profit"));
        JLabel total = new JLabel((run.profit() > 0 ? "+" : "") + numbers.format(run.profit()) + " gp");
        total.setForeground(run.profit() >= 0 ? GREEN : ColorScheme.PROGRESS_ERROR_COLOR);
        total.setFont(BODY.deriveFont(Font.BOLD, 24)); parent.add(fit(total)); parent.add(Box.createVerticalStrut(10));
        row(parent, "Harvest", run.harvestValue()); row(parent, "Supplies", run.costs());
        JLabel xp = text(numbers.format(run.farmingXp) + " Farming XP"); xp.setForeground(MUTED); parent.add(xp);
        parent.add(Box.createVerticalStrut(8));
        Map<String, Integer> harvest = new LinkedHashMap<>();
        run.entries.stream().filter(e -> !e.cost).forEach(e -> harvest.merge(e.name, e.quantity, Integer::sum));
        harvest.forEach((name, quantity) -> parent.add(text(quantity + " x " + name)));
        JButton detail = button("View breakdown"); detail.addActionListener(e -> showLedger(run)); parent.add(fit(detail));
        if (!run.warnings.isEmpty())
        {
            parent.add(Box.createVerticalStrut(8));
            JButton review = button(run.warnings.size() + " item(s) to review"); review.setForeground(GOLD);
            review.addActionListener(e -> showLedger(run)); parent.add(fit(review));
        }
    }

    private void deleteCurrentRun()
    {
        String expectedId = activeId;
        if (expectedId == null) { return; }
        Object[] choices = {"Keep run", "Delete run"};
        int choice = JOptionPane.showOptionDialog(this,
            "Delete this run and all its entries?\nIt will not be saved to History. This cannot be undone.",
            "Delete current run", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
            null, choices, choices[0]);
        if (choice == 1) { plugin.deleteManual(expectedId); }
    }

    private void showHelp()
    {
        JTextArea help = new JTextArea("Estimated profit is harvest value minus supplies used during this run. Earlier planting costs are not included.\n\nExisting fruit trees and cactus count when harvested. Moving, noting or banking produce does not count.\n\nConsumed basalt, teleport tablets and inventory scrolls count during an active run. Start the run before your first teleport. Rune costs, jewellery charges, scroll-book teleports and missed costs can be added using Add / correct entry.\n\nGE values are estimates before sale tax. Sack counts inferred from XP are marked for review. Automatic detection is still being validated.", 13, 34);
        help.setFont(BODY); help.setLineWrap(true); help.setWrapStyleWord(true); help.setEditable(false);
        help.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(help), "About farming profit", JOptionPane.PLAIN_MESSAGE);
    }

    private void showLedger(FarmRun run)
    {
        JTextArea area = new JTextArea(20, 45); area.setEditable(false); area.setLineWrap(true); area.setWrapStyleWord(true);
        StringBuilder content = new StringBuilder("Estimated profit: " + numbers.format(run.profit()) + " gp\n\n");
        for (FarmRun.Entry entry : run.entries)
        {
            content.append(entry.cost ? "Cost: " : "Harvest: ").append(entry.quantity).append(" × ").append(entry.name)
                .append(" @ ").append(entry.unitPrice).append(" gp = ").append(entry.quantity * entry.unitPrice).append(" gp\n")
                .append(entry.patch).append(" · ").append(entry.source).append("\n\n");
        }
        run.warnings.forEach(w -> content.append("Review: ").append(w).append('\n'));
        area.setText(content.toString()); area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Farming run ledger", JOptionPane.PLAIN_MESSAGE);
    }

    private void correction()
    {
        JPanel form = new JPanel(new GridLayout(0, 1, 0, 4));
        JComboBox<Object> crop = new JComboBox<>(); crop.addItem("GP adjustment / supply cost"); CropCatalog.CROPS.values().forEach(crop::addItem);
        JComboBox<String> kind = new JComboBox<>(new String[]{"Cost", "Harvest"});
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(1, -1000000, 1000000, 1));
        JSpinner price = new JSpinner(new SpinnerNumberModel(1L, 0L, 2_000_000_000L, 1L));
        JTextField reason = new JTextField();
        form.add(new JLabel("Item")); form.add(crop); form.add(new JLabel("Entry type")); form.add(kind);
        form.add(new JLabel("Quantity (negative corrects an overcount)")); form.add(quantity);
        form.add(new JLabel("Unit price, gp (enter 1 for GP adjustments)")); form.add(price);
        form.add(new JLabel("Reason (required)")); form.add(reason);
        while (JOptionPane.showConfirmDialog(this, form, "Add or correct entry", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
        {
            try { quantity.commitEdit(); price.commitEdit(); } catch (java.text.ParseException ex) { continue; }
            if ((Integer) quantity.getValue() == 0 || reason.getText().trim().isEmpty())
            { JOptionPane.showMessageDialog(this, "Enter a nonzero quantity and a reason."); continue; }
            int id = crop.getSelectedItem() instanceof CropCatalog.Crop ? ((CropCatalog.Crop) crop.getSelectedItem()).id : ItemID.COINS;
            plugin.adjust(id, (Integer) quantity.getValue(), ((Number) price.getValue()).longValue(), kind.getSelectedIndex() == 0, reason.getText().trim());
            break;
        }
    }

    private void settings()
    {
        boolean[] options = plugin.settingsValues();
        JCheckBox automatic = new JCheckBox("Start runs automatically", options[0]);
        JCheckBox compost = new JCheckBox("Include compost costs", options[1]);
        JCheckBox diagnostics = new JCheckBox("Local farming debug logs", options[2]);
        JCheckBox travel = new JCheckBox("Include consumed teleport items", options[3]);
        JSpinner idle = new JSpinner(new SpinnerNumberModel(plugin.idleMinutes(), 1, 60, 1));
        JPanel form = new JPanel(new GridLayout(0, 1)); form.add(automatic); form.add(compost); form.add(travel); form.add(diagnostics); form.add(new JLabel("Finish after idle minutes")); form.add(idle);
        if (JOptionPane.showConfirmDialog(this, form, "Farming Profit settings", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
        {
            try { idle.commitEdit(); } catch (java.text.ParseException ex) { return; }
            plugin.updateSettings(automatic.isSelected(), compost.isSelected(), diagnostics.isSelected(), travel.isSelected(), (Integer) idle.getValue());
        }
    }
}
