package pro.sandiao.plugin.commandwhitelist;

import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import pro.sandiao.plugin.commandwhitelist.listener.BungeeListener;
import pro.sandiao.plugin.commandwhitelist.manager.BungeeWhitelistManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

public class BungeeMain extends Plugin {

    private Configuration config;
    private Configuration groupConfig;
    private BungeeWhitelistManager bungeeWhitelistManager;

    @Override
    public void onEnable() {

        File configFile = new File(getDataFolder(), "config.yml");
        File groupFile = new File(getDataFolder(), "group.yml");

        config = saveDefaultConfig(configFile);
        groupConfig = saveDefaultConfig(groupFile);

        bungeeWhitelistManager = new BungeeWhitelistManager(this);

        getProxy().getPluginManager().registerListener(this, new BungeeListener(this));
    }

    public Configuration getConfig() {
        return config;
    }

    public Configuration getGroupConfig() {
        return groupConfig;
    }

    public BungeeWhitelistManager getBungeeWhitelistManager() {
        return bungeeWhitelistManager;
    }

    private Configuration saveDefaultConfig(File file) {
        ConfigurationProvider provider = ConfigurationProvider.getProvider(YamlConfiguration.class);
        try {
            if (!file.exists()) {
                try (InputStream resource = getResourceAsStream(file.getName())) {
                    file.getParentFile().mkdirs();
                    Files.copy(resource, file.toPath());
                }
            }
            return provider.load(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
