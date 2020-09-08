package dev.latvian.kubejs.util.nbt;

import net.minecraft.nbt.NBTBase;

import javax.annotation.Nullable;

/**
 * @author LatvianModder
 */
public class NBTNullJS implements NBTBaseJS
{
	public static final NBTNullJS INSTANCE = new NBTNullJS();

	private NBTNullJS()
	{
	}

	@Override
	@Nullable
	public NBTBase createNBT()
	{
		return null;
	}

	@Override
	public NBTCompoundJS asCompound()
	{
		return NBTCompoundJS.NULL;
	}

	@Override
	public NBTListJS asList()
	{
		return NBTListJS.NULL;
	}

	public boolean equals(Object o)
	{
		return o == this;
	}

	public String toString()
	{
		return "null";
	}

	public int hashCode()
	{
		return 0;
	}

	@Override
	public boolean isEmpty()
	{
		return true;
	}

	@Override
	public boolean isNull()
	{
		return true;
	}
}