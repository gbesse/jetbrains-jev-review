package org.jev.review;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ReviewCoreTest {
    @Test void preservesEditorCoordinates() {
        var lines = ReviewCore.selectedLines("first\n\nthird", 9, 100);
        assertEquals(10, lines.get(0).editorLine() + 1);
        assertEquals(12, lines.get(1).editorLine() + 1);
        assertEquals(107, lines.get(1).startOffset());
    }

    @Test void requestCriteriaContainsOnlySelectedLines() {
        var lines = ReviewCore.selectedLines("a\nb", 0, 0);
        var criteria = ReviewCore.request(lines).getAsJsonObject("questions").getAsJsonObject("behavioral_risk__line").getAsJsonObject("criteria");
        assertEquals(java.util.Set.of("L1", "L2"), criteria.keySet());
    }

    @Test void rejectsInventedCitation() {
        var lines = ReviewCore.selectedLines("a", 0, 0); var response = new JsonObject(); var answers = new JsonObject(); response.add("answers", answers);
        for (var id : java.util.List.of("ambiguous_intent", "behavioral_risk")) { var verdict = new JsonObject(); verdict.addProperty("type", "noul"); verdict.addProperty("noul", id.equals("ambiguous_intent") ? .9 : .1); answers.add(id + "__present", verdict); var citation = new JsonObject(); citation.addProperty("type", "choice"); citation.addProperty("choice", "invented"); answers.add(id + "__line", citation); }
        assertThrows(IllegalArgumentException.class, () -> ReviewCore.findings(response, lines, .75));
    }
}
