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
