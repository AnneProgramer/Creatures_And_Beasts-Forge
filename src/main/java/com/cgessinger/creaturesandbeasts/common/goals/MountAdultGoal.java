package com.cgessinger.creaturesandbeasts.common.goals;

import com.cgessinger.creaturesandbeasts.common.entites.GrebeEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;

import java.util.List;

public class MountAdultGoal extends Goal
{
	private final Animal childAnimal;
	private final double moveSpeed;

	public MountAdultGoal(Animal child, double speed)
	{
		this.childAnimal = child;
		this.moveSpeed = speed;
	}

	@Override
	public boolean canUse ()
	{
		if(!this.childAnimal.isPassenger() && this.childAnimal.isBaby())
		{
			List<GrebeEntity> entities = this.childAnimal.level.getEntitiesOfClass(GrebeEntity.class, this.childAnimal.getBoundingBox().inflate(10, 3, 10));

			for(GrebeEntity entity : entities)
			{
				if(!entity.isBaby() && !entity.isVehicle())
				{
					this.childAnimal.getNavigation().moveTo(this.childAnimal.getNavigation().createPath(entity, 0), this.moveSpeed);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Keep ticking a continuous task that has already been started
	 */
	@Override
	public void tick()
	{
		List<GrebeEntity> list = this.childAnimal.level.getEntitiesOfClass(GrebeEntity.class, this.childAnimal.getBoundingBox());

		for(GrebeEntity grebe : list)
		{
			if(!grebe.equals(this.childAnimal) && !grebe.isBaby() && !grebe.isVehicle())
			{
				this.childAnimal.startRiding(grebe);
			}
		}
	}
}
