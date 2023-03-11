package dev.dexuby.auctionhousepatch;

import com.spawnchunk.auctionhouse.modules.Auctions;
import com.spawnchunk.auctionhouse.modules.ListingType;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.MethodDelegation;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

public class AuctionHousePatch extends JavaPlugin implements CommandExecutor {

    @Override
    public void onLoad() {

        ByteBuddyAgent.install();

        new ByteBuddy()
                .redefine(Auctions.class)
                .method(named("sellItemInHand").and(takesArguments(Player.class, float.class, Integer.class, ListingType.class)))
                .intercept(MethodDelegation.to(SellInterceptor.class))
                .make()
                .load(Auctions.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());

    }

}
