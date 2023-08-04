package org.byteinfo.logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class FileWriter implements Writer {
	private final Lock lock = new ReentrantLock();
	private final Path target;
	private final Rolling rolling;
	private final int backups;

	private volatile long nextRollingTime;
	private volatile BufferedWriter writer;

	public FileWriter(Path target, Rolling rolling, int backups) {
		if (Files.notExists(target.getParent())) {
			throw new LogException("Path not exist: " + target.getParent());
		}
		Objects.requireNonNull(rolling);
		this.target = target;
		this.rolling = rolling;
		this.backups = backups;
	}

	@Override
	public void write(long time, Level level, String thread, String message, Object[] params, Supplier<String> supplier, Throwable throwable) throws Exception {
		lock.lock();
		try {
			if (time > nextRollingTime) {
				try {
					if (writer != null) {
						writer.close();
					}
				} catch (Exception e) {
					// ignore
				}
				nextRollingTime = rolling.getNextRollingTime(time);
				String fileName = target.getFileName().toString();
				Path currentTarget = target.getParent().resolve(fileName.replace("{}", rolling.getRollingSuffix(time)));
				writer = Files.newBufferedWriter(currentTarget, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);

				Thread.startVirtualThread(() -> {
					String baseName = fileName.substring(0, fileName.indexOf("{}"));
					try (Stream<Path> list = Files.list(target.getParent())) {
						list.filter(path -> path.getFileName().toString().startsWith(baseName)).sorted(Comparator.reverseOrder()).skip(backups + 1L).forEach(path -> {
							try {
								Files.delete(path);
							} catch (IOException e) {
								throw new LogException("Failed to delete file: " + path, e);
							}
						});
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
				});
			}

			writer.write(Writer.formatLog(time, level, thread, message, params, supplier, throwable));
			writer.newLine();
			writer.flush();
		} finally {
			lock.unlock();
		}

	}

	@Override
	public void close() throws Exception {
		writer.close();
	}
}
