package me.deecaad.weaponmechanics.weapon.skin;

import me.deecaad.core.file.SerializeData;
import me.deecaad.core.file.Serializer;
import me.deecaad.core.file.SerializerException;
import me.deecaad.weaponmechanics.WeaponMechanics;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @see SkinHandler
 */
public class SkinList implements Serializer<SkinList> {

    private final Map<String, Map<SkinIdentifier, BaseSkin>> map;

    /**
     * Default constructor for serializer.
     */
    public SkinList() {
        map = Collections.emptyMap();
    }

    public SkinList(Map<String, Map<SkinIdentifier, BaseSkin>> map) {
        this.map = map;
    }

    public Set<String> getSkins() {
        return new HashSet<>(map.keySet());
    }

    public BaseSkin getSkin(@Nullable String skin, @Nullable SkinIdentifier id) {
        if (skin == null) {
            skin = "default";
        }

        if (id == null) {
            id = SkinIdentifier.DEFAULT;
        }

        Map<SkinIdentifier, BaseSkin> temp = map.get(skin);
        return temp == null ? null : temp.get(id);
    }

    @Override
    public String getKeyword() {
        return "skin";
    }

    @NotNull
    @Override
    public SkinList serialize(SerializeData data) throws SerializerException {
        Map<String, Map<SkinIdentifier, BaseSkin>> map = new HashMap<>();
        Map<SkinIdentifier, BaseSkin> defaultSkinData = new HashMap<>();

        ConfigurationSection section = data.of().assertExists().assertType(ConfigurationSection.class).get();
        Set<String> keys = section.getKeys(false);

        // Extra check to make sure the user has a default skin
        data.of(SkinIdentifier.DEFAULT.key).assertExists();

        for (String key : keys) {
            SkinIdentifier id = SkinIdentifier.fromString(key);

            if (id == null) {

                // Of course, it wouldn't be hard to bypass this, especially
                // since this is open source, but good on them if they put in
                // the effort for their own server.
                if (!hasCosmetics()) {
                    WeaponMechanics.debug.debug("Tried to use skins when WeaponMechanicsCosmetics wasn't installed");
                    continue;
                }

                // Hand skin requires 'Item'. Skip this
                if ("Item".equals(key))
                    continue;

                // Skip anything that isn't a configuration section. This is
                // used for the HAND feature in WMC.
                if (!data.of(key).is(ConfigurationSection.class)) {
                    WeaponMechanics.debug.debug("Found bad skinlist? at " + data.of(key).getLocation());
                    continue;
                }

                Map<SkinIdentifier, BaseSkin> temp = serializeOne(data.move(key));
                map.put(key.toLowerCase(Locale.ROOT), temp);
                continue;
            }

            // Since SCOPE_STACK matches to different keys, we need to handle
            // Scope_1, Scope_2, Scope_3, etc.
            if (id == SkinIdentifier.SCOPE_STACK)
                id = new SkinIdentifier(key);

            defaultSkinData.put(id, data.of(key).assertExists().serialize(BaseSkin.class));
        }

        map.put("default", defaultSkinData);
        return new SkinList(map);
    }

    private Map<SkinIdentifier, BaseSkin> serializeOne(SerializeData data) throws SerializerException {
        Map<SkinIdentifier, BaseSkin> map = new HashMap<>();

        ConfigurationSection section = data.of().assertExists().assertType(ConfigurationSection.class).get();
        Set<String> keys = section.getKeys(false);

        // Extra check to make sure the user has a default skin
        data.of(SkinIdentifier.DEFAULT.key).assertExists();

        for (String key : keys) {
            SkinIdentifier id = SkinIdentifier.fromString(key);

            if (id == null)
                throw data.exception(key, "Found an unknown skin identifier",
                        SerializerException.forValue(key),
                        SerializerException.didYouMean(key, Arrays.stream(SkinIdentifier.VALUES).map(SkinIdentifier::getKey).collect(Collectors.toList())),
                        "See: https://github.com/WeaponMechanics/MechanicsMain/wiki/Skins");

            // Since SCOPE_STACK matches to different keys, we need to handle
            // Scope_1, Scope_2, Scope_3, etc.
            if (id == SkinIdentifier.SCOPE_STACK)
                id = new SkinIdentifier(key);

            map.put(id, data.of(key).assertExists().serialize(BaseSkin.class));
        }

        return map;
    }

    /**
     * Returns <code>true</code> if WeaponMechanicsCosmetics is installed on
     * the server.
     *
     * @return true if WMC is installed.
     */
    public static boolean hasCosmetics() {
        return Bukkit.getPluginManager().getPlugin("WeaponMechanicsCosmetics") != null;
    }

    /**
     * An identifier for a skin type. Imagine a gun has its default skin, and
     * a red skin. The default skin can have
     */
    public static class SkinIdentifier {

        public static final SkinIdentifier DEFAULT = new SkinIdentifier("Default");
        public static final SkinIdentifier SCOPE = new SkinIdentifier("Scope");
        public static final SkinIdentifier SCOPE_STACK = new SkinIdentifier("Scope_\\d\\d?", true);
        public static final SkinIdentifier NO_AMMO = new SkinIdentifier("No_Ammo");
        public static final SkinIdentifier RELOAD = new SkinIdentifier("Reload");
        public static final SkinIdentifier SPRINT = new SkinIdentifier("Sprint");

        private static final SkinIdentifier[] VALUES = new SkinIdentifier[] { DEFAULT, SCOPE, SCOPE_STACK, NO_AMMO, RELOAD, SPRINT };

        private final String key;
        private final boolean useRegex;

        public SkinIdentifier(String key) {
            this(key, false);
        }

        public SkinIdentifier(String key, boolean useRegex) {
            this.key = key;
            this.useRegex = useRegex;
        }

        public String getKey() {
            return key;
        }

        public boolean matches(String key) {
            if (useRegex) {
                Pattern pattern = Pattern.compile(this.key);
                Matcher matcher = pattern.matcher(key);
                return matcher.matches();
            }

            return this.key.equals(key);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SkinIdentifier that = (SkinIdentifier) o;
            return key.equals(that.key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        public static SkinIdentifier fromString(String str) {
            for (SkinIdentifier identifier : VALUES) {
                if (identifier.matches(str))
                    return identifier;
            }

            return null;
        }
    }
}
