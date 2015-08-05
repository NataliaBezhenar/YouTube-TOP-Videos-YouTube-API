package com.nbezhenar;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class Sorter {
	public static Map<String, Integer> compareByRating(
			Map<String, Integer> linksMap) {
		List<Map.Entry<String, Integer>> entries = new LinkedList<Map.Entry<String, Integer>>(
				linksMap.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Entry<String, Integer> o1,
					Entry<String, Integer> o2) {
				return o2.getValue().compareTo(o1.getValue());
			}
		});

		Map<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();

		for (Map.Entry<String, Integer> entry : entries) {
			sortedMap.put(entry.getKey(), entry.getValue());
		}
		return sortedMap;
	}
	
	public static Map<String, Integer> getTopNValues(
			Map<String, Integer> linksMap, int linksNumber) {
		Map<String, Integer> trimmedMap = new LinkedHashMap<String, Integer>();
		Object[] temp = linksMap.values().toArray();
		Object[] temp1 = new Object[linksNumber];
		for (int i = 0; i < linksNumber; i++) {
			temp1[i] = temp[i];
		}
		for (Entry<String, Integer> entry : linksMap.entrySet()) {
			for (Object o : temp1) {
				if (entry.getValue().equals(o))
					trimmedMap.put("https://www.youtube.com/watch?v="+entry.getKey(), (Integer) o);
			}
		}

		return trimmedMap;
	}
	
}
