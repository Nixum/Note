package graph;

import java.util.*;

public class DFS {

    private static Map<String, Integer> isVisit;
    private static List<String> pathStrList;

    public static void convert(Map<String, List<String>> inputMap, String currentKey, String pathStr) {
        if (!inputMap.containsKey(currentKey)) {
            pathStrList.add(pathStr);
            return;
        }
        for (String item : inputMap.get(currentKey)) {
            if (isVisit.get(item) == 1) {
                return;
            }
            isVisit.put(item, 1);
            convert(inputMap, item, pathStr + "," + item);
            isVisit.put(item, 0);
        }
    }
    public static void main(String []args) {
        List<List<String>> results = new ArrayList<>();
        isVisit = new HashMap<>();
        pathStrList = new ArrayList<>();
        Map<String, List<String>> inputMap = new HashMap<>();
        inputMap.put("B", new ArrayList<>(Arrays.asList("C", "X")));
        inputMap.put("C", new ArrayList<>(Arrays.asList("D")));
        inputMap.put("F", new ArrayList<>(Arrays.asList("B", "A")));
        inputMap.put("J", new ArrayList<>(Arrays.asList("K")));
        inputMap.put("A", new ArrayList<>(Arrays.asList("Z")));
        inputMap.put("K", new ArrayList<>(Arrays.asList("L")));
        inputMap.put("X", new ArrayList<>(Arrays.asList("Y")));

        for (Map.Entry<String, List<String>> entry : inputMap.entrySet()) {
            isVisit.put(entry.getKey(), 0);
            for (String val : entry.getValue()){
                isVisit.put(val, 0);
            }
        }
        for (String key : inputMap.keySet()) {
            convert(inputMap, key, key);
        }
        for (String p1 : pathStrList) {
            boolean isContain = false;
            for (String p2 : pathStrList) {
                if (!p1.equals(p2) && p2.contains(p1)) {
                    isContain = true;
                    break;
                }
            }
            if (!isContain) {
                results.add(Arrays.asList(p1.split(",")));
            }
        }
        // 结果打印
        for (List<String> p : results) {
            System.out.println(Arrays.toString(p.toArray()));
        }
    }
}
