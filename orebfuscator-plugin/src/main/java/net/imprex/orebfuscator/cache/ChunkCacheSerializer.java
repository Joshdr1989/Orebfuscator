package net.imprex.orebfuscator.cache;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collection;

import net.imprex.orebfuscator.NmsInstance;
import net.imprex.orebfuscator.util.BlockCoords;
import net.imprex.orebfuscator.util.ChunkPosition;

public class ChunkCacheSerializer {

	private static final int CACHE_VERSION = 1;

	private DataInputStream createInputStream(ChunkPosition key) throws IOException {
		return NmsInstance.get().getRegionFileCache().createInputStream(key);
	}

	private DataOutputStream createOutputStream(ChunkPosition key) throws IOException {
		return NmsInstance.get().getRegionFileCache().createOutputStream(key);
	}

	public ChunkCacheEntry read(ChunkPosition key) throws IOException {
		try (DataInputStream dataInputStream = this.createInputStream(key)) {
			if (dataInputStream != null) {
				// check if cache entry has right version and if chunk is present
				if (dataInputStream.readInt() != CACHE_VERSION || !dataInputStream.readBoolean()) {
					return null;
				}

				byte[] hash = new byte[dataInputStream.readInt()];
				dataInputStream.readFully(hash);

				byte[] data = new byte[dataInputStream.readInt()];
				dataInputStream.readFully(data);

				ChunkCacheEntry chunkCacheEntry = new ChunkCacheEntry(hash, data);

				Collection<BlockCoords> proximityBlocks = chunkCacheEntry.getProximityBlocks();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					proximityBlocks.add(BlockCoords.fromLong(dataInputStream.readLong()));
				}

				Collection<BlockCoords> removedEntities = chunkCacheEntry.getRemovedTileEntities();
				for (int i = dataInputStream.readInt(); i > 0; i--) {
					removedEntities.add(BlockCoords.fromLong(dataInputStream.readLong()));
				}

				return chunkCacheEntry;
			}
		} catch (IOException e) {
			throw new IOException("Unable to read chunk: " + key, e);
		}
		return null;
	}

	// TODO consider size limit for cache since RegionFile before 1.14 have a hard limit of 256 * 4kb 
	public void write(ChunkPosition key, ChunkCacheEntry value) throws IOException {
		try (DataOutputStream dataOutputStream = this.createOutputStream(key)) {
			dataOutputStream.writeInt(CACHE_VERSION);

			if (value != null) {
				dataOutputStream.writeBoolean(true);

				byte[] hash = value.getHash();
				dataOutputStream.writeInt(hash.length);
				dataOutputStream.write(hash, 0, hash.length);

				byte[] data = value.getData();
				dataOutputStream.writeInt(data.length);
				dataOutputStream.write(data, 0, data.length);

				Collection<BlockCoords> proximityBlocks = value.getProximityBlocks();
				dataOutputStream.writeInt(proximityBlocks.size());
				for (BlockCoords blockPosition : proximityBlocks) {
					dataOutputStream.writeLong(blockPosition.toLong());
				}

				Collection<BlockCoords> removedEntities = value.getRemovedTileEntities();
				dataOutputStream.writeInt(removedEntities.size());
				for (BlockCoords blockPosition : removedEntities) {
					dataOutputStream.writeLong(blockPosition.toLong());
				}	
			} else {
				dataOutputStream.writeBoolean(false);
			}
		}
	}

}
