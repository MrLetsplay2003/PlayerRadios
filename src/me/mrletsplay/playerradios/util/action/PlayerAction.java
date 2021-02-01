package me.mrletsplay.playerradios.util.action;

public abstract class PlayerAction {

	protected CancelTask cancelTask;
	
	public PlayerAction() {}
	
	public CancelTask getCancelTask() {
		return cancelTask;
	}
	
	public void removeCancelTask() {
		if(cancelTask != null) cancelTask.cancel();
	}
	
}
