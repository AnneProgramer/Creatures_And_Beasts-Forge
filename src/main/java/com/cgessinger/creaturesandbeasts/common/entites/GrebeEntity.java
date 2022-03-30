package com.cgessinger.creaturesandbeasts.common.entites;

import java.util.Random;
import java.util.UUID;

import javax.annotation.Nullable;

import com.cgessinger.creaturesandbeasts.common.config.CNBConfig;
import com.cgessinger.creaturesandbeasts.common.goals.GoToWaterGoal;
import com.cgessinger.creaturesandbeasts.common.goals.MountAdultGoal;
import com.cgessinger.creaturesandbeasts.common.goals.SmoothSwimGoal;

import com.cgessinger.creaturesandbeasts.common.init.ModEntityTypes;
import com.cgessinger.creaturesandbeasts.common.init.ModSoundEventTypes;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import net.minecraft.entity.*;
import net.minecraft.world.entity.ai.util.RandomPos;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;

import net.minecraft.world.entity.AgableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;

public class GrebeEntity extends Animal
{
	private final UUID healthReductionUUID = UUID.fromString("189faad9-35de-4e15-a598-82d147b996d7");
    private final float babyHealth = 5.0F;
	private static final EntityDataAccessor<BlockPos> TRAVEL_POS = SynchedEntityData.defineId(GrebeEntity.class, EntityDataSerializers.BLOCK_POS);
	public static final Ingredient TEMPTATION_ITEMS = Ingredient.of(Items.COD, Items.SALMON, Items.TROPICAL_FISH);
	public float wingRotation;
	public float destPos;
	public float oFlapSpeed;
	public float oFlap;
	public float wingRotDelta = 1.0F;

	public GrebeEntity (EntityType<? extends Animal> type, Level worldIn)
	{
		super(type, worldIn);
		this.setPathfindingMalus(BlockPathTypes.WATER, 10.0F);
	}

