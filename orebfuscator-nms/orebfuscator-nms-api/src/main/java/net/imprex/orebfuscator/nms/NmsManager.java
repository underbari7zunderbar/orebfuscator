package net.imprex.orebfuscator.nms;

import org.bukkit.World;
import org.bukkit.entity.Player;

import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.BlockProperties;
import net.imprex.orebfuscator.util.NamespacedKey;

public interface NmsManager {

	AbstractRegionFileCache<?> getRegionFileCache();

	int getUniqueBlockStateCount();

	int getMaxBitsPerBlockState();

	BlockProperties getBlockByName(NamespacedKey key);

	boolean isAir(int blockId);

	boolean isOccluding(int blockId);

	boolean isBlockEntity(int blockId);

	ReadOnlyChunk getReadOnlyChunk(World world, int chunkX, int chunkZ);

	int getBlockState(World world, int x, int y, int z);

	void sendBlockUpdates(World world, Iterable<BlockPos> iterable);

	void sendBlockUpdates(Player player, Iterable<BlockPos> iterable);

	void close();
}