package io.github.pulverizer.movecraft.mixin;

import net.minecraft.server.management.PlayerChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(PlayerChunkMap.class)
public abstract class PlayerChunkMapMixin {

    @ModifyConstant(method = "setPlayerViewRadius", constant = @Constant(intValue = 32))
    private static int increaseViewLimit(int maxRadius) {
        return 64;
    }

}

