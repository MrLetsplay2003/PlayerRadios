package me.mrletsplay.playerradios;

import org.bukkit.entity.Player;

public class CancelTask implements Runnable{

	private Player p;
	
	public CancelTask(Player pl) {
		p = pl;
	}
	
	@Override
	public void run() {
		cancelForPlayer(p);
	}
	
	public static void cancelForPlayer(Player p){
		if(Events.typing.containsKey(p)){
			Events.typing.remove(p);
			if(p.isOnline()) {
				p.sendMessage(Config.getMessage("station.rename-cancelled"));
			}
		}
	}

}
