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
    private final JPanel reminders = column();
    private java.util.List<String> reminderMessages = java.util.List.of();
    private final JLabel status = text("");
    private final JButton start = button("Start run");
    private final JButton finish = button("Finish run");
    private final JButton pause = button("Pause run");
    private final JButton delete = button("Delete run");
    private final JButton adjust = button("Add / correct entry");
    private final NumberFormat numbers = NumberFormat.getIntegerInstance();
    private final JTabbedPane tabs = new JTabbedPane()
    {
        @Override public Dimension getPreferredSize()
        {
            Dimension size = super.getPreferredSize();
            // BasicTabbedPaneUI reserves space for the largest tab, including hidden history.
            int tallest = 0;
            for (int i = 0; i < getTabCount(); i++)
            { tallest = Math.max(tallest, getComponentAt(i).getPreferredSize().height); }
            if (getSelectedComponent() != null)
            { size.height += getSelectedComponent().getPreferredSize().height - tallest; }
            return size;
        }
        @Override public Dimension getMaximumSize()
        { return new Dimension(Integer.MAX_VALUE, getPreferredSize().height); }
    };
    private boolean ready;
    private boolean active;
    private boolean paused;
    private String activeId;
    private long accountGeneration;
    private final java.util.List<JDialog> ledgerDialogs = new java.util.ArrayList<>();
    private static final int HISTORY_PAGE_SIZE = 25;
    private java.util.List<FarmRun> renderedHistory;
    private String historyMessage;
    private boolean historyReady;
    private int historyPage;
    private long historyProfit;

    FarmingProfitPanel(FarmingProfitPlugin plugin)
    {
        this.plugin = plugin;
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 10, 12, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel content = column();
        JPanel heading = new JPanel(new BorderLayout(4, 0));
        heading.setOpaque(false);
        JLabel title = new JLabel("<html>Farming Run<br>Profit</html>");
        title.setIcon(new ImageIcon(BrandAssets.MARK)); title.setIconTextGap(6);
        title.setFont(BODY.deriveFont(Font.BOLD, 14)); title.setForeground(GOLD);
        heading.add(title, BorderLayout.CENTER);
        JButton settings = button("Settings");
        settings.setFont(BODY.deriveFont(10f));
        settings.setBorder(new EmptyBorder(6, 5, 6, 5));
        settings.addActionListener(e -> settings());
        heading.add(settings, BorderLayout.EAST);
        content.add(fit(heading)); content.add(Box.createVerticalStrut(12));
        reminders.setVisible(false); content.add(reminders);
        tabs.setFont(BODY); tabs.setAlignmentX(LEFT_ALIGNMENT);
        current.setBorder(new EmptyBorder(12, 0, 4, 0));
        history.setBorder(new EmptyBorder(12, 0, 4, 0));
        tabs.addTab("Current run", current); tabs.addTab("History", history);
        content.add(tabs); content.add(Box.createVerticalStrut(12));
        content.add(actions); content.add(Box.createVerticalStrut(10));
        status.setForeground(MUTED); content.add(status);
        JButton help = button("How profit is calculated");
        help.setFont(BODY.deriveFont(10f));
        help.setForeground(MUTED);
        help.setIcon(new InformationIcon()); help.setIconTextGap(5);
        help.setToolTipText("Harvest value, supply costs, travel costs and estimates");
        help.addActionListener(e -> showHelp());
        JPanel helpRow = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 0));
        helpRow.setOpaque(false); helpRow.add(help); content.add(fit(helpRow));
        add(content, BorderLayout.NORTH);
        start.setFont(BODY.deriveFont(Font.BOLD));
        start.addActionListener(e -> plugin.startManual(accountGeneration));
        finish.setIcon(RunActionIcon.FINISH); finish.setIconTextGap(4);
        pause.setIconTextGap(4);
        finish.addActionListener(e -> plugin.finishManual(accountGeneration, activeId));
        pause.addActionListener(e -> plugin.pauseManual(accountGeneration, activeId, !paused));
        delete.setForeground(new Color(244, 151, 145));
        delete.addActionListener(e -> deleteCurrentRun());
        adjust.addActionListener(e -> correction());
        tabs.addChangeListener(e -> {
            updateActions();
            for (java.awt.Container parent = tabs; parent != null; parent = parent.getParent())
            { parent.invalidate(); }
            revalidate(); repaint();
        });
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
        JButton button = new JButton(label)
        {
            @Override protected void paintComponent(java.awt.Graphics graphics)
            {
                super.paintComponent(graphics);
                if (isFocusOwner())
                {
                    graphics.setColor(GOLD);
                    graphics.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
                }
            }
        };
        button.setFont(BODY); button.setFocusPainted(false);
        button.setOpaque(true); button.setContentAreaFilled(true);
        button.setBackground(new Color(32, 32, 32));
        button.setRolloverEnabled(true);
        button.getModel().addChangeListener(e -> {
            boolean hover = button.isEnabled() && button.getModel().isRollover();
            button.setBackground(hover ? new Color(49, 49, 45) : new Color(32, 32, 32));
        });
        button.setBorder(new EmptyBorder(8, 8, 8, 8)); return button;
    }
    /** Small vector marks use the button foreground and scale with Swing's graphics. */
    private enum RunActionIcon implements Icon
    {
        PAUSE, RESUME, FINISH;
        public int getIconWidth() { return 10; }
        public int getIconHeight() { return 10; }
        public void paintIcon(java.awt.Component component, java.awt.Graphics graphics, int x, int y)
        {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            try
            {
                g.translate(x, y);
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.isEnabled() ? component.getForeground() : MUTED);
                if (this == PAUSE)
                { g.fillRect(1, 1, 3, 8); g.fillRect(6, 1, 3, 8); }
                else if (this == RESUME)
                { g.fillPolygon(new int[]{2, 9, 2}, new int[]{1, 5, 9}, 3); }
                else
                {
                    g.setStroke(new java.awt.BasicStroke(1.7f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    g.drawPolyline(new int[]{1, 4, 9}, new int[]{5, 8, 2}, 3);
                }
            }
            finally { g.dispose(); }
        }
    }

    /** Vector information mark stays crisp at client UI scaling levels. */
    private static final class InformationIcon implements Icon
    {
        public int getIconWidth() { return 12; }
        public int getIconHeight() { return 12; }
        public void paintIcon(java.awt.Component component, java.awt.Graphics graphics, int x, int y)
        {
            java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
            try
            {
                g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.getForeground()); g.drawOval(x, y, 11, 11);
                g.fillOval(x + 5, y + 2, 2, 2); g.fillRect(x + 5, y + 5, 2, 4);
            }
            finally { g.dispose(); }
        }
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
    private static JLabel centeredText(String value)
    {
        JLabel label = text(value);
        label.setText("<html><div style='width:145px; text-align:center'>" + escape(value) + "</div></html>");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        return fit(label);
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
                pause.setIcon(paused ? RunActionIcon.RESUME : RunActionIcon.PAUSE);
                JPanel controls = new JPanel(new GridLayout(1, 2, 6, 0)); controls.setOpaque(false);
                controls.add(pause); controls.add(finish); actions.add(fit(controls));
                actions.add(Box.createVerticalStrut(6)); adjust.setEnabled(ready); actions.add(fit(adjust));
                actions.add(Box.createVerticalStrut(10)); actions.add(fit(delete));
            }
            else { actions.add(fit(start)); }
        }
        actions.revalidate(); actions.repaint();
    }

    void renderReminders(java.util.List<String> messages)
    {
        if (messages.equals(reminderMessages)) { return; }
        reminderMessages = java.util.List.copyOf(messages);
        reminders.removeAll();
        for (String message : messages)
        {
            JLabel notice = text(message); notice.setForeground(GOLD); reminders.add(notice);
        }
        reminders.setVisible(!messages.isEmpty());
        reminders.revalidate(); reminders.repaint();
    }

    void render(FarmingProfitPlugin.Data data, String message, boolean ready)
    { render(data, message, ready, 0); }

    void render(FarmingProfitPlugin.Data data, String message, boolean ready, long generation)
    {
        if (accountGeneration != generation) { closeLedgers(); }
        accountGeneration = generation;
        current.removeAll();
        this.ready = ready; active = data.active != null;
        activeId = active ? data.active.id : null; paused = active && data.active.paused;
        String concise = message.startsWith("Saved locally") ? "Saved locally | GE estimates" : message;
        setText(status, concise); status.setToolTipText(message);
        status.setVisible(ready && (active || !message.startsWith("Saved locally")));
        if (data.active == null)
        {
            JLabel illustration = new JLabel(BrandAssets.HARVEST, SwingConstants.CENTER);
            illustration.getAccessibleContext().setAccessibleName("Herbs and gold coins in a harvest crate");
            current.add(fit(illustration)); current.add(Box.createVerticalStrut(12));
            JLabel title = centeredText(ready ? "Ready for a farming run?" : "Waiting for your account");
            title.setFont(BODY.deriveFont(Font.BOLD, 13)); current.add(fit(title));
            JLabel hint = centeredText(ready ? "Harvest or plant a patch to start tracking automatically." : message);
            hint.setForeground(MUTED); current.add(hint);
        }
        else { summary(current, data.active); }
        if (renderedHistory != data.history || historyReady != ready || !message.equals(historyMessage))
        {
            if (renderedHistory != data.history)
            {
                if (renderedHistory == null || renderedHistory.isEmpty() || data.history.isEmpty()
                    || !renderedHistory.get(0).id.equals(data.history.get(0).id)) { historyPage = 0; }
                historyProfit = data.history.stream().mapToLong(FarmRun::profit).sum();
            }
            renderedHistory = data.history; historyReady = ready; historyMessage = message;
            renderHistory();
        }
        updateActions();
        current.invalidate(); history.invalidate(); actions.invalidate();
        for (java.awt.Container parent = tabs; parent != null; parent = parent.getParent())
        { parent.invalidate(); }
        revalidate(); repaint();
    }

    private void renderHistory()
    {
        history.removeAll();
        if (renderedHistory.isEmpty())
        {
            JLabel title = text(historyReady ? "No completed runs yet" : "Waiting for your account"); title.setFont(BODY.deriveFont(Font.BOLD, 13)); history.add(fit(title));
            JLabel hint = text(historyReady ? "Finish a run to see its harvest, costs and profit here." : historyMessage); hint.setForeground(MUTED); history.add(hint);
        }
        else
        {
            row(history, "Estimated total", historyProfit);
            history.add(text(renderedHistory.size() + " saved runs"));
            historyPage = Math.min(historyPage, (renderedHistory.size() - 1) / HISTORY_PAGE_SIZE);
            int first = historyPage * HISTORY_PAGE_SIZE;
            int end = Math.min(first + HISTORY_PAGE_SIZE, renderedHistory.size());
            java.time.LocalDate previousDay = null;
            java.time.LocalDate today = java.time.LocalDate.now();
            for (FarmRun run : renderedHistory.subList(first, end))
            {
                java.time.ZonedDateTime started = Instant.ofEpochMilli(run.startedAt).atZone(ZoneId.systemDefault());
                java.time.LocalDate day = started.toLocalDate();
                if (!day.equals(previousDay))
                {
                    String date = day.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
                    String name = day.equals(today) ? "Today · " + date
                        : day.equals(today.minusDays(1)) ? "Yesterday · " + date : date;
                    JLabel heading = text(name); heading.setForeground(GOLD);
                    heading.setFont(BODY.deriveFont(Font.BOLD, 11f));
                    history.add(Box.createVerticalStrut(12)); history.add(heading); previousDay = day;
                }
                RunLocations locations = new RunLocations(run);
                JButton entry = button(""); entry.setLayout(new BorderLayout(0, 5));
                JPanel top = new JPanel(new BorderLayout(4, 0)); top.setOpaque(false);
                JLabel time = new JLabel(started.format(DateTimeFormatter.ofPattern("HH:mm")));
                time.setFont(BODY.deriveFont(Font.BOLD));
                JLabel profit = new JLabel((run.profit() > 0 ? "+" : "") + numbers.format(run.profit()) + " gp");
                profit.setFont(BODY.deriveFont(Font.BOLD)); profit.setForeground(run.profit() < 0 ? ColorScheme.PROGRESS_ERROR_COLOR : GREEN);
                top.add(time, BorderLayout.WEST); top.add(profit, BorderLayout.EAST);
                JLabel detail = text(RunLocations.elapsed(run) + " · " + locations.tiles.size() + " patch tiles");
                detail.setForeground(MUTED);
                JLabel places = text(locations.shortPlaces()); places.setForeground(MUTED);
                JPanel lower = column(); lower.setOpaque(false); lower.add(detail); lower.add(places);
                entry.add(top, BorderLayout.NORTH); entry.add(lower, BorderLayout.CENTER);
                entry.getAccessibleContext().setAccessibleName("Open run " + DATE.format(Instant.ofEpochMilli(run.startedAt))
                    + ", estimated profit " + numbers.format(run.profit()) + " gp");
                entry.setToolTipText("Open run breakdown and locations");
                entry.addActionListener(e -> showLedger(run));
                history.add(fit(entry)); history.add(Box.createVerticalStrut(4));
            }
            if (historyPage > 0)
            {
                JButton newer = button("Newer runs");
                newer.addActionListener(e -> { historyPage--; renderHistory(); }); history.add(fit(newer));
            }
            if (renderedHistory.size() > HISTORY_PAGE_SIZE)
            {
                history.add(text("Showing " + (first + 1) + "–" + end + " of " + renderedHistory.size() + " runs"));
                JButton older = button("Load older runs"); older.setEnabled(end < renderedHistory.size());
                older.addActionListener(e -> { historyPage++; renderHistory(); });
                history.add(fit(older));
            }
        }
        for (java.awt.Container parent = history; parent != null; parent = parent.getParent())
        { parent.invalidate(); }
        history.revalidate(); history.repaint();
    }

    private void summary(JPanel parent, FarmRun run)
    {
        if (run.paused && !reminderMessages.contains(FarmingReminders.PAUSED_MESSAGE))
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
        long generation = accountGeneration;
        if (expectedId == null) { return; }
        Object[] choices = {"Keep run", "Delete run"};
        int choice = JOptionPane.showOptionDialog(this,
            "Delete this run and all its entries?\nIt will not be saved to History. This cannot be undone.",
            "Delete current run", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
            null, choices, choices[0]);
        if (choice == 1) { plugin.deleteManual(generation, expectedId); }
    }

    private boolean deleteHistoryRun(FarmRun run, long generation)
    {
        Object[] choices = {"Keep run", "Delete run"};
        int choice = JOptionPane.showOptionDialog(this,
            "Delete the run from " + DATE.format(Instant.ofEpochMilli(run.startedAt))
                + "?\nEstimated profit: " + numbers.format(run.profit())
                + " gp\nAll entries in this saved run will be removed. This cannot be undone.",
            "Delete saved run", JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,
            null, choices, choices[0]);
        if (choice == 1) { plugin.deleteHistoryManual(generation, run.id); return true; }
        return false;
    }

    private void showHelp()
    {
        JTextArea help = new JTextArea("Estimated profit is harvest value minus supplies used during this run. Earlier planting costs are not included.\n\nExisting fruit trees and cactus count when harvested. Moving, noting or banking produce does not count.\n\nConsumed basalt, teleport tablets and inventory scrolls count during an active run. To include supported teleport costs, start your run before teleporting. Rune costs, jewellery charges, scroll-book teleports and missed costs can be added using Add / correct entry.\n\nGE values are estimates before sale tax. Sack counts inferred from XP are marked for review. Automatic detection is still being validated.", 13, 34);
        help.setFont(BODY); help.setLineWrap(true); help.setWrapStyleWord(true); help.setEditable(false);
        help.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(help), "About farming profit", JOptionPane.PLAIN_MESSAGE);
    }

    private void showLedger(FarmRun run)
    {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Farming run ledger", java.awt.Dialog.ModalityType.MODELESS);
        ledgerDialogs.add(dialog);
        dialog.addWindowListener(new java.awt.event.WindowAdapter()
        {
            @Override public void windowClosed(java.awt.event.WindowEvent event) { ledgerDialogs.remove(dialog); }
        });
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.add(new LedgerPanel(run), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 12, 8));
        buttons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        if (renderedHistory != null && renderedHistory.stream().anyMatch(saved -> saved.id.equals(run.id)))
        {
            long generation = accountGeneration;
            JButton remove = button("Delete run"); remove.setForeground(new Color(244, 151, 145));
            remove.setEnabled(ready);
            remove.addActionListener(e -> { if (deleteHistoryRun(run, generation)) { dialog.dispose(); } }); buttons.add(remove);
        }
        JButton close = button("Close"); close.addActionListener(e -> dialog.dispose()); buttons.add(close);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(close);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack(); dialog.setMinimumSize(new Dimension(520, 400));
        dialog.setLocationRelativeTo(this); dialog.setVisible(true);
    }

    void closeLedgers()
    {
        for (JDialog dialog : new java.util.ArrayList<>(ledgerDialogs)) { dialog.dispose(); }
        ledgerDialogs.clear();
    }

    private void correction()
    {
        long generation = accountGeneration;
        String expectedId = activeId;
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
            if ((Integer) quantity.getValue() == 0 || reason.getText().trim().isEmpty() || reason.getText().trim().length() > 1000)
            { JOptionPane.showMessageDialog(this, "Enter a nonzero quantity and a reason of 1–1,000 characters."); continue; }
            int id = crop.getSelectedItem() instanceof CropCatalog.Crop ? ((CropCatalog.Crop) crop.getSelectedItem()).id : ItemID.COINS;
            plugin.adjust(generation, expectedId, id, (Integer) quantity.getValue(), ((Number) price.getValue()).longValue(), kind.getSelectedIndex() == 0, reason.getText().trim());
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
        JCheckBox secateurs = new JCheckBox("Remind: missing magic secateurs", options[4]);
        JCheckBox sack = new JCheckBox("Remind: closed herb sack", options[5]);
        JCheckBox paused = new JCheckBox("Remind: farming while paused", options[6]);
        JCheckBox outline = new JCheckBox("Outline closed sack during reminder", options[7]);
        JSpinner idle = new JSpinner(new SpinnerNumberModel(plugin.idleMinutes(), 1, 60, 1));
        JPanel form = new JPanel(new GridLayout(0, 1)); form.add(automatic); form.add(compost); form.add(travel); form.add(diagnostics); form.add(new JLabel("Finish after idle minutes")); form.add(idle);
        form.add(secateurs); form.add(sack); form.add(paused); form.add(outline);
        if (JOptionPane.showConfirmDialog(this, form, "Farming Run Profit settings", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION)
        {
            try { idle.commitEdit(); } catch (java.text.ParseException ex) { return; }
            plugin.updateSettings(automatic.isSelected(), compost.isSelected(), diagnostics.isSelected(), travel.isSelected(), (Integer) idle.getValue());
            plugin.updateReminderSettings(secateurs.isSelected(), sack.isSelected(), paused.isSelected(), outline.isSelected());
        }
    }
}
