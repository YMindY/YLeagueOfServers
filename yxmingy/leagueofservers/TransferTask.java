package yxmingy.leagueofservers;
import cn.nukkit.scheduler.Task;
import cn.nukkit.Player;
import cn.nukkit.Server;
public class TransferTask extends Task{
  private Player player;
  private String cmd;
  public TransferTask(Player p,String c)
  {
    player = p;
    cmd = c;
  }
  public void onRun(int i)
  {
    Server.getInstance().dispatchCommand(player,cmd);
  }
}
