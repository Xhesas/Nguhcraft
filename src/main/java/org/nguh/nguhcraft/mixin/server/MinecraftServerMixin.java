package org.nguh.nguhcraft.mixin.server;

import net.minecraft.server.MinecraftServer;
import org.nguh.nguhcraft.protect.ProtectionManagerAccessor;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.DisplayManager;
import org.nguh.nguhcraft.server.MCBASIC;
import org.nguh.nguhcraft.server.ServerProtectionManager;
import org.nguh.nguhcraft.server.accessors.ManagerAccessor;
import org.nguh.nguhcraft.server.accessors.ProcedureManagerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin implements
    ManagerAccessor,
    ProcedureManagerAccessor,
    ProtectionManagerAccessor
{
    @Unique private final ProtectionManager ProtManager = new ServerProtectionManager();
    @Unique private final MCBASIC.ProcedureManager ProcManager = new MCBASIC.ProcedureManager();
    @Unique private final DisplayManager DisplayMgr = new DisplayManager((MinecraftServer)(Object)(this));

    @Override public ProtectionManager Nguhcraft$GetProtectionManager() { return ProtManager; }
    @Override public void Nguhcraft$SetProtectionManager(ProtectionManager manager) {
        throw new UnsupportedOperationException("Cannot overwrite server-side protection manager");
    }

    @Override public DisplayManager Nguhcraft$GetDisplayManager() { return DisplayMgr; }
    @Override public MCBASIC.ProcedureManager Nguhcraft$GetProcedureManager() { return ProcManager; }
}
