import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BooleanSearchEngine implements SearchEngine {
    private Map<String, List<PageEntry>> searchIndex;

    public BooleanSearchEngine(File pdfsDir) throws IOException {
        searchIndex = new HashMap<>();
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
    public List<PageEntry> search(String word) {
        return searchIndex.entrySet().stream().filter(x -> x.getKey().equals(word))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .sorted().collect(Collectors.toList());
    }
}
