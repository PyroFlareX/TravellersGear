package travellersgear.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import travellersgear.TravellersGear;
import travellersgear.api.IActiveAbility;
import travellersgear.api.TravellersGearAPI;
import travellersgear.common.util.ModCompatability;
import travellersgear.common.util.Utils;
import baubles.api.BaublesApi;
import cpw.mods.fml.common.network.ByteBufUtils;

public class PacketActiveAbility extends AbstractPacket
{
	int dim;
	int playerid;
	ItemStack item;
	int slot;
	public PacketActiveAbility(){}
	public PacketActiveAbility(EntityPlayer player, ItemStack stack, int slot)
	{
		this.dim = player.worldObj.provider.dimensionId;
		this.playerid = player.getEntityId();
		this.item = stack;
		this.slot = slot;
	}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf buffer)
	{
		buffer.writeInt(dim);
		buffer.writeInt(playerid);
		ByteBufUtils.writeItemStack(buffer, item);
		buffer.writeInt(slot);
	}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf buffer)
	{
		this.dim = buffer.readInt();
		this.playerid = buffer.readInt();
		this.item=ByteBufUtils.readItemStack(buffer);
		this.slot = buffer.readInt();
	}

	@Override
	public void handleClientSide(EntityPlayer p2)
	{
	}

	@Override
	public void handleServerSide(EntityPlayer p2)
	{
		World world = DimensionManager.getWorld(this.dim);
		if (world == null)
			return;
		Entity ent = world.getEntityByID(this.playerid);
		if(!(ent instanceof EntityPlayer))
			return;
		EntityPlayer player = (EntityPlayer) ent;
		if(item!=null && item.getItem() instanceof IActiveAbility)
			if(((IActiveAbility)item.getItem()).canActivate(player, item))
				((IActiveAbility)item.getItem()).activate(player, item);

		switch(slot)
		{
		case 0:
		case 1:
		case 2:
		case 3: // ARMOR
			ItemStack[] armorInv = player.inventory.armorInventory;
			if(!Utils.itemsMatch(armorInv[slot], item, true, true))
				armorInv[slot]=item;
			player.inventory.armorInventory=armorInv;
			break;
		case 4:
		case 5:
		case 6:
		case 7: // BAUBLES
			IInventory baubInv = BaublesApi.getBaubles(player);
			if(baubInv!=null)
				if(!Utils.itemsMatch(baubInv.getStackInSlot(slot-4), item, true, true))
					baubInv.setInventorySlotContents(slot-4, item);
			ModCompatability.setPlayerBaubles(player, baubInv);
			break;
		case 8:
		case 9:
		case 10:
		case 11: // TRAVELLER'S GEAR
			ItemStack[] tgInv = TravellersGearAPI.getExtendedInventory(player);
			if(tgInv!=null)
				if(!Utils.itemsMatch(tgInv[slot-8], item, true, true))
					tgInv[slot-8]= item;
			TravellersGearAPI.setExtendedInventory(player, tgInv);
			TravellersGear.instance.packetPipeline.sendToAll(new PacketNBTSync(player));
			break;
		case 12:
		case 13:
		case 14: // MARICULTURE
			IInventory mariInv = ModCompatability.getMariInventory(player);
			if(mariInv!=null)
				if(!Utils.itemsMatch(mariInv.getStackInSlot(slot-12), item, true, true))
					mariInv.setInventorySlotContents(slot-12, item);
			break;
		case 16:
		case 17: // TCON
			IInventory tconInv = ModCompatability.getTConArmorInv(player);
			if(tconInv!=null)
				if(!Utils.itemsMatch(tconInv.getStackInSlot(slot-15), item, true, true))
					tconInv.setInventorySlotContents(slot-15, item);
			break;
		default:
			break;
		}
	}

}