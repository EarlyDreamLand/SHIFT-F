package org.earlydreamland.shift_f;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Main extends JavaPlugin implements Listener {
    private boolean enabled;
    private List<String> commands;
    String PluginName = "Shift+F";

    @Override
    public void onEnable() {
        // 初始化配置
        initPlugin();

        getCommand("sf").setExecutor(this);
        getCommand("sf").setTabCompleter(this);

        getServer().getPluginManager().registerEvents(this, this);
    }

    private void initPlugin() {
        // 保存默认配置（如果不存在）
        saveDefaultConfig();
        // 重新加载配置
        reloadPluginConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length == 0) {
            return showHelp(sender);
        }

        String subCommand = args[0].toLowerCase();

        return switch (subCommand) {
            case "reload" -> handleReloadCommand(sender);
            case "version" -> handleVersionCommand(sender);
            default -> {
                sender.sendMessage("§6" + PluginName + " §7› §c未知命令！");
                yield true;
            }
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("sf")) {
            return null;
        }

        if (args.length == 1) {
            return getFirstLevelCompletions(args[0]);
        }

        return Collections.emptyList();
    }

    private boolean showHelp(CommandSender sender) {
        sender.sendMessage("§6===== " + PluginName + " 插件帮助 =====");
        sender.sendMessage("§a/sf reload §7- 重载插件配置");
        sender.sendMessage("§a/sf version §7- 显示插件版本");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender) {
        if (!sender.hasPermission("shiftf.reload")) {
            sender.sendMessage("§c你没有权限执行此命令！");
            return true;
        }

        reloadPluginConfig();
        sender.sendMessage("§6" + PluginName + " §7› §a配置已成功重载！");

        sender.sendMessage("§7插件状态: " + (enabled ? "§a启用" : "§c禁用"));

        // 显示加载的命令列表
        if (!commands.isEmpty()) {
            sender.sendMessage("§7已加载命令：");
            for (String cmd : commands) {
                sender.sendMessage("§8- §7" + cmd);
            }
        } else {
            sender.sendMessage("§7未加载任何命令");
        }

        return true;
    }

    private boolean handleVersionCommand(CommandSender sender) {
        PluginDescriptionFile description = getDescription();
        String pluginVersion = description.getVersion();
        List<String> pluginAuthors = description.getAuthors();

        sender.sendMessage("§6插件版本：§a" + pluginVersion);
        sender.sendMessage("§6开发作者：§a" + pluginAuthors);
        sender.sendMessage("§6项目地址：§ahttps://github.com/EarlyDreamLand/SHIFT-F");
        return true;
    }

    private List<String> getFirstLevelCompletions(String input) {
        List<String> commands = Arrays.asList("reload", "version");
        return filterCompletions(commands, input);
    }

    private List<String> filterCompletions(List<String> options, String input) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(options);
        }

        String lowerInput = input.toLowerCase();
        return options.stream()
                .filter(option -> option.toLowerCase().startsWith(lowerInput))
                .sorted()
                .collect(Collectors.toList());
    }

    private void reloadPluginConfig() {
        reloadConfig();

        enabled = getConfig().getBoolean("enable", true);
        commands = getConfig().getStringList("commands");

        getLogger().info(PluginName + " 插件" + (enabled ? "已启用" : "已禁用") + "！");
        getLogger().info("已加载 " + commands.size() + " 个命令");
    }

    @EventHandler
    public void onShiftF(PlayerSwapHandItemsEvent event) {
        if (!enabled) return; // 确保插件已启用

        Player player = event.getPlayer();

        // 添加权限检查
        if (!player.hasPermission("shiftf.use")) {
            return;
        }

        if (player.isSneaking()) { // 检测 Shift 键
            event.setCancelled(true); // 取消默认的物品交换动作

            // 执行配置中的命令
            for (String cmd : commands) {
                boolean success = executeCommand(cmd, player);

                if (!success) {
                    player.sendMessage("§6" + PluginName + " §7› §c执行命令时出错：" + cmd);
                }
            }
        }
    }

    private boolean executeCommand(String rawCommand, Player player) {
        try {
            if (rawCommand.startsWith("[player]")) {
                String actualCmd = rawCommand.substring(8).trim()
                        .replace("{player}", player.getName());
                return player.performCommand(actualCmd);
            } else if (rawCommand.startsWith("[console]")) {
                String actualCmd = rawCommand.substring(9).trim()
                        .replace("{player}", player.getName());
                return getServer().dispatchCommand(getServer().getConsoleSender(), actualCmd);
            }
            return false;
        } catch (Exception e) {
            getLogger().warning("执行命令时出错：" + rawCommand);
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void onDisable() {
        getLogger().info(PluginName + " 插件已禁用！");
    }
}