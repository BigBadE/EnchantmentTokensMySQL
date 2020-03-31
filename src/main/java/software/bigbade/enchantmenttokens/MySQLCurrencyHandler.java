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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;

public class MySQLCurrencyHandler implements CurrencyHandler {
    private Connection connection;
    private String playerSection;
    private boolean contains = false;

    long gems = 0;

    @SuppressWarnings("SqlResolve")
    public MySQLCurrencyHandler(Player player, Connection connection, String playerSection) throws SQLException {
        this.connection = connection;
        this.playerSection = playerSection;
        ResultSet set = null;
        try (PreparedStatement preparedStatement = connection.prepareStatement("SELECT gems FROM " + playerSection + " WHERE uuid=? LIMIT 1;")) {
            preparedStatement.setString(1, player.getUniqueId().toString());
            set = preparedStatement.executeQuery();
            if (set.next()) {
                contains = true;
                setAmount(set.getLong(1));
            } else {
                setAmount(0);
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

    @SuppressWarnings("SqlResolve")
    @Override
    public void savePlayer(Player player, boolean async) {
        PreparedStatement statement = null;
        try {
            if (contains) {
                statement = connection.prepareStatement("UPDATE ? SET gems=? WHERE uuid=? LIMIT 1;");
                statement.setString(1, playerSection);
                statement.setLong(2, getAmount());
                statement.setString(3, player.getUniqueId().toString());
            } else {
                statement = connection.prepareStatement("INSERT INTO " + playerSection + " (uuid, gems) VALUES (?, ?);");
                statement.setString(1, player.getUniqueId().toString());
                statement.setLong(2, getAmount());
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
