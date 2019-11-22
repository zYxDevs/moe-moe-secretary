package core;

import core.adapters.IWaifuAdapter;
import core.entities.FileWatcher;
import core.entities.Secretary;
import core.settings.Settings;
import core.utils.WaifuUtils;

import java.nio.file.Paths;

public class Main {

    private static Secretary secretary = null;

    private static void inizialize() {
        if (secretary != null) {
            secretary.close();
        }

        Settings.reload();

        String adapter = Settings.get("adapter", "Ship");
        String name = Settings.get("waifu.name", "");

        System.out.println("Starting " + adapter + " with name " + name);
        IWaifuAdapter waifu = WaifuUtils.getWaifuFromAdapterName(adapter, name);

        if (waifu != null) {
            try {
                secretary = new Secretary(waifu);
            } catch (Exception e) {
                System.out.println("Cannot start the waifu: " + name + " with adapter " + adapter);
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        inizialize();
        new FileWatcher(Paths.get(Settings.configPath), Main::inizialize).watch();
    }
}
