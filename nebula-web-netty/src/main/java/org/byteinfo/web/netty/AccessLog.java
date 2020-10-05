package org.byteinfo.web.netty;

import io.netty.handler.codec.http.HttpHeaderNames;
import org.byteinfo.logging.Rolling;
import org.byteinfo.util.io.IOUtil;
import org.byteinfo.util.misc.Config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class AccessLog {
	// Rolling File Base
	private static Path target;

	// Next Rolling Time
	private static volatile long nextRollingTime;

	// Current Rolling File Writer
	private static volatile BufferedWriter writer;

	public static void init() {
		String logPath = Config.get("http.access");
		if (logPath != null) {
			target = Path.of(logPath);
		}
	}

	public static void log(HttpContext context, long startTime) throws IOException {
		String accessLog = Instant.ofEpochMilli(startTime) + "|"
				+ context.remoteAddress() + "|"
				+ context.method() + "|"
				+ context.requestPath() + "|"
				+ context.responseStatus().code() + "|"
				+ context.responseLength() + "|"
				+ (System.currentTimeMillis() - startTime) + "|"
				+ context.headers().get(HttpHeaderNames.USER_AGENT) + "|"
				+ context.headers().get(HttpHeaderNames.REFERER);

		if (target == null) {
			System.out.println(accessLog);
		} else {
			if (startTime > nextRollingTime) {
				synchronized (AccessLog.class) {
					if (startTime > nextRollingTime) {
						nextRollingTime = Rolling.DAILY.getNextRollingTime(startTime);
						Path currentTarget = target.resolve(target + Rolling.DAILY.getSuffix(startTime));
						IOUtil.closeQuietly(writer);
						writer = Files.newBufferedWriter(currentTarget, StandardCharsets.UTF_8, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
					}
				}
			}
			writer.write(accessLog);
			writer.newLine();
			writer.flush();
		}
	}

	public static void destroy() {
		IOUtil.closeQuietly(writer);
	}
}
