package com.fieldbook.tracker.brapi;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.arch.core.util.Function;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.fieldbook.tracker.DataHelper;
import com.fieldbook.tracker.fields.FieldObject;
import com.fieldbook.tracker.traits.TraitObject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;

public class BrAPIService {
    private String brapiBaseURL;
    private Context context;
    private RequestQueue queue;
    private DataHelper dataHelper;

    public BrAPIService(Context context, String brapiBaseURL) {
        this.context = context;
        this.brapiBaseURL = brapiBaseURL;
        this.queue = Volley.newRequestQueue(context);
        this.dataHelper = new DataHelper(context);
    }

    public void getStudies(final Function<List<StudySummary>, Void> function){
        String url = this.brapiBaseURL + "/studies-search?pageSize=20&page=0"; // BrAPI v1.2, try "/studies" for v1.3
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        List<StudySummary> studies = parseStudiesJson(response);
                        function.apply(studies);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context.getApplicationContext(), "Error loading data", Toast.LENGTH_SHORT).show();

                Log.e("error", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    public void getStudyDetails(String studyDbId, final Function<StudyDetails, Void> function) {

        String url = this.brapiBaseURL + "/studies/" + studyDbId;
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        StudyDetails study = parseStudyDetailsJson(response);
                        function.apply(study);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context.getApplicationContext(), "Error loading data", Toast.LENGTH_SHORT).show();

                Log.e("error", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    public void getPlotDetails(final String studyDbId, final Function<StudyDetails, Void> function) {
        String url = this.brapiBaseURL + "/studies/" + studyDbId + "/observationunits?observationLevel=plot&pageSize=1000&page=0";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        StudyDetails studyDetails = new StudyDetails();
                        studyDetails.setAttributes(new ArrayList<String>());
                        studyDetails.setValues(new ArrayList<List<String>>());
                        parsePlotJson(response, studyDetails);

                        function.apply(studyDetails);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context.getApplicationContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                Log.e("error", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    public void getTraits(final String studyDbId, final Function<StudyDetails, Void> function) {
        String url = this.brapiBaseURL + "/studies/" + studyDbId + "/observationvariables?pageSize=200&page=0";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        StudyDetails studyDetails = new StudyDetails();
                        List<TraitObject> traits = parseTraitsJson(response);
                        studyDetails.setTraits(traits);
                        function.apply(studyDetails);

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(context.getApplicationContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                Log.e("error", error.toString());
            }
        });

        queue.add(stringRequest);
    }

    // Get the ontology from breedbase so the users can select the ontology
    public void getOntology(final Function< List<TraitObject>, Void > function) {

        String url = this.brapiBaseURL + "/variables";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        // Parse the response

                        //TODO: Replace this class and parse function when Pete releases
                        // his brapi java library
                        List<TraitObject> traits = parseTraitsJson(response);
                        function.apply(traits);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context.getApplicationContext(), "Error loading data", Toast.LENGTH_SHORT).show();
                        Log.e("error", error.toString());
                    }
                });
        queue.add(stringRequest);
    }

    public void postPhenotypes() {
        String url = this.brapiBaseURL + "/phenotypes";

        List<Map<String, String>> data = dataHelper.getDataBrapiExport();
        // TODO: group by studyid and group observations in json

        JSONObject request = new JSONObject();
        JSONArray jsonData = new JSONArray();

        try {
            for (Map<String, String> observation : data) {

                JSONObject observationJson = new JSONObject();
                observationJson.put("collector", "NickFieldBook"); //TODO: get user profile name
                observationJson.put("observationDbId", ""); // TODO: handle updates, not just new


                SimpleDateFormat timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ",
                        Locale.getDefault());
                Date time = timeStamp.parse(observation.get("timeTaken"));
                String iso8601Time = TimestampUtils.getISO8601StringForDate(time);


                observationJson.put("observationTimeStamp", iso8601Time);
                observationJson.put("observationUnitDbId", observation.get("observationUnitDbId"));
                observationJson.put("observationVariableDbId", "MO_123:100002"); // TODO: get this from somewhere
                observationJson.put("observationVariableName", "Plant Height"); // TODO: get this from somewhere
                observationJson.put("season", "Spring 2018"); // Needs to be two words to work around BrAPI test server bug
                observationJson.put("value", observation.get("userValue"));
                JSONArray observations = new JSONArray();
                observations.put(observationJson);
                JSONObject observationStudy = new JSONObject();
                observationStudy.put("studyDbId", observation.get("exp_alias"));
                observationStudy.put("observatioUnitDbId", "1"); // TODO: get this from somewhere
                observationStudy.put("observations", observations);
                jsonData.put(observationStudy);
            }

            request.put("data", jsonData);
            Log.d("json", request.toString());


        }catch (JSONException e){
            e.printStackTrace();
        }catch (ParseException e) {
            e.printStackTrace();
        }

        JsonObjectRequest putObservationsRequest = new JsonObjectRequest(Request.Method.POST, url, request,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        //TODO: verify that response indicates everything was written
                        //TODO: update observationDId for observations in database
                        Toast.makeText(context.getApplicationContext(), "BrAPI Export Successful", Toast.LENGTH_SHORT).show();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(context.getApplicationContext(), "BrAPI Export Failed", Toast.LENGTH_SHORT).show();
                        Log.e("error", error.toString());
                    }
                })
        {
            @Override
            public Map<String, String> getHeaders () throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                headers.put("Authorization", "Bearer YYYY");
                return headers;
            }
        };
        queue.add(putObservationsRequest);
    }

    // dummy data test for now
    public void putStudyObservations() {
        final String studyDbId = "1001";
        String url = this.brapiBaseURL + "/studies/" + studyDbId + "/observations";

        // Send dummy data to test server creating new observations
        // TODO: Populate with actual collected data from database
        JSONObject request = new JSONObject();
        JSONArray observations = new JSONArray();
        JSONObject observation0 = new JSONObject();
        JSONObject observation1 = new JSONObject();

        try{
            observation0.put("collector", "NickFieldBook");
            observation0.put("observationDbId", "");
            observation0.put("observationTimeStamp", "2019-08-21T21:37:08.888Z");
            observation0.put("observationUnitDbId", "1");
            observation0.put("observationVariableDbId", "MO_123:100002");
            observation0.put("value", "5");

            observation1.put("collector", "NickFieldBook");
            observation1.put("observationDbId", "");
            observation1.put("observationTimeStamp", "2019-08-21T21:37:08.888Z");
            observation1.put("observationUnitDbId", "1");
            observation1.put("observationVariableDbId", "MO_123:100002");
            observation1.put("value", "666");

            observations.put(observation0);
            observations.put(observation1);
            request.put("observations", observations);

            Log.d("json", observations.toString());
        }catch (JSONException e){
            e.printStackTrace();
        }

        JsonObjectRequest putObservationsRequest = new JsonObjectRequest(Request.Method.PUT, url, request,
            new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    //TODO: verify that response indicates everything was written
                    //TODO: update observationDId for observations in database
                    Toast.makeText(context.getApplicationContext(), "BrAPI Export Successful", Toast.LENGTH_SHORT).show();
                }
            },
            new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(context.getApplicationContext(), "BrAPI Export Failed", Toast.LENGTH_SHORT).show();
                    Log.e("error", error.toString());
                }
            })
            {
                @Override
                public Map<String, String> getHeaders () throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Accept", "application/json");
                    headers.put("Authorization", "Bearer YYYY");
                    return headers;
                }
            };
        queue.add(putObservationsRequest);
    }

    private List<StudySummary> parseStudiesJson(String json) {

        List<StudySummary> studies = new ArrayList<>();
        try {
            JSONObject js = new JSONObject(json);
            JSONObject result = (JSONObject) js.get("result");
            JSONArray data = (JSONArray) result.get("data");
            for (int i = 0; i < data.length(); ++i) {
                JSONObject studyJSON = data.getJSONObject(i);
                StudySummary studySummary = new StudySummary();
                //s.setStudyName(studyJSON.getString("studyName"));//Brapi v1.3
                studySummary.setStudyName(studyJSON.getString("name"));//Brapi v1.2
                studySummary.setStudyDbId(studyJSON.getString("studyDbId"));
                studies.add(studySummary);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return studies;
    }

    private List<TraitObject> parseTraitsJson(String json) {
        List<TraitObject> traits = new ArrayList<>();

        try {
            JSONObject js = new JSONObject(json);
            JSONObject result = (JSONObject) js.get("result");
            JSONArray data = (JSONArray) result.get("data");
            for (int i = 0; i < data.length(); ++i) {
                JSONObject tmp = data.getJSONObject(i);
                TraitObject t = new TraitObject();
                t.defaultValue = tmp.getString("defaultValue");
                JSONObject traitJson = tmp.getJSONObject("trait");
                t.trait = traitJson.getString("name");
                t.details = traitJson.getString("description");
                JSONObject scale = tmp.getJSONObject("scale");

                JSONObject validValue = scale.getJSONObject("validValues");
                //TODO: Add integer parsing to get min and max as integers
                // Requires changes to breedbase as well
                t.minimum = validValue.getString("min");
                t.maximum = validValue.getString("max");
                JSONArray cat = validValue.getJSONArray("categories");
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < cat.length(); ++j) {
                    sb.append(cat.get(j));
                    if (j != cat.length() - 1) {
                        sb.append("/");
                    }
                }
                t.categories = sb.toString();
                //TODO: datatype field should be dataType. Breedbase needs to be fixed.
                t.format = convertBrAPIDataType(scale.getString("datatype"));
                if (t.format.equals("integer")) {
                    t.format = "numeric";
                }

                // Get database id of external system to sync to enabled pushing through brAPI
                t.external_db_id = tmp.getString("observationVariableDbId");

                // Need to set where we are getting the data from so we don't push to a different
                // external link than where the trait was retrieved from.
                Integer url_path_start = this.brapiBaseURL.indexOf("/brapi", 0);
                t.trait_data_source = this.brapiBaseURL.substring(0, url_path_start);

                t.visible = true;
                traits.add(t);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return traits;
    }

    private String convertBrAPIDataType(String dataType) {
        switch (dataType){
            case "Code":
            case "Nominal":
                return "categorical";
            case "Date":
                return "date";
            case "Numerical":
            case "Ordinal":
            case "Duration":
                return "numeric";
            case "Text":
            default:
                return "text";
        }
    }

    private void parsePlotJson(String json, StudyDetails studyDetails) {

        try {
            JSONObject js = new JSONObject(json);
            JSONObject result = (JSONObject) js.get("result");
            JSONArray data = (JSONArray) result.get("data");

            studyDetails.setNumberOfPlots(data.length());


            JSONObject first = (JSONObject) data.get(0);
            Iterator<String> firstIter = first.keys();
            while (firstIter.hasNext()) {
                String key = firstIter.next();
                if (!ignoreBrAPIAttribute(key)) {
                    studyDetails.getAttributes().add(key);
                }
            }
            Collections.sort(studyDetails.getAttributes());

            for (int i = 0; i < data.length(); ++i) {
                JSONObject unit = (JSONObject) data.get(i);
                List<String> dataRow = new ArrayList<>();
                for(String key: studyDetails.getAttributes()){
                    if(unit.has(key)){
                        dataRow.add(unit.getString(key));
                    }else{
                        dataRow.add("");
                    }
                }
                studyDetails.getValues().add(dataRow);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean ignoreBrAPIAttribute(String key) {
        List<String> ignoredBrAPIAttributes = new ArrayList<>();
        ignoredBrAPIAttributes.add("trialDbId");
        ignoredBrAPIAttributes.add("trialName");
        ignoredBrAPIAttributes.add("treatments");
        ignoredBrAPIAttributes.add("studyName");
        ignoredBrAPIAttributes.add("studyDbId");
        ignoredBrAPIAttributes.add("studyLocation");
        ignoredBrAPIAttributes.add("studyLocationDbId");
        ignoredBrAPIAttributes.add("programName");
        ignoredBrAPIAttributes.add("programDbId");
        ignoredBrAPIAttributes.add("observations");
        ignoredBrAPIAttributes.add("observationUnitXref");
        ignoredBrAPIAttributes.add("locationName");
        ignoredBrAPIAttributes.add("locationDbId");
        return ignoredBrAPIAttributes.contains(key);
    }


    private StudyDetails parseStudyDetailsJson(String response) {

        StudyDetails studyDetails = new StudyDetails();
        try {
            JSONObject js = new JSONObject(response);
            JSONObject result = (JSONObject) js.get("result");
            if(result.has("studyDbId"))
                studyDetails.setStudyDbId(result.getString("studyDbId"));
            if(result.has("name"))
                studyDetails.setStudyName(result.getString("name")); // BrAPI v1.2
            if(result.has("studyName"))
                studyDetails.setStudyName(result.getString("studyName")); // BrAPI v1.3
            if(result.has("studyDescription"))
                studyDetails.setStudyDescription(result.getString("studyDescription"));
            if(result.has("locationName"))
                studyDetails.setStudyLocation(result.getString("locationName")); // BrAPI v1.2
            if(result.has("location")){
                JSONObject location = result.getJSONObject("location");
                if(location.has("name"))
                    studyDetails.setStudyLocation(location.getString("name")); // BrAPI v1.2
                if(location.has("locationName"))
                    studyDetails.setStudyLocation(location.getString("locationName")); // BrAPI v1.3
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return studyDetails;
    }

    public void saveStudyDetails(StudyDetails studyDetails) {
        FieldObject field = new FieldObject();
        field.setExp_name(studyDetails.getStudyName());
        field.setExp_alias(studyDetails.getStudyDbId()); //hack for now to get in table alias not used for anything
        field.setExp_species(studyDetails.getCommonCropName());
        field.setCount(studyDetails.getNumberOfPlots().toString());
        field.setUnique_id("observationUnitDbId");
        field.setPrimary_id("X");
        field.setSecondary_id("Y");
        field.setExp_sort("plotNumber");
        int expId = dataHelper.createField(field, studyDetails.getAttributes());

        for(List<String> dataRow: studyDetails.getValues()) {
            dataHelper.createFieldData(expId, studyDetails.getAttributes(), dataRow);
        }

        // The traits are now retrieved from a different avenue.
        /*for(TraitObject t : studyDetails.getTraits()){
            dataHelper.insertTraits(t);
        }*/
    }

}
