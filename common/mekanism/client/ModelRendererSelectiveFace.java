package mekanism.client;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.TextureOffset;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.opengl.GL11;

public class ModelRendererSelectiveFace
{
    public float textureWidth;
    public float textureHeight;
    public float offsetX;
    public float offsetY;
    public float offsetZ;
    public float rotationPointX;
    public float rotationPointY;
    public float rotationPointZ;
    public float rotateAngleX;
    public float rotateAngleY;
    public float rotateAngleZ;
    public boolean mirror;
    public boolean showModel;
    public boolean isHidden;
    public List<ModelBoxSelectiveFace> cubeList = new ArrayList<ModelBoxSelectiveFace>();
    private int textureOffsetX;
    private int textureOffsetY;
    private ModelBase baseModel;
    private Map<BooleanArray, Integer> displayLists = new HashMap<BooleanArray, Integer>();
    

    public ModelRendererSelectiveFace(ModelBase modelBase)
    {
        textureWidth = 64.0F;
        textureHeight = 32.0F;
        showModel = true;
        baseModel = modelBase;
        setTextureSize(modelBase.textureWidth, modelBase.textureHeight);
    }

    public ModelRendererSelectiveFace(ModelBase modelBase, int offsetX, int offsetY)
    {
        this(modelBase);
        setTextureOffset(offsetX, offsetY);
    }

    public ModelRendererSelectiveFace setTextureOffset(int offsetX, int offsetY)
    {
        textureOffsetX = offsetX;
        textureOffsetY = offsetY;
        return this;
    }

    public ModelRendererSelectiveFace addBox(float minX, float minY, float minZ, int sizeX, int sizeY, int sizeZ)
    {
        cubeList.add(new ModelBoxSelectiveFace(this, textureOffsetX, textureOffsetY, minX, minY, minZ, sizeX, sizeY, sizeZ, 0.0F));
        return this;
    }

    public void setRotationPoint(float pointX, float pointY, float pointZ)
    {
        rotationPointX = pointX;
        rotationPointY = pointY;
        rotationPointZ = pointZ;
    }
    
    @SideOnly(Side.CLIENT)
    public void render(boolean[] dontRender, float scaleFactor)
    {
        if (!isHidden)
        {
            if (showModel)
            {
            	Integer currentDisplayList = displayLists.get(new BooleanArray(dontRender));
                if (currentDisplayList == null)
                {
                    currentDisplayList = compileDisplayList(dontRender, scaleFactor);
                }

                GL11.glTranslatef(offsetX, offsetY, offsetZ);
                int i;

                if (rotateAngleX == 0.0F && rotateAngleY == 0.0F && rotateAngleZ == 0.0F)
                {
                    if (rotationPointX == 0.0F && rotationPointY == 0.0F && rotationPointZ == 0.0F)
                    {
                        GL11.glCallList(currentDisplayList);
                    }
                    else
                    {
                        GL11.glTranslatef(rotationPointX * scaleFactor, rotationPointY * scaleFactor, rotationPointZ * scaleFactor);
                        GL11.glCallList(currentDisplayList);
                        GL11.glTranslatef(-rotationPointX * scaleFactor, -rotationPointY * scaleFactor, -rotationPointZ * scaleFactor);
                    }
                }
                else
                {
                    GL11.glPushMatrix();
                    GL11.glTranslatef(rotationPointX * scaleFactor, rotationPointY * scaleFactor, rotationPointZ * scaleFactor);

                    if (rotateAngleZ != 0.0F)
                    {
                        GL11.glRotatef(rotateAngleZ * (180F / (float)Math.PI), 0.0F, 0.0F, 1.0F);
                    }

                    if (rotateAngleY != 0.0F)
                    {
                        GL11.glRotatef(rotateAngleY * (180F / (float)Math.PI), 0.0F, 1.0F, 0.0F);
                    }

                    if (rotateAngleX != 0.0F)
                    {
                        GL11.glRotatef(rotateAngleX * (180F / (float)Math.PI), 1.0F, 0.0F, 0.0F);
                    }

                    GL11.glCallList(currentDisplayList);
                    GL11.glPopMatrix();
                }

                GL11.glTranslatef(-offsetX, -offsetY, -offsetZ);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private int compileDisplayList(boolean[] dontRender, float scaleFactor)
    {
        int displayList = GLAllocation.generateDisplayLists(1);
        GL11.glNewList(displayList, GL11.GL_COMPILE);
        Tessellator tessellator = Tessellator.instance;

        for (int i = 0; i < cubeList.size(); ++i)
        {
            cubeList.get(i).render(tessellator, dontRender, scaleFactor);
        }

        GL11.glEndList();
        displayLists.put(new BooleanArray(dontRender), displayList);
        return displayList;
    }

    public ModelRendererSelectiveFace setTextureSize(int sizeX, int sizeY)
    {
        textureWidth = (float)sizeX;
        textureHeight = (float)sizeY;
        return this;
    }
    
    private class BooleanArray
    {
    	private final boolean[] boolArray;
    	
    	public BooleanArray(boolean[] array)
		{
			boolArray = array.clone();
		}
    	
    	@Override
    	public boolean equals(Object o)
    	{
    		if(o instanceof BooleanArray)
    		{
    			return Arrays.equals(boolArray, ((BooleanArray)o).boolArray);
    		}
    		else if(o instanceof boolean[]) 
    		{
    			return Arrays.equals(boolArray, (boolean[])o);
    		}
    		else
    		{
    			return false;
    		}
    	}
    	
    	@Override
    	public int hashCode()
    	{
    		return Arrays.hashCode(boolArray);
    	}
    	
    	
    }
}
