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

import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.currency.CurrencyHandler;
import software.bigbade.enchantmenttokens.utils.SchedulerHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.logging.Level;

public class MySQLCurrencyHandler implements CurrencyHandler {
    private final Connection connection;
    private final String playerSection;
    private final SchedulerHandler scheduler;
    private boolean contains = false;

    private long gems = 0;
    private Locale locale;

    public MySQLCurrencyHandler(SchedulerHandler scheduler, Connection connection, String playerSection) {
        this.scheduler = scheduler;
        this.connection = connection;
        this.playerSection = playerSection;
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
                try {
                    this.locale = Locale.forLanguageTag(player.getLocale());
                } catch (NullPointerException e) {
                    //Some resource packs can mess this up
                    this.locale = Locale.getDefault();
                }
                setLocale(locale);
            }
        } finally {
            if (set != null)
                safeClose(set);
        }
    }

    @Override
    public long getAmount() {
        return gems;
    }

    @Override
    public void setAmount(long amount) {
        gems = amount;
    }

    @Override
    public void addAmount(long amount) {
        gems += amount;
    }

    @Override
    public void savePlayer(Player player, boolean async) {
        if(async) {
            scheduler.runTaskAsync(() -> save(player));
        } else {
            save(player);
        }
    }

    @SuppressWarnings("SqlResolve")
    private void save(Player player) {
        PreparedStatement statement = null;
        try {
            if (contains) {
                statement = connection.prepareStatement("UPDATE ? SET gems=?,locale=? WHERE uuid=? LIMIT 1;");
                statement.setString(1, playerSection);
                statement.setLong(2, getAmount());
                statement.setString(3, getLocale().toLanguageTag());
                statement.setString(4, player.getUniqueId().toString());
            } else {
                statement = connection.prepareStatement("INSERT INTO " + playerSection + " (uuid, gems, locale) VALUES (?, ?, ?);");
                statement.setString(1, player.getUniqueId().toString());
                statement.setLong(2, getAmount());
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

    public static void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Could not close MySQL statement", e);
        }
    }
}
