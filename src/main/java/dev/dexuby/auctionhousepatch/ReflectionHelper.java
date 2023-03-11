package dev.dexuby.auctionhousepatch;

import com.spawnchunk.auctionhouse.modules.Auctions;
import com.spawnchunk.auctionhouse.modules.ListingType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class ReflectionHelper {

    public static final Method IS_SIMILAR_METHOD = getDeclaredMethodOrNull(Auctions.class, true, "isSimilar", ItemStack.class, ItemStack.class, String.class);
    public static final Method OPEN_CONFIRM_LISTING_MENU = getDeclaredMethodOrNull(Auctions.class, true, "openConfirmListingMenu", Player.class, ItemStack.class, float.class, double.class, ListingType.class);

    private static Method getDeclaredMethodOrNull(final Class<?> type, final boolean forceAccessible, final String name, final Class<?>... parameterTypes) {

        try {
            final Method method = type.getDeclaredMethod(name, parameterTypes);
            if (forceAccessible) method.setAccessible(true);
            return method;
        } catch (final NoSuchMethodException ex) {
            return null;
        }

    }

}
