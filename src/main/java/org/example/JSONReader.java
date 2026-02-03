package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class JSONReader {
    private final Gson gson = new Gson();

    private String ReadJSONFromFile(String filepath){
        StringBuilder jsonText= new StringBuilder();
        try{
            // Read the JSON file line by line and convert it to a String
            BufferedReader bufferedReader= new BufferedReader(new FileReader(filepath));
            String line;
            while((line = bufferedReader.readLine()) != null){
                jsonText.append(line).append("\n");
            }
            bufferedReader.close();

        } catch (Exception e){
            throw new RuntimeException(e);
        }

        return jsonText.toString();
    }

    public List<WasteSite> GetList (String filepath){

        String jsonlist = ReadJSONFromFile(filepath);
        List<WasteSite> wasteSites = new ArrayList<>();

        if(jsonlist != null && !jsonlist.isEmpty()){
            Type listType = new TypeToken<List<WasteSite>>(){}.getType(); //Create a Type to use with gson library
            try {
                //Create an ArrayList of WasteSite Objects out of the JSON that was stored in the String
                wasteSites=gson.fromJson(jsonlist, listType);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return wasteSites;
    }
}
