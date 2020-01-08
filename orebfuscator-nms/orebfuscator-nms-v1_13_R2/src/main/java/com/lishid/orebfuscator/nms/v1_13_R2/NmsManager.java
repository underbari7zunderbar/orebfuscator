/**
 * @author lishid
 * @author Aleksey Terzi
 *
 */

package com.lishid.orebfuscator.nms.v1_13_R2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_13_R2.CraftWorld;
import org.bukkit.craftbukkit.v1_13_R2.block.data.CraftBlockData;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_13_R2.util.CraftChatMessage;
import org.bukkit.entity.Player;

import com.lishid.orebfuscator.nms.IBlockInfo;
import com.lishid.orebfuscator.nms.IChunkCache;
import com.lishid.orebfuscator.nms.INmsManager;
import com.lishid.orebfuscator.types.ConfigDefaults;

import net.imprex.orebfuscator.util.BlockCoords;
import net.minecraft.server.v1_13_R2.Block;
import net.minecraft.server.v1_13_R2.BlockPosition;
import net.minecraft.server.v1_13_R2.Chunk;
import net.minecraft.server.v1_13_R2.ChunkProviderServer;
import net.minecraft.server.v1_13_R2.IBlockData;
import net.minecraft.server.v1_13_R2.IChatBaseComponent;
import net.minecraft.server.v1_13_R2.Packet;
import net.minecraft.server.v1_13_R2.TileEntity;
import net.minecraft.server.v1_13_R2.WorldServer;

public class NmsManager implements INmsManager {

	private static final int BITS_PER_BLOCK = 14;

	private final int BLOCK_ID_CAVE_AIR;
	private final Set<Integer> BLOCK_ID_AIRS;
	private final Set<Integer> BLOCK_ID_SIGNS;

	private final ConfigDefaults configDefaults;
	private int maxLoadedCacheFiles;
	private HashMap<Material, Set<Integer>> materialIds;

