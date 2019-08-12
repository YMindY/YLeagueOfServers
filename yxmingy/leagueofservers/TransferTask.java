package yxmingy.leagueofservers;
import cn.nukkit.scheduler.Task;
import org.itxtech.synapseapi.SynapsePlayer;
import cn.nukkit.Server;
public class TransferTask extends Task{
  private SynapsePlayer player;
  private String target;
  public TransferTask(SynapsePlayer p,String c)
  {
    player = p;
    target = c;
  }
  public void onRun(int i)
  {
    if(!player.transferByDescription(target))
    {
      player.sendMessage("跨服失败");
      Main.transfer.remove(player.getName());
    }else {
      Server.getInstance().broadcastMessage("玩家"+player.getName()+"即将传送到"+target+"服务器");
    }
  }
}
