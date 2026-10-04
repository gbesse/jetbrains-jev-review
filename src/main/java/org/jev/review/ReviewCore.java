package org.jev.review;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ReviewCore {
    public static final String MODEL = "jev-1.13.0";
    private ReviewCore() {}

    public record SourceLine(String id, int editorLine, int startOffset, int endOffset, String text) {}
    public record Finding(String issue, String label, SourceLine line, double probability) {}

    public static List<SourceLine> selectedLines(String text, int startLine, int selectionStart) {
        var result = new ArrayList<SourceLine>();
        var rows = text.split("\\R", -1);
        int offset = selectionStart;
        for (int index = 0; index < rows.length; index++) {
            String row = rows[index];
            if (!row.isBlank()) result.add(new SourceLine("L" + (result.size() + 1), startLine + index, offset, offset + row.length(), row));
            offset += row.length() + 1;
        }
        if (result.isEmpty()) throw new IllegalArgumentException("Select at least one non-empty line");
        if (result.size() > 255) throw new IllegalArgumentException("Select at most 255 non-empty lines");
        return result;
    }

    public static JsonObject request(List<SourceLine> lines) {
        var root = new JsonObject(); root.addProperty("model", MODEL);
        var state = new JsonObject(); var source = new JsonArray();
        for (var line : lines) { var item = new JsonObject(); item.addProperty("id", line.id()); item.addProperty("line", line.editorLine() + 1); item.addProperty("text", line.text()); source.add(item); }
        state.add("selectedLines", source); root.add("state", state);
        var questions = new JsonObject();
        addIssue(questions, "ambiguous_intent", "Does the selected code contain materially ambiguous intent that could change runtime behavior?", lines);
        addIssue(questions, "behavioral_risk", "Does the selected code introduce a plausible behavioral regression not enforced by local code or types?", lines);
        root.add("questions", questions); return root;
    }

    private static void addIssue(JsonObject questions, String id, String instruction, List<SourceLine> lines) {
        var verdict = new JsonObject(); verdict.addProperty("type", "noul"); verdict.addProperty("instructions", instruction); questions.add(id + "__present", verdict);
        var location = new JsonObject(); location.addProperty("type", "choice"); location.addProperty("instructions", "Select the exact line that best demonstrates: " + instruction);
        var criteria = new JsonObject(); for (var line : lines) criteria.add(line.id(), new JsonObject()); location.add("criteria", criteria); questions.add(id + "__line", location);
    }

    public static List<Finding> findings(JsonObject response, List<SourceLine> lines, double threshold) {
        var answers = response.getAsJsonObject("answers"); if (answers == null) throw new IllegalArgumentException("Missing answers");
        var ids = Set.of("ambiguous_intent", "behavioral_risk"); var output = new ArrayList<Finding>();
        for (var id : ids) {
            var verdict = answers.getAsJsonObject(id + "__present"); var location = answers.getAsJsonObject(id + "__line");
            if (verdict == null || !"noul".equals(verdict.get("type").getAsString())) throw new IllegalArgumentException("Invalid verdict: " + id);
            double probability = verdict.get("noul").getAsDouble(); if (probability < threshold) continue;
            String lineId = location.get("choice").getAsString();
            SourceLine line = lines.stream().filter(candidate -> candidate.id().equals(lineId)).findFirst().orElseThrow(() -> new IllegalArgumentException("Provider cited a line outside the selection"));
            output.add(new Finding(id, id.equals("ambiguous_intent") ? "Ambiguous intent" : "Behavioral risk", line, probability));
        }
        return output;
    }
}
