package dev.vegui.fabricapi;

import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;

public class FabricNetworkingApi implements ServerPostBootstrapEntrypoint {
	@Override
	public void onInitialize() {
		net.fabricmc.fabric.impl.networking.CommonPacketsImpl.init();
		net.fabricmc.fabric.impl.networking.NetworkingImpl.init();
	}
}
