package com.farmingprofit;

import net.runelite.api.gameval.ItemID;
import org.junit.Test;
import static org.junit.Assert.*;

public class FarmingRemindersTest
{
    @Test public void secateursAdviceOnlyForSupportedCrops()
    {
        for (String crop : new String[]{"Potato plant", "Snape grass", "Limpwurt plant", "Redberry bush", "Hammerstone hops", "Grape vine", "Celastrus tree", "Elkhorn coral"})
        { assertTrue(crop, FarmingReminders.benefitsFromSecateurs(crop, false)); }
        for (String crop : new String[]{"Potato cactus", "Seaweed", "Dragonfruit tree", "Crystal tree", "Belladonna", "Mushrooms", "Marigold", "Unknown"})
        { assertFalse(crop, FarmingReminders.benefitsFromSecateurs(crop, false)); }
        assertTrue(FarmingReminders.benefitsFromSecateurs("Ranarr weed", true));
    }

    @Test public void carriedOrEquippedSecateursSuppressReminder()
    {
        assertTrue(FarmingReminders.messages(true, true, true, false, true, false, true, true, true).isEmpty());
        assertEquals(1, FarmingReminders.messages(true, true, true, false, false, false, true, true, true).size());
    }

    @Test public void bothSackVariantsAreSupportedButOpenSacksAreNotHighlighted()
    {
        assertTrue(FarmingReminders.closedSack(ItemID.SLAYER_HERB_SACK));
        assertTrue(FarmingReminders.closedSack(ItemID.SLAYER_HERB_SACK_SILK));
        assertFalse(FarmingReminders.closedSack(ItemID.SLAYER_HERB_SACK_OPEN));
        assertFalse(FarmingReminders.closedSack(ItemID.SLAYER_HERB_SACK_SILK_OPEN));
        assertTrue(FarmingReminders.messages(true, false, false, false, false, true, true, true, true).isEmpty());
    }

    @Test public void eachToggleSuppressesOnlyItsReminder()
    {
        assertEquals(3, FarmingReminders.messages(true, true, true, true, false, true, true, true, true).size());
        assertEquals(2, FarmingReminders.messages(true, true, true, true, false, true, false, true, true).size());
        assertEquals(2, FarmingReminders.messages(true, true, true, true, false, true, true, false, true).size());
        assertEquals(2, FarmingReminders.messages(true, true, true, true, false, true, true, true, false).size());
        assertTrue(FarmingReminders.messages(true, true, true, true, false, true, false, false, false).isEmpty());
    }

    @Test public void plantingOnlyWarnsAboutPausedTracking()
    {
        assertEquals(1, FarmingReminders.messages(false, true, true, true, false, true, true, true, true).size());
        assertTrue(FarmingReminders.messages(false, true, true, false, false, true, true, true, true).isEmpty());
    }
}
