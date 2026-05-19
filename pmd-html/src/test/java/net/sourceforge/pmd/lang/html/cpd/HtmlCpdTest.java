package net.sourceforge.pmd.lang.html.cpd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import net.sourceforge.pmd.cpd.CPDConfiguration;
import net.sourceforge.pmd.cpd.CpdAnalysis;
import net.sourceforge.pmd.cpd.Match;
import net.sourceforge.pmd.internal.util.IOUtil;
import net.sourceforge.pmd.lang.html.HtmlLanguageModule;

class HtmlCpdTest {
    private Path testdir;

    @BeforeEach
    void setUp() {
        String path = IOUtil.normalizePath("src/test/resources/net/sourceforge/pmd/lang/html/cpd/testdata");
        testdir = Paths.get(path);
    }

    @Test
    void testIssue5313() throws Exception {
        CPDConfiguration configuration = new CPDConfiguration();
        configuration.setMinimumTileSize(100);
        configuration.setOnlyRecognizeLanguage(HtmlLanguageModule.getInstance());
        try (CpdAnalysis cpd = CpdAnalysis.create(configuration)) {
            cpd.files().addFile(testdir.resolve("issue5313_no_newline.html"));
            cpd.files().addFile(testdir.resolve("issue5313_newline.html"));

            cpd.performAnalysis(matches -> {
                assertEquals(1, matches.getMatches().size());
                Match duplication = matches.getMatches().get(0);
                assertTrue(matches.getSourceCodeSlice(duplication.getFirstMark()).toString().contains("</html>"));
            });
        }
    }

}
