package io.fand.api.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.fand.api.VanillaKey;
import io.fand.api.item.component.CatSoundVariantKey;
import io.fand.api.item.component.CatVariantKey;
import io.fand.api.item.component.ChickenSoundVariantKey;
import io.fand.api.item.component.ChickenVariantKey;
import io.fand.api.item.component.CustomModelData;
import io.fand.api.item.component.CowSoundVariantKey;
import io.fand.api.item.component.CowVariantKey;
import io.fand.api.item.component.EnchantmentKey;
import io.fand.api.item.component.FrogVariantKey;
import io.fand.api.item.component.InstrumentKey;
import io.fand.api.item.component.ItemAdventureModePredicate;
import io.fand.api.item.component.ItemArmorTrim;
import io.fand.api.item.component.ItemAttackRange;
import io.fand.api.item.component.ItemAttributeModifiers;
import io.fand.api.item.component.ItemBannerPatterns;
import io.fand.api.item.component.ItemBees;
import io.fand.api.item.component.ItemBlocksAttacks;
import io.fand.api.item.component.ItemBlockStateProperties;
import io.fand.api.item.component.ItemComponentData;
import io.fand.api.item.component.ItemEnchantments;
import io.fand.api.item.component.ItemComponentKeys;
import io.fand.api.item.component.ItemComponents;
import io.fand.api.item.component.ItemConsumable;
import io.fand.api.item.component.ItemContainerContents;
import io.fand.api.item.component.ItemContainerLoot;
import io.fand.api.item.component.ItemDamageResistant;
import io.fand.api.item.component.ItemDebugStickState;
import io.fand.api.item.component.ItemDeathProtection;
import io.fand.api.item.component.ItemDyeColor;
import io.fand.api.item.component.ItemEntityVariant;
import io.fand.api.item.component.ItemEquippable;
import io.fand.api.item.component.ItemFireworkExplosion;
import io.fand.api.item.component.ItemFireworks;
import io.fand.api.item.component.ItemFood;
import io.fand.api.item.component.ItemKeySet;
import io.fand.api.item.component.ItemKineticWeapon;
import io.fand.api.item.component.ItemLock;
import io.fand.api.item.component.ItemLodestoneTracker;
import io.fand.api.item.component.ItemMapDecorations;
import io.fand.api.item.component.ItemMapPostProcessing;
import io.fand.api.item.component.ItemPiercingWeapon;
import io.fand.api.item.component.ItemPotDecorations;
import io.fand.api.item.component.ItemPotionContents;
import io.fand.api.item.component.ItemProfile;
import io.fand.api.item.component.ItemRarity;
import io.fand.api.item.component.ItemRepairable;
import io.fand.api.item.component.ItemSuspiciousStewEffects;
import io.fand.api.item.component.ItemSwingAnimation;
import io.fand.api.item.component.ItemTemplate;
import io.fand.api.item.component.ItemTool;
import io.fand.api.item.component.ItemTooltipDisplay;
import io.fand.api.item.component.ItemTypedEntityData;
import io.fand.api.item.component.ItemUseCooldown;
import io.fand.api.item.component.ItemUseEffects;
import io.fand.api.item.component.ItemWeapon;
import io.fand.api.item.component.ItemWritableBookContent;
import io.fand.api.item.component.ItemWrittenBookContent;
import io.fand.api.item.component.PaintingVariantKey;
import io.fand.api.item.component.PigSoundVariantKey;
import io.fand.api.item.component.PigVariantKey;
import io.fand.api.item.component.TrimMaterialKey;
import io.fand.api.item.component.VillagerVariantKey;
import io.fand.api.item.component.WolfSoundVariantKey;
import io.fand.api.item.component.WolfVariantKey;
import io.fand.api.item.component.ZombieNautilusVariantKey;
import io.fand.api.persistence.PersistentDataContainer;
import io.fand.api.world.DamageTypeKey;
import io.fand.api.world.sound.JukeboxSongKey;
import io.fand.api.world.sound.SoundKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jspecify.annotations.Nullable;

/**
 * An immutable item stack: a {@link ItemType}, a positive {@code amount}, and
 * a patch of modern vanilla data components.
 *
 * <p>The empty stack is represented by {@link #EMPTY}. Use {@code with*}
 * methods to build modified copies; no method mutates this instance.
 */
public record ItemStack(@Nullable ItemType type, int amount, ItemComponents components) {

    /** Sentinel empty stack (type {@code null}, amount 0). Prefer {@link #isEmpty()} over null checks. */
    public static final ItemStack EMPTY = new ItemStack(null, 0, ItemComponents.EMPTY);
    private static final String PERSISTENT_DATA_KEY = "fand:persistent_data";

    public ItemStack(ItemType type, int amount) {
        this(type, amount, ItemComponents.EMPTY);
    }

    public ItemStack {
        components = components == null ? ItemComponents.EMPTY : components;
        if (type == null) {
            if (amount != 0) {
                throw new IllegalArgumentException("Empty stack must have amount 0");
            }
            components = ItemComponents.EMPTY;
        } else {
            Objects.requireNonNull(type, "type");
            if (amount < 1) {
                throw new IllegalArgumentException("Non-empty stack amount must be >= 1, got " + amount);
            }
            int maxStackSize = maxStackSize(type, components);
            if (amount > maxStackSize) {
                throw new IllegalArgumentException(
                        "Amount " + amount + " exceeds max stack size " + maxStackSize + " for " + type.key().asString());
            }
        }
    }

    public boolean isEmpty() {
        return type == null;
    }

    /** Effective maximum stack size, including a {@code max_stack_size} component override. */
    public int maxStackSize() {
        return isEmpty() ? 0 : maxStackSize(type, components);
    }

