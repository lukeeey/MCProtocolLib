package org.geysermc.mcprotocollib.protocol.data.game.item.component;

import java.util.List;

public record CustomModelData(List<Float> floats, List<Boolean> flags, List<String> strings, List<Integer> colors) {
}
