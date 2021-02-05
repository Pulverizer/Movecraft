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
package io.github.pulverizer.movecraft.mixin;

import com.flowpowered.math.vector.Vector3d;
import com.google.common.collect.Sets;
import io.github.pulverizer.movecraft.config.Settings;
import io.github.pulverizer.movecraft.craft.Craft;
import io.github.pulverizer.movecraft.craft.CraftManager;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentProtection;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.event.SpongeEventFactory;
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.explosion.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.common.SpongeImpl;
import org.spongepowered.common.bridge.world.ExplosionBridge;
import org.spongepowered.common.event.ShouldFire;
import org.spongepowered.common.util.VecHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Mixin(value = net.minecraft.world.Explosion.class, priority = 1001)
public abstract class ExplosionMixin_Movecraft {

    @Shadow @Final private List<BlockPos> affectedBlockPositions;
    @Shadow @Final private Map<EntityPlayer, Vec3d> playerKnockbackMap;
    @Shadow @Final private Random random;
    @Shadow @Final private net.minecraft.world.World world;
    @Shadow @Final private double x;
    @Shadow @Final private double y;
    @Shadow @Final private double z;
    @Shadow @Final private Entity exploder;
    @Shadow @Final private float size;

    /**
     * @author gabizou - September 8th, 2016
     * @reason Rewrites to use our own hooks that will patch with forge perfectly well,
     * and allows for maximal capability.
     *
     * @author BernardisGood - July 17th, 2020
     * @reason Movecraft - Configurable blast resistances for armour, all entities knockback reduced by obstructions, and players also benefit from
     * the knockback reducing effects of the Blast Protection enchantment
     */
    @Overwrite
    public void doExplosionA() {
        // Sponge Start - If the explosion should not break blocks, don't bother calculating it
        if (((ExplosionBridge) this).bridge$getShouldDamageBlocks()) {
            final Set<BlockPos> set = Sets.newHashSet();

            for (int j = 0; j < ((ExplosionBridge) this).bridge$getResolution(); ++j) {
                for (int k = 0; k < ((ExplosionBridge) this).bridge$getResolution(); ++k) {
                    for (int l = 0; l < ((ExplosionBridge) this).bridge$getResolution(); ++l) {

                        if (j == 0 || j == ((ExplosionBridge) this).bridge$getResolution() - 1
                                || k == 0 || k == ((ExplosionBridge) this).bridge$getResolution() - 1
                                || l == 0 || l == ((ExplosionBridge) this).bridge$getResolution() - 1) {

                            double d0 = ((float) j / (float) (((ExplosionBridge) this).bridge$getResolution() - 1) * 2.0F - 1.0F);
                            double d1 = ((float) k / (float) (((ExplosionBridge) this).bridge$getResolution() - 1) * 2.0F - 1.0F);
                            double d2 = ((float) l / (float) (((ExplosionBridge) this).bridge$getResolution() - 1) * 2.0F - 1.0F);
                            final double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                            d0 = d0 / d3;
                            d1 = d1 / d3;
                            d2 = d2 / d3;
                            //    f =   radius  * ( 1   + (([     random between 0 and 0.6     ] - 0.3 ) * [ randomness  ]))
                            float f = this.size * (1.0F + (((this.world.rand.nextFloat() * 0.6F) - 0.3F) * ((ExplosionBridge) this)
                                    .bridge$getRandomness()));
                            //Sponge End
                            double d4 = this.x;
                            double d6 = this.y;
                            double d8 = this.z;

                            for (final float f1 = 0.3F; f > 0.0F; f -= 0.22500001F) {
                                final BlockPos blockpos = new BlockPos(d4, d6, d8);
                                final IBlockState iblockstate = this.world.getBlockState(blockpos);


                                if (iblockstate.getMaterial() != Material.AIR) {
                                    // Movecraft Start
                                    final BlockType blockType = ((BlockState) iblockstate).getType();
                                    double resistance;

                                    if (Settings.DurabilityOverride != null && Settings.DurabilityOverride.containsKey(blockType)) {
                                        resistance = Settings.DurabilityOverride.get(blockType).get(0);
                                        System.out.printf("%s : %.2f%n", blockType.getName(), resistance);

                                    } else {
                                        resistance = this.exploder != null
                                                ? this.exploder.getExplosionResistance((net.minecraft.world.Explosion) (Object) this
                                                , this.world, blockpos, iblockstate)
                                                : iblockstate.getBlock().getExplosionResistance((Entity) null);
                                    }

                                    //TODO: Add to config
                                    double bonus = 1;

                                    for (Craft craft : CraftManager.getInstance().getCraftsFromLocation(new Location<>((World) world,
                                            VecHelper.toVector3i(blockpos)))) {
                                        double testNum = 1 + Math.min((double) craft.getSize() / 50000, 1);

                                        if (testNum > bonus) {
                                            bonus = testNum;
                                        }
                                    }

                                    resistance = resistance * bonus;

                                    if (Settings.DurabilityOverride != null && Settings.DurabilityOverride.containsKey(blockType)) {
                                        double rayStrength = (f * 3) - 0.3;

                                        if (rayStrength <= resistance && rayStrength > Settings.DurabilityOverride.get(blockType).get(1) * bonus) {
                                            int chance = (int) ((rayStrength / resistance)
                                                    * Settings.DurabilityOverride.get(blockType).get(2) * 10
                                                    * Math.max(Math.min(1 - (new Vector3d(d4, d6, d8).distance(x, y, z) / (double) size), 1), 0));

                                            if (random.nextInt(1000) <= chance) {
                                                resistance = rayStrength - 0.3;
                                            }
                                        }
                                    }

                                    f -= (resistance + 0.3F) * 0.3F;
                                    // Movecraft End
                                }

                                if (f > 0.0F && (this.exploder == null || this.exploder
                                        .canExplosionDestroyBlock((net.minecraft.world.Explosion) (Object) this, this.world, blockpos, iblockstate,
                                                f))) {
                                    set.add(blockpos);
                                }

                                d4 += d0 * 0.30000001192092896D;
                                d6 += d1 * 0.30000001192092896D;
                                d8 += d2 * 0.30000001192092896D;
                            }
                        }
                    }
                }
            }

            this.affectedBlockPositions.addAll(set);
        } // Sponge - Finish if statement
        final float f3 = this.size * 2.0F;
        final int k1 = MathHelper.floor(this.x - (double) f3 - 1.0D);
        final int l1 = MathHelper.floor(this.x + (double) f3 + 1.0D);
        final int i2 = MathHelper.floor(this.y - (double) f3 - 1.0D);
        final int i1 = MathHelper.floor(this.y + (double) f3 + 1.0D);
        final int j2 = MathHelper.floor(this.z - (double) f3 - 1.0D);
        final int j1 = MathHelper.floor(this.z + (double) f3 + 1.0D);

        // Sponge Start - Check if this explosion should damage entities
        final List<Entity> list = ((ExplosionBridge) this).bridge$getShouldDamageEntities()
                ? this.world.getEntitiesWithinAABBExcludingEntity(this.exploder,
                new AxisAlignedBB(k1, i2, j2, l1, i1, j1))
                : Collections.emptyList();
        // Now we can throw our Detonate Event
        if (ShouldFire.EXPLOSION_EVENT_DETONATE) {
            final List<Location<World>> blockPositions = new ArrayList<>(this.affectedBlockPositions.size());
            final List<org.spongepowered.api.entity.Entity> entities = new ArrayList<>(list.size());
            for (final BlockPos pos : this.affectedBlockPositions) {
                blockPositions.add(new Location<>((World) this.world, pos.getX(), pos.getY(), pos.getZ()));
            }
            for (final Entity entity : list) {
                // Make sure to check the entity is immune first.
                if (!entity.isImmuneToExplosions()) {
                    entities.add((org.spongepowered.api.entity.Entity) entity);
                }
            }
            final Cause cause = Sponge.getCauseStackManager().getCurrentCause();
            final ExplosionEvent.Detonate detonate =
                    SpongeEventFactory.createExplosionEventDetonate(cause, blockPositions, entities, (Explosion) this, (World) this.world);
            SpongeImpl.postEvent(detonate);
            // Clear the positions so that they can be pulled from the event
            this.affectedBlockPositions.clear();
            if (detonate.isCancelled()) {
                return;
            }
            if (((ExplosionBridge) this).bridge$getShouldDamageBlocks()) {
                for (final Location<World> worldLocation : detonate.getAffectedLocations()) {
                    this.affectedBlockPositions.add(VecHelper.toBlockPos(worldLocation));
                }
            }
            // Clear the list of entities so they can be pulled from the event.
            list.clear();
            if (((ExplosionBridge) this).bridge$getShouldDamageEntities()) {
                for (final org.spongepowered.api.entity.Entity entity : detonate.getEntities()) {
                    try {
                        list.add((Entity) entity);
                    } catch (final Exception e) {
                        // Do nothing, a plugin tried to use the wrong entity somehow.
                    }
                }
            }
        }
        // Sponge End

        final Vec3d vec3d = new Vec3d(this.x, this.y, this.z);

        for (final Entity entity : list) {
            if (!entity.isImmuneToExplosions()) {
                final double d12 = entity.getDistance(this.x, this.y, this.z) / (double) f3;

                if (d12 <= 1.0D) {
                    double d5 = entity.posX - this.x;
                    double d7 = entity.posY + (double) entity.getEyeHeight() - this.y;
                    double d9 = entity.posZ - this.z;
                    final double d13 = MathHelper.sqrt(d5 * d5 + d7 * d7 + d9 * d9);

                    if (d13 != 0.0D) {
                        d5 = d5 / d13;
                        d7 = d7 / d13;
                        d9 = d9 / d13;
                        final double d14 = this.world.getBlockDensity(vec3d, entity.getEntityBoundingBox());
                        final double d10 = (1.0D - d12) * d14;
                        entity.attackEntityFrom(
                                DamageSource.causeExplosionDamage((net.minecraft.world.Explosion) (Object) this),
                                (float) ((int) ((d10 * d10 + d10) / 2.0D * 7.0D * (double) f3 + 1.0D)));
                        //Movecraft - all entities should have knockback reduced by obstructions
                        double d11 = d10;

                        if (entity instanceof EntityLivingBase) {
                            d11 = EnchantmentProtection.getBlastDamageReduction((EntityLivingBase) entity, d10);
                        }

                        //Sponge Start
                        entity.motionX += d5 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2;
                        entity.motionY += d7 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2;
                        entity.motionZ += d9 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2;

                        if (entity instanceof EntityPlayer) {
                            final EntityPlayer entityplayer = (EntityPlayer) entity;

                            if (!entityplayer.isSpectator() && (!entityplayer.isCreative() || !entityplayer.capabilities.isFlying)) {
                                //Movecraft - players should also benefit from Blast Protection enchant reducing knockback
                                this.playerKnockbackMap.put(entityplayer,
                                        new Vec3d(d5 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2,
                                                d7 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2,
                                                d9 * d11 * ((ExplosionBridge) this).bridge$getKnockback() * 2));
                                //Sponge End
                            }
                        }
                    }
                }
            }
        }
    }
}
