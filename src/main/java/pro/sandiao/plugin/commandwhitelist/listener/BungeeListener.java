package pro.sandiao.plugin.commandwhitelist.listener;

import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.Connection;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.TabCompleteEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.protocol.packet.Commands;
import pro.sandiao.plugin.commandwhitelist.BungeeMain;

import java.lang.reflect.Field;

public class BungeeListener implements Listener {

    private final BungeeMain bungeeMain;

    public BungeeListener(BungeeMain bungeeMain) {
        this.bungeeMain = bungeeMain;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        Connection sender = event.getSender();
        if (event.isCommand() && sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            String message = event.getMessage();
            int i = message.indexOf(' ');
            if (i == -1) {
                i = message.length();
            }
            String command = message.substring(1, i);

            if (bungeeMain.getBungeeWhitelistManager().hasCommandWhitelist(player, command)) {
                return;
            }

            String xx = bungeeMain.getConfig().getString("command-whitelist.blocked-message");
            if (!xx.isEmpty()) {
                player.sendMessage(new TextComponent(ChatColor.translateAlternateColorCodes('&', xx)));
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        Connection sender = event.getSender();
        String cursor = event.getCursor();
        if (!cursor.isEmpty() && cursor.charAt(0) == '/' && sender instanceof ProxiedPlayer) {
            ProxiedPlayer player = (ProxiedPlayer) sender;
            int i = cursor.indexOf(' ');
            if (i == -1) {
                i = cursor.length();
            }
            String command = cursor.substring(1, i);
            if (!bungeeMain.getBungeeWhitelistManager().hasTabCompleteWhitelist(player, command)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onJoin(LoginEvent event) {
        PendingConnection connection = event.getConnection();
        if (connection instanceof InitialHandler) {
            try {
                Field chField = InitialHandler.class.getDeclaredField("ch");
                chField.setAccessible(true);
                ChannelWrapper channelWrapper = (ChannelWrapper) chField.get(connection);
                Channel channel = channelWrapper.getHandle();
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addAfter("packet-encoder", "cmdw-packet-listener", new ChannelDuplexHandler() {
                    @Override
                    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                        if (msg instanceof Commands) {
                            Commands commands = (Commands) msg;
                            ProxiedPlayer player = ProxyServer.getInstance().getPlayer(connection.getUniqueId());
                            if (player != null) {
                                RootCommandNode<?> root = commands.getRoot();
                                RootCommandNode<?> newRoot = new RootCommandNode<>();
                                for (CommandNode<?> child : root.getChildren()) {
                                    if (bungeeMain.getBungeeWhitelistManager().hasTabCompleteWhitelist(player, child.getName())) {
                                        newRoot.addChild((CommandNode) child);
                                    }
                                }
                                commands.setRoot(newRoot);
                            }
                        }

                        super.write(ctx, msg, promise);
                    }
                });
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
