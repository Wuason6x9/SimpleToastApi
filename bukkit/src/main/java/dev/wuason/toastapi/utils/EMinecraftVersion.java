/*
 *     Copyright (C) 2026 Wuason6x9 and RubenArtz
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.wuason.toastapi.utils;

import org.bukkit.Bukkit;

import java.util.Objects;

public enum EMinecraftVersion {
    V1_16_5(754, NMSVersion.V1_16_R3),

    V1_17(755, NMSVersion.V1_17_R1),
    V1_17_1(756, NMSVersion.V1_17_R1),

    V1_18(757, NMSVersion.V1_18_R1),
    V1_18_1(757, NMSVersion.V1_18_R1),
    V1_18_2(758, NMSVersion.V1_18_R2),

    V1_19(759, NMSVersion.V1_19_R1),
    V1_19_1(760, NMSVersion.V1_19_R1),
    V1_19_2(760, NMSVersion.V1_19_R1),
    V1_19_3(761, NMSVersion.V1_19_R2),
    V1_19_4(762, NMSVersion.V1_19_R3),

    V1_20(763, NMSVersion.V1_20_R1),
    V1_20_1(763, NMSVersion.V1_20_R1),
    V1_20_2(764, NMSVersion.V1_20_R2),
    V1_20_3(765, NMSVersion.V1_20_R3),
    V1_20_4(765, NMSVersion.V1_20_R3),
    V1_20_5(766, NMSVersion.V1_20_R4),
    V1_20_6(766, NMSVersion.V1_20_R4),

    V1_21(767, NMSVersion.V1_21_R1),
    V1_21_1(767, NMSVersion.V1_21_R1),
    V1_21_2(768, NMSVersion.V1_21_R1),
    V1_21_3(768, NMSVersion.V1_21_R2),
    V1_21_4(769, NMSVersion.V1_21_R3),
    V1_21_5(770, NMSVersion.V1_21_R4),
    V1_21_6(771, NMSVersion.V1_21_R5),
    V1_21_7(772, NMSVersion.V1_21_R5),
    V1_21_8(772, NMSVersion.V1_21_R5),
    V1_21_9(773, NMSVersion.V1_21_R6),
    V1_21_10(773, NMSVersion.V1_21_R6),
    V1_21_11(774, NMSVersion.V1_21_R7),

    V26_1(775, NMSVersion.V26_1),
    V26_1_1(775, NMSVersion.V26_1_1),
    V26_1_2(775, NMSVersion.V26_1_2),

    UNSUPPORTED(-1, NMSVersion.UNSUPPORTED);


    /**
     * Represents the specific version of the Minecraft server that has been selected.
     * This variable is used to store the selected Minecraft version to ensure compatibility
     * and proper configuration of the server.
     * <p>
     * It is initialized to null, indicating that no version has been selected upon
     * initialization.
     */
    private static EMinecraftVersion serverVersionSelected = null;

    /**
     * The version number of the current instance.
     * This variable is used to track the version of an object,
     * typically for purposes such as version control, data compatibility checks,
     * and to maintain state consistency across different instances.
     */
    private final int versionNumber;
    /**
     * The version of the Network Management System (NMS) being used.
     * This variable stores an instance of NMSVersion which provides
     * specific information and functionalities related to that version.
     * Since this is a final variable, once assigned, its reference cannot be changed.
     */
    private final NMSVersion nmsVersion;

    /**
     * Initializes a new instance of the MinecraftVersion class with the specified version number
     * and NMS version.
     *
     * @param versionNumber The version number of Minecraft.
     * @param nmsVersion    The NMS version associated with this Minecraft version. If null,
     *                      the NMS version will be set to NMSVersion.UNSUPPORTED.
     */
    EMinecraftVersion(int versionNumber, NMSVersion nmsVersion) {
        this.versionNumber = versionNumber;
        this.nmsVersion = Objects.requireNonNullElse(nmsVersion, NMSVersion.UNSUPPORTED);
    }

    /**
     * Converts a string representation of a Minecraft version into a MinecraftVersion enum.
     *
     * @param version the string representation of the Minecraft version (e.g., "1.16.4").
     * @return the corresponding MinecraftVersion enum if it exists, else returns UNSUPPORTED.
     */
    public static EMinecraftVersion fromString(String version) {
        String normalized = version.replace(".", "_");

        try {
            return valueOf("V" + normalized);
        } catch (IllegalArgumentException e) {
            return UNSUPPORTED;
        }
    }

    /**
     * Returns the MinecraftVersion corresponding to the specified version number.
     *
     * @param i the version number of the Minecraft version to be retrieved.
     * @return the MinecraftVersion that matches the given version number,
     * or UNSUPPORTED if no matching version is found.
     */
    public static EMinecraftVersion fromVersionNumber(int i) {
        for (EMinecraftVersion srv : values()) {
            if (srv.versionNumber == i) return srv;
        }
        return UNSUPPORTED;
    }

    /**
     * Retrieves the selected server version. If it has not been selected yet,
     * it determines the version from Bukkit's version string and sets it.
     *
     * @return The selected Minecraft server version.
     */
    public static EMinecraftVersion getServerVersionSelected() {
        if (serverVersionSelected != null) {
            return serverVersionSelected;
        }

        synchronized (EMinecraftVersion.class) {
            if (serverVersionSelected != null) {
                return serverVersionSelected;
            }
            serverVersionSelected = resolveVersion();
        }

        return serverVersionSelected;
    }

    private static EMinecraftVersion resolveVersion() {
        String raw = extractVersionString();
        String key = normalizeToEnumKey(raw);
        EMinecraftVersion resolved = fromString(key);

        if (resolved == null) {
            throw new IllegalStateException(
                    "Unsupported server version: '" + raw + "' -> key: '" + key + "'. " +
                            "Please report this at https://github.com/wuason6x9/toastapi"
            );
        }

        return resolved;
    }

    /**
     * Extracts the raw version string from the Bukkit API.
     * Works on all Bukkit implementations (Spigot, Paper, Purpur, Folia, etc.)
     * without reflection.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code "1.21.11-R0.1-SNAPSHOT"} -> {@code "1.21.11"}</li>
     *   <li>{@code "26.1.2-R0.1-SNAPSHOT"}  -> {@code "26.1.2"}</li>
     * </ul>
     */
    private static String extractVersionString() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /**
     * Normalizes a raw version string into an enum key format.
     *
     * <p>New versioning scheme (>= 25.x): segments joined by underscores.
     * <ul>
     *   <li>{@code "26.1"}   -> {@code "26_1"}</li>
     *   <li>{@code "26.1.1"} -> {@code "26_1_1"}</li>
     *   <li>{@code "26.1.2"} -> {@code "26_1_2"}</li>
     *   <li>{@code "27.1"}   -> {@code "27_1"}</li>
     * </ul>
     *
     * <p>Classic versioning scheme (1.x): passed through as-is.
     * <ul>
     *   <li>{@code "1.21.4"}  -> {@code "1.21.4"}</li>
     *   <li>{@code "1.21.11"} -> {@code "1.21.11"}</li>
     * </ul>
     *
     * @param raw the raw version string (e.g. {@code "26.1.2"})
     * @return the normalized enum key (e.g. {@code "26_1_2"})
     */
    private static String normalizeToEnumKey(String raw) {
        String[] parts = raw.split("\\.");

        if (parts.length == 0) {
            return raw;
        }

        try {
            int first = Integer.parseInt(parts[0]);

            if (first >= 25) {
                StringBuilder key = new StringBuilder();
                for (int i = 0; i < Math.min(parts.length, 3); i++) {
                    if (!parts[i].matches("\\d+")) break;
                    if (i > 0) key.append("_");
                    key.append(parts[i]);
                }
                return key.toString();
            }

            return raw;

        } catch (NumberFormatException ignored) {
            return raw;
        }
    }

    /**
     * Retrieves the server version of Bukkit.
     *
     * @return The server version string from Bukkit.
     */
    public static String getServerVersionBukkit() {
        return Bukkit.getBukkitVersion().split("-")[0];
    }

    /**
     * Retrieves the last supported version of Minecraft from an array of version values.
     *
     * @return The second to last Minecraft version indicating the last officially supported version.
     */
    public static EMinecraftVersion getLastSupportedVersion() {
        return values()[values().length - 2];
    }

    /**
     * Retrieves the current NMS (Net Minecraft Server) version.
     *
     * @return the NMSVersion representing the current version.
     */
    public NMSVersion getNMSVersion() {
        return nmsVersion;
    }

    /**
     * Retrieves the version number.
     *
     * @return the current version number as an integer.
     */
    public int getVersionNumber() {
        return versionNumber;
    }

    /**
     * Retrieves the version name of the current enum constant.
     * The version name is derived by removing the "V" character and replacing underscores with dots.
     *
     * @return A String representing the version name of the enum constant, with "V" removed and underscores replaced by dots.
     */
    public String getVersionName() {
        return name().replace("V", "").replace("_", ".");
    }

    /**
     * Checks if the current Minecraft version is at least the specified version.
     *
     * @param otherVersion The MinecraftVersion object to compare against.
     * @return true if the current version number is greater than or equal to the otherVersion's version number, false otherwise.
     */
    public boolean isAtLeast(EMinecraftVersion otherVersion) {
        return versionNumber >= otherVersion.versionNumber;
    }

    /**
     * Compares the current Minecraft version with another version to determine if it is less than the other version.
     *
     * @param otherVersion The MinecraftVersion object to compare with.
     * @return true if the current version is less than the other version; false otherwise.
     */
    public boolean isLessThan(EMinecraftVersion otherVersion) {
        return versionNumber < otherVersion.versionNumber;
    }

    /**
     * Compares the current MinecraftVersion object with another MinecraftVersion object.
     *
     * @param otherVersion the MinecraftVersion object to compare against.
     * @return true if the current version number is greater than the specified version number, false otherwise.
     */
    public boolean isGreaterThan(EMinecraftVersion otherVersion) {
        return versionNumber > otherVersion.versionNumber;
    }

    /**
     * Compares the current version with the specified version to check if the current version is at most
     * the specified version.
     *
     * @param otherVersion the MinecraftVersion instance to compare with the current version.
     * @return true if the current version is less than or equal to the specified version, false otherwise.
     */
    public boolean isAtMost(EMinecraftVersion otherVersion) {
        return versionNumber <= otherVersion.versionNumber;
    }

    /**
     * The NMSVersion enum defines various versions of Minecraft NMS (Net Minecraft Server).
     * Each enum constant is associated with an integer value that signifies the version.
     */
    public enum NMSVersion {
        V1_16_R3(0),
        V1_17_R1(1),
        V1_18_R1(2),
        V1_18_R2(3),
        V1_19_R1(4),
        V1_19_R2(5),
        V1_19_R3(6),
        V1_20_R1(7),
        V1_20_R2(8),
        V1_20_R3(9),
        V1_20_R4(10),
        V1_21_R1(11),
        V1_21_R2(12),
        V1_21_R3(13),
        V1_21_R4(14),
        V1_21_R5(15),
        V1_21_R6(16),
        V1_21_R7(17),
        V26_1(18),
        V26_1_1(19),
        V26_1_2(20),
        UNSUPPORTED(-1);

        /**
         * Represents the integer value associated with a specific Minecraft NMS (Net Minecraft Server) version.
         */
        private final int version;

        /**
         * Constructs an instance of NMSVersion with the specified version.
         *
         * @param version the integer value representing the version of the Minecraft NMS
         */
        NMSVersion(int version) {
            this.version = version;
        }

        /**
         * Checks if the current NMSVersion is supported.
         *
         * @return true if the current version is not UNSUPPORTED; false otherwise.
         */
        public boolean isSupported() {
            return this != UNSUPPORTED;
        }

        /**
         * Determines if the current NMS version is at least the specified version.
         *
         * @param otherVersion the NMSVersion to compare against
         * @return true if the current version is greater than or equal to the other version; false otherwise
         */
        public boolean isAtLeast(NMSVersion otherVersion) {
            return version >= otherVersion.version;
        }

        /**
         * Determines if the current version is less than the specified version.
         *
         * @param otherVersion the version to compare with the current version
         * @return true if the current version is less than the specified version, false otherwise
         */
        public boolean isLessThan(NMSVersion otherVersion) {
            return version < otherVersion.version;
        }

        /**
         * Compares this NMSVersion object with another to determine if it is greater.
         *
         * @param otherVersion the NMSVersion to compare with this version
         * @return true if this version is greater than the specified version, false otherwise
         */
        public boolean isGreaterThan(NMSVersion otherVersion) {
            return version > otherVersion.version;
        }

        /**
         * Determines if the current NMSVersion is at most the specified NMSVersion.
         *
         * @param otherVersion the other NMSVersion to compare against
         * @return true if the current NMSVersion is less than or equal to the specified NMSVersion; false otherwise
         */
        public boolean isAtMost(NMSVersion otherVersion) {
            return version <= otherVersion.version;
        }

        /**
         * Retrieves the version name of the enum constant by removing the
         * leading 'V' character from its name.
         *
         * @return the version name as a string without the leading 'V'.
         */
        public String getVersionName() {
            return name().replace("V", "");
        }

        /**
         * Gets the integer value associated with the version.
         *
         * @return the version number as an integer.
         */
        public int getVersion() {
            return version;
        }
    }
}