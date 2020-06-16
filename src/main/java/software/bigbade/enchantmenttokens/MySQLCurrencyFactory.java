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

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.api.wrappers.EnchantmentChain;
import software.bigbade.enchantmenttokens.configuration.ConfigurationType;
import software.bigbade.enchantmenttokens.currency.CurrencyFactory;
import software.bigbade.enchantmenttokens.currency.CurrencyHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class MySQLCurrencyFactory implements CurrencyFactory {
    private Connection connection;
    private String playerSection;
    private boolean loaded;

    public MySQLCurrencyFactory(ConfigurationSection section) {
        new EnchantmentChain().async(() -> {
            try {
                String url = "jdbc:" + new ConfigurationType<>("").getValue("database", section);
                String username = new ConfigurationType<>("").getValue("username", section);
                String password = new ConfigurationType<>("").getValue("password", section);

                connection = DriverManager.getConnection(url, username, password);

                getDatabase(section);

                loaded = true;
            } catch (SQLException e) {
                EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Could not open connection to database", e);
                loaded = false;
            }
        }).execute();
    }

    @SuppressWarnings("SqlResolve")
    private void getDatabase(ConfigurationSection section) throws SQLException {
        playerSection = new ConfigurationType<>("players").getValue("section", section);
        if (Pattern.matches("[^a-zA-Z\\d\\s:]", playerSection)) {
            loaded = false;
            EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "NON-ALPHANUMERIC CHARACTER DETECTED. POSSIBLE MYSQL INJECTION!");
            return;
        }
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM information_schema.TABLES WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'EnchantmentTokens' LIMIT 1;")) {
            statement.setString(1, playerSection);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                try (PreparedStatement createTable = connection.prepareStatement("CREATE TABLE " + playerSection + "(uuid CHAR(36) NOT NULL, gems LONG NOT NULL, locale CHAR(36) NOT NULL);")) {
                    createTable.executeUpdate();
                }
            }
        } finally {
            if (resultSet != null)
                MySQLCurrencyHandler.safeClose(resultSet);
        }
    }

    @Override
    public CurrencyHandler newInstance(Player player) {
        MySQLCurrencyHandler handler = new MySQLCurrencyHandler(connection, playerSection, player.getUniqueId().toString());
        new EnchantmentChain().async(() -> {
            try {
                handler.setup(player);
            } catch (SQLException e) {
                EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Could not setup MySQL currency handler", e);
            }
        }).execute();
        return handler;
    }

    @Override
    public String name() {
        return "mysql";
    }

    @Override
    public void shutdown() {
        try {
            connection.close();
        } catch (SQLException e) {
            EnchantmentTokens.getEnchantLogger().log(Level.SEVERE, "Problem stopping database connection", e);
        }
    }

    @Override
    public boolean loaded() {
        return loaded;
    }
}
