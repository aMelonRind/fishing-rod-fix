package com.andrewchik.fishingrodfix;

import net.fabricmc.api.ClientModInitializer;
import org.joml.Matrix4f;

public class FishingRodFixClient implements ClientModInitializer {
	public static Matrix4f projection = new Matrix4f();

	@Override
	public void onInitializeClient() {}

}
