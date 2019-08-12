package yxmingy.leagueofservers;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.PlayerDataSerializeEvent;
import cn.nukkit.event.player.PlayerCommandPreprocessEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import me.onebone.economyapi.EconomyAPI;
import java.io.OutputStream;
import java.util.UUID;
import java.nio.ByteOrder;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.Server;
import java.util.HashSet;

import java.io.*;

public class Main extends PluginBase implements Listener
{
  private Config conf;
  private boolean is_host;
  private LeagueHandler handler;
  private String pdatas;
  public static HashSet<String> transfer;
  @Override
  public void onLoad()
  {
    getLogger().info("YLeagueOfServers is loading...");
    conf = new Config(getDataFolder()+"/config.yml",Config.YAML);
    if(conf.getAll().isEmpty()) {
      conf.set("是否为主服务器", "否");
      conf.set("主服务器主目录", "");
      conf.save();
      getLogger().notice("已产生配置文件！ 请先修改配置文件！");
      getServer().forceShutdown();
    }
    is_host = "否".contentEquals(""+conf.get("是否为主服务器")) ? false : true;
    if(is_host) {
      File pdataf = new File(getDataFolder()+"/playerdatas");
      pdataf.mkdirs();
      pdatas = pdataf+"/";
    }else {
      if((""+conf.get("主服务器主目录")).contentEquals("")) {
        getLogger().notice("请先填写配置文件！");
        getServer().forceShutdown();
      }else {
        pdatas = conf.get("主服务器主目录") + "/plugins/YLeagueOfServers/playerdatas/";
      }
    }
  }
  @Override
  public void onEnable()
  {
    getServer().getPluginManager().registerEvents(this,this);
    handler = new LeagueHandler(String.valueOf(conf.get("主服务器主目录")));
    getLogger().info("YLeagueOfServers is enabled! auther: xMing");
  }
  @Override
  public void onDisable()
  {
    getLogger().warning("YLeagueOfServers is disabled");
  }
  @EventHandler
  public void onPlayerDataHandle(PlayerDataSerializeEvent event)
  {
    if(!is_host) {
      event.setSerializer(handler);
    }
  }
  @EventHandler
  public void onJoin(PlayerJoinEvent event)
  {
    Player player = event.getPlayer();
    Config playerdata = new Config(pdatas + player.getName() + ".yml", Config.YAML);
    if(is_host) {
      if (playerdata.getAll().isEmpty()) {
        playerdata.set("钱数", EconomyAPI.getInstance().myMoney(player));
        playerdata.save();
      }else {
        EconomyAPI.getInstance().setMoney(player,Double.parseDouble(""+playerdata.get("钱数")));
      }
    }else {
      EconomyAPI.getInstance().setMoney(player,Double.parseDouble(""+playerdata.get("钱数")));
    }
  }
  @EventHandler
  public void onCommand(PlayerCommandPreprocessEvent event)
  {
    if(!"/".contentEquals(event.getMessage().substring(0,1)))
      return;
    String cmd = event.getMessage().substring(1);
    if("transfer".contentEquals(cmd.substring(0,8))) {
      Player player = event.getPlayer();
      savePlayerData(player);
      event.setCancelled();
      transfer.add(player.getName());
      player.sendMessage("检测到你使用了跨服指令，保存数据中...指令在3秒后执行");
      getServer().getScheduler().scheduleDelayedTask(new TransferTask(player,cmd),60);
    }
  }
  @EventHandler
  public void onQuit(PlayerQuitEvent event)
  {
    Player player = event.getPlayer();
    savePlayerData(player);
    if(transfer.remove(player.getName())) {
      event.setQuitMessage("");
    }
  }
  private void savePlayerData(Player player) {
    try {
      Config playerdata = new Config(pdatas + player.getName() + ".yml", Config.YAML);
      playerdata.set("钱数",EconomyAPI.getInstance().myMoney(player));
      playerdata.save();
      UUID uuid = player.getUniqueId();
      CompoundTag tag = player.namedTag;
      String name = uuid.toString().toLowerCase();
      OutputStream dataStream = handler.write(name, uuid);
      NBTIO.writeGZIPCompressed(tag, dataStream, ByteOrder.BIG_ENDIAN);
    } catch (Exception e) {
      Server.getInstance().getLogger().error(Server.getInstance().getLanguage().translateString("nukkit.data.saveError", player.namedTag.toString().toLowerCase(), e));
    }
  }
}
