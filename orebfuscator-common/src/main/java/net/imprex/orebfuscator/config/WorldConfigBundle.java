package net.imprex.orebfuscator.config;

public interface WorldConfigBundle {

	BlockFlags blockFlags();

	ObfuscationConfig obfuscation();

	ProximityConfig proximity();

	boolean needsObfuscation();

	boolean skipReadSectionIndex(int index);

	boolean skipProcessingSectionIndex(int index);

	int minSectionIndex();

	int maxSectionIndex();

	boolean shouldObfuscate(int y);

	int nextRandomObfuscationBlock(int y);

	int nextRandomProximityBlock(int y);
}
