package net.querz.playerdata2mcaselector;

import net.querz.nbt.io.NBTUtil;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.*;
import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Main {

	private static final Map<String, Integer> dimMap = new HashMap<>();

	static {
		dimMap.put("minecraft:overworld", 0);
		dimMap.put("minecraft:the_end", 1);
		dimMap.put("minecraft:the_nether", -1);
	}

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

		Map<Integer, Map<Point2i, Set<Point2i>>> selections = new HashMap<>();
		int totalChunks = 0;

		for (File playerFile : playerFiles) {
			try {
				NamedTag data = NBTUtil.read(playerFile);
				CompoundTag root = (CompoundTag) data.getTag();
				ListTag<DoubleTag> pos = root.getListTag("Pos").asDoubleTagList();
				Tag<?> dimTag = root.get("Dimension");
				int dim;
				if (dimTag instanceof IntTag) {
					dim = root.getInt("Dimension");
				} else if (dimTag instanceof StringTag) {
					if (dimMap.containsKey(root.getString("Dimension"))) {
						dim = dimMap.get(root.getString("Dimension"));
					} else {
						System.out.println("invalid Dimension " + root.getString("Dimension"));
						continue;
					}
				} else {
					System.out.println("invalid Dimension tag type " + dimTag.getID());
					continue;
				}

				Map<Point2i, Set<Point2i>> regions = selections.computeIfAbsent(dim, k -> new HashMap<>());

				Point2i block = new Point2i(pos.get(0).asInt(), pos.get(2).asInt());
				Point2i chunk = block.blockToChunk();

				for (int x = chunk.getX() - radius; x < chunk.getX() + radius + 1; x++) {
					for (int z = chunk.getZ() - radius; z < chunk.getZ() + radius + 1; z++) {
						Point2i inRadius = new Point2i(x, z);
						Point2i region = inRadius.chunkToRegion();
						Set<Point2i> chunks = regions.computeIfAbsent(region, k -> new HashSet<>());
						if (chunks.add(inRadius)) {
							totalChunks++;
						}
					}
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		for (Map.Entry<Integer, Map<Point2i, Set<Point2i>>> dimension : selections.entrySet()) {
			String fileName = output.getName();
			if (selections.size() > 1) {
				fileName = output.getName().substring(0, output.getName().length() - 4) + "_DIM" + dimension.getKey() + ".csv";
			}

			try (BufferedWriter bw = new BufferedWriter(new FileWriter(new File(output.getParentFile(), fileName)))) {

				for (Map.Entry<Point2i, Set<Point2i>> selection : dimension.getValue().entrySet()) {
					Point2i region = selection.getKey();

					if (selection.getValue().size() == 1024) {
						bw.write(String.format("%d;%d\n", region.getX(), region.getZ()));
						continue;
					}

					for (Point2i chunk : selection.getValue()) {
						bw.write(String.format("%d;%d;%d;%d\n", region.getX(), region.getZ(), chunk.getX(), chunk.getZ()));
					}
				}
			}
		}

		System.out.println("created selection with " + totalChunks + " chunks.");
	}
}
