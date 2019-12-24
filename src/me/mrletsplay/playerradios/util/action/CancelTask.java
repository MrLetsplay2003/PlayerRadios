package me.mrletsplay.playerradios.util.action;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import me.mrletsplay.playerradios.Config;
import me.mrletsplay.playerradios.Main;

public class CancelTask implements Runnable {

	private Player p;
	private BukkitTask task;
	
	public CancelTask(Player p) {
		this.p = p;
	}
	
	public void schedule(int delayTicks) {
		task = Bukkit.getScheduler().runTaskLater(Main.pl, this, delayTicks);
	}
	
	public void cancel() {
		if(task != null) task.cancel();
	}
	
	@Override
	public void run() {
		cancelForPlayer(p);
	}
	
	public static void cancelForPlayer(Player p){
		if(PlayerActions.hasAction(p)){
			PlayerActions.removeAction(p);
			if(p.isOnline()) p.sendMessage(Config.getMessage("station.rename-cancelled"));
		}
	}

}
