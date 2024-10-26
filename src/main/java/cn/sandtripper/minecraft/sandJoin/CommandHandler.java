package cn.sandtripper.minecraft.sandJoin;

import com.velocitypowered.api.command.SimpleCommand;

public class CommandHandler implements SimpleCommand {

    private final SandJoin plugin;

    public CommandHandler(SandJoin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!invocation.source().hasPermission("sandjoin.admin")) {
            invocation.source().sendMessage(net.kyori.adventure.text.Component.text("你没有权限"));
            return;
        }

        String[] args = invocation.arguments();

        if (args.length == 0) {
            invocation.source().sendMessage(net.kyori.adventure.text.Component.text("未知的格式"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reload();
                invocation.source().sendMessage(net.kyori.adventure.text.Component.text("插件重载成功"));
                break;
            case "start":
                plugin.start();
                invocation.source().sendMessage(net.kyori.adventure.text.Component.text("插件启动！"));
                break;
            case "stop":
                plugin.stop();
                invocation.source().sendMessage(net.kyori.adventure.text.Component.text("插件已暂停！"));
                break;
            default:
                invocation.source().sendMessage(net.kyori.adventure.text.Component.text("未知的格式"));
                break;
        }
    }
}
