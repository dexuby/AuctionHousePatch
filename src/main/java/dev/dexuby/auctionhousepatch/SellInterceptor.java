package dev.dexuby.auctionhousepatch;

import com.spawnchunk.auctionhouse.AuctionHouse;
import com.spawnchunk.auctionhouse.config.Config;
import com.spawnchunk.auctionhouse.modules.Auctions;
import com.spawnchunk.auctionhouse.modules.Economy;
import com.spawnchunk.auctionhouse.modules.ListingType;
import com.spawnchunk.auctionhouse.util.ItemUtil;
import com.spawnchunk.auctionhouse.util.MessageUtil;
import com.spawnchunk.auctionhouse.util.TimeUtil;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class SellInterceptor {

    @RuntimeType
    public static void intercept(@AllArguments Object[] allArguments) throws InvocationTargetException, IllegalAccessException {

        assert ReflectionHelper.IS_SIMILAR_METHOD != null;
        assert ReflectionHelper.OPEN_CONFIRM_LISTING_MENU != null;

        final Player player = (Player) allArguments[0];
        final float price = (float) allArguments[1];
        final Integer count = (Integer) allArguments[2];
        final ListingType type = (ListingType) allArguments[3];

        if (!player.isOnline() || !player.isValid()) return;

        boolean bypass = player.isOp() && type.isServer();
        if (player.getGameMode().equals(GameMode.CREATIVE) && Config.auction_prevent_creative) {
            MessageUtil.sendMessage(player, "warning.sell.creative", Config.locale);
        } else if (player.getGameMode().equals(GameMode.SPECTATOR) && Config.auction_prevent_spectator) {
            MessageUtil.sendMessage(player, "warning.sell.spectator", Config.locale);
        } else {
            final PlayerInventory inventory = player.getInventory();
            final ItemStack item = inventory.getItemInMainHand();
            if (!item.getType().isAir()) {
                if (!bypass) {
                    for (String section : Config.restricted_items.keySet()) {
                        ItemStack prohibited = Config.restricted_items.get(section);
                        if ((boolean) ReflectionHelper.IS_SIMILAR_METHOD.invoke(null, item, prohibited, section)) {
                            MessageUtil.sendMessage(player, "warning.sell.restricted_item", Config.locale);
                            return;
                        }
                    }
                }

                if (Config.auction_prevent_filled_containers && ItemUtil.isFilledContainer(item) && !bypass) {
                    MessageUtil.sendMessage(player, "warning.sell.filled_container", Config.locale);
                } else if (!Config.auction_allow_damaged_items && ItemUtil.hasDamage(item) && !bypass) {
                    MessageUtil.sendMessage(player, "warning.sell.damaged_item", Config.locale);
                } else if ((double) price > Config.auction_max_sell_price && !bypass) {
                    MessageUtil.sendMessage(player, "warning.sell.max_price", Config.locale, Config.auction_max_sell_price);
                } else if ((double) price < Config.auction_min_sell_price && !bypass) {
                    MessageUtil.sendMessage(player, "warning.sell.min_price", Config.locale, Config.auction_min_sell_price);
                } else if (price < 0.0F || !Float.isFinite(price)) {
                    MessageUtil.sendMessage(player, "warning.sell.negative_price", Config.locale);
                } else if (count != null && (count <= 0 || count > item.getAmount())) {
                    MessageUtil.sendMessage(player, "warning.sell.invalid_amount", Config.locale);
                } else {
                    Auctions.updateCounts(player);
                    int listings = Auctions.getPlayerListingsCount(player);
                    int max_listings = Auctions.getMaxListings(player);
                    if (max_listings == 0 && !player.isOp()) {
                        MessageUtil.sendMessage(player, "warning.sell.unavailable", Config.locale);
                    } else if (listings >= max_listings && !bypass) {
                        MessageUtil.sendMessage(player, "warning.sell.max_listings", Config.locale, max_listings);
                    } else {
                        UUID uuid = player.getUniqueId();
                        if (AuctionHouse.playerCooldowns.containsKey(uuid) && !bypass) {
                            long lastListing = AuctionHouse.playerCooldowns.get(uuid);
                            long nextListing = lastListing + Config.auction_listing_cooldown;
                            if (TimeUtil.now() < nextListing) {
                                long remaining = nextListing - TimeUtil.now();
                                MessageUtil.sendMessage(player, "warning.listing.cooldown", Config.locale, TimeUtil.duration(remaining, true));
                                return;
                            }
                        }

                        double listing_fee = bypass ? 0.0D : (double) price * (Config.auction_listing_rate / 100.0D) + Config.auction_listing_price;
                        OfflinePlayer seller = AuctionHouse.plugin.getServer().getOfflinePlayer(player.getUniqueId());
                        String world = player.getWorld().getName();
                        if (listing_fee > 0.0D) {
                            double balance = Economy.getBalance(seller, world);
                            if (listing_fee > balance) {
                                MessageUtil.sendMessage(player, "warning.sell.insufficient_funds", Config.locale);
                                if (Config.debug) {
                                    AuctionHouse.logger.info(String.format("listing_fee = %.2f", listing_fee));
                                }

                                if (Config.debug) {
                                    AuctionHouse.logger.info(String.format("balance = %.2f", balance));
                                }

                                return;
                            }
                        }

                        Auctions.lock.add(player.getUniqueId());
                        if (count != null && item.getAmount() != count) {
                            if (item.getAmount() <= count) {
                                MessageUtil.sendMessage(player, "warning.sell.invalid_amount", Config.locale);
                                Auctions.lock.remove(player.getUniqueId());
                                return;
                            }

                            ItemStack is = inventory.getItemInMainHand().clone();
                            is.setAmount(is.getAmount() - count);
                            inventory.setItemInMainHand(is);
                            item.setAmount(count);
                        } else {
                            inventory.setItemInMainHand(new ItemStack(Material.AIR, 1));
                        }

                        if (listing_fee > 0.0D) {
                            ReflectionHelper.OPEN_CONFIRM_LISTING_MENU.invoke(null, player, item, price, listing_fee, type);
                        } else {
                            Auctions.completeListing(player, item, price, listing_fee, type, false);
                        }

                    }
                }
            } else {
                MessageUtil.sendMessage(player, "warning.sell.no_item", Config.locale);
            }
        }

    }

}