	public static AttributeSupplier.Builder setCustomAttributes ()
	{
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 10.0D) // Max Health
				.add(Attributes.MOVEMENT_SPEED, 0.25D); // Movement Speed
	}

	@Override
	public SpawnGroupData finalizeSpawn (ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, MobSpawnType reason, @Nullable SpawnGroupData spawnDataIn, @Nullable CompoundTag dataTag)
	{
		if (spawnDataIn == null)
		{
			spawnDataIn = new AgableMob.AgableMobGroupData(0.6F);
		}

		return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
	}

	@Override
	protected void registerGoals ()
	{
		this.goalSelector.addGoal(1, new MountAdultGoal(this, 1.2D));
		this.goalSelector.addGoal(2, new SmoothSwimGoal(this));
		this.goalSelector.addGoal(3, new PanicGoal(this, 1.0D));
		this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
		this.goalSelector.addGoal(3, new GrebeEntity.SwimTravelGoal(this, 1.0D));
		this.goalSelector.addGoal(4, new GrebeEntity.WanderGoal(this, 1.0D, 2));
		this.goalSelector.addGoal(5, new TemptGoal(this, 1.0D, false, TEMPTATION_ITEMS));
		this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.goalSelector.addGoal(8, new GoToWaterGoal(this, 0.8D));
	}

	@Override
	public void setAge (int age)
	{
		super.setAge(age);
        double MAX_HEALTH = this.getAttribute(Attributes.MAX_HEALTH).getValue();
		if(isBaby() && MAX_HEALTH > this.babyHealth)
		{
			Multimap<Attribute, AttributeModifier> multimap = HashMultimap.create();
			multimap.put(Attributes.MAX_HEALTH, new AttributeModifier(this.healthReductionUUID, "yeti_health_reduction", this.babyHealth - MAX_HEALTH, AttributeModifier.Operation.ADDITION));
			this.getAttributes().addTransientAttributeModifiers(multimap);
			this.setHealth(this.babyHealth);
		}
	}
    
	@Override
	protected void ageBoundaryReached ()
	{
		this.stopRiding();
		this.getAttribute(Attributes.MAX_HEALTH).removeModifier(this.healthReductionUUID);
		this.setHealth((float) this.getAttribute(Attributes.MAX_HEALTH).getValue());
	}

	@Override
	public void aiStep ()
	{
		super.aiStep();
		this.oFlap = this.wingRotation;
		this.oFlapSpeed = this.destPos;
		this.destPos = (float) ((double) this.destPos + (double) (this.onGround || this.isInWater() || this.isPassenger() ? -1 : 4) * 0.3D);
		this.destPos = Mth.clamp(this.destPos, 0.0F, 1.0F);
		if (!this.onGround && this.wingRotDelta < 1.0F && !this.isInWater() && !this.isPassenger())
		{
			this.wingRotDelta = 1.0F;
		}

		this.wingRotDelta *= 0.9F;
		Vec3 motion = this.getDeltaMovement();
		if (!this.onGround && motion.y < 0.0D)
		{
			this.setDeltaMovement(motion.multiply(1.0D, 0.6D, 1.0D));
		}

		this.wingRotation += this.wingRotDelta * 2.0F;
	}

	@Nullable
	@Override
	public AgableMob getBreedOffspring (ServerLevel p_241840_1_, AgableMob p_241840_2_)
	{
		return ModEntityTypes.LITTLE_GREBE.get().create(p_241840_1_);
	}

	@Override
	public boolean causeFallDamage (float distance, float damageMultiplier)
	{
		return false;
	}

	@Override
	protected float getSoundVolume ()
	{
		return 0.6F;
	}

	@Nullable
	@Override
	protected SoundEvent getAmbientSound ()
	{
		if(this.isBaby())
		{
			return ModSoundEventTypes.LITTLE_GREBE_CHICK_AMBIENT.get();
		}
		return ModSoundEventTypes.LITTLE_GREBE_AMBIENT.get();
	}

	@Nullable
	@Override
	protected SoundEvent getHurtSound (DamageSource damageSourceIn)
	{
		return ModSoundEventTypes.LITTLE_GREBE_HURT.get();
	}

	@Nullable
	@Override
	protected SoundEvent getDeathSound ()
	{
		return ModSoundEventTypes.LITTLE_GREBE_HURT.get();
	}

	@Override
	public double getPassengersRidingOffset ()
	{
		return this.getBbHeight() * 0.3D;
	}

	/**
	 * Rewrite of the original @applyEntityCollision with code cleanup and ability
	 * to be pushed when mounted
	 */
	@Override
	public void push (Entity entityIn)
	{
		if (!this.isPassengerOfSameVehicle(entityIn) && !entityIn.noPhysics && !this.noPhysics)
		{
			double d0 = entityIn.getX() - this.getX();
			double d1 = entityIn.getZ() - this.getZ();
			double d2 = Mth.absMax(d0, d1);
			if (d2 >= 0.01D)
			{
				d2 = Mth.sqrt(d2);
				double d3 = 1.0D / d2;
				if (d3 > 1.0D)
				{
					d3 = 1.0D;
				}

				d0 = d0 / d2 * d3 * 0.05D - this.pushthrough;
				d1 = d1 / d2 * d3 * 0.05D - this.pushthrough;
				this.push(-d0, 0.0D, -d1);

				if (!entityIn.isVehicle())
				{
					entityIn.push(d0, 0.0D, d1);
				}
			}
		}
	}

	@Override
	public boolean isPushedByFluid ()
	{
		return false;
	}

	private BlockPos getTravelPos ()
	{
		return this.entityData.get(TRAVEL_POS);
	}

	@Override
	protected void defineSynchedData ()
	{
		super.defineSynchedData();
		this.entityData.define(TRAVEL_POS, new BlockPos(0, 2, 0));
	}

	@Override
	public boolean isFood (ItemStack stack)
	{
		return TEMPTATION_ITEMS.test(stack);
	}

	@Override
	public void travel (Vec3 travelVector)
	{
		if (this.isEffectiveAi() && this.isInWater())
		{
			this.moveRelative(0.1F, travelVector);
			this.move(MoverType.SELF, this.getDeltaMovement());
			this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
			if (this.getTarget() == null)
			{
				this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
			}
		} else
		{
			super.travel(travelVector);
		}

	}

    @Override
    public void checkDespawn() 
    {
        if(!CNBConfig.ServerConfig.GREBE_CONFIG.shouldExist)
        {
            this.remove();
            return;
        }
        super.checkDespawn();
    }

    public static boolean canGrebeSpawn( EntityType<GrebeEntity> animal, LevelAccessor worldIn,
                                             MobSpawnType reason, BlockPos pos, Random randomIn )
    {
        return worldIn.getRawBrightness(pos, 0) > 8;
    }

	static class WanderGoal extends RandomStrollGoal
	{
		private WanderGoal (GrebeEntity entity, double speedIn, int chance)
		{
			super(entity, speedIn, chance);
		}

		@Override
		public boolean canUse ()
		{
			return !this.mob.isInWater() && super.canUse();
		}
	}

	static class SwimTravelGoal extends Goal
	{
		private final GrebeEntity turtle;
		private final double speed;
		private boolean stuck;

		SwimTravelGoal (GrebeEntity turtle, double speedIn)
		{
			this.turtle = turtle;
			this.speed = speedIn;
		}

		@Override
		public boolean canUse ()
		{
			return this.turtle.isInWater();
		}

		@Override
		public void start ()
		{
			this.stuck = false;
		}

		@Override
		public void tick ()
		{
			if (this.turtle.getNavigation().isDone())
			{
				Vec3 vector3d = Vec3.atBottomCenterOf(this.turtle.getTravelPos());
				Vec3 vector3d1 = RandomPos.getPosTowards(this.turtle, 16, 3, vector3d, ((float) Math.PI / 10F));
				if (vector3d1 == null)
				{
					vector3d1 = RandomPos.getPosTowards(this.turtle, 8, 7, vector3d);
				}

				if (vector3d1 != null)
				{
					int i = Mth.floor(vector3d1.x);
					int j = Mth.floor(vector3d1.z);
					if (!this.turtle.level.hasChunksAt(i - 34, 0, j - 34, i + 34, 0, j + 34))
					{
						vector3d1 = null;
					}
				}

				if (vector3d1 == null)
				{
					this.stuck = true;
					return;
				}

				this.turtle.getNavigation().moveTo(vector3d1.x, vector3d1.y, vector3d1.z, this.speed);
			}

		}

		@Override
		public boolean canContinueToUse ()
		{
			return !this.turtle.getNavigation().isDone() && !this.stuck && !this.turtle.isInLove();
		}
	}
}
