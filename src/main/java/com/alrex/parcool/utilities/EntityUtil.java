package com.alrex.parcool.utilities;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;

public class EntityUtil {
	public static void addVelocity(Entity entity, Vector3d vec) {
		entity.setDeltaMovement(entity.getDeltaMovement().add(vec));
	}
}
