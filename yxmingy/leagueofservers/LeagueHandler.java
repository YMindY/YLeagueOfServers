package yxmingy.leagueofservers;

import cn.nukkit.utils.PlayerDataSerializer;
import com.google.common.base.Preconditions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Optional;
import java.util.UUID;

public class LeagueHandler implements PlayerDataSerializer
{
  private final String path;
  public LeagueHandler(String path)
  {
    this.path = path;
  }
  @Override
  public Optional<InputStream> read (String name, UUID uuid) throws IOException {
    String path = this.path + "/players/" + name + ".dat";
    File file = new File(path);
    return !file.exists() ? Optional.empty() : Optional.of(new FileInputStream(file));
  }
  @Override
  public OutputStream write(String name, UUID uuid) throws IOException {
    Preconditions.checkNotNull(name, "name");
    String path = this.path + "/players/" + name + ".dat";
    File file = new File(path);
    return new FileOutputStream(file);
  }
}
