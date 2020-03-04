package software.bigbade.enchantmenttokens;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.utils.EnchantLogger;
import software.bigbade.enchantmenttokens.utils.configuration.ConfigurationType;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyFactory;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyHandler;

import java.sql.*;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class MySQLCurrencyFactory implements CurrencyFactory {
    private Connection connection;
    private String playerSection;
    private boolean loaded;

    public MySQLCurrencyFactory(ConfigurationSection section) {
        try {
            String url = "jdbc:" + new ConfigurationType<>("").getValue("database", section);
            String username = new ConfigurationType<>("").getValue("username", section);
            String password = new ConfigurationType<>("").getValue("password", section);

            connection = DriverManager.getConnection(url, username, password);

            getDatabase(section);

            loaded = true;
        } catch (SQLException e) {
            EnchantLogger.log("Could not open connection to database", e);
            loaded = false;
        }
    }

    private void getDatabase(ConfigurationSection section) throws SQLException {
        playerSection = new ConfigurationType<String>("players").getValue("section", section);
        if(Pattern.matches("[^a-zA-Z\\d\\s:]", playerSection)) {
            loaded = false;
            EnchantLogger.log(Level.SEVERE, "NON-ALPHANUMERIC CHARACTER DETECTED. POSSIBLE MYSQL INJECTION!");
            return;
        }
        ResultSet resultSet = null;
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM information_schema.TABLES WHERE TABLE_NAME = ? AND TABLE_SCHEMA = 'EnchantmentTokens' LIMIT 1;")) {
            statement.setString(1, playerSection);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                try(PreparedStatement createTable = connection.prepareStatement("CREATE TABLE " + playerSection + "(gems LONG NOT NULL, uuid CHAR(36) NOT NULL);")) {
                    createTable.executeUpdate();
                }
            }
        } finally {
            if(resultSet != null)
                MySQLCurrencyHandler.safeClose(resultSet);
        }
    }

    @Override
    public CurrencyHandler newInstance(Player player) {
        try {
            return new MySQLCurrencyHandler(player, connection, playerSection);
        } catch (SQLException e) {
            EnchantLogger.log("Problem initializing MySQL", e);
        }
        return null;
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
            EnchantLogger.log("Problem stopping database", e);
        }
    }

    @Override
    public boolean loaded() {
        return loaded;
    }
}
