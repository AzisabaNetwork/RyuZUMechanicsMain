package me.deecaad.core.placeholder;

import me.deecaad.core.utils.LogLevel;
import me.deecaad.core.utils.StringUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static me.deecaad.core.MechanicsCore.debug;

public final class PlaceholderAPI {

    private static final Map<String, PlaceholderHandler> placeholderHandlers = new HashMap<>();
    public static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("<[a-zA-Z_\\-]+>");

    /**
     * Don't let anyone instantiate this class.
     */
    private PlaceholderAPI() {
    }

    public static String addDiamond(String str) {
        str = str.toLowerCase(Locale.ROOT);
        if (!str.startsWith("<"))
            str = "<" + str;
        if (!str.endsWith(">"))
            str = str + ">";

        return str;
    }

    public static String removeDiamond(String str) {
        str = str.toLowerCase(Locale.ROOT);
        if (str.startsWith("<"))
            str = str.substring(1);
        if (!str.endsWith(">"))
            str = str.substring(0, str.length() - 1);

        return str;
    }

    public static PlaceholderHandler getPlaceholder(String str) {
        return placeholderHandlers.get(removeDiamond(str));
    }

    public static Collection<PlaceholderHandler> listPlaceholders() {
        return Collections.unmodifiableCollection(placeholderHandlers.values());
    }

    /**
     * Same placeholder name can't be added twice
     *
     * @param placeholderHandler the new placeholder handler
     */
    public static void addPlaceholderHandler(PlaceholderHandler placeholderHandler) {
        if (placeholderHandlers.get(placeholderHandler.getPlaceholderName()) != null) {
            debug.log(LogLevel.ERROR,
                    "Tried to add placeholder handler with same name twice (" + placeholderHandler.getPlaceholderName() + ").",
                    "Ignoring this new placeholder...");
            return;
        }
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(placeholderHandler.getPlaceholderName());
        if (!matcher.find()) {
            debug.log(LogLevel.ERROR,
                    "Tried to add placeholder handler which format wasn't valid (" + placeholderHandler.getPlaceholderName() + ").",
                    "Correct format:",
                    "- has placeholder between % chars. (example %my-placeholder%)",
                    "- does not contain any whitespaces");
            return;
        }
        placeholderHandlers.put(placeholderHandler.getPlaceholderName(), placeholderHandler);
    }

    /**
     * Applies all possible placeholders. Includes Clip's PlaceholderAPI support.
     * Also colorizes string.
     *
     * @param to the string where to apply placeholders
     * @param player the player involved in event or null
     * @param itemStack the item stack involved in event or null
     * @param itemTitle the item title involved in this request, can be null
     * @param slot the weapon slot involved in this request, can be null
     * @return the string with applied placeholders
     */
    public static String applyPlaceholders(String to, @Nullable Player player, @Nullable ItemStack itemStack, @Nullable String itemTitle, @Nullable EquipmentSlot slot) {
        return applyPlaceholders(to, player, itemStack, itemTitle, slot, null);
    }

    /**
     * Applies all possible placeholders. Includes Clip's PlaceholderAPI support.
     * Also colorizes string.
     *
     * @param to the string where to apply placeholders
     * @param player the player involved in event or null
     * @param itemStack the item stack involved in event or null
     * @param itemTitle the item title involved in this request, can be null
     * @param slot the weapon slot involved in this request, can be null
     * @param temp the temporary placeholders to be used
     * @return the string with applied placeholders
     */
    public static String applyPlaceholders(String to, @Nullable Player player, @Nullable ItemStack itemStack, @Nullable String itemTitle, @Nullable EquipmentSlot slot, @Nullable Map<String, String> temp) {
        if (to == null) {
            return null;
        }
        if (!placeholderHandlers.isEmpty()) {
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(to);
            while (matcher.find()) {
                String currentPlaceholder = matcher.group(1).toLowerCase(Locale.ROOT);

                // Check for temp placeholders
                if (temp != null) {
                    String request = temp.get("%" + currentPlaceholder + "%");
                    if (request != null) {
                        to = to.replace("%" + currentPlaceholder + "%", request);
                        continue;
                    }
                }

                PlaceholderHandler placeholderHandler = placeholderHandlers.get("%" + currentPlaceholder + "%");
                if (placeholderHandler == null) {
                    continue;
                }
                String request = null;
                try {
                    request = placeholderHandler.onRequest(player, itemStack, itemTitle, slot);
                } catch (Exception e) {
                    debug.log(LogLevel.WARN, "Placeholder using keyword " + placeholderHandler.getPlaceholderName() + " caused this exception!", e);
                }
                if (request == null) {
                    continue;
                }
                to = to.replace("%" + currentPlaceholder + "%", request);
            }
        }
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            to = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, to);
        } catch (ClassNotFoundException e) {/**/}
        return StringUtil.color(to);
    }

    /**
     * Creates new list based on given collection.
     *
     * @see PlaceholderAPI#applyPlaceholders(String, Player, ItemStack, String, EquipmentSlot)
     */
    public static List<String> applyPlaceholders(Collection<String> to, @Nullable Player player, @Nullable ItemStack itemStack, @Nullable String itemTitle, @Nullable EquipmentSlot slot) {
        if (to == null)
            return null;
        else if (to.isEmpty())
            return new ArrayList<>(1);

        Iterator<String> iterator = to.iterator();

        List<String> tempList = new ArrayList<>();
        while (iterator.hasNext()) {
            tempList.add(applyPlaceholders(iterator.next(), player, itemStack, itemTitle, slot));
        }
        return tempList;
    }

    /**
     * Applies only given placeholder to string. Doesn't do anything else.
     *
     * @param to the string where to apply placeholders
     * @param placeholder the placeholder key (e.g. victim)
     * @param value the string to replace victim with
     * @return the string with applied placeholder
     */
    public static String applyTempPlaceholder(String to, String placeholder, String value) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(to);
        while (matcher.find()) {
            String currentPlaceholder = matcher.group(1);
            if (currentPlaceholder.equalsIgnoreCase(placeholder)) {
                to = to.replace("%" + currentPlaceholder + "%", value);
            }
        }
        return to;
    }

    /**
     * Creates new list based on given collection.
     *
     * @see PlaceholderAPI#applyTempPlaceholder(String, String, String)
     */
    public static List<String> applyTempPlaceholder(Collection<String> to, String placeholder, String value) {
        Iterator<String> iterator = to.iterator();

        List<String> tempList = new ArrayList<>();
        while (iterator.hasNext()) {
            tempList.add(applyTempPlaceholder(iterator.next(), placeholder, value));
        }
        return tempList;
    }

    /**
     * This should be called when reloading or shutting down server!
     */
    public static void onDisable() {
        placeholderHandlers.clear();
    }
}