package org.geysermc.mcprotocollib.protocol.data.game.level.particle;

import org.cloudburstmc.math.vector.Vector3d;

public record TrailParticleData(Vector3d target, int color, int duration) implements ParticleData {
}
