package com.mrboomdev.awery.util.io;

import static com.mrboomdev.awery.app.AweryLifecycle.getAnyContext;
import static com.mrboomdev.awery.app.AweryLifecycle.getAppContext;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class FileUtil {
	private static final int BUFFER_SIZE = 1024 * 5;

	@SuppressLint("Range")
	@Nullable
	public static String getUriFileName(Uri uri) {
		var resolver = getAnyContext().getContentResolver();

		try(var cursor = resolver.query(uri, new String[] {
				MediaStore.MediaColumns.DISPLAY_NAME
		}, null, null, null)) {
			if(cursor == null) {
				return null;
			}

			cursor.moveToFirst();
			return cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME));
		}
	}

	public static void zip(@NonNull Map<File, String> paths, OutputStream into) throws IOException {
		try(var out = new ZipOutputStream(into)) {
			var data = new byte[BUFFER_SIZE];

			for(var file : paths.entrySet()) {
				try(var is = new BufferedInputStream(new FileInputStream(file.getKey()), BUFFER_SIZE)) {
					var zipEntry = new ZipEntry(file.getValue());
					out.putNextEntry(zipEntry);
					int read;

					while((read = is.read(data)) != -1) {
						out.write(data, 0, read);
					}

					out.closeEntry();
				}
			}
		}
	}

	public static void zip(@NonNull Map<File, String> paths, Uri into) throws IOException {
		try(var out = getAnyContext().getContentResolver().openOutputStream(into)) {
			zip(paths, out);
		}
	}

	public static void unzip(Uri input, File output) throws IOException {
		try(var stream = getAnyContext().getContentResolver().openInputStream(input)) {
			unzip(stream, output);
		}
	}

	public static void unzip(InputStream input, @NonNull File output) throws IOException {
		output.mkdirs();

		try(var zin = new ZipInputStream(new BufferedInputStream(input, BUFFER_SIZE))) {
			ZipEntry ze;

			while((ze = zin.getNextEntry()) != null) {
				var path = new File(output, ze.getName());

				if(ze.isDirectory() && !path.isDirectory()) {
					path.mkdirs();
				} else {
					var parent = path.getParentFile();

					if(parent != null) {
						parent.mkdirs();
					}

					try(var fout = new BufferedOutputStream(new FileOutputStream(path, false), BUFFER_SIZE)) {
						for(int c = zin.read(); c != -1; c = zin.read()) {
							fout.write(c);
						}
					}
				}
			}
		}
	}

	public static void deleteFile(File file) {
		if(file == null) return;

		if(file.isDirectory()) {
			var children = file.listFiles();
			if(children == null) return;

			for(var child : children) {
				deleteFile(child);
			}
		}

		file.delete();
	}

	public static long getFileSize(@NonNull File file) {
		if(file.isDirectory()) {
			var children = file.listFiles();
			if(children == null) return 0;

			long totalSize = 0;

			for(var child : children) {
				totalSize += getFileSize(child);
			}

			return totalSize;
		}

		return file.length();
	}

	@NonNull
	public static String readAssets(@NonNull File file) throws IOException {
		return readAssets(file.getAbsolutePath().substring(1));
	}

	@NonNull
	public static String readAssets(String path) throws IOException {
		try(var reader = new BufferedReader(new InputStreamReader(
				getAppContext().getAssets().open(path), StandardCharsets.UTF_8))
		) {
			var builder = new StringBuilder();
			String line;

			while((line = reader.readLine()) != null) {
				builder.append(line);
			}

			return builder.toString();
		}
	}
}