package net.sourceforge.offroad.actions;

import java.awt.event.ActionEvent;

import net.osmand.plus.routing.RouteProvider.RouteService;
import net.sourceforge.offroad.OsmWindow;
import net.sourceforge.offroad.actions.OffRoadAction.SelectableAction;

public class ChooseRouteServiceAction extends OffRoadAction implements SelectableAction {

	
	private RouteService mService;


	public ChooseRouteServiceAction(OsmWindow pContext, RouteService pService) {
		super(pContext, pContext.getOffRoadString("offroad.routing_service_"+pService.name()), null);
		mService = pService;
		
	}

	@Override
	public boolean isSelected() {
		return mService.equals(mContext.getSettings().ROUTER_SERVICE.get());
	}
	
	@Override
	public void actionPerformed(ActionEvent pE) {
		mContext.getSettings().ROUTER_SERVICE.set(mService);
	}


	@Override
	public void save() {
	}
}