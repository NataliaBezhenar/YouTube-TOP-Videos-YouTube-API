package com.nbezhenar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.Search;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;
import com.google.api.services.youtube.model.Video;

public class SearchVideos {
	private static String PROPERTIES_FILENAME = "youtube.properties";
	private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
	private static final JsonFactory JSON_FACTORY = new JacksonFactory();
	private static final long NUMBER_OF_VIDEOS_RETURNED = 10;
	private static YouTube youtube;

	public static void main(String[] args) {

		try {
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					new HttpRequestInitializer() {
						public void initialize(HttpRequest request)
								throws IOException {
						}
					}).setApplicationName("youtube-cmdline-search").build();

			Map<String, Integer> topVideos = getTopVideos("2vjPBrBU-TM", 10, 3);
			for (Map.Entry<String, Integer> entry : topVideos.entrySet()) {
				System.out.println(entry);
			}

		} catch (GoogleJsonResponseException e) {
			System.err.println("There was a service error: "
					+ e.getDetails().getCode() + " : "
					+ e.getDetails().getMessage());
		} catch (IOException e) {
			System.err.println("There was an IO error: " + e.getCause() + " : "
					+ e.getMessage());
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	public static Map<String, Integer> getTopVideos(String videoId,
			int numberOfVideos, int nestingLevel) throws IOException {
		List<String> videoIDs = getVideoIdWithNestingLevels(
				getVideosIds(videoId), nestingLevel);
		return Sorter.getTopNValues(Sorter.compareByViewCount(getMap(videoIDs,
				getViewCount(videoIDs))), numberOfVideos);
	}

	private static List<String> getVideoIdWithNestingLevels(
			List<String> initialList, int nestingLevel) throws IOException {
		YouTube.Search.List search = youtube.search().list("snippet");
		String apiKey = authorization();
		search.setKey(apiKey);
		int count = 1;
		List<String> tempVideoIds = new ArrayList<String>();
		List<String> tempID = new ArrayList<String>();
		List<String> returnIds = new ArrayList<String>();
		tempVideoIds.addAll(initialList);
		returnIds.addAll(initialList);
		while (count != nestingLevel) {
			for (String videoId : tempVideoIds) {
				tempID.addAll(getVideosIds(videoId));
			}
			returnIds.addAll(tempID);
			tempVideoIds.clear();
			tempVideoIds.addAll(tempID);
			tempID.clear();
			count++;
		}
		return returnIds;
	}

	private static List<String> getVideosIds(String videoId) throws IOException {
		YouTube.Search.List search = youtube.search().list("snippet");
		String apiKey = authorization();
		search.setKey(apiKey);
		search.setType("video");
		search.setRelatedToVideoId(videoId);
		search.setFields("items(id/kind,id/videoId)");
		search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
		SearchListResponse searchResponse = search.execute();

		List<SearchResult> searchResultList = searchResponse.getItems();
		List<String> videoIds = new ArrayList<String>();

		if (searchResultList != null) {
			Iterator<SearchResult> iteratorSearchResults = searchResultList
					.iterator();
			while (iteratorSearchResults.hasNext()) {
				SearchResult singleVideo = iteratorSearchResults.next();
				ResourceId rId = singleVideo.getId();
				if (rId.getKind().equals("youtube#video")) {

					videoIds.add(rId.getVideoId());
				}
			}
		}
		return videoIds;
	}

	private static List<Integer> getViewCount(List<String> videoIds)
			throws IOException {
		com.google.api.services.youtube.YouTube.Videos.List search = youtube
				.videos().list("statistics");
		String apiKey = authorization();
		search.setKey(apiKey);
		List<Integer> viewCount = new ArrayList<Integer>();
		for (String vidID : videoIds) {
			search.setId(vidID);
			try {
				Video v = search.execute().getItems().get(0);
				viewCount.add(v.getStatistics().getViewCount().intValue());
			} catch (IndexOutOfBoundsException e) {
				viewCount.add(0);
			}
		}
		return viewCount;
	}

	private static Map<String, Integer> getMap(List<String> ls, List<Integer> li) {
		Map<String, Integer> relatedLinksMap = new HashMap<String, Integer>();
		Iterator<String> links_iter = ls.iterator();
		Iterator<Integer> rating_iter = li.iterator();
		while (links_iter.hasNext() && rating_iter.hasNext()) {
			relatedLinksMap.put(links_iter.next(), rating_iter.next());
		}
		return relatedLinksMap;
	}

	private static String authorization() {
		Properties properties = new Properties();
		try {InputStream in = Search.class.getResourceAsStream("/"+ PROPERTIES_FILENAME);
			properties.load(in);
		} catch (IOException e) {
			System.err.println("There was an error reading "
					+ PROPERTIES_FILENAME + ": " + e.getCause() + " : "
					+ e.getMessage());
			System.exit(1);
		}
		String apiKey = properties.getProperty("youtube.apikey");
		return apiKey;
	}
}