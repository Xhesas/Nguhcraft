package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.ChainBlock
import net.minecraft.block.LanternBlock
import net.minecraft.block.MapColor
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.enums.ChestType
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.client.data.BlockStateModelGenerator
import net.minecraft.client.data.ItemModels
import net.minecraft.client.data.ModelIds
import net.minecraft.client.data.Models
import net.minecraft.client.data.TextureMap
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.item.model.special.ChestModelRenderer
import net.minecraft.client.render.item.property.select.SelectProperty
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.component.ComponentType
import net.minecraft.entity.LivingEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.ModelTransformationMode
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.function.ValueLists
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import java.util.function.IntFunction


@Environment(EnvType.CLIENT)
private fun MakeSprite(S: String) = SpriteIdentifier(
    TexturedRenderLayers.CHEST_ATLAS_TEXTURE,
    Id("entity/chest/$S")
)

@Environment(EnvType.CLIENT)
class LockedChestVariant(
    val Locked: SpriteIdentifier,
    val Unlocked: SpriteIdentifier
) {
    constructor(S: String) : this(
        Locked = MakeSprite("${S}_locked"),
        Unlocked = MakeSprite(S)
    )
}

@Environment(EnvType.CLIENT)
class ChestTextureOverride(
    val Single: LockedChestVariant,
    val Left: LockedChestVariant,
    val Right: LockedChestVariant,
) {
    internal constructor(S: String) : this(
        Single = LockedChestVariant(S),
        Left = LockedChestVariant("${S}_left"),
        Right = LockedChestVariant("${S}_right")
    )

    internal fun get(CT: ChestType, Locked: Boolean) = when (CT) {
        ChestType.LEFT -> if (Locked) Left.Locked else Left.Unlocked
        ChestType.RIGHT -> if (Locked) Right.Locked else Right.Unlocked
        else -> if (Locked) Single.Locked else Single.Unlocked
    }

    companion object {
        internal val Normal = OverrideVanillaModel(
            Single = TexturedRenderLayers.NORMAL,
            Left = TexturedRenderLayers.NORMAL_LEFT,
            Right = TexturedRenderLayers.NORMAL_RIGHT,
            Key = "chest"
        )


        @Environment(EnvType.CLIENT)
        private val OVERRIDES = mapOf(
            ChestVariant.CHRISTMAS to OverrideVanillaModel(
                Single = TexturedRenderLayers.CHRISTMAS,
                Left = TexturedRenderLayers.CHRISTMAS_LEFT,
                Right = TexturedRenderLayers.CHRISTMAS_RIGHT,
                Key = "christmas"
            ),

            ChestVariant.PALE_OAK to ChestTextureOverride("pale_oak")
        )

        @Environment(EnvType.CLIENT)
        @JvmStatic
        fun GetTexture(CV: ChestVariant?, CT: ChestType, Locked: Boolean) =
            (CV?.let { OVERRIDES[CV] } ?: Normal).get(CT, Locked)

        internal fun OverrideVanillaModel(
            Single: SpriteIdentifier,
            Left: SpriteIdentifier,
            Right: SpriteIdentifier,
            Key: String,
        ) = ChestTextureOverride(
            Single = LockedChestVariant(MakeSprite("${Key}_locked"), Single),
            Left = LockedChestVariant(MakeSprite("${Key}_left_locked"), Left),
            Right = LockedChestVariant(MakeSprite("${Key}_right_locked"), Right)
        )
    }
}

enum class ChestVariant : StringIdentifiable {
    CHRISTMAS,
    PALE_OAK;

    val DefaultName: Text = Text.translatable("chest_variant.nguhcraft.${asString()}")
        .setStyle(Style.EMPTY.withItalic(false))

    override fun asString() = name.lowercase()

    companion object {
        val BY_ID: IntFunction<ChestVariant> = ValueLists.createIdToValueFunction(
            ChestVariant::ordinal,
            entries.toTypedArray(),
            ValueLists.OutOfBoundsHandling.ZERO
        )

        val CODEC: Codec<ChestVariant> = StringIdentifiable.createCodec(ChestVariant::values)
        val PACKET_CODEC: PacketCodec<ByteBuf, ChestVariant> = PacketCodecs.indexed(BY_ID, ChestVariant::ordinal)
    }
}

@Environment(EnvType.CLIENT)
class ChestVariantProperty : SelectProperty<ChestVariant> {
    override fun getValue(
        St: ItemStack,
        CW: ClientWorld?,
        LE: LivingEntity?,
        Seed: Int,
        MTM: ModelTransformationMode
    ) = St.get(NguhBlocks.CHEST_VARIANT_COMPONENT)

    override fun getType() = TYPE
    companion object {
        val TYPE: SelectProperty.Type<ChestVariantProperty, ChestVariant> = SelectProperty.Type.create(
            MapCodec.unit(ChestVariantProperty()),
            ChestVariant.CODEC
        )
    }
}

