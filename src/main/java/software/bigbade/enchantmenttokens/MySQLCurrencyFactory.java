package software.bigbade.enchantmenttokens;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.utils.configuration.ConfigurationManager;
import software.bigbade.enchantmenttokens.utils.EnchantLogger;
import software.bigbade.enchantmenttokens.utils.configuration.ConfigurationType;
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
            String url = new ConfigurationType<String>("").getValue("database", section);
            String username = new ConfigurationType<String>(null).getValue("username", section);
            String password = new ConfigurationType<String>(null).getValue("password", section);

            connection = DriverManager.getConnection(url, username, password);

            statement = connection.createStatement();

            getDatabase(section);
        } catch (SQLException e) {
            EnchantLogger.log(Level.SEVERE, "Could not open connection to database", e);
        }
    }

    private void getDatabase(ConfigurationSection section) throws SQLException {
        playerSection = new ConfigurationType<String>("players").getValue("section", section);
        ResultSet result = statement.executeQuery("SELECT * FROM information_schema.TABLES WHERE TABLE_NAME = '" + playerSection + "' AND TABLE_SCHEMA = 'EnchantmentTokens' LIMIT 1;");
        if (!result.next())
            statement.execute("CREATE TABLE " + playerSection + "(gems LONG NOT NULL, uuid CHAR(36) NOT NULL);");
        result.close();
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
