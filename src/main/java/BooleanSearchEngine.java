import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

public class BooleanSearchEngine implements SearchEngine {
    private final String stopWordsFileName = "stop-ru.txt";
    private Map<String, List<PageEntry>> searchIndex;
    private Set<String> stopWords;

    private void ParseStopWordsFile() {
        try (BufferedReader bufReader = new BufferedReader(new FileReader(stopWordsFileName))) {
            String line;
            while ((line = bufReader.readLine()) != null) {
                stopWords.add(line.toLowerCase());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public BooleanSearchEngine(File pdfsDir) throws IOException {
        searchIndex = new HashMap<>();
        stopWords = new HashSet<>();
        ParseStopWordsFile();
        System.out.println("Indexing started ...");

        if (!pdfsDir.isDirectory()) {
            throw new IllegalArgumentException("Wrong directory to index");
        }
        String[] files = pdfsDir.list();

        assert files != null;
        for (String file : files) {
            PdfDocument doc = new PdfDocument(new PdfReader(pdfsDir + "/" + file));
            for (int pageNum = 0; pageNum < doc.getNumberOfPages(); pageNum++) {
                String pageText = PdfTextExtractor.getTextFromPage(doc.getPage(pageNum + 1)).toLowerCase();
                String[] words = pageText.split("\\P{IsAlphabetic}+");
                Map<String, Integer> freqs = new HashMap<>();;
                for (String word : words) {
                    //update freqs
                    if (!word.isEmpty()) {
                        freqs.put(word, freqs.getOrDefault(word, 0) + 1);
                    }
                }
                for (var freq : freqs.entrySet()) {
                    List<PageEntry> entries = searchIndex.get(freq.getKey());
                    if (entries == null) {
                        entries = new ArrayList<>();
                        searchIndex.put(freq.getKey(), entries);
                    }
                    entries.add(new PageEntry(file, pageNum + 1, freq.getValue()));
                }
            }
        }
        System.out.println("Finished indexing, " + searchIndex.size() + " unique words");
    }

    @Override
    public List<PageEntry> search(String query) {
        Set<String> words = new HashSet<>(List.of(query.split(" ")));
        return searchIndex.entrySet().stream()
                .filter(x -> !stopWords.contains(x.getKey()))
                .filter(x -> words.contains(x.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .collect(groupingBy(PageEntry::getPdfName,
                        groupingBy(PageEntry::getPage, Collectors.summingInt(PageEntry::getCount))))
                .entrySet().stream()
                .flatMap(page -> {
                    return page.getValue().entrySet().stream()
                            .map(numberCount -> new PageEntry(page.getKey(), numberCount.getKey(), numberCount.getValue()));
                })
                .sorted().collect(Collectors.toList());
    }
}
