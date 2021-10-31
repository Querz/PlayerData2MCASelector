package net.querz.playerdata2mcaselector;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import net.querz.nbt.tag.DoubleTag;
import net.querz.nbt.tag.ListTag;
import java.io.*;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;

public class Main {

	public static void main(String[] args) throws IOException {

		if (args.length < 2) {
			throw new IllegalArgumentException("missing arguments. required are: <directory> <output>");
		}

		File inputDir = new File(args[0]);
		if (!inputDir.exists() || !inputDir.isDirectory()) {
			throw new IllegalArgumentException("directory " + inputDir + " does not exist or is not a directory");
		}

		if (!args[1].endsWith(".csv")) {
			throw new IllegalArgumentException("output file must be a .csv file");
		}
		File output = new File(args[1]);
		Files.createDirectories(output.getParentFile().toPath());

		int radius = 0;
		if (args.length >= 3) {
			radius = Integer.parseInt(args[2]);
		}

		System.out.println("using radius " + radius);

		// 2bdaa75a-0e78-4356-a5b8-4d2e5fc91a88
		File[] playerFiles = inputDir.listFiles((d, f) -> f.matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.dat$"));
		if (playerFiles == null || playerFiles.length == 0) {
			throw new FileNotFoundException("no player files found in " + inputDir);
		}

		Set<Point2i> chunks = new HashSet<>();

		try (BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
			for (File playerFile : playerFiles) {
				try {
					NamedTag data = NBTUtil.read(playerFile);
					CompoundTag root = (CompoundTag) data.getTag();
					ListTag<DoubleTag> pos = root.getListTag("Pos").asDoubleTagList();

					Point2i block = new Point2i(pos.get(0).asInt(), pos.get(2).asInt());
					Point2i chunk = block.blockToChunk();

					for (int x = chunk.getX() - radius; x < chunk.getX() + radius + 1; x++) {
						for (int z = chunk.getZ() - radius; z < chunk.getZ() + radius + 1; z++) {
							Point2i inRadius = new Point2i(x, z);

							if (chunks.contains(inRadius)) {
								continue;
							}
							chunks.add(inRadius);

							Point2i region = inRadius.chunkToRegion();

							bw.write(String.format("%d;%d;%d;%d\n", region.getX(), region.getZ(), inRadius.getX(), inRadius.getZ()));
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
			}
		}

		System.out.println("created selection with " + chunks.size() + " chunks.");
	}
}
