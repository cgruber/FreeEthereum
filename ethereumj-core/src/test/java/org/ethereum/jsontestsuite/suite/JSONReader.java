package org.ethereum.jsontestsuite.suite;

import org.apache.commons.codec.binary.Base64;
import org.ethereum.config.SystemProperties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class JSONReader {

    private static final Logger logger = LoggerFactory.getLogger("TCK-Test");

    public static String loadJSON(String filename) {
        String json = "";
        if (!SystemProperties.getDefault().vmTestLoadLocal())
            json = getFromUrl("https://raw.githubusercontent.com/ethereum/tests/develop/" + filename);
        return json.isEmpty() ? getFromLocal(filename) : json;
    }

    public static String loadJSONFromCommit(String filename, String shacommit) {
        String json = "";
        if (!SystemProperties.getDefault().vmTestLoadLocal())
            json = getFromUrl("https://raw.githubusercontent.com/ethereum/tests/" + shacommit + "/" + filename);
        if (!json.isEmpty()) json = json.replaceAll("//", "data");
        return json.isEmpty() ? getFromLocal(filename) : json;
    }

    public static String getFromLocal(String filename) {
        System.out.println("Loading local file: " + filename);
        try {
            File vmTestFile = new File(filename);
            if (!vmTestFile.exists()){
                System.out.println(" Error: no file: " +filename);
                System.exit(1);
            }
            return new String(Files.readAllBytes(vmTestFile.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    public static String getFromUrl(String urlToRead) {
        URL url;
        HttpURLConnection conn;
        BufferedReader rd;
        StringBuilder result = new StringBuilder();
        String line;
        try {
            url = new URL(urlToRead);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();
            InputStream in = conn.getInputStream();
            rd = new BufferedReader(new InputStreamReader(in), 819200);

            logger.info("Loading remote file: " + urlToRead);
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            rd.close();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return result.toString();
    }

    public static String getTestBlobForTreeSha(String shacommit, String testcase){

        String result = getFromUrl("https://api.github.com/repos/ethereum/tests/git/trees/" + shacommit);

        JSONParser parser = new JSONParser();
        JSONObject testSuiteObj = null;

        List<String> fileNames = new ArrayList<>();
        try {
            testSuiteObj = (JSONObject) parser.parse(result);
            JSONArray tree = (JSONArray)testSuiteObj.get("tree");

            for (Object oEntry : tree) {
                JSONObject entry = (JSONObject) oEntry;
                String testName = (String) entry.get("path");
                if ( testName.equals(testcase) ) {
                    String blobresult = getFromUrl( (String) entry.get("url") );

                    testSuiteObj = (JSONObject) parser.parse(blobresult);
                    String blob  = (String) testSuiteObj.get("content");
                    byte[] valueDecoded= Base64.decodeBase64(blob.getBytes() );
                    //System.out.println("Decoded value is " + new String(valueDecoded));
                    return new String(valueDecoded);
                }
            }
        } catch (ParseException e) {e.printStackTrace();}

        return "";
    }

    public static List<String> getFileNamesForTreeSha(String sha){

        String result = getFromUrl("https://api.github.com/repos/ethereum/tests/git/trees/" + sha + "?recursive=1");

        JSONParser parser = new JSONParser();
        JSONObject testSuiteObj = null;

        List<String> fileNames = new ArrayList<>();
        try {
            testSuiteObj = (JSONObject) parser.parse(result);
            JSONArray tree = (JSONArray)testSuiteObj.get("tree");

            for (Object oEntry : tree) {
                JSONObject entry = (JSONObject) oEntry;
                String testName = (String) entry.get("path");
                fileNames.add(testName);
            }
        } catch (ParseException e) {e.printStackTrace();}

        return fileNames;
    }
}
