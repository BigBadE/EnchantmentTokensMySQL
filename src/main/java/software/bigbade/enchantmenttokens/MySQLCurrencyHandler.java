package software.bigbade.enchantmenttokens;

import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.utils.EnchantLogger;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyHandler;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MySQLCurrencyHandler implements CurrencyHandler {
    private long gems;
    private Connection connection;
    private String playerSection;
    private boolean contains = false;

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
                gems = set.getLong(1);
            } else {
                gems = 0;
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
    public void savePlayer(Player player) {
        PreparedStatement statement = null;
        try {
            if (contains) {
                statement = connection.prepareStatement("UPDATE ? SET gems=? WHERE uuid=? LIMIT 1;");
                statement.setString(1, playerSection);
                statement.setLong(2, gems);
                statement.setString(3, player.getUniqueId().toString());
            } else {
                statement = connection.prepareStatement("INSERT INTO " + playerSection + " (uuid, gems) VALUES (?, ?);");
                statement.setString(1, player.getUniqueId().toString());
                statement.setLong(2, gems);
            }
            statement.executeUpdate();
        } catch (SQLException e) {
            EnchantLogger.log("Problem updating MySQL table", e);
        } finally {
            if (statement != null) {
                safeClose(statement);
            }
        }
    }

    public static void safeClose(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            EnchantLogger.log("Could not close MySQL statement", e);
        }
    }

    @Override
    public String name() {
        return "mysql";
    }
}
