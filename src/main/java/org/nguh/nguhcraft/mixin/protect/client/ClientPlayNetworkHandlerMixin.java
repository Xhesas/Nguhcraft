package org.nguh.nguhcraft.mixin.protect.client;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.client.ClientProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.protect.ProtectionManagerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin implements ProtectionManagerAccess {
    @Unique private ProtectionManager Manager = ClientProtectionManager.EMPTY;
    @Override public @NotNull ProtectionManager Nguhcraft$GetProtectionManager() { return Manager; }
    @Override public void Nguhcraft$SetProtectionManager(@NotNull ProtectionManager Mgr) { Manager = Mgr; }
}
