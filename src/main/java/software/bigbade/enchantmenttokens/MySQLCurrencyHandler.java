package software.bigbade.enchantmenttokens;

import org.bukkit.entity.Player;
import software.bigbade.enchantmenttokens.utils.currency.CurrencyHandler;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLCurrencyHandler implements CurrencyHandler {
    private long gems;
    private Statement statement;
    private String playerSection;
    private boolean contains = false;

    public MySQLCurrencyHandler(Player player, Statement statement, String playerSection) {
        this.statement = statement;
        this.playerSection = playerSection;
        try {
            ResultSet set = statement.executeQuery("SELECT * FROM " + playerSection + " WHERE uuid='" + player.getUniqueId() + "' LIMIT 1;");
            if (set.next()) {
                contains = true;
                //set.beforeFirst();
                gems = set.getLong(1);
            } else {
                gems = 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
    public void savePlayer(Player player) {
        try {
            if (contains)
                statement.execute("UPDATE " + playerSection + " SET gems=" + gems + " WHERE uuid='" + player.getUniqueId() + "' LIMIT 1;");
            else
                statement.execute("INSERT INTO " + playerSection + "(uuid, gems) VALUES ('" + player.getUniqueId() + "', '" + gems + "');");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String name() {
        return "mysql";
    }
}
