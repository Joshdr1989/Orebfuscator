package net.imprex.orebfuscator.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import net.imprex.orebfuscator.util.ChunkPosition;
import net.imprex.orebfuscator.util.OFCLogger;

public class OrebfuscatorCacheConfig implements CacheConfig {

	private boolean enabled = true;
	private Path baseDirectory = Bukkit.getWorldContainer().toPath().resolve("orebfuscator_cache/");

	private int maximumOpenRegionFiles = 256;
	private long deleteRegionFilesAfterAccess = TimeUnit.DAYS.toMillis(2);

	private int maximumSize = 4096;
	private long expireAfterAccess = TimeUnit.SECONDS.toMillis(30);

	public void serialize(ConfigurationSection section) {
		this.enabled = section.getBoolean("enabled", true);
		this.serializeBaseDirectory(section, "orebfuscator_cache/");

		this.maximumOpenRegionFiles = section.getInt("maximumOpenRegionFiles", 256);
		this.deleteRegionFilesAfterAccess = section.getLong("deleteRegionFilesAfterAccess", TimeUnit.DAYS.toMillis(2));

		this.maximumSize = section.getInt("maximumSize", 4096);
		this.expireAfterAccess = section.getLong("expireAfterAccess", TimeUnit.SECONDS.toMillis(30));
	}

	private void serializeBaseDirectory(ConfigurationSection section, String defaultPath) {
		Path worldPath = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
		String baseDirectory = section.getString("baseDirectory", defaultPath);

		try {
			this.baseDirectory = worldPath.resolve(baseDirectory).normalize();
		} catch (InvalidPathException e) {
			OFCLogger
					.log(Level.WARNING, "config path '" + section.getCurrentPath() + ".baseDirectory' contains malformed path '"
							+ baseDirectory + "', using default path '" + defaultPath + "'");
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
		}

		if (!this.baseDirectory.startsWith(worldPath)) {
			OFCLogger
					.log(Level.WARNING, "config path '" + section.getCurrentPath() + ".baseDirectory' is no child directory of '"
							+ worldPath + "', using default path: '" + defaultPath + "'");
			this.baseDirectory = worldPath.resolve(defaultPath).normalize();
		}

		if (this.enabled()) {
			try {
				if (Files.notExists(this.baseDirectory)) {
					Files.createDirectories(this.baseDirectory);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public boolean enabled() {
		return this.enabled;
	}

	@Override
	public Path baseDirectory() {
		return this.baseDirectory;
	}

	@Override
	public Path regionFile(ChunkPosition key) {
		return this.baseDirectory.resolve(key.getWorld())
				.resolve("r." + (key.getX() >> 5) + "." + (key.getZ() >> 5) + ".mca");
	}

	@Override
	public int maximumOpenRegionFiles() {
		return this.maximumOpenRegionFiles;
	}

	@Override
	public long deleteRegionFilesAfterAccess() {
		return this.deleteRegionFilesAfterAccess;
	}

	@Override
	public int maximumSize() {
		return this.maximumSize;
	}

	@Override
	public long expireAfterAccess() {
		return this.expireAfterAccess;
	}
}
