package ph.edu.usc24100050.ItirenaryPlannerCore;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.concurrent.CompletableFuture;

import ph.edu.usc24100050.Model.ActivityRoot;
import ph.edu.usc24100050.Model.Itinerary;
import ph.edu.usc24100050.Model.UserItineraryPreference;

public class ItineraryPlanner {
    private LLMAPI ai;
    
    public ItineraryPlanner(LLMAPI ai) {
        this.ai = ai;
    }

    public ActivityRoot createActivityRootResponse(String rawResponseText) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(rawResponseText);
            String contentJson = root.get("choices").get(0).get("message").get("content").asText();
            JsonNode userPrefNode = mapper.readTree(contentJson);
            return mapper.treeToValue(userPrefNode, ActivityRoot.class);
        } catch (Exception e) {
            Log.e("ItineraryPlanner", "Error parsing activity root: " + e.getMessage());
            return null;
        }
    }

    public String createBetterItineraryResponse(String rawResponseText) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(rawResponseText);
            return root.get("choices").get(0).get("message").get("content").asText();
        } catch (Exception e) {
            Log.e("ItineraryPlanner", "Error parsing better itinerary: " + e.getMessage());
            return null;
        }
    }

    public Itinerary createItinerary(String rawResponseText) {
        ObjectMapper mapper = new ObjectMapper();
        // Removed SnakeCaseStrategy because models now use @JsonProperty for consistency
        try {
            JsonNode root = mapper.readTree(rawResponseText);
            String contentJson = root.get("choices").get(0).get("message").get("content").asText();
            JsonNode contentNode = mapper.readTree(contentJson);
            JsonNode itineraryNode = contentNode.get("itinerary");
            return mapper.treeToValue(itineraryNode, Itinerary.class);
        } catch (Exception e) {
            Log.e("ItineraryPlanner", "Error parsing itinerary: " + e.getMessage());
            return null;
        }
    }

    public UserItineraryPreference createUserItineraryPreference(String rawResponseText) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode root = mapper.readTree(rawResponseText);
            String contentJson = root.get("choices").get(0).get("message").get("content").asText();
            JsonNode userPrefNode = mapper.readTree(contentJson);
            return mapper.treeToValue(userPrefNode, UserItineraryPreference.class);
        } catch (Exception e) {
            Log.e("ItineraryPlanner", "Error parsing user preference: " + e.getMessage());
            return null;
        }
    }

    public CompletableFuture<String> createBetterItinerary(String prompt, String activity) {
        String role = """
            You are Umiko, a warm local travel guide for Cebu.
            The user wants to visit: """ + activity + """
            .
            
            Write the itinerary in this EXACT format for each activity block:
            
            ## 🗓️ [Day Label e.g. "Day 1 — Cebu City Heritage"]
            
            ---
            
            ### 🕘 [Time] | 📍 [Venue Name]
            
            - 🎯 **What to do:** [Specific activity — not generic]
            - 💰 **Cost:** [Entrance fee or estimated spend, or "Free"]
            - ⏱️ **Duration:** [How long to spend here]
            - 🚗 **Getting there:** [From previous stop — transport type + estimated time + cost]
            - 🗺️ **Map:** [https://www.google.com/maps/search/?api=1&query=[VenueName+Location] ]
            - ⭐ **Reviews:** [https://www.google.com/search?q=[VenueName+Cebu+reviews] ]
            
            ---
            
            Rules:
            - NEVER write generic lines like "enjoy the view" or "take lots of photos"
            - Every field must have a real, specific value — no placeholders
            - For map and review links, encode the venue name and location properly in the URL (replace spaces with +)
            - Group nearby venues in the same time block area
            - Between distant areas, add a travel break line: "🚗 ~[X] min by [transport] to next area"
            - End each day with: "🏨 **End of Day** — [one-line note on where to rest or next day prep]"
            - Keep bullet points short — max 1 sentence each
            """;

        return ai.ask(prompt, role)
                .thenApply(this::createBetterItineraryResponse);
    }

    public CompletableFuture<ActivityRoot> createActivityRoot(String activity) {
        String responseFormat = """
                "response_format": {
                   "type": "json_schema",
                   "json_schema": {
                     "name": "cebu_activity_recommendations",
                     "strict": true,
                     "schema": {
                       "type": "object",
                       "properties": {
                         "activity": { "type": "string" },
                         "categories": {
                           "type": "array",
                           "items": {
                             "type": "object",
                             "properties": {
                               "categoryName": { "type": "string" },
                               "backgroundColor": { "type": "string" },
                               "categoryList": {
                                 "type": "array",
                                 "items": {
                                   "type": "object",
                                   "properties": {
                                     "title": { "type": "string" },
                                     "description": { "type": "string" },
                                     "location": { "type": "string" }
                                   },
                                   "required": ["title", "description", "location"],
                                   "additionalProperties": false
                                 }
                               }
                             },
                             "required": ["categoryName", "backgroundColor", "categoryList"],
                             "additionalProperties": false
                           }
                         }
                       },
                       "required": ["activity", "categories"],
                       "additionalProperties": false
                     }
                   }
                 }
                """;

        String role = "You are Umiko, a Cebu travel guide. Categorize spots in Cebu for: " + activity + ". " +
                "Use pastel background colors and JSON format.";

        return ai.ask(activity, role, responseFormat)
                .thenApply(this::createActivityRootResponse);
    }
    public CompletableFuture<Itinerary> createPlan(String plan) {
        String responseFormat = """
            "response_format": {
                "type": "json_schema",
                "json_schema": {
                 "name": "cebu_itinerary",
                 "strict": true,
                 "schema": {
                     "type": "object",
                     "properties": {
                         "itinerary": {
                             "type": "object",
                             "properties": {
                                 "start_date": { "type": "string" },
                                 "stop_date": { "type": "string" },
                                 "total": { "type": "integer" },
                                 "days": {
                                     "type": "array",
                                     "items": {
                                         "type": "object",
                                         "properties": {
                                             "date": { "type": "string" },
                                             "day_number": { "type": "integer" },
                                             "activities": {
                                                 "type": "array",
                                                 "items": {
                                                     "type": "object",
                                                     "properties": {
                                                         "venue": { "type": "string" },
                                                         "activity": { "type": "string" },
                                                         "start_time": { "type": "string", "pattern": "^[0-9]{4}$" },
                                                         "stop_time": { "type": "string", "pattern": "^[0-9]{4}$" },
                                                         "place_type": { "type": "string", "enum": ["HISTORICAL", "BEACH", "FOOD", "NATURE", "SHOPPING", "RELIGIOUS"] },
                                                         "duration_minutes": { "type": "integer" },
                                                         "notes": { "type": "string" },
                                                         "travel_from_previous": { "type": "string" },
                                                         "latitude": { "type": "number" },
                                                         "longitude": { "type": "number" }
                                                     },
                                                     "required": ["venue", "activity", "start_time", "stop_time", "place_type", "duration_minutes", "notes", "travel_from_previous", "latitude", "longitude"],
                                                     "additionalProperties": false
                                                 }
                                             }
                                         },
                                         "required": ["date", "day_number", "activities"],
                                         "additionalProperties": false
                                     }
                                 }
                             },
                             "required": ["start_date", "stop_date", "total", "days"],
                             "additionalProperties": false
                         }
                     },
                     "required": ["itinerary"],
                     "additionalProperties": false
                 }
                }
            }
            """;

        String role = """
            You are Umiko, a warm and knowledgeable local travel expert for Cebu, Philippines.
            
            When creating an itinerary, follow these principles:
            
            1. LOGICAL FLOW: Order activities by geography to minimize backtracking. Group nearby spots together.
            
            2. REALISTIC TIMING: Account for travel time between venues in travel_from_previous (e.g., "~15 min by habal-habal from Tops"). Include buffer time for meals, rest, and transit.
            
            3. RICH NOTES: The notes field should include:
               - What makes this spot special or worth visiting
               - Practical tips (opening hours, entrance fees, what to bring, best time of day)
               - Insider advice (what to order, where to park, what to avoid)
               - Any reminders relevant to the traveler's context (e.g., "store luggage at hotel before heading out")
            
            4. HONEST SCHEDULING: If a day has a free/flexible block (e.g., personal meetup, open afternoon), include it as an activity with venue like "TBD - Free Time" and use notes to suggest nearby options.
            
            5. TRANSITIONS: For the last activity of a day or trip, include departure logistics in notes (e.g., transport to airport, check-in reminder, recommended departure time).
            
            6. ACCURATE COORDINATES: Use precise, real-world latitude and longitude for every venue in Cebu.
            
            Use HHMM format for times (e.g., '0900'). Respond only in valid JSON matching the schema.
            """;

        return ai.ask(plan, role, responseFormat)
                .thenApply(this::createItinerary);
    }
}
