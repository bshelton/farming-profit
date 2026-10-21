package com.farmingprofit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
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
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm").withZone(ZoneId.systemDefault());
    private final FarmingProfitPlugin plugin;
    private final JPanel current = column();
    private final JPanel history = column();
    private final JLabel status = new JLabel();
    private final JButton start = new JButton("Start run");
    private final JButton finish = new JButton("Finish run");
    private final JButton adjust = new JButton("Add / correct entry");
    private final NumberFormat numbers = NumberFormat.getIntegerInstance();

    FarmingProfitPanel(FarmingProfitPlugin plugin)
    {
        this.plugin = plugin;
        setLayout(new BorderLayout(0, 10)); setBorder(new EmptyBorder(10, 8, 10, 8));
        JPanel heading = new JPanel(new BorderLayout());
        JLabel title = new JLabel("Farming Profit"); title.setForeground(GOLD); title.setFont(title.getFont().deriveFont(Font.BOLD, 16));
        heading.add(title, BorderLayout.CENTER);
        JButton settings = new JButton("Settings"); settings.addActionListener(e -> settings());
        heading.add(settings, BorderLayout.SOUTH); add(heading, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane(); tabs.addTab("Run", current); tabs.addTab("History", history); add(tabs, BorderLayout.CENTER);
        JPanel footer = column();
        JPanel actions = new JPanel(new GridLayout(1, 2, 4, 0)); actions.add(start); actions.add(finish); footer.add(actions); footer.add(adjust); footer.add(status);
        start.addActionListener(e -> plugin.startManual()); finish.addActionListener(e -> plugin.finishManual()); adjust.addActionListener(e -> correction());
        add(footer, BorderLayout.SOUTH);
        render(new FarmingProfitPlugin.Data(), "Log in to start tracking.", false);
    }

    private static JPanel column()
    {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.DARK_GRAY_COLOR); panel.setAlignmentX(LEFT_ALIGNMENT); return panel;
    }
    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
    private static JLabel text(String text)
    {
        JLabel label = new JLabel("<html><div style='width:165px'>" + escape(text) + "</div></html>");
        label.setBorder(new EmptyBorder(5, 0, 5, 0)); label.setAlignmentX(LEFT_ALIGNMENT); return label;
    }
    private void row(JPanel parent, String label, long value)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0)); row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.add(new JLabel(label), BorderLayout.WEST); row.add(new JLabel(numbers.format(value) + " gp"), BorderLayout.EAST);
        row.setBorder(new EmptyBorder(4, 0, 4, 0)); row.setAlignmentX(LEFT_ALIGNMENT); parent.add(row);
    }

    void render(FarmingProfitPlugin.Data data, String message, boolean ready)
    {
        current.removeAll(); history.removeAll();
        start.setEnabled(ready && data.active == null); finish.setEnabled(ready && data.active != null); adjust.setEnabled(ready);
        status.setText("<html><div style='width:175px'>" + escape(message) + "</div></html>");
        if (data.active == null)
        {
            current.add(text("No active farming run"));
            current.add(text("Harvest or plant a patch to start automatically, or select Start run."));
            current.add(text("Existing fruit trees and cactus count when harvested. Moving or noting produce does not count."));
        }
        else { summary(current, data.active); }
        current.add(text("Run basis: harvest value minus this trip’s costs. Earlier planting costs are not included."));
        current.add(text("Travel, spell supplies and missed costs can be added manually. Automatic detection is awaiting in-game validation."));
        if (data.history.isEmpty()) { history.add(text("Your completed runs will appear here.")); }
        else
        {
            long total = data.history.stream().mapToLong(FarmRun::profit).sum();
            row(history, "All runs", total);
            for (FarmRun run : data.history)
            {
                JButton button = new JButton("<html>" + DATE.format(Instant.ofEpochMilli(run.startedAt)) + "<br>" + numbers.format(run.profit()) + " gp · " + run.entries.size() + " entries</html>");
                button.setAlignmentX(LEFT_ALIGNMENT);
                button.addActionListener(e -> showLedger(run)); history.add(button);
            }
        }
        revalidate(); repaint();
    }

    private void summary(JPanel parent, FarmRun run)
    {
        parent.add(text("Tracking since " + DATE.format(Instant.ofEpochMilli(run.startedAt))));
        parent.add(text("Estimated run profit"));
        JLabel total = new JLabel(numbers.format(run.profit()) + " gp");
        total.setForeground(run.profit() >= 0 ? GREEN : ColorScheme.PROGRESS_ERROR_COLOR);
        total.setFont(total.getFont().deriveFont(Font.BOLD, 23)); parent.add(total);
        row(parent, "Harvest", run.harvestValue()); row(parent, "Costs", run.costs());
        parent.add(text(numbers.format(run.farmingXp) + " Farming XP"));
        Map<String, Integer> harvest = new LinkedHashMap<>();
        run.entries.stream().filter(e -> !e.cost).forEach(e -> harvest.merge(e.name, e.quantity, Integer::sum));
        harvest.forEach((name, quantity) -> parent.add(text(quantity + " × " + name)));
        JButton detail = new JButton("Patch / cost breakdown"); detail.addActionListener(e -> showLedger(run)); parent.add(detail);
        for (String warning : run.warnings) { JLabel label = text("Review: " + warning); label.setForeground(GOLD); parent.add(label); }
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
        JSpinner idle = new JSpinner(new SpinnerNumberModel(plugin.idleMinutes(), 1, 60, 1));
        JPanel form = new JPanel(new GridLayout(0, 1)); form.add(automatic); form.add(compost); form.add(diagnostics); form.add(new JLabel("Finish after idle minutes")); form.add(idle);
        if (JOptionPane.showConfirmDialog(this, form, "Farming Profit settings", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
        {
            try { idle.commitEdit(); } catch (java.text.ParseException ex) { return; }
            plugin.updateSettings(automatic.isSelected(), compost.isSelected(), diagnostics.isSelected(), (Integer) idle.getValue());
        }
    }
}
