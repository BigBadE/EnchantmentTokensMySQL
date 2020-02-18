package software.bigbade.enchantmenttokens;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.utils.ConfigurationManager;
import software.bigbade.enchantmenttokens.utils.EnchantLogger;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyFactory;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyHandler;

import java.sql.*;
import java.util.logging.Level;

public class MySQLCurrencyFactory implements CurrencyFactory {
    private Connection connection;
    private Statement statement;
    private String playerSection;

    public MySQLCurrencyFactory(EnchantmentTokens main, ConfigurationSection section) {
        try {
            String url = (String) ConfigurationManager.getValueOrDefault("database", section, "");
            String username = (String) ConfigurationManager.getValueOrDefault("username", section, null);
            String password = (String) ConfigurationManager.getValueOrDefault("password", section, null);

            connection = DriverManager.getConnection(url, username, password);

            statement = connection.createStatement();

            getDatabase(section);
        } catch (SQLException e) {
            EnchantLogger.log(Level.SEVERE, "Could not open connection to database", e);
        }
    }

    private void getDatabase(ConfigurationSection section) throws SQLException {
        playerSection = (String) ConfigurationManager.getValueOrDefault("section", section, "players");
        ResultSet result = statement.executeQuery("SELECT * FROM information_schema.TABLES WHERE TABLE_NAME = '" + playerSection + "' AND TABLE_SCHEMA = 'EnchantmentTokens' LIMIT 1;");
        if (!result.next())
            statement.execute("CREATE TABLE " + playerSection + "(gems LONG NOT NULL, uuid CHAR(36) NOT NULL);");
    }

    @Override
    public CurrencyHandler newInstance(Player player) {
        return new MySQLCurrencyHandler(player, statement, playerSection);
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
}
