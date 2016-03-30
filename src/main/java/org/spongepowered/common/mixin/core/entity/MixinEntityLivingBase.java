/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.mixin.core.entity;

import com.flowpowered.math.vector.Vector3d;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.CombatTracker;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.DataManipulator;
import org.spongepowered.api.data.manipulator.mutable.entity.DamageableData;
import org.spongepowered.api.data.manipulator.mutable.entity.HealthData;
import org.spongepowered.api.data.value.mutable.MutableBoundedValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.entity.Entity;
import org.spongepowered.api.entity.living.Living;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.cause.entity.damage.DamageModifier;
import org.spongepowered.api.event.cause.entity.damage.source.FallingBlockDamageSource;
import org.spongepowered.api.event.entity.DamageEntityEvent;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.util.Tuple;
import org.spongepowered.api.util.annotation.NonnullByDefault;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.common.data.manipulator.mutable.entity.SpongeHealthData;
import org.spongepowered.common.data.value.SpongeValueFactory;
import org.spongepowered.common.data.value.mutable.SpongeOptionalValue;
import org.spongepowered.common.entity.living.human.EntityHuman;
import org.spongepowered.common.event.DamageEventHandler;
import org.spongepowered.common.event.DamageObject;
import org.spongepowered.common.interfaces.entity.IMixinEntityLivingBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;

@SuppressWarnings("rawtypes")
@NonnullByDefault
@Mixin(value = EntityLivingBase.class, priority = 999)
public abstract class MixinEntityLivingBase extends MixinEntity implements Living, IMixinEntityLivingBase {

    private static final String WORLD_SPAWN_PARTICLE = "Lnet/minecraft/world/World;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDDDD[I)V";

    private EntityLivingBase nmsEntityLiving = (EntityLivingBase) (Object) this;
    private int maxAir = 300;
    private DamageSource lastDamageSource;

    @Shadow public int maxHurtResistantTime;
    @Shadow public int hurtTime;
    @Shadow public int maxHurtTime;
    @Shadow public int deathTime;
    @Shadow public boolean potionsNeedUpdate;
    @Shadow public CombatTracker _combatTracker;
    @Shadow public EntityLivingBase entityLivingToAttack;
    @Shadow protected AbstractAttributeMap attributeMap;
    @Shadow public ItemStack[] armorArray;
    @Shadow protected int entityAge;
    @Shadow protected int recentlyHit;
    @Shadow protected float lastDamage;
    @Shadow protected EntityPlayer attackingPlayer;
    @Shadow protected abstract void damageArmor(float p_70675_1_);
    @Shadow protected abstract void setBeenAttacked();
    @Shadow protected abstract SoundEvent getDeathSound();
    @Shadow protected abstract float getSoundVolume();
    @Shadow protected abstract float getSoundPitch();
    @Shadow protected abstract SoundEvent getHurtSound();
    @Shadow public abstract void setHealth(float health);
    @Shadow public abstract void addPotionEffect(net.minecraft.potion.PotionEffect potionEffect);
    @Shadow protected abstract void markPotionsDirty();
    @Shadow public abstract void setItemStackToSlot(EntityEquipmentSlot slotIn, ItemStack stack);
    @Shadow public abstract void clearActivePotions();
    @Shadow public abstract void setLastAttacker(net.minecraft.entity.Entity entity);
    @Shadow public abstract boolean isPotionActive(Potion potion);
    @Shadow public abstract float getHealth();
    @Shadow public abstract float getMaxHealth();
    @Shadow public abstract float getRotationYawHead();
    @Shadow public abstract void setRotationYawHead(float rotation);
    @Shadow public abstract Collection getActivePotionEffects();
    @Shadow @Nullable public abstract EntityLivingBase getLastAttacker();
    @Shadow public abstract IAttributeInstance getEntityAttribute(IAttribute attribute);
    @Shadow public abstract ItemStack getItemStackFromSlot(EntityEquipmentSlot slotIn);
    @Shadow protected abstract void applyEntityAttributes();
    @Shadow protected abstract void func_184581_c(net.minecraft.util.DamageSource p_184581_1_);
    @Shadow protected abstract boolean func_184583_d(DamageSource p_184583_1_);
    @Shadow protected abstract void func_184590_k(float p_184590_1_);
    @Shadow public abstract void func_184598_c(EnumHand hand);
    @Shadow public abstract ItemStack getHeldItem(EnumHand hand);
    @Shadow public abstract boolean func_184587_cr();

