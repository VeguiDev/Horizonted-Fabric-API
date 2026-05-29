package dev.vegui.hfa.networking;

import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;

public final class FabricNetworkingApi implements ServerPostBootstrapEntrypoint {
	@Override
	public void onInitialize() {
		net.fabricmc.fabric.impl.networking.CommonPacketsImpl.init();
		net.fabricmc.fabric.impl.networking.NetworkingImpl.init();
	}
}
