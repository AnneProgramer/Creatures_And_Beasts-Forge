package com.cgessinger.creaturesandbeasts.client.render;

import com.cgessinger.creaturesandbeasts.client.model.LizardModel;
import com.cgessinger.creaturesandbeasts.common.entites.LizardEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class LizardRender extends GeoEntityRenderer<LizardEntity>
{
	public LizardRender (EntityRenderDispatcher renderManagerIn)
	{
		super(renderManagerIn, new LizardModel());
		this.shadowRadius = 0.3F;
	}

    @Override
    public ResourceLocation getTextureLocation(LizardEntity entity) {
        return getTextureLocation(entity);
    }

    @Override
	public void renderEarly (LizardEntity animatable, PoseStack stackIn, float ticks, @Nullable MultiBufferSource renderTypeBuffer, @Nullable VertexConsumer vertexBuilder, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float partialTicks)
	{
        float scale = animatable.isBaby() ? 0.4F : 0.8F;
		stackIn.scale(scale, scale, scale);
	}
}
