package ph.edu.usc24100050.ItirenaryPlannerCore;

import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.util.concurrent.CompletableFuture;

import ph.edu.usc24100050.Model.Itinerary;
import ph.edu.usc24100050.Model.UserItineraryPreference;

public class ItineraryPlanner {
    // this class is all about creating the itinerary that you get from AI
    // by creating the model's based on the output
    // these models can then be used in a controller to create an output
    private LLMAPI ai;
    public ItineraryPlanner(LLMAPI ai)
    {
        this.ai = ai;
    }
    public Itinerary createItinerary(String rawResponseText)
    {
        Log.d("HELLOWORLD", "IS IT WORKING CHAT?");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());

        try {
            JsonNode root = mapper.readTree(rawResponseText);
            String contentJson = root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            JsonNode contentNode = mapper.readTree(contentJson);

            JsonNode itineraryNode = contentNode.get("itinerary");
            Itinerary itinerary = mapper.treeToValue(itineraryNode, Itinerary.class);

            return itinerary;
        } catch (Exception e)
        {
            e.printStackTrace();
            Log.e("ERROR", e.getMessage());
            return null;
        }
    }

    public UserItineraryPreference createUserItineraryPreference(String rawResponseText)
    {
        Log.d("HELLOWORLD", "TEST CHAT");
        Log.d("HELLOWORLD", rawResponseText);

        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode root = mapper.readTree(rawResponseText);
            String contentJson = root
                    .get("choices")
                    .get(0)
                    .get("message")
                    .get("content")
                    .asText();

            JsonNode userPrefNode = mapper.readTree(contentJson);
            UserItineraryPreference response = mapper.treeToValue(userPrefNode, UserItineraryPreference.class);
            Log.d("HELLOWORLD", "TEST CHAT");
            Log.d("HELLOWORLD",  response.getActivityName());
            Log.d("HELLOWORLD", ""  + response.getCities().size());
            return response;

        } catch (JsonProcessingException e) {
            e.printStackTrace();
            Log.e("HELLOWORLDE", e.getMessage());

            Log.e("ERROR", e.getMessage());
            return null;
        }
    }


    public CompletableFuture<UserItineraryPreference> createUserPreferencePlan(String activity)
    {
        String responseFormat = """
                                "response_format": {
                                    "type": "json_schema",
                                    "json_schema": {
                                        "name": "user_itinerary_response",
                                        "strict": true,
                                        "schema": {
                                            "type": "object",
                                            "properties": {
                                                "activityName": {
                                                    "type": "string"
                                                },
                                                "cities": {
                                                    "type": "array",
                                                    "items": {
                                                        "type": "string"
                                                    }
                                                }
                                            },
                                            "required": [
                                                "activityName",
                                                "cities"
                                            ],
                                            "additionalProperties": false
                                        }
                                    }
                                }
                                """;


        String role = "You are an Itinerary Planner for Cebu, Your goal is to give all the cities available for an activity, if user hasn't specified a date range then try your best to create a 3 day plan. Send response in JSON";

        return ai.ask(activity, role, responseFormat)
                .thenApply(this::createUserItineraryPreference)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });

    }

    public CompletableFuture<Itinerary> createPlan(String plan)
    {
        String responseFormat = """
                            "response_format": {
                                "type": "json_schema",
                                "json_schema": {
                                 "name": "product_review",
                                 "strict": true,
                                 "schema": {
                                     "type": "object",
                                     "properties": {
                                         "itinerary": {
                                             "type": "object",
                                             "properties": {
                                                 "start_date": {
                                                     "type": "string",
                                                     "format": "date"
                                                 },
                                                 "stop_date": {
                                                     "type": "string",
                                                     "format": "date"
                                                 },
                                                 "total": {
                                                     "type": "integer"
                                                 },
                                                 "days": {
                                                     "type": "array",
                                                     "items": {
                                                         "type": "object",
                                                         "properties": {
                                                             "date": {
                                                                 "type": "string",
                                                                 "format": "date"
                                                             },
                                                             "activities": {
                                                                 "type": "array",
                                                                 "items": {
                                                                     "type": "object",
                                                                     "properties": {
                                                                         "venue": {
                                                                             "type": "string"
                                                                         },
                                                                         "activity": {
                                                                             "type": "string"
                                                                         },
                                                                         "start_time": {
                                                                             "type": "string",
                                                                             "pattern": "^[0-9]{4}$"
                                                                         },
                                                                         "stop_time": {
                                                                             "type": "string",
                                                                             "pattern": "^[0-9]{4}$"
                                                                         }
                                                                     },
                                                                     "required": [
                                                                         "venue",
                                                                         "activity",
                                                                         "start_time",
                                                                         "stop_time"
                                                                     ],
                                                                     "additionalProperties": false
                                                                 }
                                                             }
                                                         },
                                                         "required": [
                                                             "date",
                                                             "activities"
                                                         ],
                                                         "additionalProperties": false
                                                     }
                                                 }
                                             },
                                             "required": [
                                                 "start_date",
                                                 "stop_date",
                                                 "total",
                                                 "days"
                                             ],
                                             "additionalProperties": false
                                         }
                                     },
                                     "required": [
                                         "itinerary"
                                     ],
                                     "additionalProperties": false
                                 }
                                }
                            }
                """;

        String role = "You are an Itinerary Planner for Cebu, The plans should be replicable in real life. Send response in JSON";

        return ai.ask(plan, role, responseFormat)
                .thenApply(this::createItinerary)
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
    }
}
