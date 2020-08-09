/*
 * Addons for the Custom Enchantment API in Minecraft
 * Copyright (C) 2020 BigBadE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package software.bigbade.enchantmenttokens;

import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.api.EnchantmentPlayer;
import software.bigbade.enchantmenttokens.api.wrappers.EnchantmentChain;
import software.bigbade.enchantmenttokens.currency.CurrencyHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class MySQLCurrencyHandler implements CurrencyHandler {
    private static final Pattern NAMESPACED_KEY = Pattern.compile(":");

    private final Connection connection;
    private final String playerSection;
    private final String uuid;
    private final Map<NamespacedKey, String> playerData = new HashMap<>();
    private boolean contains = false;
    private long gems = 0;
    private Locale locale;

    @SuppressWarnings("deprecation")
    private static NamespacedKey getKey(String key) {
        String[] data = NAMESPACED_KEY.split(key);
        assert data.length == 2;
        return new NamespacedKey(data[0], data[1]);
    }

    public static void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Could not close MySQL statement", e);
        }
    }

    @SuppressWarnings("SqlResolve")
    public void setup(Player player) throws SQLException {
        ResultSet set = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT gems, locale FROM " + playerSection + " WHERE uuid=? LIMIT 1;")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            set = preparedStatement.executeQuery();
            if (set.next()) {
                contains = true;
                setAmount(set.getLong(1));
                setLocale(Locale.forLanguageTag(set.getString(2)));
            } else {
                setAmount(0);
                Locale foundLocale = Locale.forLanguageTag(player.getLocale());
                if (foundLocale.getLanguage().isEmpty()) {
                    //Some resource packs can mess this up
                    foundLocale = Locale.getDefault();
                }
                setLocale(foundLocale);
            }
        } finally {
            if (set != null) {
                safeClose(set);
            }
        }
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT keys, values FROM " + playerSection + "data;");
             ResultSet resultSet = preparedStatement.executeQuery()) {

            while (resultSet.next()) {
                playerData.put(getKey(resultSet.getString(1)), resultSet.getString(2));
            }
        }
    }

    @Override
    public CompletableFuture<Long> getAmount() {
        CompletableFuture<Long> future = new CompletableFuture<>();
        new EnchantmentChain<>(uuid).execute(() -> future.complete(gems));
        return future;
    }

    @Override
    public void setAmount(long amount) {
        new EnchantmentChain<>(uuid).execute(() -> gems = amount);
    }

    @Override
    public void addAmount(long amount) {
        new EnchantmentChain<>(uuid).execute(() -> gems += amount);
    }

    @Override
    public void savePlayer(EnchantmentPlayer player) {
        save();
    }

    @SuppressWarnings("SqlResolve")
    private void save() {
        PreparedStatement statement = null;
        try {
            if (contains) {
                statement = connection.prepareStatement("UPDATE ? SET gems=?,locale=? WHERE uuid=? LIMIT 1;");
                statement.setString(1, playerSection);
                statement.setLong(2, gems);
                statement.setString(3, getLocale().toLanguageTag());
                statement.setString(4, uuid);
            } else {
                statement = connection.prepareStatement("INSERT INTO " + playerSection + " (uuid, gems, locale) VALUES (?, ?, ?);");
                statement.setString(1, uuid);
                statement.setLong(2, gems);
                statement.setString(3, getLocale().toLanguageTag());
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Problem updating MySQL table", e);
        } finally {
            if (statement != null) {
                safeClose(statement);
            }
        }
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public void setLocale(Locale language) {
        this.locale = language;
    }

    @Override
    public String name() {
        return "mysql";
    }

    @Override
    public void storePlayerData(NamespacedKey namespacedKey, String value) {
        playerData.put(namespacedKey, value);
    }

    @Override
    public String getPlayerData(NamespacedKey namespacedKey) {
        return playerData.get(namespacedKey);
    }

    @Override
    public void removePlayerData(NamespacedKey namespacedKey) {
        playerData.remove(namespacedKey);
    }
}
