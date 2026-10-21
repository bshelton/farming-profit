package com.farmingprofit;

import java.util.Set;
import net.runelite.api.gameval.ItemID;

/** Only a deliberate use followed by consumption can become a travel cost. */
final class TravelCosts
{
    private static final Set<Integer> ITEMS = Set.of(
        ItemID.POH_TABLET_VARROCKTELEPORT,
        ItemID.POH_TABLET_LUMBRIDGETELEPORT,
        ItemID.POH_TABLET_FALADORTELEPORT,
        ItemID.POH_TABLET_CAMELOTTELEPORT,
        ItemID.POH_TABLET_ARDOUGNETELEPORT,
        ItemID.POH_TABLET_WATCHTOWERTELEPORT,
        ItemID.POH_TABLET_TELEPORTTOHOUSE,
        ItemID.NZONE_TELETAB_RIMMINGTON,
        ItemID.NZONE_TELETAB_TAVERLEY,
        ItemID.NZONE_TELETAB_POLLNIVNEACH,
        ItemID.NZONE_TELETAB_RELLEKKA,
        ItemID.NZONE_TELETAB_BRIMHAVEN,
        ItemID.NZONE_TELETAB_YANILLE,
        ItemID.NZONE_TELETAB_TROLLHEIM,
        ItemID.TELEPORTSCROLL_NARDAH,
        ItemID.TELEPORTSCROLL_DIGSITE,
        ItemID.TELEPORTSCROLL_FELDIP,
        ItemID.TELEPORTSCROLL_LUNARISLE,
        ItemID.TELEPORTSCROLL_MORTTON,
        ItemID.TELEPORTSCROLL_PESTCONTROL,
        ItemID.TELEPORTSCROLL_PISCATORIS,
        ItemID.TELEPORTSCROLL_TAIBWO,
        ItemID.TELEPORTSCROLL_ELF,
        ItemID.TELEPORTSCROLL_MOSLES,
        ItemID.TELEPORTSCROLL_LUMBERYARD,
        ItemID.TELEPORTSCROLL_ZULANDRA,
        ItemID.TELEPORTSCROLL_CERBERUS,
        ItemID.TELETAB_LUMBRIDGE,
        ItemID.TELETAB_DRAYNOR,
        ItemID.TELETAB_MIND_ALTAR,
        ItemID.TELETAB_SALVE,
        ItemID.TELETAB_FENK,
        ItemID.TELETAB_WESTARDY,
        ItemID.TELETAB_HARMONY,
        ItemID.TELETAB_CEMETERY,
        ItemID.TELETAB_BARROWS,
        ItemID.TELETAB_APE,
        ItemID.NZONE_TELETAB_KOUREND,
        ItemID.TELEPORTSCROLL_REVENANTS,
        ItemID.WEISS_TELEPORT_BASALT,
        ItemID.STRONGHOLD_TELEPORT_BASALT,
        ItemID.TELETAB_BATTLEFRONT,
        ItemID.TELEPORTSCROLL_WATSON,
        ItemID.NZONE_TELETAB_PRIFDDINAS,
        ItemID.LUNAR_TABLET_MOONCLAN_TELEPORT,
        ItemID.LUNAR_TABLET_OURANIA_TELEPORT,
        ItemID.LUNAR_TABLET_WATERBIRTH_TELEPORT,
        ItemID.LUNAR_TABLET_BARBARIAN_TELEPORT,
        ItemID.LUNAR_TABLET_KHAZARD_TELEPORT,
        ItemID.LUNAR_TABLET_FISHING_GUILD_TELEPORT,
        ItemID.LUNAR_TABLET_CATHERBY_TELEPORT,
        ItemID.LUNAR_TABLET_ICE_PLATEAU_TELEPORT,
        ItemID.POH_TABLET_KOURENDTELEPORT,
        ItemID.POH_TABLET_FORTISTELEPORT,
        ItemID.TELEPORTSCROLL_GUTHIXIAN_TEMPLE,
        ItemID.TELEPORTSCROLL_SPIDERCAVE,
        ItemID.TELEPORTSCROLL_COLOSSAL_WYRM,
        ItemID.NZONE_TELETAB_ALDARIN,
        ItemID.TELEPORTSCROLL_CHASMOFFIRE,
        ItemID.POH_TABLET_TELEPORTBOATTOME,
        ItemID.POH_TABLET_TELEPORTMETOBOAT,
        ItemID.TELEPORTSCROLL_ARDEAGLAIS);
    private int itemId = -1;
    private int before;
    private long expires;

    void clear() { itemId = -1; }
    int pendingItem() { return itemId; }

    void begin(int id, String option, int count, long now)
    {
        clear();
        if (ITEMS.contains(id) && count > 0
            && (option.equals("break") || option.equals("teleport") || option.equals("outside")))
        {
            itemId = id; before = count; expires = now + 15_000;
        }
    }

    int consumed(int count, long now)
    {
        if (itemId == -1) { return -1; }
        if (now > expires) { clear(); return -1; }
        if (count == before) { return -1; }
        int result = before - count == 1 ? itemId : -1;
        clear();
        return result;
    }
}