	public NmsManager() {
		this.initBlockIds();

		this.BLOCK_ID_CAVE_AIR = this.getMaterialIds(Material.CAVE_AIR).iterator().next();
		this.BLOCK_ID_AIRS = this
				.convertMaterialsToSet(new Material[] { Material.AIR, Material.CAVE_AIR, Material.VOID_AIR });
		this.BLOCK_ID_SIGNS = this.convertMaterialsToSet(new Material[] { Material.SIGN, Material.WALL_SIGN });

		this.configDefaults = new ConfigDefaults();

		// Default World
		this.configDefaults.defaultProximityHiderBlockIds = this.convertMaterialsToIds(new Material[] {
				Material.DISPENSER, Material.SPAWNER, Material.CHEST, Material.HOPPER, Material.CRAFTING_TABLE,
				Material.FURNACE, Material.ENCHANTING_TABLE, Material.EMERALD_ORE, Material.ENDER_CHEST, Material.ANVIL,
				Material.CHIPPED_ANVIL, Material.DAMAGED_ANVIL, Material.TRAPPED_CHEST, Material.DIAMOND_ORE });

		this.configDefaults.defaultDarknessBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.SPAWNER, Material.CHEST });

		this.configDefaults.defaultMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();
		this.configDefaults.defaultProximityHiderSpecialBlockId = this.getMaterialIds(Material.STONE).iterator().next();

		// The End
		this.configDefaults.endWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.BEDROCK,
				Material.OBSIDIAN, Material.END_STONE, Material.PURPUR_BLOCK, Material.END_STONE_BRICKS });

		this.configDefaults.endWorldObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.END_STONE });

		this.configDefaults.endWorldMode1BlockId = this.getMaterialIds(Material.END_STONE).iterator().next();
		this.configDefaults.endWorldRequiredObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.END_STONE });

		// Nether World
		this.configDefaults.netherWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.GRAVEL,
				Material.NETHERRACK, Material.SOUL_SAND, Material.NETHER_BRICKS, Material.NETHER_QUARTZ_ORE });

		this.configDefaults.netherWorldObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.NETHERRACK, Material.NETHER_QUARTZ_ORE });

		this.configDefaults.netherWorldMode1BlockId = this.getMaterialIds(Material.NETHERRACK).iterator().next();

		this.configDefaults.netherWorldRequiredObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.NETHERRACK });

		// Normal World
		this.configDefaults.normalWorldRandomBlockIds = this.convertMaterialsToIds(new Material[] { Material.STONE,
				Material.COBBLESTONE, Material.OAK_PLANKS, Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE,
				Material.LAPIS_ORE, Material.TNT, Material.MOSSY_COBBLESTONE, Material.OBSIDIAN, Material.DIAMOND_ORE,
				Material.REDSTONE_ORE, Material.CLAY, Material.EMERALD_ORE });

		this.configDefaults.normalWorldObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.GOLD_ORE, Material.IRON_ORE, Material.COAL_ORE,
						Material.LAPIS_ORE, Material.CHEST, Material.DIAMOND_ORE, Material.ENDER_CHEST,
						Material.REDSTONE_ORE, Material.CLAY, Material.EMERALD_ORE });

		this.configDefaults.normalWorldMode1BlockId = this.getMaterialIds(Material.STONE).iterator().next();

		this.configDefaults.normalWorldRequiredObfuscateBlockIds = this
				.convertMaterialsToIds(new Material[] { Material.STONE });
	}

	private void initBlockIds() {
		this.materialIds = new HashMap<>();

		Block.REGISTRY_ID.iterator().forEachRemaining(blockData -> {
			Material material = CraftBlockData.fromData(blockData).getMaterial();

			if (material.isBlock()) {
				int materialId = Block.REGISTRY_ID.getId(blockData);

				Set<Integer> ids = this.materialIds.get(material);

				if (ids == null) {
					this.materialIds.put(material, ids = new HashSet<>());
				}

				ids.add(materialId);
			}
		});
	}

	@Override
	public ConfigDefaults getConfigDefaults() {
		return this.configDefaults;
	}

	@Override
	public void setMaxLoadedCacheFiles(int value) {
		this.maxLoadedCacheFiles = value;
	}

	@Override
	public IChunkCache createChunkCache() {
		return new ChunkCache(this.maxLoadedCacheFiles);
	}

	@Override
	public void updateBlockTileEntity(BlockCoords blockCoord, Player player) {
		CraftWorld world = (CraftWorld) player.getWorld();
		// 1.13.2 has made this quite a bit different in later builds.
		TileEntity tileEntity = null;
		try {
			Method getTileEntityAt = world.getClass().getMethod("getTileEntityAt", int.class, int.class, int.class);
			tileEntity = (TileEntity) getTileEntityAt.invoke(world, blockCoord.x, blockCoord.y, blockCoord.z);
		} catch (NoSuchMethodException nsme) {
			tileEntity = world.getHandle().getTileEntity(new BlockPosition(blockCoord.x, blockCoord.y, blockCoord.z));
		} catch (Exception e) {
			return;
		}

		if (tileEntity == null) {
			return;
		}

		Packet<?> packet = tileEntity.getUpdatePacket();

		if (packet != null) {
			CraftPlayer player2 = (CraftPlayer) player;
			player2.getHandle().playerConnection.sendPacket(packet);
		}
	}

	@Override
	public void notifyBlockChange(World world, IBlockInfo blockInfo) {
		BlockPosition blockPosition = new BlockPosition(blockInfo.getX(), blockInfo.getY(), blockInfo.getZ());
		IBlockData blockData = ((BlockInfo) blockInfo).getBlockData();

		((CraftWorld) world).getHandle().notify(blockPosition, blockData, blockData, 0);
	}

	@Override
	public int getBlockLightLevel(World world, int x, int y, int z) {
		return ((CraftWorld) world).getHandle().getLightLevel(new BlockPosition(x, y, z));
	}

	@Override
	public IBlockInfo getBlockInfo(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, false);

		return blockData != null ? new BlockInfo(x, y, z, blockData) : null;
	}

	@Override
	public int loadChunkAndGetBlockId(World world, int x, int y, int z) {
		IBlockData blockData = getBlockData(world, x, y, z, true);
		return blockData != null ? Block.getCombinedId(blockData) : -1;
	}

	@Override
	public String getTextFromChatComponent(String json) {
		IChatBaseComponent component = IChatBaseComponent.ChatSerializer.a(json);
		return CraftChatMessage.fromComponent(component);
	}

	@Override
	public boolean isHoe(Material item) {
		return item == Material.WOODEN_HOE || item == Material.IRON_HOE || item == Material.GOLDEN_HOE
				|| item == Material.DIAMOND_HOE;
	}

	@Override
	public boolean isSign(int combinedBlockId) {
		return this.BLOCK_ID_SIGNS.contains(combinedBlockId);
	}

	@Override
	public boolean isAir(int combinedBlockId) {
		return this.BLOCK_ID_AIRS.contains(combinedBlockId);
	}

	@Override
	public boolean isTileEntity(int combinedBlockId) {
		return Block.getByCombinedId(combinedBlockId).getBlock().isTileEntity();
	}

	@Override
	public int getCaveAirBlockId() {
		return this.BLOCK_ID_CAVE_AIR;
	}

	@Override
	public int getBitsPerBlock() {
		return BITS_PER_BLOCK;
	}

	@Override
	public boolean canApplyPhysics(Material blockMaterial) {
		return blockMaterial == Material.AIR || blockMaterial == Material.CAVE_AIR || blockMaterial == Material.VOID_AIR
				|| blockMaterial == Material.FIRE || blockMaterial == Material.WATER || blockMaterial == Material.LAVA;
	}

	@Override
	public Set<Integer> getMaterialIds(Material material) {
		return this.materialIds.get(material);
	}

	@Override
	public boolean sendBlockChange(Player player, Location blockLocation) {
		IBlockData blockData = getBlockData(blockLocation.getWorld(), blockLocation.getBlockX(),
				blockLocation.getBlockY(), blockLocation.getBlockZ(), false);

		if (blockData == null) {
			return false;
		}

		CraftBlockData craftBlockData = CraftBlockData.fromData(blockData);

		player.sendBlockChange(blockLocation, craftBlockData);

		return true;
	}

	private static IBlockData getBlockData(World world, int x, int y, int z, boolean loadChunk) {
		int chunkX = x >> 4;
		int chunkZ = z >> 4;

		WorldServer worldServer = ((CraftWorld) world).getHandle();
		// like in ChunkCache, NMS change without R increment.
		ChunkProviderServer chunkProviderServer = null;
		try {
			Method getChunkProviderServer = worldServer.getClass().getDeclaredMethod("getChunkProviderServer");
			chunkProviderServer = (ChunkProviderServer) getChunkProviderServer.invoke(worldServer);
		} catch (NoSuchMethodException nmfe) {
			try {
				Method getChunkProvider = worldServer.getClass().getDeclaredMethod("getChunkProvider");
				chunkProviderServer = (ChunkProviderServer) getChunkProvider.invoke(worldServer);
			} catch (NoSuchMethodException nsme) {
				return null; // oops
			} catch (Exception e) {
				return null;
			}
		} catch (Exception e) {
			return null;
		}
		if (!loadChunk && !chunkProviderServer.isLoaded(chunkX, chunkZ)) {
			return null;
		}

		Chunk chunk = chunkProviderServer.getChunkAt(chunkX, chunkZ, true, true);

		return chunk != null ? chunk.getBlockData(x, y, z) : null;
	}

	private Set<Integer> convertMaterialsToSet(Material[] materials) {
		Set<Integer> ids = new HashSet<>();

		for (Material material : materials) {
			ids.addAll(this.getMaterialIds(material));
		}

		return ids;
	}

	private int[] convertMaterialsToIds(Material[] materials) {
		Set<Integer> ids = this.convertMaterialsToSet(materials);

		int[] result = new int[ids.size()];
		int index = 0;

		for (int id : ids) {
			result[index++] = id;
		}

		return result;
	}

	@Override
	public void sendMultiBlockChange(Player player, int chunkX, int chunkZ, Location... locations)
			throws IllegalAccessException, InvocationTargetException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasLightArray() {
		return true;
	}

	@Override
	public boolean hasBlockCount() {
		return false;
	}
}