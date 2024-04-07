package com.mrboomdev.awery.extensions.data;

import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.squareup.moshi.FromJson;
import com.squareup.moshi.Json;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.ToJson;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogMedia {
	public static final Adapter adapter = new Adapter();
	public static final CatalogMedia INVALID_MEDIA;
	private static JsonAdapter<CatalogMedia> moshiAdapter;

	/**
	 * Please use the following format:
	 * <b>EXTENSION_TYPE;;;EXTENSION_ID;;;ITEM_ID</b>
	 * <p>
	 * Example:
	 * <b>EXTENSION_JS;;;com.mrboomdev.awery.extension.anilist;;;1</b>
	 * </p>
	 */
	@NonNull
	public String globalId;

	public List<String> titles = new ArrayList<>();
	public Map<String, String> ids = new HashMap<>();
	public String banner, description, url, country;
	public MediaType type;
	public ImageVersions poster = new ImageVersions();
	public Calendar releaseDate;
	public Integer duration, episodesCount;
	public Float averageScore;
	public List<CatalogTag> tags;
	public List<String> genres, trackers, lists;
	public MediaStatus status;
	@Json(ignore = true)
	public Drawable cachedBanner;
	@Json(ignore = true)
	public long visualId;
	public String lastSource;
	public float lastEpisode = -1;
	public float lastEpisodeProgress = -1;

	/**
	 * @param globalId The unique id of the media in the following format:
	 * <p>{@code MANAGER_ID;;;EXTENSION_ID;;;ITEM_ID}</p>
	 */
	public CatalogMedia(@NonNull String globalId) {
		this.globalId = globalId;
	}

	public CatalogMedia(String managerId, String extensionId, String mediaId) {
		this(managerId + ";;;" + extensionId + ";;;" + mediaId);
	}

	@NonNull
	@Override
	public String toString() {
		return getJsonAdapter().toJson(this);
	}

	public void setTitle(@NonNull String... titles) {
		this.titles = Lists.newArrayList(titles);
	}

	public String getTitle() {
		return titles.get(0);
	}

	public void setTitles(Collection<String> titles) {
		if(titles == null) {
			this.titles = new ArrayList<>();
			return;
		}

		this.titles = List.copyOf(titles);
	}

	public String getBestBanner() {
		if(banner != null) return banner;
		return getBestPoster();
	}

	public void setId(String type, String id) {
		ids.put(type, id);
	}

	public String getId(String type) {
		return ids.get(type);
	}

	public void merge(@NonNull CatalogMedia media) {
		if(media.lastEpisode != -1) lastEpisode = media.lastEpisode;
		if(media.lastEpisodeProgress != -1) lastEpisodeProgress = media.lastEpisodeProgress;
		if(media.lists != null) lists = media.lists;
		if(media.lastSource != null) lastSource = media.lastSource;
	}

	public void clearBookmarks() {
		if(lists == null) return;
		lists.clear();
	}

	public void addToList(String list) {
		if(lists == null) lists = new ArrayList<>();
		lists.add(list);
	}

	public String getBestPoster() {
		if(poster.extraLarge != null) return poster.extraLarge;
		if(poster.large != null) return poster.large;
		if(poster.medium != null) return poster.medium;
		return banner;
	}

	public void setPoster(String poster) {
		this.poster.extraLarge = poster;
		this.poster.large = poster;
		this.poster.medium = poster;
	}

	public static JsonAdapter<CatalogMedia> getJsonAdapter() {
		if(moshiAdapter == null) {
			moshiAdapter = new Moshi.Builder().add(adapter)
					.build().adapter(CatalogMedia.class);
		}

		return moshiAdapter;
	}

	public enum MediaStatus {
		ONGOING, COMPLETED, COMING_SOON, PAUSED, CANCELLED, UNKNOWN
	}

	public enum MediaType {
		MOVIE, BOOK, TV, POST
	}

	public static class ImageVersions {
		public String extraLarge, large, medium;
	}

	@SuppressWarnings("unused")
	public static class Adapter {

		@ToJson
		public long toJson(@NonNull Calendar calendar) {
			return calendar.getTimeInMillis();
		}

		@FromJson
		public Calendar fromJson(long millis) {
			var calendar = Calendar.getInstance();
			calendar.setTimeInMillis(millis);
			return calendar;
		}
	}

	static {
		INVALID_MEDIA = new CatalogMedia("INTERNAL;;;com.mrboomdev.awery;;;0");
		INVALID_MEDIA.setTitle("Invalid!");
		INVALID_MEDIA.description = "Invalid media item!";
	}
}