    public ItemStack withAmount(int newAmount) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, newAmount, components);
    }

    public Optional<JsonElement> component(Key key) {
        return components.get(key);
    }

    public boolean hasComponent(Key key) {
        return components.has(key);
    }

    public ItemStack withComponent(Key key, JsonElement value) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.with(key, value));
    }

    /**
     * Drops an explicit override/removal for {@code key}, allowing the item
     * type's vanilla default component to show through again.
     */
    public ItemStack withoutComponent(Key key) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.without(key));
    }

    /**
     * Forces {@code key} to be absent, even if the item type has a vanilla
     * default for it.
     */
    public ItemStack removeComponent(Key key) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.remove(key));
    }

    public ItemStack withComponents(ItemComponents newComponents) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, newComponents);
    }

    public ItemStack applyComponents(ItemComponents patch) {
        if (isEmpty()) {
            return EMPTY;
        }
        return new ItemStack(type, amount, components.apply(patch));
    }

    public Optional<JsonObject> customData() {
        return component(ItemComponentKeys.CUSTOM_DATA)
                .filter(JsonElement::isJsonObject)
                .map(element -> element.getAsJsonObject().deepCopy());
    }

    public ItemStack withCustomData(JsonObject data) {
        Objects.requireNonNull(data, "data");
        return withComponent(ItemComponentKeys.CUSTOM_DATA, data);
    }

    public ItemStack withoutCustomData() {
        return withoutComponent(ItemComponentKeys.CUSTOM_DATA);
    }

    /**
     * Plugin-owned persistent data stored inside the vanilla custom-data
     * component. Keys are namespaced Adventure keys.
     */
    public PersistentDataContainer persistentData() {
        return customData()
                .map(data -> data.get(PERSISTENT_DATA_KEY))
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .map(PersistentDataContainer::new)
                .orElse(PersistentDataContainer.EMPTY);
    }

    public ItemStack withPersistentData(PersistentDataContainer data) {
        Objects.requireNonNull(data, "data");
        if (isEmpty()) {
            return EMPTY;
        }
        var customData = customData().orElseGet(JsonObject::new);
        if (data.empty()) {
            customData.remove(PERSISTENT_DATA_KEY);
        } else {
            customData.add(PERSISTENT_DATA_KEY, data.toJson());
        }
        return customData.size() == 0 ? withoutCustomData() : withCustomData(customData);
    }

    public ItemStack withPersistentData(Key key, JsonElement value) {
        return withPersistentData(persistentData().with(key, value));
    }

    public ItemStack withoutPersistentData(Key key) {
        return withPersistentData(persistentData().without(key));
    }

    public Optional<Integer> maxStackSizeOverride() {
        return intComponent(ItemComponentKeys.MAX_STACK_SIZE);
    }

    public ItemStack withMaxStackSize(int maxStackSize) {
        if (maxStackSize < 1 || maxStackSize > 99) {
            throw new IllegalArgumentException("maxStackSize must be in 1..99");
        }
        return withComponent(ItemComponentKeys.MAX_STACK_SIZE, new JsonPrimitive(maxStackSize));
    }

    public ItemStack withoutMaxStackSize() {
        return withoutComponent(ItemComponentKeys.MAX_STACK_SIZE);
    }

    public Optional<Component> customName() {
        return textComponent(ItemComponentKeys.CUSTOM_NAME);
    }

    public ItemStack withCustomName(Component name) {
        return withTextComponent(ItemComponentKeys.CUSTOM_NAME, name);
    }

    public ItemStack withoutCustomName() {
        return withoutComponent(ItemComponentKeys.CUSTOM_NAME);
    }

    public Optional<Component> itemName() {
        return textComponent(ItemComponentKeys.ITEM_NAME);
    }

    public ItemStack withItemName(Component name) {
        return withTextComponent(ItemComponentKeys.ITEM_NAME, name);
    }

    public ItemStack withoutItemName() {
        return withoutComponent(ItemComponentKeys.ITEM_NAME);
    }

    public List<Component> lore() {
        return component(ItemComponentKeys.LORE)
                .filter(JsonElement::isJsonArray)
                .stream()
                .flatMap(element -> element.getAsJsonArray().asList().stream())
                .map(ItemStack::deserializeComponent)
                .toList();
    }

    public ItemStack withLore(List<Component> lore) {
        Objects.requireNonNull(lore, "lore");
        if (isEmpty()) {
            return EMPTY;
        }
        var lines = new JsonArray();
        for (var line : lore) {
            lines.add(serializeComponent(line));
        }
        return withComponent(ItemComponentKeys.LORE, lines);
    }

    public ItemStack withLore(Component... lore) {
        return withLore(List.of(lore));
    }

    public ItemStack addLoreLine(Component line) {
        var lines = new java.util.ArrayList<>(lore());
        lines.add(Objects.requireNonNull(line, "line"));
        return withLore(lines);
    }

    public ItemStack withoutLore() {
        return withoutComponent(ItemComponentKeys.LORE);
    }

    public Optional<Key> itemModel() {
        return component(ItemComponentKeys.ITEM_MODEL)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(Key::key);
    }

    public ItemStack withItemModel(Key model) {
        Objects.requireNonNull(model, "model");
        return withComponent(ItemComponentKeys.ITEM_MODEL, new JsonPrimitive(model.asString()));
    }

    public ItemStack withoutItemModel() {
        return withoutComponent(ItemComponentKeys.ITEM_MODEL);
    }

    public Optional<CustomModelData> customModelData() {
        return component(ItemComponentKeys.CUSTOM_MODEL_DATA).map(CustomModelData::fromJson);
    }

    public ItemStack withCustomModelData(CustomModelData data) {
        Objects.requireNonNull(data, "data");
        return withComponent(ItemComponentKeys.CUSTOM_MODEL_DATA, data.toJson());
    }

    public ItemStack withCustomModelData(int value) {
        return withCustomModelData(CustomModelData.ofInt(value));
    }

    public ItemStack withoutCustomModelData() {
        return withoutComponent(ItemComponentKeys.CUSTOM_MODEL_DATA);
    }

    public Optional<Boolean> enchantmentGlintOverride() {
        return component(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsBoolean);
    }

    public ItemStack withEnchantmentGlintOverride(boolean glint) {
        return withComponent(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE, new JsonPrimitive(glint));
    }

    public ItemStack withoutEnchantmentGlintOverride() {
        return withoutComponent(ItemComponentKeys.ENCHANTMENT_GLINT_OVERRIDE);
    }

    public boolean unbreakable() {
        return hasComponent(ItemComponentKeys.UNBREAKABLE);
    }

    public ItemStack withUnbreakable(boolean unbreakable) {
        return unbreakable
                ? withComponent(ItemComponentKeys.UNBREAKABLE, new JsonObject())
                : withoutComponent(ItemComponentKeys.UNBREAKABLE);
    }

    public ItemStack withoutUnbreakable() {
        return withoutComponent(ItemComponentKeys.UNBREAKABLE);
    }

    public Optional<Integer> damage() {
        return intComponent(ItemComponentKeys.DAMAGE);
    }

    public ItemStack withDamage(int damage) {
        return withNonNegativeInt(ItemComponentKeys.DAMAGE, damage, "damage");
    }

    public ItemStack withoutDamage() {
        return withoutComponent(ItemComponentKeys.DAMAGE);
    }

    public Optional<Integer> maxDamage() {
        return intComponent(ItemComponentKeys.MAX_DAMAGE);
    }

    public ItemStack withMaxDamage(int maxDamage) {
        if (maxDamage < 1) {
            throw new IllegalArgumentException("maxDamage must be >= 1");
        }
        return withComponent(ItemComponentKeys.MAX_DAMAGE, new JsonPrimitive(maxDamage));
    }

    public ItemStack withoutMaxDamage() {
        return withoutComponent(ItemComponentKeys.MAX_DAMAGE);
    }

    public Optional<Integer> repairCost() {
        return intComponent(ItemComponentKeys.REPAIR_COST);
    }

    public ItemStack withRepairCost(int repairCost) {
        return withNonNegativeInt(ItemComponentKeys.REPAIR_COST, repairCost, "repairCost");
    }

    public ItemStack withoutRepairCost() {
        return withoutComponent(ItemComponentKeys.REPAIR_COST);
    }

    public Optional<ItemRarity> rarity() {
        return component(ItemComponentKeys.RARITY)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(ItemRarity::fromSerializedName);
    }

    public ItemStack withRarity(ItemRarity rarity) {
        Objects.requireNonNull(rarity, "rarity");
        return withComponent(ItemComponentKeys.RARITY, new JsonPrimitive(rarity.serializedName()));
    }

    public ItemStack withoutRarity() {
        return withoutComponent(ItemComponentKeys.RARITY);
    }

    public ItemEnchantments enchantments() {
        return component(ItemComponentKeys.ENCHANTMENTS)
                .map(ItemEnchantments::fromJson)
                .orElse(ItemEnchantments.EMPTY);
    }

    public ItemStack withEnchantments(ItemEnchantments enchantments) {
        Objects.requireNonNull(enchantments, "enchantments");
        return withComponent(ItemComponentKeys.ENCHANTMENTS, enchantments.toJson());
    }

    public ItemStack withEnchantment(Key enchantment, int level) {
        return withEnchantments(enchantments().with(enchantment, level));
    }

    public ItemStack withEnchantment(EnchantmentKey enchantment, int level) {
        return withEnchantments(enchantments().with(enchantment, level));
    }

    public ItemStack upgradeEnchantment(Key enchantment, int level) {
        return withEnchantments(enchantments().upgrade(enchantment, level));
    }

    public ItemStack upgradeEnchantment(EnchantmentKey enchantment, int level) {
        return withEnchantments(enchantments().upgrade(enchantment, level));
    }

    public ItemStack withoutEnchantment(Key enchantment) {
        return withEnchantments(enchantments().without(enchantment));
    }

    public ItemStack withoutEnchantment(EnchantmentKey enchantment) {
        return withEnchantments(enchantments().without(enchantment));
    }

    public ItemStack withoutEnchantments() {
        return withoutComponent(ItemComponentKeys.ENCHANTMENTS);
    }

    public ItemEnchantments storedEnchantments() {
        return component(ItemComponentKeys.STORED_ENCHANTMENTS)
                .map(ItemEnchantments::fromJson)
                .orElse(ItemEnchantments.EMPTY);
    }

    public ItemStack withStoredEnchantments(ItemEnchantments enchantments) {
        Objects.requireNonNull(enchantments, "enchantments");
        return withComponent(ItemComponentKeys.STORED_ENCHANTMENTS, enchantments.toJson());
    }

    public ItemStack withStoredEnchantment(Key enchantment, int level) {
        return withStoredEnchantments(storedEnchantments().with(enchantment, level));
    }

    public ItemStack withStoredEnchantment(EnchantmentKey enchantment, int level) {
        return withStoredEnchantments(storedEnchantments().with(enchantment, level));
    }

    public ItemStack upgradeStoredEnchantment(Key enchantment, int level) {
        return withStoredEnchantments(storedEnchantments().upgrade(enchantment, level));
    }

    public ItemStack upgradeStoredEnchantment(EnchantmentKey enchantment, int level) {
        return withStoredEnchantments(storedEnchantments().upgrade(enchantment, level));
    }

    public ItemStack withoutStoredEnchantment(Key enchantment) {
        return withStoredEnchantments(storedEnchantments().without(enchantment));
    }

    public ItemStack withoutStoredEnchantment(EnchantmentKey enchantment) {
        return withStoredEnchantments(storedEnchantments().without(enchantment));
    }

    public ItemStack withoutStoredEnchantments() {
        return withoutComponent(ItemComponentKeys.STORED_ENCHANTMENTS);
    }

    public Optional<Integer> enchantable() {
        return objectIntComponent(ItemComponentKeys.ENCHANTABLE, "value");
    }

    public ItemStack withEnchantable(int value) {
        if (value < 1) {
            throw new IllegalArgumentException("enchantable value must be >= 1");
        }
        var json = new JsonObject();
        json.addProperty("value", value);
        return withComponent(ItemComponentKeys.ENCHANTABLE, json);
    }

    public ItemStack withoutEnchantable() {
        return withoutComponent(ItemComponentKeys.ENCHANTABLE);
    }

    public Optional<Float> minimumAttackCharge() {
        return floatComponent(ItemComponentKeys.MINIMUM_ATTACK_CHARGE);
    }

    public ItemStack withMinimumAttackCharge(float charge) {
        if (charge < 0.0F || charge > 1.0F) {
            throw new IllegalArgumentException("minimumAttackCharge must be in 0.0..1.0");
        }
        return withComponent(ItemComponentKeys.MINIMUM_ATTACK_CHARGE, new JsonPrimitive(charge));
    }

    public ItemStack withoutMinimumAttackCharge() {
        return withoutComponent(ItemComponentKeys.MINIMUM_ATTACK_CHARGE);
    }

    public Optional<Key> damageType() {
        return keyComponent(ItemComponentKeys.DAMAGE_TYPE);
    }

    public ItemStack withDamageType(Key damageType) {
        return withKeyComponent(ItemComponentKeys.DAMAGE_TYPE, damageType);
    }

    public ItemStack withDamageType(DamageTypeKey damageType) {
        return withKeyComponent(ItemComponentKeys.DAMAGE_TYPE, damageType);
    }

    public ItemStack withoutDamageType() {
        return withoutComponent(ItemComponentKeys.DAMAGE_TYPE);
    }

    public Optional<Key> tooltipStyle() {
        return keyComponent(ItemComponentKeys.TOOLTIP_STYLE);
    }

    public ItemStack withTooltipStyle(Key style) {
        return withKeyComponent(ItemComponentKeys.TOOLTIP_STYLE, style);
    }

    public ItemStack withoutTooltipStyle() {
        return withoutComponent(ItemComponentKeys.TOOLTIP_STYLE);
    }

    public ItemTooltipDisplay tooltipDisplay() {
        return component(ItemComponentKeys.TOOLTIP_DISPLAY)
                .map(ItemTooltipDisplay::fromJson)
                .orElse(ItemTooltipDisplay.DEFAULT);
    }

    public ItemStack withTooltipDisplay(ItemTooltipDisplay display) {
        Objects.requireNonNull(display, "display");
        return withComponent(ItemComponentKeys.TOOLTIP_DISPLAY, display.toJson());
    }

    public ItemStack withTooltipHidden(boolean hidden) {
        return withTooltipDisplay(tooltipDisplay().withHiddenTooltip(hidden));
    }

    public ItemStack withHiddenTooltipComponent(Key component, boolean hidden) {
        return withTooltipDisplay(tooltipDisplay().withHiddenComponent(component, hidden));
    }

    public ItemStack withoutTooltipDisplay() {
        return withoutComponent(ItemComponentKeys.TOOLTIP_DISPLAY);
    }

    public Optional<Integer> dyedColor() {
        return intComponent(ItemComponentKeys.DYED_COLOR);
    }

    public ItemStack withDyedColor(int rgb) {
        return withRgbComponent(ItemComponentKeys.DYED_COLOR, rgb);
    }

    public ItemStack withoutDyedColor() {
        return withoutComponent(ItemComponentKeys.DYED_COLOR);
    }

    public Optional<Integer> mapColor() {
        return intComponent(ItemComponentKeys.MAP_COLOR);
    }

    public ItemStack withMapColor(int rgb) {
        return withRgbComponent(ItemComponentKeys.MAP_COLOR, rgb);
    }

    public ItemStack withoutMapColor() {
        return withoutComponent(ItemComponentKeys.MAP_COLOR);
    }

    public Optional<Key> noteBlockSound() {
        return keyComponent(ItemComponentKeys.NOTE_BLOCK_SOUND);
    }

    public ItemStack withNoteBlockSound(Key sound) {
        return withKeyComponent(ItemComponentKeys.NOTE_BLOCK_SOUND, sound);
    }

    public ItemStack withNoteBlockSound(SoundKey sound) {
        return withKeyComponent(ItemComponentKeys.NOTE_BLOCK_SOUND, sound);
    }

    public ItemStack withoutNoteBlockSound() {
        return withoutComponent(ItemComponentKeys.NOTE_BLOCK_SOUND);
    }

    public Optional<Integer> additionalTradeCost() {
        return intComponent(ItemComponentKeys.ADDITIONAL_TRADE_COST);
    }

    public ItemStack withAdditionalTradeCost(int cost) {
        return withComponent(ItemComponentKeys.ADDITIONAL_TRADE_COST, new JsonPrimitive(cost));
    }

    public ItemStack withoutAdditionalTradeCost() {
        return withoutComponent(ItemComponentKeys.ADDITIONAL_TRADE_COST);
    }

    public Optional<Integer> mapId() {
        return intComponent(ItemComponentKeys.MAP_ID);
    }

    public ItemStack withMapId(int id) {
        return withNonNegativeInt(ItemComponentKeys.MAP_ID, id, "mapId");
    }

    public ItemStack withoutMapId() {
        return withoutComponent(ItemComponentKeys.MAP_ID);
    }

    public Optional<ItemUseEffects> useEffects() {
        return dataComponent(ItemComponentKeys.USE_EFFECTS, ItemUseEffects::fromJson);
    }

    public ItemStack withUseEffects(ItemUseEffects effects) {
        return withComponentData(ItemComponentKeys.USE_EFFECTS, effects);
    }

    public ItemStack withoutUseEffects() {
        return withoutComponent(ItemComponentKeys.USE_EFFECTS);
    }

    public Optional<ItemAdventureModePredicate> canPlaceOn() {
        return dataComponent(ItemComponentKeys.CAN_PLACE_ON, ItemAdventureModePredicate::fromJson);
    }

    public ItemStack withCanPlaceOn(ItemAdventureModePredicate predicate) {
        return withComponentData(ItemComponentKeys.CAN_PLACE_ON, predicate);
    }

    public ItemStack withoutCanPlaceOn() {
        return withoutComponent(ItemComponentKeys.CAN_PLACE_ON);
    }

    public Optional<ItemAdventureModePredicate> canBreak() {
        return dataComponent(ItemComponentKeys.CAN_BREAK, ItemAdventureModePredicate::fromJson);
    }

    public ItemStack withCanBreak(ItemAdventureModePredicate predicate) {
        return withComponentData(ItemComponentKeys.CAN_BREAK, predicate);
    }

    public ItemStack withoutCanBreak() {
        return withoutComponent(ItemComponentKeys.CAN_BREAK);
    }

    public Optional<ItemAttributeModifiers> attributeModifiers() {
        return dataComponent(ItemComponentKeys.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers::fromJson);
    }

    public ItemStack withAttributeModifiers(ItemAttributeModifiers modifiers) {
        return withComponentData(ItemComponentKeys.ATTRIBUTE_MODIFIERS, modifiers);
    }

    public ItemStack withoutAttributeModifiers() {
        return withoutComponent(ItemComponentKeys.ATTRIBUTE_MODIFIERS);
    }

    public boolean creativeSlotLock() {
        return hasComponent(ItemComponentKeys.CREATIVE_SLOT_LOCK);
    }

    public ItemStack withCreativeSlotLock(boolean locked) {
        return withUnitComponent(ItemComponentKeys.CREATIVE_SLOT_LOCK, locked);
    }

    public ItemStack withoutCreativeSlotLock() {
        return withoutComponent(ItemComponentKeys.CREATIVE_SLOT_LOCK);
    }

    public boolean intangibleProjectile() {
        return hasComponent(ItemComponentKeys.INTANGIBLE_PROJECTILE);
    }

    public ItemStack withIntangibleProjectile(boolean intangible) {
        return withUnitComponent(ItemComponentKeys.INTANGIBLE_PROJECTILE, intangible);
    }

    public ItemStack withoutIntangibleProjectile() {
        return withoutComponent(ItemComponentKeys.INTANGIBLE_PROJECTILE);
    }

    public Optional<ItemFood> food() {
        return dataComponent(ItemComponentKeys.FOOD, ItemFood::fromJson);
    }

    public ItemStack withFood(ItemFood food) {
        return withComponentData(ItemComponentKeys.FOOD, food);
    }

    public ItemStack withoutFood() {
        return withoutComponent(ItemComponentKeys.FOOD);
    }

    public Optional<ItemConsumable> consumable() {
        return dataComponent(ItemComponentKeys.CONSUMABLE, ItemConsumable::fromJson);
    }

    public ItemStack withConsumable(ItemConsumable consumable) {
        return withComponentData(ItemComponentKeys.CONSUMABLE, consumable);
    }

    public ItemStack withoutConsumable() {
        return withoutComponent(ItemComponentKeys.CONSUMABLE);
    }

    public Optional<ItemTemplate> useRemainder() {
        return dataComponent(ItemComponentKeys.USE_REMAINDER, ItemTemplate::fromJson);
    }

    public ItemStack withUseRemainder(ItemTemplate remainder) {
        return withComponentData(ItemComponentKeys.USE_REMAINDER, remainder);
    }

    public ItemStack withoutUseRemainder() {
        return withoutComponent(ItemComponentKeys.USE_REMAINDER);
    }

    public Optional<ItemUseCooldown> useCooldown() {
        return dataComponent(ItemComponentKeys.USE_COOLDOWN, ItemUseCooldown::fromJson);
    }

    public ItemStack withUseCooldown(ItemUseCooldown cooldown) {
        return withComponentData(ItemComponentKeys.USE_COOLDOWN, cooldown);
    }

    public ItemStack withoutUseCooldown() {
        return withoutComponent(ItemComponentKeys.USE_COOLDOWN);
    }

    public Optional<ItemDamageResistant> damageResistant() {
        return dataComponent(ItemComponentKeys.DAMAGE_RESISTANT, ItemDamageResistant::fromJson);
    }

    public ItemStack withDamageResistant(ItemDamageResistant resistant) {
        return withComponentData(ItemComponentKeys.DAMAGE_RESISTANT, resistant);
    }

    public ItemStack withoutDamageResistant() {
        return withoutComponent(ItemComponentKeys.DAMAGE_RESISTANT);
    }

    public Optional<ItemTool> tool() {
        return dataComponent(ItemComponentKeys.TOOL, ItemTool::fromJson);
    }

    public ItemStack withTool(ItemTool tool) {
        return withComponentData(ItemComponentKeys.TOOL, tool);
    }

    public ItemStack withoutTool() {
        return withoutComponent(ItemComponentKeys.TOOL);
    }

    public Optional<ItemWeapon> weapon() {
        return dataComponent(ItemComponentKeys.WEAPON, ItemWeapon::fromJson);
    }

    public ItemStack withWeapon(ItemWeapon weapon) {
        return withComponentData(ItemComponentKeys.WEAPON, weapon);
    }

    public ItemStack withoutWeapon() {
        return withoutComponent(ItemComponentKeys.WEAPON);
    }

    public Optional<ItemAttackRange> attackRange() {
        return dataComponent(ItemComponentKeys.ATTACK_RANGE, ItemAttackRange::fromJson);
    }

    public ItemStack withAttackRange(ItemAttackRange range) {
        return withComponentData(ItemComponentKeys.ATTACK_RANGE, range);
    }

    public ItemStack withoutAttackRange() {
        return withoutComponent(ItemComponentKeys.ATTACK_RANGE);
    }

    public Optional<ItemEquippable> equippable() {
        return dataComponent(ItemComponentKeys.EQUIPPABLE, ItemEquippable::fromJson);
    }

    public ItemStack withEquippable(ItemEquippable equippable) {
        return withComponentData(ItemComponentKeys.EQUIPPABLE, equippable);
    }

    public ItemStack withoutEquippable() {
        return withoutComponent(ItemComponentKeys.EQUIPPABLE);
    }

    public Optional<ItemRepairable> repairable() {
        return dataComponent(ItemComponentKeys.REPAIRABLE, ItemRepairable::fromJson);
    }

    public ItemStack withRepairable(ItemRepairable repairable) {
        return withComponentData(ItemComponentKeys.REPAIRABLE, repairable);
    }

    public ItemStack withoutRepairable() {
        return withoutComponent(ItemComponentKeys.REPAIRABLE);
    }

    public boolean glider() {
        return hasComponent(ItemComponentKeys.GLIDER);
    }

    public ItemStack withGlider(boolean glider) {
        return withUnitComponent(ItemComponentKeys.GLIDER, glider);
    }

    public ItemStack withoutGlider() {
        return withoutComponent(ItemComponentKeys.GLIDER);
    }

    public Optional<ItemDeathProtection> deathProtection() {
        return dataComponent(ItemComponentKeys.DEATH_PROTECTION, ItemDeathProtection::fromJson);
    }

    public ItemStack withDeathProtection(ItemDeathProtection protection) {
        return withComponentData(ItemComponentKeys.DEATH_PROTECTION, protection);
    }

    public ItemStack withoutDeathProtection() {
        return withoutComponent(ItemComponentKeys.DEATH_PROTECTION);
    }

    public Optional<ItemBlocksAttacks> blocksAttacks() {
        return dataComponent(ItemComponentKeys.BLOCKS_ATTACKS, ItemBlocksAttacks::fromJson);
    }

    public ItemStack withBlocksAttacks(ItemBlocksAttacks blocksAttacks) {
        return withComponentData(ItemComponentKeys.BLOCKS_ATTACKS, blocksAttacks);
    }

    public ItemStack withoutBlocksAttacks() {
        return withoutComponent(ItemComponentKeys.BLOCKS_ATTACKS);
    }

    public Optional<ItemPiercingWeapon> piercingWeapon() {
        return dataComponent(ItemComponentKeys.PIERCING_WEAPON, ItemPiercingWeapon::fromJson);
    }

    public ItemStack withPiercingWeapon(ItemPiercingWeapon piercingWeapon) {
        return withComponentData(ItemComponentKeys.PIERCING_WEAPON, piercingWeapon);
    }

    public ItemStack withoutPiercingWeapon() {
        return withoutComponent(ItemComponentKeys.PIERCING_WEAPON);
    }

    public Optional<ItemKineticWeapon> kineticWeapon() {
        return dataComponent(ItemComponentKeys.KINETIC_WEAPON, ItemKineticWeapon::fromJson);
    }

    public ItemStack withKineticWeapon(ItemKineticWeapon kineticWeapon) {
        return withComponentData(ItemComponentKeys.KINETIC_WEAPON, kineticWeapon);
    }

    public ItemStack withoutKineticWeapon() {
        return withoutComponent(ItemComponentKeys.KINETIC_WEAPON);
    }

    public Optional<ItemSwingAnimation> swingAnimation() {
        return dataComponent(ItemComponentKeys.SWING_ANIMATION, ItemSwingAnimation::fromJson);
    }

    public ItemStack withSwingAnimation(ItemSwingAnimation animation) {
        return withComponentData(ItemComponentKeys.SWING_ANIMATION, animation);
    }

    public ItemStack withoutSwingAnimation() {
        return withoutComponent(ItemComponentKeys.SWING_ANIMATION);
    }

    public Optional<ItemDyeColor> dye() {
        return dyeColorComponent(ItemComponentKeys.DYE);
    }

    public ItemStack withDye(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.DYE, color);
    }

    public ItemStack withoutDye() {
        return withoutComponent(ItemComponentKeys.DYE);
    }

    public Optional<ItemMapDecorations> mapDecorations() {
        return dataComponent(ItemComponentKeys.MAP_DECORATIONS, ItemMapDecorations::fromJson);
    }

    public ItemStack withMapDecorations(ItemMapDecorations decorations) {
        return withComponentData(ItemComponentKeys.MAP_DECORATIONS, decorations);
    }

    public ItemStack withoutMapDecorations() {
        return withoutComponent(ItemComponentKeys.MAP_DECORATIONS);
    }

    public Optional<ItemMapPostProcessing> mapPostProcessing() {
        return stringComponent(ItemComponentKeys.MAP_POST_PROCESSING).map(ItemMapPostProcessing::fromSerializedName);
    }

    public ItemStack withMapPostProcessing(ItemMapPostProcessing processing) {
        Objects.requireNonNull(processing, "processing");
        return withStringComponent(ItemComponentKeys.MAP_POST_PROCESSING, processing.serializedName());
    }

    public ItemStack withoutMapPostProcessing() {
        return withoutComponent(ItemComponentKeys.MAP_POST_PROCESSING);
    }

    public List<ItemTemplate> chargedProjectiles() {
        return itemTemplates(ItemComponentKeys.CHARGED_PROJECTILES);
    }

    public ItemStack withChargedProjectiles(List<ItemTemplate> projectiles) {
        return withItemTemplates(ItemComponentKeys.CHARGED_PROJECTILES, projectiles, "projectiles");
    }

    public ItemStack withoutChargedProjectiles() {
        return withoutComponent(ItemComponentKeys.CHARGED_PROJECTILES);
    }

    public List<ItemTemplate> bundleContents() {
        return itemTemplates(ItemComponentKeys.BUNDLE_CONTENTS);
    }

    public ItemStack withBundleContents(List<ItemTemplate> contents) {
        return withItemTemplates(ItemComponentKeys.BUNDLE_CONTENTS, contents, "contents");
    }

    public ItemStack withoutBundleContents() {
        return withoutComponent(ItemComponentKeys.BUNDLE_CONTENTS);
    }

    public Optional<ItemPotionContents> potionContents() {
        return dataComponent(ItemComponentKeys.POTION_CONTENTS, ItemPotionContents::fromJson);
    }

    public ItemStack withPotionContents(ItemPotionContents contents) {
        return withComponentData(ItemComponentKeys.POTION_CONTENTS, contents);
    }

    public ItemStack withoutPotionContents() {
        return withoutComponent(ItemComponentKeys.POTION_CONTENTS);
    }

    public Optional<Float> potionDurationScale() {
        return floatComponent(ItemComponentKeys.POTION_DURATION_SCALE);
    }

    public ItemStack withPotionDurationScale(float scale) {
        if (scale < 0.0F) {
            throw new IllegalArgumentException("potionDurationScale must be >= 0");
        }
        return withComponent(ItemComponentKeys.POTION_DURATION_SCALE, new JsonPrimitive(scale));
    }

    public ItemStack withoutPotionDurationScale() {
        return withoutComponent(ItemComponentKeys.POTION_DURATION_SCALE);
    }

    public Optional<ItemSuspiciousStewEffects> suspiciousStewEffects() {
        return dataComponent(ItemComponentKeys.SUSPICIOUS_STEW_EFFECTS, ItemSuspiciousStewEffects::fromJson);
    }

    public ItemStack withSuspiciousStewEffects(ItemSuspiciousStewEffects effects) {
        return withComponentData(ItemComponentKeys.SUSPICIOUS_STEW_EFFECTS, effects);
    }

    public ItemStack withoutSuspiciousStewEffects() {
        return withoutComponent(ItemComponentKeys.SUSPICIOUS_STEW_EFFECTS);
    }

    public Optional<ItemWritableBookContent> writableBookContent() {
        return dataComponent(ItemComponentKeys.WRITABLE_BOOK_CONTENT, ItemWritableBookContent::fromJson);
    }

    public ItemStack withWritableBookContent(ItemWritableBookContent content) {
        return withComponentData(ItemComponentKeys.WRITABLE_BOOK_CONTENT, content);
    }

    public ItemStack withoutWritableBookContent() {
        return withoutComponent(ItemComponentKeys.WRITABLE_BOOK_CONTENT);
    }

    public Optional<ItemWrittenBookContent> writtenBookContent() {
        return dataComponent(ItemComponentKeys.WRITTEN_BOOK_CONTENT, ItemWrittenBookContent::fromJson);
    }

    public ItemStack withWrittenBookContent(ItemWrittenBookContent content) {
        return withComponentData(ItemComponentKeys.WRITTEN_BOOK_CONTENT, content);
    }

    public ItemStack withoutWrittenBookContent() {
        return withoutComponent(ItemComponentKeys.WRITTEN_BOOK_CONTENT);
    }

    public Optional<ItemArmorTrim> trim() {
        return dataComponent(ItemComponentKeys.TRIM, ItemArmorTrim::fromJson);
    }

    public ItemStack withTrim(ItemArmorTrim trim) {
        return withComponentData(ItemComponentKeys.TRIM, trim);
    }

    public ItemStack withoutTrim() {
        return withoutComponent(ItemComponentKeys.TRIM);
    }

    public Optional<ItemDebugStickState> debugStickState() {
        return dataComponent(ItemComponentKeys.DEBUG_STICK_STATE, ItemDebugStickState::fromJson);
    }

    public ItemStack withDebugStickState(ItemDebugStickState state) {
        return withComponentData(ItemComponentKeys.DEBUG_STICK_STATE, state);
    }

    public ItemStack withoutDebugStickState() {
        return withoutComponent(ItemComponentKeys.DEBUG_STICK_STATE);
    }

    public Optional<ItemTypedEntityData> entityData() {
        return dataComponent(ItemComponentKeys.ENTITY_DATA, ItemTypedEntityData::fromJson);
    }

    public ItemStack withEntityData(ItemTypedEntityData data) {
        return withComponentData(ItemComponentKeys.ENTITY_DATA, data);
    }

    public ItemStack withoutEntityData() {
        return withoutComponent(ItemComponentKeys.ENTITY_DATA);
    }

    public Optional<JsonObject> bucketEntityData() {
        return customDataComponent(ItemComponentKeys.BUCKET_ENTITY_DATA);
    }

    public ItemStack withBucketEntityData(JsonObject data) {
        return withJsonObjectComponent(ItemComponentKeys.BUCKET_ENTITY_DATA, data);
    }

    public ItemStack withoutBucketEntityData() {
        return withoutComponent(ItemComponentKeys.BUCKET_ENTITY_DATA);
    }

    public Optional<ItemTypedEntityData> blockEntityData() {
        return dataComponent(ItemComponentKeys.BLOCK_ENTITY_DATA, ItemTypedEntityData::fromJson);
    }

    public ItemStack withBlockEntityData(ItemTypedEntityData data) {
        return withComponentData(ItemComponentKeys.BLOCK_ENTITY_DATA, data);
    }

    public ItemStack withoutBlockEntityData() {
        return withoutComponent(ItemComponentKeys.BLOCK_ENTITY_DATA);
    }

    public Optional<Key> instrument() {
        return keyComponent(ItemComponentKeys.INSTRUMENT);
    }

    public ItemStack withInstrument(Key instrument) {
        return withKeyComponent(ItemComponentKeys.INSTRUMENT, instrument);
    }

    public ItemStack withInstrument(InstrumentKey instrument) {
        return withKeyComponent(ItemComponentKeys.INSTRUMENT, instrument);
    }

    public ItemStack withoutInstrument() {
        return withoutComponent(ItemComponentKeys.INSTRUMENT);
    }

    public Optional<Key> providesTrimMaterial() {
        return keyComponent(ItemComponentKeys.PROVIDES_TRIM_MATERIAL);
    }

    public ItemStack withProvidesTrimMaterial(Key material) {
        return withKeyComponent(ItemComponentKeys.PROVIDES_TRIM_MATERIAL, material);
    }

    public ItemStack withProvidesTrimMaterial(TrimMaterialKey material) {
        return withKeyComponent(ItemComponentKeys.PROVIDES_TRIM_MATERIAL, material);
    }

    public ItemStack withoutProvidesTrimMaterial() {
        return withoutComponent(ItemComponentKeys.PROVIDES_TRIM_MATERIAL);
    }

    public Optional<Integer> ominousBottleAmplifier() {
        return intComponent(ItemComponentKeys.OMINOUS_BOTTLE_AMPLIFIER);
    }

    public ItemStack withOminousBottleAmplifier(int amplifier) {
        if (amplifier < 0 || amplifier > 4) {
            throw new IllegalArgumentException("ominousBottleAmplifier must be in 0..4");
        }
        return withComponent(ItemComponentKeys.OMINOUS_BOTTLE_AMPLIFIER, new JsonPrimitive(amplifier));
    }

    public ItemStack withoutOminousBottleAmplifier() {
        return withoutComponent(ItemComponentKeys.OMINOUS_BOTTLE_AMPLIFIER);
    }

    public Optional<Key> jukeboxPlayable() {
        return keyComponent(ItemComponentKeys.JUKEBOX_PLAYABLE);
    }

    public ItemStack withJukeboxPlayable(Key song) {
        return withKeyComponent(ItemComponentKeys.JUKEBOX_PLAYABLE, song);
    }

    public ItemStack withJukeboxPlayable(JukeboxSongKey song) {
        return withKeyComponent(ItemComponentKeys.JUKEBOX_PLAYABLE, song);
    }

    public ItemStack withoutJukeboxPlayable() {
        return withoutComponent(ItemComponentKeys.JUKEBOX_PLAYABLE);
    }

    public Optional<ItemKeySet> providesBannerPatterns() {
        return dataComponent(ItemComponentKeys.PROVIDES_BANNER_PATTERNS, ItemKeySet::fromJson);
    }

    public ItemStack withProvidesBannerPatterns(ItemKeySet patterns) {
        return withComponentData(ItemComponentKeys.PROVIDES_BANNER_PATTERNS, patterns);
    }

    public ItemStack withoutProvidesBannerPatterns() {
        return withoutComponent(ItemComponentKeys.PROVIDES_BANNER_PATTERNS);
    }

    public List<Key> recipes() {
        return keyListComponent(ItemComponentKeys.RECIPES);
    }

    public ItemStack withRecipes(List<Key> recipes) {
        return withKeyListComponent(ItemComponentKeys.RECIPES, recipes, "recipes");
    }

    public ItemStack withoutRecipes() {
        return withoutComponent(ItemComponentKeys.RECIPES);
    }

    public Optional<ItemLodestoneTracker> lodestoneTracker() {
        return dataComponent(ItemComponentKeys.LODESTONE_TRACKER, ItemLodestoneTracker::fromJson);
    }

    public ItemStack withLodestoneTracker(ItemLodestoneTracker tracker) {
        return withComponentData(ItemComponentKeys.LODESTONE_TRACKER, tracker);
    }

    public ItemStack withoutLodestoneTracker() {
        return withoutComponent(ItemComponentKeys.LODESTONE_TRACKER);
    }

    public Optional<ItemFireworkExplosion> fireworkExplosion() {
        return dataComponent(ItemComponentKeys.FIREWORK_EXPLOSION, ItemFireworkExplosion::fromJson);
    }

    public ItemStack withFireworkExplosion(ItemFireworkExplosion explosion) {
        return withComponentData(ItemComponentKeys.FIREWORK_EXPLOSION, explosion);
    }

    public ItemStack withoutFireworkExplosion() {
        return withoutComponent(ItemComponentKeys.FIREWORK_EXPLOSION);
    }

    public Optional<ItemFireworks> fireworks() {
        return dataComponent(ItemComponentKeys.FIREWORKS, ItemFireworks::fromJson);
    }

    public ItemStack withFireworks(ItemFireworks fireworks) {
        return withComponentData(ItemComponentKeys.FIREWORKS, fireworks);
    }

    public ItemStack withoutFireworks() {
        return withoutComponent(ItemComponentKeys.FIREWORKS);
    }

    public Optional<ItemProfile> profile() {
        return dataComponent(ItemComponentKeys.PROFILE, ItemProfile::fromJson);
    }

    public ItemStack withProfile(ItemProfile profile) {
        return withComponentData(ItemComponentKeys.PROFILE, profile);
    }

    public ItemStack withoutProfile() {
        return withoutComponent(ItemComponentKeys.PROFILE);
    }

    public Optional<ItemBannerPatterns> bannerPatterns() {
        return dataComponent(ItemComponentKeys.BANNER_PATTERNS, ItemBannerPatterns::fromJson);
    }

    public ItemStack withBannerPatterns(ItemBannerPatterns patterns) {
        return withComponentData(ItemComponentKeys.BANNER_PATTERNS, patterns);
    }

    public ItemStack withoutBannerPatterns() {
        return withoutComponent(ItemComponentKeys.BANNER_PATTERNS);
    }

    public Optional<ItemDyeColor> baseColor() {
        return dyeColorComponent(ItemComponentKeys.BASE_COLOR);
    }

    public ItemStack withBaseColor(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.BASE_COLOR, color);
    }

    public ItemStack withoutBaseColor() {
        return withoutComponent(ItemComponentKeys.BASE_COLOR);
    }

    public Optional<ItemPotDecorations> potDecorations() {
        return dataComponent(ItemComponentKeys.POT_DECORATIONS, ItemPotDecorations::fromJson);
    }

    public ItemStack withPotDecorations(ItemPotDecorations decorations) {
        return withComponentData(ItemComponentKeys.POT_DECORATIONS, decorations);
    }

    public ItemStack withoutPotDecorations() {
        return withoutComponent(ItemComponentKeys.POT_DECORATIONS);
    }

    public Optional<ItemContainerContents> container() {
        return dataComponent(ItemComponentKeys.CONTAINER, ItemContainerContents::fromJson);
    }

    public ItemStack withContainer(ItemContainerContents contents) {
        return withComponentData(ItemComponentKeys.CONTAINER, contents);
    }

    public ItemStack withoutContainer() {
        return withoutComponent(ItemComponentKeys.CONTAINER);
    }

    public Optional<ItemBlockStateProperties> blockState() {
        return dataComponent(ItemComponentKeys.BLOCK_STATE, ItemBlockStateProperties::fromJson);
    }

    public ItemStack withBlockState(ItemBlockStateProperties state) {
        return withComponentData(ItemComponentKeys.BLOCK_STATE, state);
    }

    public ItemStack withoutBlockState() {
        return withoutComponent(ItemComponentKeys.BLOCK_STATE);
    }

    public Optional<ItemBees> bees() {
        return dataComponent(ItemComponentKeys.BEES, ItemBees::fromJson);
    }

    public ItemStack withBees(ItemBees bees) {
        return withComponentData(ItemComponentKeys.BEES, bees);
    }

    public ItemStack withoutBees() {
        return withoutComponent(ItemComponentKeys.BEES);
    }

    public Optional<ItemLock> lock() {
        return dataComponent(ItemComponentKeys.LOCK, ItemLock::fromJson);
    }

    public ItemStack withLock(ItemLock lock) {
        return withComponentData(ItemComponentKeys.LOCK, lock);
    }

    public ItemStack withoutLock() {
        return withoutComponent(ItemComponentKeys.LOCK);
    }

    public Optional<ItemContainerLoot> containerLoot() {
        return dataComponent(ItemComponentKeys.CONTAINER_LOOT, ItemContainerLoot::fromJson);
    }

    public ItemStack withContainerLoot(ItemContainerLoot loot) {
        return withComponentData(ItemComponentKeys.CONTAINER_LOOT, loot);
    }

    public ItemStack withoutContainerLoot() {
        return withoutComponent(ItemComponentKeys.CONTAINER_LOOT);
    }

    public Optional<Key> breakSound() {
        return keyComponent(ItemComponentKeys.BREAK_SOUND);
    }

    public ItemStack withBreakSound(Key sound) {
        return withKeyComponent(ItemComponentKeys.BREAK_SOUND, sound);
    }

    public ItemStack withBreakSound(SoundKey sound) {
        return withKeyComponent(ItemComponentKeys.BREAK_SOUND, sound);
    }

    public ItemStack withoutBreakSound() {
        return withoutComponent(ItemComponentKeys.BREAK_SOUND);
    }

    public Optional<Key> villagerVariant() {
        return keyComponent(ItemComponentKeys.VILLAGER_VARIANT);
    }

    public ItemStack withVillagerVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.VILLAGER_VARIANT, variant);
    }

    public ItemStack withVillagerVariant(VillagerVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.VILLAGER_VARIANT, variant);
    }

    public ItemStack withoutVillagerVariant() {
        return withoutComponent(ItemComponentKeys.VILLAGER_VARIANT);
    }

    public Optional<Key> wolfVariant() {
        return keyComponent(ItemComponentKeys.WOLF_VARIANT);
    }

    public ItemStack withWolfVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.WOLF_VARIANT, variant);
    }

    public ItemStack withWolfVariant(WolfVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.WOLF_VARIANT, variant);
    }

    public ItemStack withoutWolfVariant() {
        return withoutComponent(ItemComponentKeys.WOLF_VARIANT);
    }

    public Optional<Key> wolfSoundVariant() {
        return keyComponent(ItemComponentKeys.WOLF_SOUND_VARIANT);
    }

    public ItemStack withWolfSoundVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.WOLF_SOUND_VARIANT, variant);
    }

    public ItemStack withWolfSoundVariant(WolfSoundVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.WOLF_SOUND_VARIANT, variant);
    }

    public ItemStack withoutWolfSoundVariant() {
        return withoutComponent(ItemComponentKeys.WOLF_SOUND_VARIANT);
    }

    public Optional<ItemDyeColor> wolfCollar() {
        return dyeColorComponent(ItemComponentKeys.WOLF_COLLAR);
    }

    public ItemStack withWolfCollar(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.WOLF_COLLAR, color);
    }

    public ItemStack withoutWolfCollar() {
        return withoutComponent(ItemComponentKeys.WOLF_COLLAR);
    }

    public Optional<ItemEntityVariant.Fox> foxVariant() {
        return stringComponent(ItemComponentKeys.FOX_VARIANT).map(ItemEntityVariant.Fox::fromSerializedName);
    }

    public ItemStack withFoxVariant(ItemEntityVariant.Fox variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.FOX_VARIANT, variant.serializedName());
    }

    public ItemStack withoutFoxVariant() {
        return withoutComponent(ItemComponentKeys.FOX_VARIANT);
    }

    public Optional<ItemEntityVariant.SalmonSize> salmonSize() {
        return stringComponent(ItemComponentKeys.SALMON_SIZE).map(ItemEntityVariant.SalmonSize::fromSerializedName);
    }

    public ItemStack withSalmonSize(ItemEntityVariant.SalmonSize size) {
        Objects.requireNonNull(size, "size");
        return withStringComponent(ItemComponentKeys.SALMON_SIZE, size.serializedName());
    }

    public ItemStack withoutSalmonSize() {
        return withoutComponent(ItemComponentKeys.SALMON_SIZE);
    }

    public Optional<ItemEntityVariant.Parrot> parrotVariant() {
        return stringComponent(ItemComponentKeys.PARROT_VARIANT).map(ItemEntityVariant.Parrot::fromSerializedName);
    }

    public ItemStack withParrotVariant(ItemEntityVariant.Parrot variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.PARROT_VARIANT, variant.serializedName());
    }

    public ItemStack withoutParrotVariant() {
        return withoutComponent(ItemComponentKeys.PARROT_VARIANT);
    }

    public Optional<ItemEntityVariant.TropicalFishPattern> tropicalFishPattern() {
        return stringComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN).map(ItemEntityVariant.TropicalFishPattern::fromSerializedName);
    }

    public ItemStack withTropicalFishPattern(ItemEntityVariant.TropicalFishPattern pattern) {
        Objects.requireNonNull(pattern, "pattern");
        return withStringComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN, pattern.serializedName());
    }

    public ItemStack withoutTropicalFishPattern() {
        return withoutComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN);
    }

    public Optional<ItemDyeColor> tropicalFishBaseColor() {
        return dyeColorComponent(ItemComponentKeys.TROPICAL_FISH_BASE_COLOR);
    }

    public ItemStack withTropicalFishBaseColor(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.TROPICAL_FISH_BASE_COLOR, color);
    }

    public ItemStack withoutTropicalFishBaseColor() {
        return withoutComponent(ItemComponentKeys.TROPICAL_FISH_BASE_COLOR);
    }

    public Optional<ItemDyeColor> tropicalFishPatternColor() {
        return dyeColorComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN_COLOR);
    }

    public ItemStack withTropicalFishPatternColor(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN_COLOR, color);
    }

    public ItemStack withoutTropicalFishPatternColor() {
        return withoutComponent(ItemComponentKeys.TROPICAL_FISH_PATTERN_COLOR);
    }

    public Optional<ItemEntityVariant.Mooshroom> mooshroomVariant() {
        return stringComponent(ItemComponentKeys.MOOSHROOM_VARIANT).map(ItemEntityVariant.Mooshroom::fromSerializedName);
    }

    public ItemStack withMooshroomVariant(ItemEntityVariant.Mooshroom variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.MOOSHROOM_VARIANT, variant.serializedName());
    }

    public ItemStack withoutMooshroomVariant() {
        return withoutComponent(ItemComponentKeys.MOOSHROOM_VARIANT);
    }

    public Optional<ItemEntityVariant.Rabbit> rabbitVariant() {
        return stringComponent(ItemComponentKeys.RABBIT_VARIANT).map(ItemEntityVariant.Rabbit::fromSerializedName);
    }

    public ItemStack withRabbitVariant(ItemEntityVariant.Rabbit variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.RABBIT_VARIANT, variant.serializedName());
    }

    public ItemStack withoutRabbitVariant() {
        return withoutComponent(ItemComponentKeys.RABBIT_VARIANT);
    }

    public Optional<Key> pigVariant() {
        return keyComponent(ItemComponentKeys.PIG_VARIANT);
    }

    public ItemStack withPigVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.PIG_VARIANT, variant);
    }

    public ItemStack withPigVariant(PigVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.PIG_VARIANT, variant);
    }

    public ItemStack withoutPigVariant() {
        return withoutComponent(ItemComponentKeys.PIG_VARIANT);
    }

    public Optional<Key> pigSoundVariant() {
        return keyComponent(ItemComponentKeys.PIG_SOUND_VARIANT);
    }

    public ItemStack withPigSoundVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.PIG_SOUND_VARIANT, variant);
    }

    public ItemStack withPigSoundVariant(PigSoundVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.PIG_SOUND_VARIANT, variant);
    }

    public ItemStack withoutPigSoundVariant() {
        return withoutComponent(ItemComponentKeys.PIG_SOUND_VARIANT);
    }

    public Optional<Key> cowVariant() {
        return keyComponent(ItemComponentKeys.COW_VARIANT);
    }

    public ItemStack withCowVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.COW_VARIANT, variant);
    }

    public ItemStack withCowVariant(CowVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.COW_VARIANT, variant);
    }

    public ItemStack withoutCowVariant() {
        return withoutComponent(ItemComponentKeys.COW_VARIANT);
    }

    public Optional<Key> cowSoundVariant() {
        return keyComponent(ItemComponentKeys.COW_SOUND_VARIANT);
    }

    public ItemStack withCowSoundVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.COW_SOUND_VARIANT, variant);
    }

    public ItemStack withCowSoundVariant(CowSoundVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.COW_SOUND_VARIANT, variant);
    }

    public ItemStack withoutCowSoundVariant() {
        return withoutComponent(ItemComponentKeys.COW_SOUND_VARIANT);
    }

    public Optional<Key> chickenVariant() {
        return keyComponent(ItemComponentKeys.CHICKEN_VARIANT);
    }

    public ItemStack withChickenVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.CHICKEN_VARIANT, variant);
    }

    public ItemStack withChickenVariant(ChickenVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.CHICKEN_VARIANT, variant);
    }

    public ItemStack withoutChickenVariant() {
        return withoutComponent(ItemComponentKeys.CHICKEN_VARIANT);
    }

    public Optional<Key> chickenSoundVariant() {
        return keyComponent(ItemComponentKeys.CHICKEN_SOUND_VARIANT);
    }

    public ItemStack withChickenSoundVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.CHICKEN_SOUND_VARIANT, variant);
    }

    public ItemStack withChickenSoundVariant(ChickenSoundVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.CHICKEN_SOUND_VARIANT, variant);
    }

    public ItemStack withoutChickenSoundVariant() {
        return withoutComponent(ItemComponentKeys.CHICKEN_SOUND_VARIANT);
    }

    public Optional<Key> zombieNautilusVariant() {
        return keyComponent(ItemComponentKeys.ZOMBIE_NAUTILUS_VARIANT);
    }

    public ItemStack withZombieNautilusVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.ZOMBIE_NAUTILUS_VARIANT, variant);
    }

    public ItemStack withZombieNautilusVariant(ZombieNautilusVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.ZOMBIE_NAUTILUS_VARIANT, variant);
    }

    public ItemStack withoutZombieNautilusVariant() {
        return withoutComponent(ItemComponentKeys.ZOMBIE_NAUTILUS_VARIANT);
    }

    public Optional<Key> frogVariant() {
        return keyComponent(ItemComponentKeys.FROG_VARIANT);
    }

    public ItemStack withFrogVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.FROG_VARIANT, variant);
    }

    public ItemStack withFrogVariant(FrogVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.FROG_VARIANT, variant);
    }

    public ItemStack withoutFrogVariant() {
        return withoutComponent(ItemComponentKeys.FROG_VARIANT);
    }

    public Optional<ItemEntityVariant.Horse> horseVariant() {
        return stringComponent(ItemComponentKeys.HORSE_VARIANT).map(ItemEntityVariant.Horse::fromSerializedName);
    }

    public ItemStack withHorseVariant(ItemEntityVariant.Horse variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.HORSE_VARIANT, variant.serializedName());
    }

    public ItemStack withoutHorseVariant() {
        return withoutComponent(ItemComponentKeys.HORSE_VARIANT);
    }

    public Optional<Key> paintingVariant() {
        return keyComponent(ItemComponentKeys.PAINTING_VARIANT);
    }

    public ItemStack withPaintingVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.PAINTING_VARIANT, variant);
    }

    public ItemStack withPaintingVariant(PaintingVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.PAINTING_VARIANT, variant);
    }

    public ItemStack withoutPaintingVariant() {
        return withoutComponent(ItemComponentKeys.PAINTING_VARIANT);
    }

    public Optional<ItemEntityVariant.Llama> llamaVariant() {
        return stringComponent(ItemComponentKeys.LLAMA_VARIANT).map(ItemEntityVariant.Llama::fromSerializedName);
    }

    public ItemStack withLlamaVariant(ItemEntityVariant.Llama variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.LLAMA_VARIANT, variant.serializedName());
    }

    public ItemStack withoutLlamaVariant() {
        return withoutComponent(ItemComponentKeys.LLAMA_VARIANT);
    }

    public Optional<ItemEntityVariant.Axolotl> axolotlVariant() {
        return stringComponent(ItemComponentKeys.AXOLOTL_VARIANT).map(ItemEntityVariant.Axolotl::fromSerializedName);
    }

    public ItemStack withAxolotlVariant(ItemEntityVariant.Axolotl variant) {
        Objects.requireNonNull(variant, "variant");
        return withStringComponent(ItemComponentKeys.AXOLOTL_VARIANT, variant.serializedName());
    }

    public ItemStack withoutAxolotlVariant() {
        return withoutComponent(ItemComponentKeys.AXOLOTL_VARIANT);
    }

    public Optional<Key> catVariant() {
        return keyComponent(ItemComponentKeys.CAT_VARIANT);
    }

    public ItemStack withCatVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.CAT_VARIANT, variant);
    }

    public ItemStack withCatVariant(CatVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.CAT_VARIANT, variant);
    }

    public ItemStack withoutCatVariant() {
        return withoutComponent(ItemComponentKeys.CAT_VARIANT);
    }

    public Optional<Key> catSoundVariant() {
        return keyComponent(ItemComponentKeys.CAT_SOUND_VARIANT);
    }

    public ItemStack withCatSoundVariant(Key variant) {
        return withKeyComponent(ItemComponentKeys.CAT_SOUND_VARIANT, variant);
    }

    public ItemStack withCatSoundVariant(CatSoundVariantKey variant) {
        return withKeyComponent(ItemComponentKeys.CAT_SOUND_VARIANT, variant);
    }

    public ItemStack withoutCatSoundVariant() {
        return withoutComponent(ItemComponentKeys.CAT_SOUND_VARIANT);
    }

    public Optional<ItemDyeColor> catCollar() {
        return dyeColorComponent(ItemComponentKeys.CAT_COLLAR);
    }

    public ItemStack withCatCollar(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.CAT_COLLAR, color);
    }

    public ItemStack withoutCatCollar() {
        return withoutComponent(ItemComponentKeys.CAT_COLLAR);
    }

    public Optional<ItemDyeColor> sheepColor() {
        return dyeColorComponent(ItemComponentKeys.SHEEP_COLOR);
    }

    public ItemStack withSheepColor(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.SHEEP_COLOR, color);
    }

    public ItemStack withoutSheepColor() {
        return withoutComponent(ItemComponentKeys.SHEEP_COLOR);
    }

    public Optional<ItemDyeColor> shulkerColor() {
        return dyeColorComponent(ItemComponentKeys.SHULKER_COLOR);
    }

    public ItemStack withShulkerColor(ItemDyeColor color) {
        return withDyeColorComponent(ItemComponentKeys.SHULKER_COLOR, color);
    }

    public ItemStack withoutShulkerColor() {
        return withoutComponent(ItemComponentKeys.SHULKER_COLOR);
    }

    public static ItemStack empty() {
        return EMPTY;
    }

    private Optional<Component> textComponent(Key key) {
        return component(key).map(ItemStack::deserializeComponent);
    }

    private ItemStack withTextComponent(Key key, Component component) {
        Objects.requireNonNull(component, "component");
        return withComponent(key, serializeComponent(component));
    }

    private Optional<JsonObject> customDataComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonObject)
                .map(element -> element.getAsJsonObject().deepCopy());
    }

    private ItemStack withJsonObjectComponent(Key key, JsonObject value) {
        Objects.requireNonNull(value, "value");
        return withComponent(key, value);
    }

    private Optional<String> stringComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString);
    }

    private ItemStack withStringComponent(Key key, String value) {
        Objects.requireNonNull(value, "value");
        return withComponent(key, new JsonPrimitive(value));
    }

    private <T> Optional<T> dataComponent(Key key, Function<JsonElement, T> reader) {
        Objects.requireNonNull(reader, "reader");
        return component(key).map(reader);
    }

    private ItemStack withComponentData(Key key, ItemComponentData value) {
        Objects.requireNonNull(value, "value");
        return withComponent(key, value.toJson());
    }

    private ItemStack withUnitComponent(Key key, boolean present) {
        return present ? withComponent(key, new JsonObject()) : withoutComponent(key);
    }

    private Optional<ItemDyeColor> dyeColorComponent(Key key) {
        return stringComponent(key).map(ItemDyeColor::fromSerializedName);
    }

    private ItemStack withDyeColorComponent(Key key, ItemDyeColor color) {
        Objects.requireNonNull(color, "color");
        return withStringComponent(key, color.serializedName());
    }

    private List<Key> keyListComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonArray)
                .stream()
                .flatMap(element -> element.getAsJsonArray().asList().stream())
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(Key::key)
                .toList();
    }

    private ItemStack withKeyListComponent(Key key, List<Key> values, String name) {
        Objects.requireNonNull(values, name);
        var array = new JsonArray();
        values.forEach(value -> array.add(Objects.requireNonNull(value, name + " entry").asString()));
        return withComponent(key, array);
    }

    private List<ItemTemplate> itemTemplates(Key key) {
        return component(key)
                .filter(JsonElement::isJsonArray)
                .stream()
                .flatMap(element -> element.getAsJsonArray().asList().stream())
                .map(ItemTemplate::fromJson)
                .toList();
    }

    private ItemStack withItemTemplates(Key key, List<ItemTemplate> templates, String name) {
        Objects.requireNonNull(templates, name);
        var array = new JsonArray();
        templates.forEach(template -> array.add(Objects.requireNonNull(template, name + " entry").toJson()));
        return withComponent(key, array);
    }

    private Optional<Integer> intComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt);
    }

    private Optional<Float> floatComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsFloat);
    }

    private Optional<Integer> objectIntComponent(Key key, String property) {
        return component(key)
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .filter(object -> object.has(property) && object.get(property).isJsonPrimitive())
                .map(object -> object.get(property).getAsInt());
    }

    private Optional<Key> keyComponent(Key key) {
        return component(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString)
                .map(Key::key);
    }

    private ItemStack withKeyComponent(Key componentKey, Key value) {
        Objects.requireNonNull(value, "value");
        return withComponent(componentKey, new JsonPrimitive(value.asString()));
    }

    private ItemStack withKeyComponent(Key componentKey, VanillaKey value) {
        Objects.requireNonNull(value, "value");
        return withKeyComponent(componentKey, value.key());
    }

    private ItemStack withNonNegativeInt(Key key, int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be >= 0");
        }
        return withComponent(key, new JsonPrimitive(value));
    }

    private ItemStack withRgbComponent(Key key, int rgb) {
        if (rgb < 0 || rgb > 0xFFFFFF) {
            throw new IllegalArgumentException("rgb must be in 0x000000..0xFFFFFF");
        }
        return withComponent(key, new JsonPrimitive(rgb));
    }

    private static JsonElement serializeComponent(Component component) {
        return GsonComponentSerializer.gson().serializeToTree(component);
    }

    private static Component deserializeComponent(JsonElement component) {
        return GsonComponentSerializer.gson().deserializeFromTree(component);
    }

    private static int maxStackSize(ItemType type, ItemComponents components) {
        return components.get(ItemComponentKeys.MAX_STACK_SIZE)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsInt)
                .orElseGet(type::maxStackSize);
    }
}