    @Override
    public Vector3d getHeadRotation() {
        // pitch, yaw, roll -- Minecraft does not currently support head roll
        return new Vector3d(getRotation().getX(), getRotationYawHead(), 0);
    }

    @Override
    public void setHeadRotation(Vector3d rotation) {
        setRotation(getRotation().mul(0, 1, 1).add(rotation.getX(), 0, 0));
        setRotationYawHead((float) rotation.getY());
    }

    @Override
    public int getMaxAir() {
        return this.maxAir;
    }

    @Override
    public void setMaxAir(int air) {
        this.maxAir = air;
    }

    @Override
    public double getLastDamage() {
        return this.lastDamage;
    }

    @Override
    public void setLastDamage(double damage) {
        this.lastDamage = (float) damage;
    }

    @Override
    public void readFromNbt(NBTTagCompound compound) {
        super.readFromNbt(compound);
        if (compound.hasKey("maxAir")) {
            this.maxAir = compound.getInteger("maxAir");
        }
    }

    @Override
    public void writeToNbt(NBTTagCompound compound) {
        super.writeToNbt(compound);
        compound.setInteger("maxAir", this.maxAir);
    }

    @Override
    public Text getTeamRepresentation() {
        return Text.of(this.getUniqueID().toString());
    }

    @Redirect(method = "onDeath(Lnet/minecraft/util/DamageSource;)V", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/World;setEntityState(Lnet/minecraft/entity/Entity;B)V"))
    public void onDeathSendEntityState(World world, net.minecraft.entity.Entity self, byte state) {
        // Don't send the state if this is a human. Fixes ghost items on client.
        if (!((net.minecraft.entity.Entity) (Object) this instanceof EntityHuman)) {
            world.setEntityState(self, state);
        }
    }

    @Redirect(method = "applyPotionDamageCalculations", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isPotionActive(Lnet/minecraft/potion/Potion;)Z") )
    public boolean onIsPotionActive(EntityLivingBase entityIn, Potion potion) {
        return false; // handled in our damageEntityHook
    }

    @Redirect(method = "applyArmorCalculations", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;damageArmor(F)V") )
    protected void onDamageArmor(EntityLivingBase entityIn, float damage) {
        // do nothing as this is handled in our damageEntityHook
    }

    /**
     * @author bloodmc - November 21, 2015
     *
     * Purpose: This shouldn't be used internally but a mod may still call it so we simply reroute to our hook.
     */
    @Overwrite
    protected void damageEntity(DamageSource damageSource, float damage) {
        this.damageEntityHook(damageSource, damage);
    }

    /**
     * @author bloodmc - November 22, 2015
     *
     * Purpose: Reroute damageEntity calls to our hook in order to prevent damage.
     */
    @Override
    @Overwrite
    public boolean attackEntityFrom(DamageSource source, float amount) {
        this.lastDamageSource = source;
        if (source == null) {
            Thread.dumpStack();
        }
        if (!hookModAttack(this.nmsEntityLiving, source, amount)) {
            return false;
        }
        if (this.nmsEntityLiving.isEntityInvulnerable(source)) {
            return false;
        } else if (this.worldObj.isRemote) {
            return false;
        } else {
            this.entityAge = 0;

            if (this.nmsEntityLiving.getHealth() <= 0.0F) {
                return false;
            } else if (source.isFireDamage() && this.nmsEntityLiving.isPotionActive(MobEffects.fireResistance)) {
                return false;
            } else {
                // Sponge - ignore as this is handled in our damageEntityHook
                if (false && (source == DamageSource.anvil || source == DamageSource.fallingBlock)
                        && this.nmsEntityLiving.getItemStackFromSlot(EntityEquipmentSlot.HEAD) != null) {
                    this.nmsEntityLiving.getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem((int) (amount * 4.0F + this.rand.nextFloat() *
                            amount * 2.0F), this.nmsEntityLiving);
                    amount *= 0.75F;
                }

                boolean flag = false;

                if (amount > 0.0F && this.func_184583_d(source)) {
                    this.func_184590_k(amount);

                    if (source.isProjectile()) {
                        amount = 0.0F;
                    } else {
                        amount *= 0.33F;

                        if (source.getSourceOfDamage() instanceof EntityLivingBase) {
                            ((EntityLivingBase) source.getSourceOfDamage())
                                    .knockBack(this.nmsEntityLiving, 0.5F, this.posX - source.getSourceOfDamage().posX, this
                                            .posZ - source.getSourceOfDamage().posZ);
                        }
                    }

                    flag = true;
                }

                this.nmsEntityLiving.limbSwingAmount = 1.5F;
                boolean flag1 = true;

                if ((float) this.hurtResistantTime > (float) this.nmsEntityLiving.maxHurtResistantTime / 2.0F) {
                    if (amount <= this.lastDamage) {
                        return false;
                    }

                    // Sponge start - reroute to our damage hook
                    if (!this.damageEntityHook(source, amount - this.lastDamage)) {
                        return false;
                    }
                    // Sponge end

                    this.lastDamage = amount;
                    flag1 = false;
                } else {
                    // Sponge start - reroute to our damage hook
                    if (!this.damageEntityHook(source, amount)) {
                        return false;
                    }
                    this.lastDamage = amount;
                    this.hurtResistantTime = this.nmsEntityLiving.maxHurtResistantTime;
                    // this.damageEntity(source, amount); // handled above
                    // Sponge end
                    this.nmsEntityLiving.hurtTime = this.nmsEntityLiving.maxHurtTime = 10;
                }

                this.nmsEntityLiving.attackedAtYaw = 0.0F;
                net.minecraft.entity.Entity entity = source.getEntity();

                if (entity != null) {
                    if (entity instanceof EntityLivingBase) {
                        this.nmsEntityLiving.setRevengeTarget((EntityLivingBase) entity);
                    }

                    if (entity instanceof EntityPlayer) {
                        this.recentlyHit = 100;
                        this.attackingPlayer = (EntityPlayer) entity;
                    } else if (entity instanceof EntityWolf) {
                        EntityWolf entitywolf = (EntityWolf) entity;

                        if (entitywolf.isTamed()) {
                            this.recentlyHit = 100;
                            this.attackingPlayer = null;
                        }
                    }
                }

                if (flag1) {
                    if (flag) {
                        this.worldObj.setEntityState(this.nmsEntityLiving, (byte) 29);
                    } else if (source instanceof net.minecraft.util.EntityDamageSource && ((net.minecraft.util.EntityDamageSource) source)
                            .getIsThornsDamage()) {
                        this.worldObj.setEntityState(this.nmsEntityLiving, (byte) 33);
                    } else {
                        this.worldObj.setEntityState(this.nmsEntityLiving, (byte) 2);
                    }

                    if (source != DamageSource.drown && (!flag || amount > 0.0F)) {
                        this.setBeenAttacked();
                    }

                    if (entity != null) {
                        double d1 = entity.posX - this.posX;
                        double d0;

                        for (d0 = entity.posZ - this.posZ; d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D) {
                            d1 = (Math.random() - Math.random()) * 0.01D;
                        }

                        this.nmsEntityLiving.attackedAtYaw = (float) (net.minecraft.util.math.MathHelper.atan2(d0, d1) * (180D / Math.PI) - (double)
                                this.rotationYaw);
                        this.nmsEntityLiving.knockBack(entity, 0.4F, d1, d0);
                    } else {
                        this.nmsEntityLiving.attackedAtYaw = (float) ((int) (Math.random() * 2.0D) * 180);
                    }
                }

                if (this.getHealth() <= 0.0F) {
                    SoundEvent soundevent = this.getDeathSound();

                    if (flag1 && soundevent != null) {
                        this.nmsEntityLiving.playSound(soundevent, this.getSoundVolume(), this.getSoundPitch());
                    }

                    this.nmsEntityLiving.onDeath(source);
                } else if (flag1) {
                    this.func_184581_c(source);
                }

                return !flag || amount > 0.0F;
            }
        }
    }

    /**
     * @author gabizou - January 4th, 2016
     * This is necessary for invisibility checks so that invisible players don't actually send the particle stuffs.
     */
    @Redirect(method = "updateItemUse", at = @At(value = "INVOKE", target = WORLD_SPAWN_PARTICLE))
    public void spawnItemParticle(World world, EnumParticleTypes particleTypes, double xCoord, double yCoord, double zCoord, double xOffset,
            double yOffset, double zOffset, int ... p_175688_14_) {
        if (!this.isVanished()) {
            this.worldObj.spawnParticle(particleTypes, xCoord, yCoord, zCoord, xOffset, yOffset, zOffset, p_175688_14_);
        }
    }

    @Override
    public boolean damageEntityHook(DamageSource damageSource, float damage) {
        if (!this.nmsEntityLiving.isEntityInvulnerable(damageSource)) {
            final boolean human = this.nmsEntityLiving instanceof EntityPlayer;
            // apply forge damage hook
            damage = applyModDamage(this.nmsEntityLiving, damageSource, damage);
            float originalDamage = damage; // set after forge hook.
            if (damage <= 0) {
                damage = 0;
            }

            List<Tuple<DamageModifier, Function<? super Double, Double>>> originalFunctions = new ArrayList<>();
            Optional<Tuple<DamageModifier, Function<? super Double, Double>>> hardHatFunction =
                DamageEventHandler.createHardHatModifier(this.nmsEntityLiving, damageSource);
            Optional<List<Tuple<DamageModifier, Function<? super Double, Double>>>> armorFunction =
                provideArmorModifiers(this.nmsEntityLiving, damageSource, damage);
            Optional<Tuple<DamageModifier, Function<? super Double, Double>>> resistanceFunction =
                DamageEventHandler.createResistanceModifier(this.nmsEntityLiving, damageSource);
            Optional<List<Tuple<DamageModifier, Function<? super Double, Double>>>> armorEnchantments =
                DamageEventHandler.createEnchantmentModifiers(this.nmsEntityLiving, damageSource);
            Optional<Tuple<DamageModifier, Function<? super Double, Double>>> absorptionFunction =
                DamageEventHandler.createAbsorptionModifier(this.nmsEntityLiving, damageSource);

            if (hardHatFunction.isPresent()) {
                originalFunctions.add(hardHatFunction.get());
            }

            if (armorFunction.isPresent()) {
                originalFunctions.addAll(armorFunction.get());
            }

            if (resistanceFunction.isPresent()) {
                originalFunctions.add(resistanceFunction.get());
            }

            if (armorEnchantments.isPresent()) {
                originalFunctions.addAll(armorEnchantments.get());
            }

            if (absorptionFunction.isPresent()) {
                originalFunctions.add(absorptionFunction.get());
            }
            final Cause cause = DamageEventHandler.generateCauseFor(damageSource);

            DamageEntityEvent event = SpongeEventFactory.createDamageEntityEvent(cause, originalFunctions,
                         (Entity) this.nmsEntityLiving, originalDamage);
            Sponge.getEventManager().post(event);
            if (event.isCancelled()) {
                return false;
            }

            damage = (float) event.getFinalDamage();

            // Helmet
            if ((damageSource instanceof FallingBlockDamageSource) && this.nmsEntityLiving.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND) != null) {
                this.nmsEntityLiving.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).damageItem(
                    (int) (event.getBaseDamage() * 4.0F + this.rand.nextFloat() * event.getBaseDamage() * 2.0F), this.nmsEntityLiving);
            }

            // Armor
            if (!damageSource.isUnblockable()) {
                for (Tuple<DamageModifier, Function<? super Double, Double>> modifier : event.getModifiers()) {
                    applyArmorDamage(this.nmsEntityLiving, damageSource, event, modifier.getFirst());
                }
            }

            double absorptionModifier = 0;
            if (absorptionFunction.isPresent()) {
                absorptionModifier = event.getDamage(absorptionFunction.get().getFirst());
            }

            this.nmsEntityLiving.setAbsorptionAmount(Math.max(this.nmsEntityLiving.getAbsorptionAmount() + (float) absorptionModifier, 0.0F));
            if (damage != 0.0F) {
                if (human) {
                    ((EntityPlayer) this.nmsEntityLiving).addExhaustion(damageSource.getHungerDamage());
                }
                float f2 = this.nmsEntityLiving.getHealth();

                this.nmsEntityLiving.setHealth(f2 - damage);
                this.nmsEntityLiving.getCombatTracker().trackDamage(damageSource, f2, damage);

                if (human) {
                    return true;
                }

                this.nmsEntityLiving.setAbsorptionAmount(this.nmsEntityLiving.getAbsorptionAmount() - damage);
            }
            return true;
        }
        return false;
    }

    @Override
    public float applyModDamage(EntityLivingBase entityLivingBase, DamageSource source, float damage) {
        return damage;
    }

    @Override
    public Optional<List<Tuple<DamageModifier, Function<? super Double, Double>>>> provideArmorModifiers(EntityLivingBase entityLivingBase,
                                                                                                         DamageSource source, double damage) {
        return DamageEventHandler.createArmorModifiers(entityLivingBase, source, damage);
    }

    @Override
    public void applyArmorDamage(EntityLivingBase entityLivingBase, DamageSource source, DamageEntityEvent entityEvent, DamageModifier modifier) {
        Optional<DamageObject> optional = modifier.getCause().first(DamageObject.class);
        if (optional.isPresent()) {
            DamageEventHandler.acceptArmorModifier(this.nmsEntityLiving, source, modifier, entityEvent.getDamage(modifier));
        }
    }

    @Override
    public boolean hookModAttack(EntityLivingBase entityLivingBase, DamageSource source, float amount) {
        return true;
    }

    /**
     * @author gabizou - January 4th, 2016
     *
     * This allows invisiblity to ignore entity collisions.
     */
    @Overwrite
    public boolean canBeCollidedWith() {
        return !(this.isVanished() && this.ignoresCollision()) && !this.isDead;
    }

    @Override
    public DamageSource getLastDamageSource() {
        return this.lastDamageSource;
    }

    @Override
    public int getRecentlyHit() {
        return this.recentlyHit;
    }

    @Redirect(method = "updateFallState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/WorldServer;spawnParticle(Lnet/minecraft/util/EnumParticleTypes;DDDIDDDD[I)V"))
    private void spongeSpawnParticleForFallState(WorldServer worldServer, EnumParticleTypes particleTypes, double xCoord, double yCoord,
            double zCoord, int numberOfParticles, double xOffset, double yOffset, double zOffset, double particleSpeed, int... extraArgs) {
        if (!this.isVanished()) {
            worldServer.spawnParticle(particleTypes, xCoord, yCoord, zCoord, numberOfParticles, xOffset, yOffset, zOffset, particleSpeed, extraArgs);
        }

    }

