package com.mrboomdev.awery.extensions.data;

import androidx.annotation.NonNull;

import com.google.common.collect.Lists;
import com.mrboomdev.awery.util.Parser;
import com.squareup.moshi.Json;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CatalogMedia {
	@NonNull
	public String globalId;
	public List<String> titles = new ArrayList<>();
	public Map<String, String> ids = new HashMap<>();
	public Map<String, String> authors = new HashMap<>();
	public String banner, description, country, ageRating, extra;
	public MediaType type;
	public ImageVersions poster = new ImageVersions();
	public Calendar releaseDate;
	public Integer duration, episodesCount, latestEpisode;
	public Float averageScore;
	public List<CatalogTag> tags;
	public List<String> genres;
	public MediaStatus status;
	@Json(ignore = true)
	public long visualId;

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

	public String getManagerId() {
		return globalId.split(";;;")[0];
	}

	public String getExtensionId() {
		return globalId.split(";;;")[1];
	}

	public String getMediaId() {
		return globalId.split(";;;")[2];
	}

	@NonNull
	@Override
	public String toString() {
		return Parser.toString(CatalogMedia.class, this);
	}

	public void setTitle(@NonNull String... titles) {
		this.titles = Lists.newArrayList(titles);
	}

	public String getTitle() {
		if(titles.isEmpty()) return null;
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

	public enum MediaStatus {
		ONGOING, COMPLETED, COMING_SOON, PAUSED, CANCELLED, UNKNOWN
	}

	public enum MediaType {
		MOVIE, BOOK, TV, POST
	}

	public static class ImageVersions {
		public String extraLarge, large, medium;
	}
}