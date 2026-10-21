package com.farmingprofit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import net.runelite.client.ui.ColorScheme;

/** Read-only snapshot. Groups identical transactions without losing price or patch provenance. */
final class LedgerPanel extends JPanel
{
    private static final Font BODY = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Color GREEN = new Color(169, 219, 131);
    private static final Color RED = new Color(244, 151, 145);
    private static final Color GOLD = new Color(242, 190, 101);
    private static final Color MUTED = new Color(185, 185, 185);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d, HH:mm:ss").withZone(ZoneId.systemDefault());
    private final NumberFormat numbers = NumberFormat.getIntegerInstance();

    LedgerPanel(FarmRun run)
    {
        super(new BorderLayout(0, 12));
        setBorder(new EmptyBorder(16, 16, 12, 16)); setBackground(ColorScheme.DARK_GRAY_COLOR);
        setPreferredSize(new Dimension(620, 520));
        JPanel heading = column();
        JLabel title = label("Run breakdown"); title.setFont(BODY.deriveFont(Font.BOLD, 18)); heading.add(title);
        JLabel date = label("Started " + DATE.format(Instant.ofEpochMilli(run.startedAt))); date.setForeground(MUTED);
        heading.add(date); heading.add(Box.createVerticalStrut(12));
        JPanel totals = new JPanel(new java.awt.GridLayout(1, 3, 18, 0)); totals.setOpaque(false);
        totals.setAlignmentX(LEFT_ALIGNMENT);
        totals.add(total("Harvest value", run.harvestValue(), GREEN, false));
        totals.add(total("Supply costs", run.costs(), RED, false));
        totals.add(total("Estimated profit", run.profit(), run.profit() < 0 ? RED : GREEN, true));
        heading.add(totals); add(heading, BorderLayout.NORTH);

        Model model = new Model(run);
        JTable table = new JTable(model);
        table.setFont(BODY); table.setRowHeight(30); table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0)); table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setFont(BODY.deriveFont(Font.BOLD));
        table.getTableHeader().setReorderingAllowed(false);
        table.getAccessibleContext().setAccessibleName("Farming ledger entries");
        TableRowSorter<Model> sorter = new TableRowSorter<>(model); table.setRowSorter(sorter);
        int[] widths = {110, 220, 50, 90, 100};
        for (int i = 0; i < widths.length; i++)
        {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            table.getColumnModel().getColumn(i).setCellRenderer(new DefaultTableCellRenderer()
            {
                @Override public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                    boolean focus, int row, int col)
                {
                    super.getTableCellRendererComponent(t, value, selected, focus, row, col);
                    setFont(BODY); setBorder(new EmptyBorder(4, 8, 4, 8));
                    setHorizontalAlignment(col >= 2 ? RIGHT : LEFT);
                    if (value instanceof Number)
                    { setText(col == 4 ? signed(((Number) value).longValue()) : numbers.format(value)); }
                    // Type labels and explicit signs convey meaning independently of color.
                    setBackground(selected ? new Color(60, 57, 45)
                        : row % 2 == 0 ? ColorScheme.DARKER_GRAY_COLOR : ColorScheme.DARK_GRAY_COLOR);
                    setForeground(col == 4 ? ((Number) value).longValue() < 0 ? RED : GREEN
                        : selected ? new Color(255, 232, 187) : Color.WHITE);
                    if (selected)
                    {
                        setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(1, 0, 1, 0, new Color(134, 111, 66)), new EmptyBorder(3, 8, 3, 8)));
                    }
                    setToolTipText(col < 2 ? String.valueOf(value) : null);
                    return this;
                }
            });
        }

        JPanel body = new JPanel(new BorderLayout(0, 8)); body.setOpaque(false);
        JPanel toolbar = new JPanel(new BorderLayout(8, 0)); toolbar.setOpaque(false);
        JComboBox<String> filter = new JComboBox<>(new String[]{"All entries", "Harvest", "Costs"}); filter.setFont(BODY);
        filter.getAccessibleContext().setAccessibleName("Filter ledger entries");
        JLabel count = label(""); count.setForeground(MUTED);
        Runnable updateCount = () -> count.setText(table.getRowCount() + " rows · " + run.entries.size() + " recorded entries");
        filter.addActionListener(e -> {
            int selected = filter.getSelectedIndex();
            sorter.setRowFilter(selected == 0 ? null : new RowFilter<Model, Integer>()
            {
                @Override public boolean include(Entry<? extends Model, ? extends Integer> entry)
                { return model.rows.get(entry.getIdentifier()).cost == (selected == 2); }
            });
            updateCount.run();
        });
        toolbar.add(filter, BorderLayout.WEST); toolbar.add(count, BorderLayout.EAST); updateCount.run();
        body.add(toolbar, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(table); scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setColumnHeaderView(table.getTableHeader());
        body.add(scroll, BorderLayout.CENTER);
        SelectionDetails details = new SelectionDetails(model.rows.isEmpty());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) { return; }
            int row = table.getSelectedRow();
            details.showRow(row < 0 ? null : model.rows.get(table.convertRowIndexToModel(row)));
        });
        JScrollPane detailScroll = new JScrollPane(details); detailScroll.setBorder(BorderFactory.createEmptyBorder());
        detailScroll.setPreferredSize(new Dimension(0, 142)); body.add(detailScroll, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        JPanel footer = column();
        if (!run.warnings.isEmpty())
        {
            String noteCount = run.warnings.size() + (run.warnings.size() == 1 ? " review note" : " review notes");
            JToggleButton review = new JToggleButton("Show " + noteCount);
            review.setFont(BODY); review.setForeground(GOLD); review.setAlignmentX(LEFT_ALIGNMENT);
            JTextArea notes = area(String.join("\n\n", run.warnings), 4); notes.setForeground(GOLD);
            JScrollPane noteScroll = new JScrollPane(notes); noteScroll.setAlignmentX(LEFT_ALIGNMENT);
            noteScroll.setPreferredSize(new Dimension(0, 90)); noteScroll.setVisible(false);
            review.addActionListener(e -> {
                noteScroll.setVisible(review.isSelected());
                review.setText((review.isSelected() ? "Hide " : "Show ") + noteCount);
                revalidate(); repaint();
            });
            footer.add(review); footer.add(noteScroll); footer.add(Box.createVerticalStrut(8));
        }
        JLabel footnote = label("Snapshot when opened · GE estimates before tax"); footnote.setForeground(MUTED);
        footer.add(footnote); add(footer, BorderLayout.SOUTH);
    }

    private final class SelectionDetails extends JPanel
    {
        private final JLabel name = label("");
        private final JLabel impact = label("");
        private final JLabel calculation = label("");
        private final JPanel fields = new JPanel(new java.awt.GridBagLayout());
        private final boolean empty;

        SelectionDetails(boolean empty)
        {
            super(new BorderLayout(0, 10)); this.empty = empty;
            setBackground(ColorScheme.DARK_GRAY_COLOR);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(75, 75, 75)), new EmptyBorder(12, 4, 0, 4)));
            JPanel header = new JPanel(new BorderLayout(8, 0)); header.setOpaque(false);
            name.setFont(BODY.deriveFont(Font.BOLD, 14)); impact.setFont(BODY.deriveFont(Font.BOLD, 14));
            header.add(name, BorderLayout.WEST); header.add(impact, BorderLayout.EAST);
            JPanel title = new JPanel(new BorderLayout(0, 4)); title.setOpaque(false);
            title.add(header, BorderLayout.NORTH); calculation.setForeground(MUTED); title.add(calculation, BorderLayout.SOUTH);
            add(title, BorderLayout.NORTH); fields.setOpaque(false); add(fields, BorderLayout.CENTER);
            showRow(null);
        }

        void showRow(Row row)
        {
            fields.removeAll();
            if (row == null)
            {
                name.setText(empty ? "No entries yet" : "Explore an entry"); name.setForeground(MUTED);
                impact.setText("");
                calculation.setText(empty ? "Harvest or add a manual entry to populate this ledger." : "Select a row to see how it was tracked.");
            }
            else
            {
                name.setText(row.name); name.setForeground(Color.WHITE);
                impact.setText(signed(row.impact()) + " gp"); impact.setForeground(row.impact() < 0 ? RED : GREEN);
                calculation.setText(row.type() + " · " + numbers.format(row.quantity) + " × " + numbers.format(row.unitPrice) + " gp each");
                field(0, "Patch", row.patch.replace(" @ ", " · Tile "), Color.WHITE);
                boolean inferred = row.source.equals("Sack harvest — XP inference");
                field(1, "Tracking", inferred ? "Estimated from Farming XP — verify sack count" : row.source, inferred ? GOLD : Color.WHITE);
                String lastTime = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(row.last));
                boolean sameDay = Instant.ofEpochMilli(row.first).atZone(ZoneId.systemDefault()).toLocalDate()
                    .equals(Instant.ofEpochMilli(row.last).atZone(ZoneId.systemDefault()).toLocalDate());
                field(2, "Recorded", row.count + (row.count == 1 ? " entry" : " entries combined") + " · "
                    + DATE.format(Instant.ofEpochMilli(row.first)) + (row.first == row.last ? "" : "–" + (sameDay ? lastTime : DATE.format(Instant.ofEpochMilli(row.last)))), Color.WHITE);
            }
            revalidate(); repaint();
        }

        private void field(int index, String caption, String value, Color color)
        {
            java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
            c.gridx = 0; c.gridy = index; c.anchor = java.awt.GridBagConstraints.NORTHWEST;
            c.insets = new java.awt.Insets(0, 0, 6, 16);
            JLabel key = label(caption); key.setForeground(MUTED); fields.add(key, c);
            c.gridx = 1; c.weightx = 1; c.fill = java.awt.GridBagConstraints.HORIZONTAL;
            c.insets = new java.awt.Insets(0, 0, 6, 0);
            JLabel text = label(value); text.setForeground(color); text.setToolTipText(value); fields.add(text, c);
        }
    }

    private JPanel total(String name, long value, Color color, boolean sign)
    {
        JPanel panel = column(); JLabel caption = label(name); caption.setForeground(MUTED); panel.add(caption);
        JLabel amount = label((sign ? signed(value) : numbers.format(value)) + " gp");
        amount.setFont(BODY.deriveFont(Font.BOLD, 17)); amount.setForeground(color); panel.add(amount); return panel;
    }
    private String signed(long value) { return (value > 0 ? "+" : "") + numbers.format(value); }
    private static JLabel label(String value) { JLabel label = new JLabel(value); label.setFont(BODY); label.setAlignmentX(LEFT_ALIGNMENT); return label; }
    private static JPanel column()
    {
        JPanel panel = new JPanel(); panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS)); panel.setOpaque(false); panel.setAlignmentX(LEFT_ALIGNMENT); return panel;
    }
    private static JTextArea area(String value, int rows)
    {
        JTextArea area = new JTextArea(value, rows, 0); area.setFont(BODY); area.setEditable(false);
        area.setLineWrap(true); area.setWrapStyleWord(true); area.setBackground(ColorScheme.DARK_GRAY_COLOR); return area;
    }

    static final class Row
    {
        final String name, patch, source;
        final boolean cost;
        final long unitPrice;
        long quantity, first, last;
        int count;
        Row(FarmRun.Entry entry)
        {
            name = entry.name; patch = entry.patch; source = entry.source; cost = entry.cost; unitPrice = entry.unitPrice;
            first = last = entry.at;
        }
        long impact() { return quantity * unitPrice * (cost ? -1 : 1); }
        String type() { return (cost ? "Cost" : "Harvest") + (quantity < 0 ? " adj." : ""); }
        String details()
        {
            return name + " · " + patch + "\n" + (quantity < 0 ? "Correction · " : "") + source + "\n" + count + " recorded " + (count == 1 ? "entry" : "entries")
                + " · " + DATE.format(Instant.ofEpochMilli(first))
                + (first == last ? "" : " to " + DATE.format(Instant.ofEpochMilli(last)));
        }
    }

    static final class Model extends AbstractTableModel
    {
        final List<Row> rows;
        private static final String[] COLUMNS = {"Type", "Item", "Qty", "Each (gp)", "Net (gp)"};
        Model(FarmRun run)
        {
            Map<List<Object>, Row> grouped = new LinkedHashMap<>();
            for (FarmRun.Entry entry : run.entries)
            {
                List<Object> key = List.of(entry.itemId, entry.name, entry.cost, entry.unitPrice, entry.patch, entry.source, Integer.signum(entry.quantity));
                Row row = grouped.computeIfAbsent(key, ignored -> new Row(entry));
                row.quantity += entry.quantity; row.count++;
                row.first = Math.min(row.first, entry.at); row.last = Math.max(row.last, entry.at);
            }
            rows = new ArrayList<>(grouped.values());
        }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return COLUMNS.length; }
        @Override public String getColumnName(int column) { return COLUMNS[column]; }
        @Override public Class<?> getColumnClass(int column) { return column < 2 ? String.class : Long.class; }
        @Override public Object getValueAt(int rowIndex, int column)
        {
            Row row = rows.get(rowIndex);
            switch (column)
            {
                case 0: return row.type();
                case 1: return row.name;
                case 2: return row.quantity;
                case 3: return row.unitPrice;
                default: return row.impact();
            }
        }
    }
}
