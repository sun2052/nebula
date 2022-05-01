package org.byteinfo.logging;

import org.byteinfo.util.io.IOUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileWriter implements Writer {
	private final Level current;
	private final Path target;
	private final Rolling rolling;
	private final int backups;

	private volatile long nextRollingTime;
	private volatile BufferedWriter writer;

	public FileWriter(Level current, Path target, Rolling rolling, int backups) {
		this.current = current;
		this.target = target;
		this.rolling = rolling;
		this.backups = backups;
	}

	@Override
	public void write(int offset, long time, DateTimeFormatter formatter, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception {
		if (isEnabled(level, current)) {
			if (time > nextRollingTime) {
				synchronized (this) {
					if (time > nextRollingTime) {
						nextRollingTime = rolling.getNextRollingTime(time);
						String baseName = target.getFileName().toString();
						Path currentTarget = target.getParent().resolve(baseName + rolling.getSuffix(time));
						IOUtil.closeQuietly(writer);
						writer = Files.newBufferedWriter(currentTarget, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
						String prefix = baseName + ".";
						try (Stream<Path> list = Files.list(target.getParent())) {
							list.filter(path -> path.getFileName().toString().startsWith(prefix)).sorted(Comparator.reverseOrder()).skip(backups + 1L).forEach(path -> {
								try {
									Files.delete(path);
								} catch (IOException e) {
									throw new LogException("Failed to delete file: " + path, e);
								}
							});
						}
					}
				}
			}
			writer.write(format(offset, time, formatter, level, thread, message, params, supplier, throwable));
			writer.newLine();
			writer.flush();
		}
	}

	@Override
	public void close() throws Exception {
		IOUtil.closeQuietly(writer);
	}
}
