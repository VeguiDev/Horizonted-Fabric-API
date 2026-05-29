package dev.vegui.hfa.projection;

import dev.vegui.hfa.impl.client.projection.v0.ClientProjectionAnalyzer;
import io.canvasmc.horizon.service.entrypoint.ServerPostBootstrapEntrypoint;

public final class FabricClientProjectionApi implements ServerPostBootstrapEntrypoint {
	@Override
	public void onInitialize() {
		ClientProjectionAnalyzer.analyze();
	}
}
