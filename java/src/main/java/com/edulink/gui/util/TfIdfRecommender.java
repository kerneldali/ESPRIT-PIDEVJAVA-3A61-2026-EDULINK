package com.edulink.gui.util;

import com.edulink.gui.models.courses.Course;
import com.edulink.gui.models.courses.Matiere;
import com.edulink.gui.services.courses.CourseService;

import java.util.*;
import java.util.stream.Collectors;

public class TfIdfRecommender {

    public static class MatchResult {
        public Matiere matiere;
        public double score;

        public MatchResult(Matiere matiere, double score) {
            this.matiere = matiere;
            this.score = score;
        }
    }

    public static List<MatchResult> recommend(String query, List<Matiere> matieres) {
        CourseService courseService = new CourseService();
        List<Course> allCourses = courseService.getAll();

        List<List<String>> documents = new ArrayList<>();
        Map<Integer, Matiere> docIndexMap = new HashMap<>();

        // Create corpus
        int idx = 0;
        for (Matiere m : matieres) {
            StringBuilder sb = new StringBuilder();
            // Weighting: Repeat the category name 3 times to boost its relevance
            sb.append(m.getName()).append(" ").append(m.getName()).append(" ").append(m.getName()).append(" ");
            if (m.getDescription() != null) sb.append(m.getDescription()).append(" ");
            
            // Get courses for this matiere
            List<Course> courses = allCourses.stream()
                .filter(c -> c.getMatiereId() == m.getId())
                .collect(Collectors.toList());

            for (Course c : courses) {
                // Boost course titles too
                sb.append(c.getTitle()).append(" ").append(c.getTitle()).append(" ");
                if (c.getDescription() != null) sb.append(c.getDescription()).append(" ");
            }

            documents.add(tokenize(sb.toString()));
            docIndexMap.put(idx++, m);
        }

        List<String> queryTokens = tokenize(query);
        if (queryTokens.isEmpty()) return new ArrayList<>();
        
        // Build vocabulary and IDF
        Set<String> vocab = new HashSet<>();
        for (List<String> doc : documents) vocab.addAll(doc);
        vocab.addAll(queryTokens);

        Map<String, Double> idf = new HashMap<>();
        int N = documents.size();
        for (String term : vocab) {
            int df = 0;
            for (List<String> doc : documents) {
                if (doc.contains(term)) df++;
            }
            // Use smoothing for IDF calculation
            idf.put(term, Math.log((double) (N + 1) / (df + 1)) + 1.0);
        }

        // Vectorize query
        Map<String, Double> queryVec = buildVector(queryTokens, idf);
        
        List<MatchResult> results = new ArrayList<>();
        for (int i = 0; i < documents.size(); i++) {
            Map<String, Double> docVec = buildVector(documents.get(i), idf);
            double sim = cosineSimilarity(queryVec, docVec);
            if (sim > 0.05) { // Threshold to avoid irrelevant matches
                results.add(new MatchResult(docIndexMap.get(i), sim));
            }
        }

        // Sort by score descending and take top 3
        results.sort((a, b) -> Double.compare(b.score, a.score));
        return results.stream().limit(3).collect(Collectors.toList());
    }

    private static List<String> tokenize(String text) {
        if (text == null) return new ArrayList<>();
        String[] words = text.toLowerCase().replaceAll("[^a-z0-9\\u00e0-\\u00ff]", " ").split("\\s+");
        List<String> tokens = new ArrayList<>();
        for (String w : words) {
            if (w.length() > 2) tokens.add(w);
        }
        return tokens;
    }

    private static Map<String, Double> buildVector(List<String> tokens, Map<String, Double> idf) {
        Map<String, Double> vec = new HashMap<>();
        Map<String, Integer> tf = new HashMap<>();
        for (String t : tokens) {
            tf.put(t, tf.getOrDefault(t, 0) + 1);
        }
        for (String t : tf.keySet()) {
            vec.put(t, tf.get(t) * idf.getOrDefault(t, 0.0));
        }
        return vec;
    }

    private static double cosineSimilarity(Map<String, Double> v1, Map<String, Double> v2) {
        double dot = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String k : v1.keySet()) {
            double val = v1.get(k);
            dot += val * v2.getOrDefault(k, 0.0);
            norm1 += val * val;
        }
        for (String k : v2.keySet()) {
            double val = v2.get(k);
            norm2 += val * val;
        }

        if (norm1 == 0 || norm2 == 0) return 0.0;
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
}
