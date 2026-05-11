package xyz.mantevian.mgames.util

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.DyeColor
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.*
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect
import net.minecraft.world.item.enchantment.Enchantment
import java.util.*

class ItemStackBuilder() {
    private var stack: ItemStack = ItemStack.EMPTY

    constructor(item: Item) : this() {
        stack = ItemStack(item)
    }

    constructor(itemId: String) : this() {
        stack =
            ItemStack(
                getRegistry(Registries.ITEM).get(Identifier.parse(itemId)).get().value()
            )
    }

    fun withCount(count: Int): ItemStackBuilder {
        stack.count = count
        return this
    }

    fun enchant(enchantment: Enchantment, level: Int): ItemStackBuilder {
        stack.enchant(Holder.direct(enchantment), level)
        return this
    }

    fun addStatusEffect(effect: MobEffectInstance): ItemStackBuilder {
        val component = stack.getOrDefault(DataComponents.CONSUMABLE, Consumable.builder().build())
        val componentBuilder = Consumable.builder()
        component.onConsumeEffects.forEach { componentBuilder.onConsume(it) }
        componentBuilder.onConsume(ApplyStatusEffectsConsumeEffect(effect))
        stack.set(DataComponents.CONSUMABLE, componentBuilder.build())
        return this
    }

    fun setPotionColor(statusEffect: MobEffect): ItemStackBuilder {
        stack.set(
            DataComponents.POTION_CONTENTS,
            PotionContents(Optional.empty(), Optional.of(statusEffect.color), listOf(), Optional.empty())
        )
        return this
    }

    fun setItemName(name: Component): ItemStackBuilder {
        stack.set(DataComponents.ITEM_NAME, name)
        return this
    }

    fun setCustomName(name: Component): ItemStackBuilder {
        stack.set(DataComponents.CUSTOM_NAME, name)
        return this
    }

    fun appendCustomName(text: Component): ItemStackBuilder {
        val name = (stack.customName ?: stack.displayName).copy()
        stack.set(DataComponents.CUSTOM_NAME, name.append(text))
        return this
    }

    fun addLore(lore: Component): ItemStackBuilder {
        val component = stack.getOrDefault(DataComponents.LORE, ItemLore.EMPTY)
        stack.set(DataComponents.LORE, component.withLineAdded(lore))
        return this
    }

    fun ofColorMix(colors: List<String>): ItemStackBuilder {
        stack = DyedItemColor.applyDyes(stack, colors.map { DyeColor.byName(it, DyeColor.BLACK)!! })
        return this
    }

    fun hideAdditionalTooltip(componentType: DataComponentType<*>): ItemStackBuilder {
        val component = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        stack.set(DataComponents.TOOLTIP_DISPLAY, component.withHidden(componentType, true))
        return this
    }

    fun hideAdditionalTooltip(): ItemStackBuilder {
        hideAdditionalTooltip(DataComponents.ENCHANTMENTS)
        hideAdditionalTooltip(DataComponents.POTION_CONTENTS)
        hideAdditionalTooltip(DataComponents.ATTRIBUTE_MODIFIERS)
        return this
    }

    fun hideTooltip(): ItemStackBuilder {
        val component = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT)
        val newComponent = TooltipDisplay(true, component.hiddenComponents)
        stack.set(DataComponents.TOOLTIP_DISPLAY, newComponent)
        return this
    }

    fun setCooldown(cooldownGroup: Identifier, ticks: Int): ItemStackBuilder {
        stack.set(
            DataComponents.USE_COOLDOWN,
            UseCooldown(ticks.toFloat().div(20.0f), Optional.of(cooldownGroup))
        )
        return this
    }

    fun addAttributeModifier(
        attribute: Attribute,
        modifier: AttributeModifier,
        slot: EquipmentSlotGroup
    ): ItemStackBuilder {
        val component = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY)
        val newComponent = component.withModifierAdded(Holder.direct(attribute), modifier, slot)
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, newComponent)

        return this
    }

    fun build(): ItemStack {
        return stack.copy()
    }
}