object NguhBlocks {
    // Components.
    @JvmField val CHEST_VARIANT_ID = Id("chest_variant")

    @JvmField
    val CHEST_VARIANT_COMPONENT: ComponentType<ChestVariant> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        CHEST_VARIANT_ID,
        ComponentType.builder<ChestVariant>()
            .codec(ChestVariant.CODEC)
            .packetCodec(ChestVariant.PACKET_CODEC)
            .build()
    )

    // Blocks.
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        ::DecorativeHopperBlock,
        AbstractBlock.Settings.copy(Blocks.HOPPER)
    )

    val LOCKED_DOOR =  Register(
        "locked_door",
        ::LockedDoorBlock,
        AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD)
            .requiresTool().strength(5.0f, 3600000.0F)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.IGNORE)
    )

    val PEARLESCENT_LANTERN = Register(
        "pearlescent_lantern",
        ::LanternBlock,
        AbstractBlock.Settings.copy(Blocks.LANTERN)
            .mapColor(MapColor.DULL_PINK)
    )

    val PEARLESCENT_CHAIN = Register(
        "pearlescent_chain",
        ::ChainBlock,
        AbstractBlock.Settings.copy(Blocks.CHAIN)
            .mapColor(MapColor.GRAY)
    )

    // Block entities.
    val LOCKED_DOOR_BLOCK_ENTITY = RegisterEntity(
        "lockable_door",
        FabricBlockEntityTypeBuilder
            .create(::LockedDoorBlockEntity, LOCKED_DOOR)
            .build()
    )

    val PICKAXE_MINEABLE = arrayOf(
        DECORATIVE_HOPPER,
        LOCKED_DOOR,
        PEARLESCENT_LANTERN,
        PEARLESCENT_CHAIN
    )

    val DROPS_SELF = arrayOf(
        DECORATIVE_HOPPER,
        PEARLESCENT_LANTERN,
        PEARLESCENT_CHAIN
    )

    fun BootstrapModels(G: BlockStateModelGenerator) {
        // The door and hopper block state models are very complicated and not exposed
        // as helper functions (the door is actually exposed but our door has an extra
        // block state), so those are currently hard-coded as JSON files instead of being
        // generated here.
        G.registerLantern(PEARLESCENT_LANTERN)
        G.registerItemModel(PEARLESCENT_CHAIN.asItem())
        G.registerItemModel(DECORATIVE_HOPPER.asItem())
        G.registerItemModel(LOCKED_DOOR.asItem())
        G.registerAxisRotated(PEARLESCENT_CHAIN, ModelIds.getBlockModelId(PEARLESCENT_CHAIN))

        // Chest variants. Copied from registerChest().
        val Template = Models.TEMPLATE_CHEST.upload(Items.CHEST, TextureMap.particle(Blocks.OAK_PLANKS), G.modelCollector)
        val Normal = ItemModels.special(Template, ChestModelRenderer.Unbaked(ChestModelRenderer.NORMAL_ID))
        val Christmas = ItemModels.special(Template, ChestModelRenderer.Unbaked(ChestModelRenderer.CHRISTMAS_ID))
        val ChristmasOrNormal = ItemModels.christmasSelect(Christmas, Normal)
        val PaleOak = ItemModels.special(Template, ChestModelRenderer.Unbaked(Id("pale_oak")))
        G.itemModelOutput.accept(Items.CHEST, ItemModels.select(
            ChestVariantProperty(),
            ChristmasOrNormal,
            ItemModels.switchCase(ChestVariant.CHRISTMAS, Christmas),
            ItemModels.switchCase(ChestVariant.PALE_OAK, PaleOak),
        ))
    }

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.add(DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register {
            it.add(LOCKED_DOOR)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(PEARLESCENT_LANTERN)
            it.add(PEARLESCENT_CHAIN)
        }
    }

    private fun Register(
        Key: String,
        Ctor: (S: AbstractBlock.Settings) -> Block,
        S: AbstractBlock.Settings,
        ItemCtor: (B: Block, S: Item.Settings) -> Item = ::BlockItem
    ): Block {
        // Create registry keys.
        val ItemKey = RegistryKey.of(RegistryKeys.ITEM, Id(Key))
        val BlockKey = RegistryKey.of(RegistryKeys.BLOCK, Id(Key))

        // Set the registry key for the block settings.
        S.registryKey(BlockKey)

        // Create and register the block.
        val B = Ctor(S)
        Registry.register(Registries.BLOCK, BlockKey, B)

        // Create and register the item.
        val ItemSettings = Item.Settings()
            .useBlockPrefixedTranslationKey()
            .registryKey(ItemKey)
        val I = ItemCtor(B, ItemSettings)
        Registry.register(Registries.ITEM, ItemKey, I)
        return B
    }

    private fun <C : BlockEntity> RegisterEntity(
        Key: String,
        Type: BlockEntityType<C>
    ): BlockEntityType<C> = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Id(Key),
        Type
    )
}