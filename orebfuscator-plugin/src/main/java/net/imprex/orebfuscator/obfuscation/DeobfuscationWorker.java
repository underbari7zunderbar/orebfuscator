package net.imprex.orebfuscator.obfuscation;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.block.Block;

import com.google.common.collect.Iterables;

import net.imprex.orebfuscator.Orebfuscator;
import net.imprex.orebfuscator.OrebfuscatorNms;
import net.imprex.orebfuscator.cache.ObfuscationCache;
import net.imprex.orebfuscator.config.BlockFlags;
import net.imprex.orebfuscator.config.ObfuscationConfig;
import net.imprex.orebfuscator.config.OrebfuscatorConfig;
import net.imprex.orebfuscator.config.WorldConfigBundle;
import net.imprex.orebfuscator.util.BlockPos;
import net.imprex.orebfuscator.util.ChunkPosition;

public class DeobfuscationWorker {

	private final OrebfuscatorConfig config;
	private final ObfuscationCache cache;

	public DeobfuscationWorker(Orebfuscator orebfuscator) {
		this.config = orebfuscator.getOrebfuscatorConfig();
		this.cache = orebfuscator.getObfuscationCache();
	}

	void deobfuscate(Block block) {
		if (block == null || !block.getType().isOccluding()) {
			return;
		}

		deobfuscate(Arrays.asList(block), true);
	}

	public void deobfuscate(Collection<? extends Block> blocks, boolean occluding) {
		if (blocks.isEmpty()) {
			return;
		}

		World world = Iterables.get(blocks, 0).getWorld();
		WorldConfigBundle bundle = this.config.world(world);

		ObfuscationConfig obfuscationConfig = bundle.obfuscation();
		if (obfuscationConfig == null || !obfuscationConfig.isEnabled()) {
			return;
		}

		int updateRadius = this.config.general().updateRadius();
		BlockFlags blockFlags = bundle.blockFlags();

		try (Processor processor = new Processor(world, blockFlags)) {
			for (Block block : blocks) {
				if (!occluding || block.getType().isOccluding()) {
					BlockPos position = new BlockPos(block.getX(), block.getY(), block.getZ());
					processor.processPosition(position, updateRadius);
				}
			}
		}
	}

	public class Processor implements AutoCloseable {

		private final Set<BlockPos> updatedBlocks = new HashSet<>();
		private final Set<ChunkPosition> invalidChunks = new HashSet<>();

		private final World world;
		private final BlockFlags blockFlags;

		public Processor(World world, BlockFlags blockFlags) {
			this.world = world;
			this.blockFlags = blockFlags;
		}

		public void processPosition(BlockPos position, int depth) {
			int blockId = OrebfuscatorNms.getBlockState(this.world, position);
			if (BlockFlags.isObfuscateBitSet(blockFlags.flags(blockId)) && updatedBlocks.add(position)) {

				// invalidate cache if enabled
				if (config.cache().enabled()) {
					ChunkPosition chunkPosition = position.toChunkPosition(world);
					if (this.invalidChunks.add(chunkPosition)) {
						cache.invalidate(chunkPosition);
					}
				}
			}
	
			if (depth-- > 0) {
				processPosition(position.add( 1,  0,  0), depth);
				processPosition(position.add(-1,  0,  0), depth);
				processPosition(position.add( 0,  1,  0), depth);
				processPosition(position.add( 0, -1,  0), depth);
				processPosition(position.add( 0,  0,  1), depth);
				processPosition(position.add( 0,  0, -1), depth);
			}
		}

		@Override
		public void close() {
			OrebfuscatorNms.sendBlockUpdates(this.world, this.updatedBlocks);
		}
	}
}
