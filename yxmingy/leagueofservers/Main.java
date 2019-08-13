package yxmingy.leagueofservers;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerJoinEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.event.server.PlayerDataSerializeEvent;
import cn.nukkit.utils.PlayerDataSerializer;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.command.*;
import me.onebone.economyapi.EconomyAPI;
import java.io.OutputStream;
import java.util.UUID;
import java.nio.ByteOrder;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.Server;
import java.util.HashSet;
import org.itxtech.synapseapi.SynapsePlayer;

import java.util.List;
import java.util.Random;


import java.io.*;

public class Main extends PluginBase implements Listener
{
  private Config conf;
  private boolean is_host;
  private PlayerDataSerializer handler;
  private String pdatas;
  //synapsetools
  Config c;
  public static HashSet<String> transfer;
  @Override
  public void onLoad()
  {
    getLogger().info("YLeagueOfServers is loading...");
    transfer = new HashSet<String>();
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
    handler = is_host ? getServer().getPlayerDataSerializer() : new LeagueHandler(String.valueOf(conf.get("主服务器主目录")));
    
    //synapsetools>>
        saveResource("synapsetools.yml");
        c = new Config(getDataFolder()+"/synapsetools.yml",Config.YAML);
        if (c.getBoolean("enableFoodBarHack")) {
            getServer().getScheduler().scheduleRepeatingTask(new cn.nukkit.scheduler.Task() {
                @Override
                public void onRun(int i) {
                    try {
                        for (Player p : Server.getInstance().getOnlinePlayers().values()) {
                            p.getFoodData().sendFoodLevel();
                        }
                    } catch (Exception ignore) {}
                }
            }, 1, true);
        }
    //<<synapsetools
    
    getLogger().info("YLeagueOfServers is enabled! auther: xMing (Included synapsetools' part source)");
    
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
  private void transferPlayer(SynapsePlayer player,String target)
  {
    savePlayerData(player);
    transfer.add(player.getName());
    player.sendMessage("保存数据中...在3秒后跨服");
    getServer().getScheduler().scheduleDelayedTask(new TransferTask(player,target),60);
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
      player.save();
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
  
  //synapsetools>>
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof SynapsePlayer) {
            SynapsePlayer p = (SynapsePlayer) sender;
            if (command.getName().equalsIgnoreCase("transfer")) {
                if (c.getBoolean("transferCommandEnabled")) {
                    if (args.length > 0) {
                        if (p.getSynapseEntry().getServerDescription().equals(args[0])) {
                            p.sendMessage("\u00A7cYou are already on this server");
                        } else {
                            transferPlayer(p,args[0]);
                        }
                    } else {
                        p.sendMessage("Usage: /transfer <target>");
                    }
                } else {
                    return false;
                }
            } else if (command.getName().equalsIgnoreCase("hub") || command.getName().equalsIgnoreCase("lobby")) {
                if (c.getBoolean("hubCommandEnabled")) {
                    List<String> l = c.getStringList("lobbiesForThisServer");
                    if (l.size() == 0) {
                        p.sendMessage("\u00A7cThere is no lobbies set for this server");
                        return true;
                    }
                    if (!l.contains(p.getSynapseEntry().getServerDescription())) {
                        transferPlayer(p,l.get(new Random().nextInt(l.size())));
                    } else {
                        p.sendMessage("\u00A7cYou are already on a lobby server");
                    }
                } else {
                    return false;
                }
            }
        }

        return true;
    }
    //<<synapsetools
    
}