    // Data delegated methods


    @Override
    public HealthData getHealthData() {
        return new SpongeHealthData(this.getHealth(), this.getMaxHealth());
    }

    @Override
    public MutableBoundedValue<Double> health() {
        return SpongeValueFactory.boundedBuilder(Keys.HEALTH)
                .minimum(0D)
                .maximum((double) this.getMaxHealth())
                .defaultValue((double) this.getMaxHealth())
                .actualValue((double) this.getHealth())
                .build();
    }

    @Override
    public MutableBoundedValue<Double> maxHealth() {
        return SpongeValueFactory.boundedBuilder(Keys.MAX_HEALTH)
                .minimum(1D)
                .maximum((double) Float.MAX_VALUE)
                .defaultValue(20D)
                .actualValue((double) this.getMaxHealth())
                .build();
    }

    // TODO uncomment when the processor is implemented
//    @Override
//    public DamageableData getMortalData() {
//        return null;
//    }

    @Override
    public OptionalValue<Living> lastAttacker() {
        return new SpongeOptionalValue<>(Keys.LAST_ATTACKER, Optional.ofNullable((Living) this.getLastAttacker()));
    }

    @Override
    public OptionalValue<Double> lastDamage() {
        return new SpongeOptionalValue<>(Keys.LAST_DAMAGE, Optional.ofNullable(this.getLastAttacker() == null ? null : (double) this.lastDamage));
    }

    @Override
    public void supplyVanillaManipulators(List<DataManipulator<?, ?>> manipulators) {
        super.supplyVanillaManipulators(manipulators);
        manipulators.add(getHealthData());
    }
}
