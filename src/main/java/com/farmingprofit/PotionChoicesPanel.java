package com.farmingprofit;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.*;
import static com.farmingprofit.SidebarStyle.*;

/** Inline recipe editor; item sprites are supplied by RuneLite, never bundled substitutes. */
final class PotionChoicesPanel extends JPanel
{
    PotionChoicesPanel(HerbloreEstimate.Choices choices, BiConsumer<JLabel, Integer> icons,
        Consumer<HerbloreEstimate.Choices> save, Runnable cancel)
    {
        setLayout(new BorderLayout()); setOpaque(false);
        JPanel content = column();
        JLabel brand = label("<html>Farming Run<br>Profit</html>"); brand.setFont(BODY.deriveFont(Font.BOLD, 14)); brand.setForeground(GOLD);
        brand.setIcon(new ImageIcon(BrandAssets.MARK)); brand.setIconTextGap(6); content.add(fit(brand)); content.add(Box.createVerticalStrut(16));
        JButton back = button("Back"); back.setFont(BODY.deriveFont(11f)); back.setForeground(MUTED);
        back.addActionListener(e -> cancel.run()); content.add(back);
        content.add(Box.createVerticalStrut(12));
        JLabel heading = label("Potion choices"); heading.setFont(BODY.deriveFont(Font.BOLD, 15)); content.add(heading);
        content.add(Box.createVerticalStrut(6)); content.add(copy("Choose what you plan to make."));
        Map<HerbloreEstimate.Herb, JComboBox<HerbloreEstimate.Recipe>> fields = new LinkedHashMap<>();
        JPanel more = column();
        for (HerbloreEstimate.Herb herb : HerbloreEstimate.HERBS)
        {
            JPanel row = column();
            JLabel name = label(herb.name); name.setFont(BODY.deriveFont(Font.BOLD)); icons.accept(name, herb.id); name.setIconTextGap(5);
            row.add(fit(name));
            JComboBox<HerbloreEstimate.Recipe> select = new JComboBox<>(herb.recipes.toArray(new HerbloreEstimate.Recipe[0]));
            select.setFont(BODY.deriveFont(11f)); select.setSelectedItem(choices.recipe(herb));
            select.getAccessibleContext().setAccessibleName(herb.name + " potion choice");
            select.setPrototypeDisplayValue(herb.recipes.get(0));
            select.setPreferredSize(new Dimension(150, 30));
            JLabel potion = label(""); potion.setPreferredSize(new Dimension(24, 30));
            icons.accept(potion, choices.recipe(herb).id);
            select.addActionListener(e -> {
                HerbloreEstimate.Recipe recipe = (HerbloreEstimate.Recipe) select.getSelectedItem();
                icons.accept(potion, recipe.id); select.setToolTipText(recipe.name + " · " + HerbloreEstimate.format(recipe.xpTenths) + " per herb");
            });
            select.setToolTipText(choices.recipe(herb).name + " · " + HerbloreEstimate.format(choices.recipe(herb).xpTenths) + " per herb");
            JPanel choice = new JPanel(new BorderLayout(4, 0)); choice.setOpaque(false);
            choice.add(potion, BorderLayout.WEST); choice.add(select, BorderLayout.CENTER);
            row.add(fit(choice)); row.add(Box.createVerticalStrut(10)); fields.put(herb, select);
            if (herb.name.equals("Ranarr") || herb.name.equals("Snapdragon") || herb.name.equals("Toadflax")) { content.add(row); }
            else { more.add(row); }
        }
        JButton expand = button("More herbs"); more.setVisible(false);
        expand.addActionListener(e -> { more.setVisible(!more.isVisible()); expand.setText(more.isVisible() ? "Fewer herbs" : "More herbs"); revalidate(); repaint(); });
        content.add(fit(expand)); content.add(more); divider(content);
        JCheckBox cleaning = new JCheckBox("Include herb-cleaning XP", choices.cleaning);
        cleaning.setFont(BODY.deriveFont(11f)); cleaning.setOpaque(false); content.add(fit(cleaning));
        divider(content);
        content.add(copy("Assumes all secondaries are available."));
        content.add(copy("Changing choices recalculates XP for recorded runs."));
        JButton done = button("Done"); selected(done, true);
        done.addActionListener(e -> {
            StringJoiner encoded = new StringJoiner(";");
            fields.forEach((herb, field) -> encoded.add(herb.id + ":" + ((HerbloreEstimate.Recipe) field.getSelectedItem()).id));
            save.accept(new HerbloreEstimate.Choices(cleaning.isSelected(), encoded.toString()));
        });
        content.add(fit(done)); content.add(Box.createVerticalStrut(10));
        content.add(copy("Applies to harvested herbs only. Base XP; no outfit or processing bonuses."));
        add(content, BorderLayout.NORTH);
    }
}
