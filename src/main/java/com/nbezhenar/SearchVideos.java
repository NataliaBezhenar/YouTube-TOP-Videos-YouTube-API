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
	private static final long NUMBER_OF_VIDEOS_RETURNED = 6;
	private static YouTube youtube;

	public static void main(String[] args) {

		Properties properties = new Properties();
		try {
			InputStream in = Search.class.getResourceAsStream("/"
					+ PROPERTIES_FILENAME);
			properties.load(in);

		} catch (IOException e) {
			System.err.println("There was an error reading "
					+ PROPERTIES_FILENAME + ": " + e.getCause() + " : "
					+ e.getMessage());
			System.exit(1);
		}

		try {
			youtube = new YouTube.Builder(HTTP_TRANSPORT, JSON_FACTORY,
					new HttpRequestInitializer() {
						public void initialize(HttpRequest request)
								throws IOException {
						}
					}).setApplicationName("youtube-cmdline-search").build();

			List<String> videoIds = getIdVideo("NZi_M-TfioM", properties);

			List<Integer> viewCount = getViewCount(videoIds, properties);

			Map<String, Integer> relatedLinksMap = getMap(videoIds, viewCount);

			List<String> intermediateStrL = new ArrayList<String>();

			int nestingLevel = 2;
			int count = 0;
			while (count != nestingLevel) {
				Map<String, Integer> test = new HashMap<String, Integer>();
				test.putAll(relatedLinksMap);

				for (String key : test.keySet()) {

					getIdVideo(key, properties);
					intermediateStrL.addAll(getIdVideo(key, properties));

				}
				List<Integer> intermediateIntL = getViewCount(intermediateStrL,
						properties);

				Map<String, Integer> relatedLinksMapTest = getMap(
						intermediateStrL, intermediateIntL);
				relatedLinksMap.putAll(relatedLinksMapTest);

				test.clear();
				intermediateStrL.clear();
				intermediateIntL.clear();
				relatedLinksMapTest.clear();
				count++;
			}

			System.out.println("Total quantity of links "
					+ relatedLinksMap.size());

			System.out.println("TOP " + NUMBER_OF_VIDEOS_RETURNED + " values");

			Map<String, Integer> sortedAndTrimmedMap = Sorter.getTopNValues(
					Sorter.compareByRating(relatedLinksMap),
					(int) NUMBER_OF_VIDEOS_RETURNED);
			for (Map.Entry<String, Integer> entry : sortedAndTrimmedMap
					.entrySet()) {
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

	private static List<Integer> getViewCount(List<String> videoIds,
			Properties properties) throws IOException {
		com.google.api.services.youtube.YouTube.Videos.List search = youtube
				.videos().list("statistics");
		String apiKey = properties.getProperty("youtube.apikey");
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

	private static List<String> getIdVideo(String gueryLine,
			Properties properties) throws IOException {
		YouTube.Search.List search = youtube.search().list("snippet");
		String apiKey = properties.getProperty("youtube.apikey");
		search.setKey(apiKey);
		search.setType("video");
		search.setRelatedToVideoId(gueryLine);
		search.setFields("items(id/kind,id/videoId)");
		search.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);
		SearchListResponse searchResponse = search.execute();

		List<SearchResult> searchResultList = searchResponse.getItems();
		List<String> videoIds = null;

		if (searchResultList != null) {
			videoIds = getVideoIds(searchResultList.iterator());
		}
		return videoIds;
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

	private static List<String> getVideoIds(
			Iterator<SearchResult> iteratorSearchResults) {

		List<String> linksIds = new ArrayList<String>();
		while (iteratorSearchResults.hasNext()) {

			SearchResult singleVideo = iteratorSearchResults.next();
			ResourceId rId = singleVideo.getId();

			if (rId.getKind().equals("youtube#video")) {

				linksIds.add(rId.getVideoId());
			}
		}
		return linksIds;
	}

}
