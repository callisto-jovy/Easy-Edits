package de.yugata.easy.edits.editor.filter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.yugata.easy.edits.editor.EditInfo;
import de.yugata.easy.edits.editor.FlutterWrapper;
import de.yugata.easy.edits.util.FFmpegUtil;
import org.bytedeco.javacpp.tools.ParserException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterParser {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$([A-Za-z]+)\\$");

    private final EditInfo editInfo;


    public FilterParser(EditInfo editInfo) {
        this.editInfo = editInfo;
    }


    private static Optional<JsonElement> getOr(final JsonObject element, final String key) {
        return Optional.ofNullable(element.get(key));
    }

    public static FlutterWrapper.FlutterFilterWrapper getFilterWrapper(final JsonElement element) throws Exception {
        if (!element.isJsonObject()) {
            throw new ParserException("Filter root object is not a json object.");
        }

        final JsonObject root = element.getAsJsonObject();
        // The id has to be specified. If a nullpointer is thrown, that means that the filter is not formatted properly
        // TODO: Handle these exceptions.
        final String name = root.get("name").getAsString();
        // grab description or map default.
        final String description = getOr(root, "description").map(JsonElement::getAsString).orElse("No description available for this filter.");

        // Get the display name. If no display name is there, we just fall back to the name (id)
        final String displayName = getOr(root, "display_name").map(JsonElement::getAsString).orElse(name);

        // Grab the type.
        final String type = root.get("type").getAsString().toUpperCase();
        final FilterType filterType = FilterType.valueOf(type);

        // optional help text
        final String helpText = getOr(root, "help").map(JsonElement::getAsString).orElse("No help text available");


        final List<FilterValue> values = new ArrayList<>();

        root.getAsJsonArray("settings").forEach(jsonElement -> {
            final JsonObject setting = jsonElement.getAsJsonObject();
            final String valueName = setting.get("name").getAsString();
            final String value = setting.has("default") ? setting.get("default").getAsString() : "";

            final TokenType tokenType = TokenType.valueOf(setting.get("type").getAsString().toUpperCase());

            if (tokenType == TokenType.VALUE) {
                values.add(new FilterValue(valueName, value));
            }
        });

        return new FlutterWrapper.FlutterFilterWrapper(name, displayName, description, helpText, filterType, values);
    }


    public boolean doesIdMatch(final String name, final JsonElement element) throws ParserException {
        if (!element.isJsonObject()) {
            throw new ParserException("Filter root object is not a json object.");
        }
        return element.getAsJsonObject().get("name").getAsString().equals(name);
    }


    public Filter parseFilter(final FilterWrapper filterWrapper, final JsonElement element) throws Exception {

        if (!element.isJsonObject()) {
            throw new ParserException("Filter root object is not a json object.");
        }

        final JsonObject root = element.getAsJsonObject();

        // Grab type.
        final String type = root.get("type").getAsString().toUpperCase();
        final FilterType filterType = FilterType.valueOf(type);

        // grab the id (name)
        final String name = root.get("name").getAsString();
        // The raw command that may be parsed.
        final String rawCommand = root.get("command").getAsString();

        // Array of all the "settings", e.g. tokens that have to be resolved.
        final JsonArray settings = root.getAsJsonArray("settings");

        // Nothing to parse
        if (settings.isEmpty())
            return new Filter(name, rawCommand, filterType);


        // Parse the command
        final String parsedCommand = parseCommand(filterWrapper, rawCommand, settings);

        return new Filter(name, parsedCommand, filterType);
    }


    private String parseCommand(final FilterWrapper filterWrapper, final String command, final JsonArray settings) {
        // Scan the command for items.
        final Matcher matcher = VARIABLE_PATTERN.matcher(command);

        //TODO: Go through the settings, rather than the matches. In that case, we dont even need the matches..

        String parsedCommand = command;

        while (matcher.find()) {
            parsedCommand = matcher.replaceAll(matchResult -> parseToken(matchResult.group(1), settings, filterWrapper));
        }

        return parsedCommand;
    }

    private String parseToken(final String group, final JsonArray settings, final FilterWrapper filterWrapper) {
        for (JsonElement element : settings) {
            final JsonObject setting = element.getAsJsonObject();
            final String name = setting.get("name").getAsString();

            if (!name.equalsIgnoreCase(group))
                continue;

            final String type = setting.get("type").getAsString().toUpperCase();
            final TokenType tokenType = TokenType.valueOf(type);


            if (tokenType == TokenType.VARIABLE) {
                return getVariable(setting);
            } else if (tokenType == TokenType.VALUE) {
                final Optional<FilterValue> value = filterWrapper.getValues().stream().filter(filterValue -> filterValue.getName().equals(name)).findFirst();

                return value.map(FilterValue::getValue).orElse("");
            }
            // TODO: variable offsets may be set by values.
            // TODO: make it possible to use time formatting.
        }
        return "";
    }

    private String getVariable(final JsonObject variable) {
        final String map = variable.get("mapped").getAsString();
        final boolean hasOffset = variable.has("offset");     // Offset in seconds

        for (final VariableMapper value : VariableMapper.values()) {
            if (value.variable.equalsIgnoreCase(map)) {
                final String mapped = value.mapper.apply(editInfo);

                return hasOffset ? value.offsetMapper.apply(editInfo, variable.get("offset").getAsString()) : mapped;
            }
        }

        return "";
    }


    private enum TokenType {

        // TODO:  Add a possibility for dropdown options in the json code.
        VARIABLE, VALUE

    }


    private enum VariableMapper {
        EDIT_DURATION(
                "edit_time",
                editInfo -> String.valueOf(editInfo.getEditTime()),
                (editInfo, s) -> String.valueOf(editInfo.getEditTime() + TimeUnit.SECONDS.toMicros(Long.parseLong(s)))),
        EDIT_DURATION_SECONDS(
                "edit_time_s",
                editInfo -> String.valueOf(editInfo.getEditTime() / 1000000L),
                (editInfo, s) -> String.valueOf(editInfo.getEditTime() / 1000000L + Long.parseLong(s))
        ),
        FONT_FILE("font_file", editInfo -> FFmpegUtil.getFontFile(), (editInfo1, s) -> FFmpegUtil.getFontFile());


        private final String variable;
        private final Function<EditInfo, String> mapper;

        private final BiFunction<EditInfo, String, String> offsetMapper;


        VariableMapper(String variable, Function<EditInfo, String> mapper, BiFunction<EditInfo, String, String> offsetMapper) {
            this.variable = variable;
            this.mapper = mapper;
            this.offsetMapper = offsetMapper;
        }
    }
}
