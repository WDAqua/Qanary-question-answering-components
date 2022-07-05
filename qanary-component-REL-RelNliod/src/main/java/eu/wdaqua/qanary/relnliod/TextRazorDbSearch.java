package eu.wdaqua.qanary.relnliod;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

public class TextRazorDbSearch {

	private ArrayList<String> sentencesWord = new ArrayList<String>();
	private ArrayList<String> propertyList = new ArrayList<String>();
	private HashSet<String> dbLinkListSet = new HashSet<String>();
	private boolean  relationsFlag = false;
	private final DbpediaRecordProperty dbpediaRecordProperty;
	private final RemovalList removalList;

	public TextRazorDbSearch(final DbpediaRecordProperty dbpediaRecordProperty, RemovalList removalList) {
		this.dbpediaRecordProperty = dbpediaRecordProperty;
		this.removalList = removalList;
	}


	public ArrayList<String> createArrayWordsList(JSONArray jsonArraySent) {

		if (jsonArraySent.length() != 0) {
			for (int i = 0; i < jsonArraySent.length(); i++) {
				JSONArray jsonArrayWords = jsonArraySent.getJSONObject(i).getJSONArray("words");
				for (int j = 0; j < jsonArrayWords.length(); j++) {
					sentencesWord.add(jsonArrayWords.getJSONObject(j).getString("token"));
				}
			}
		} else {

			System.out.println("createArrayWordsList, Error: No Sentence to parse");
		}

		return sentencesWord;
	}

	public String getWordFromList(int i) {
		return sentencesWord.get(i);
	}
	
	public HashSet<String> getDbLinkListSet() {
		return dbLinkListSet;
	}
   
	public boolean getRelationsFlag() {
		return relationsFlag;
	}
	public void createPropertyList(JSONObject response1) {
        try {
		if (response1.has("relations")) {
			relationsFlag = true;
			JSONArray jsonArray = (JSONArray) response1.get("relations");
			System.out.println(":createPropertyListRelations: "+jsonArray.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray wordPos = jsonArray.getJSONObject(i).getJSONArray("wordPositions");
				System.out.println("createPropertyList:wordPos: "+wordPos.toString());
				
				String str = "" ;
				for(int j = 0; j < wordPos.length(); j++) {
					str +=" "+getWordFromList(wordPos.getInt(j));
					
				}
				System.out.println("createPropertyList:propertyList "+i+" "+str);
				propertyList.add(str);
			}

		} else if (response1.has("properties")) {
			JSONArray jsonArray = (JSONArray) response1.get("properties");
			System.out.println("createPropertyList:Properties: "+jsonArray.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray wordPos = jsonArray.getJSONObject(i).getJSONArray("wordPositions");
				System.out.println("createPropertyList:wordPos: "+wordPos.toString());
				String str ="" ;
				for(int j = 0; j < wordPos.length(); j++){
					
					str = str+" "+ getWordFromList(wordPos.getInt(j));
					
				}
				System.out.println("createPropertyList:propertyList "+i+" "+str);
				propertyList.add(str);
				
			}
		}}
        catch(Exception e) {
        	System.out.println("createPropertyList:createPropertyList"+e);
        }
		
		System.out.println("createPropertyList:"+propertyList.toString());

	}
	
	public void createRePropertyList(JSONObject response1) {
		propertyList.clear();
		try {
		if (response1.has("properties")) {
			JSONArray jsonArray = (JSONArray) response1.get("properties");
			System.out.println("createRePropertyList:Properties: "+jsonArray.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				JSONArray wordPos = jsonArray.getJSONObject(i).getJSONArray("wordPositions");
				System.out.println("createRePropertyList:wordPos: "+wordPos.toString());
				String str = "";
				for(int j = 0; j < wordPos.length(); j++){
					
					str = str+" "+ getWordFromList(wordPos.getInt(j));
				}
				System.out.println("Error:createRePropertyList:propertyList "+i+" "+str);
				propertyList.add(str);
				
			}
		}
		
		else {
			
			System.out.println("Error:createRePropertyList No propertyList");
		}
	}
	catch(Exception e) {
    	System.out.println("createRePropertyList "+e);
    }
		System.out.println("createPropertyList: "+propertyList.toString());
	}

	public String searchDbLinkInTTL(String myKey) {
		
		String dbpediaProperty = null;
		try {
		String myKey1 = myKey.trim();
		if(myKey1!=null && !myKey1.equals("")) {
		System.out.println("searchDbLinkInTTL: "+myKey1);
			for (Entry<String, String> e : dbpediaRecordProperty.get().tailMap(myKey1).entrySet()) {
				    if(e.getKey().contains(myKey1)) 
				    {
				     	dbpediaProperty = e.getValue();
				      	break;
				    }
					ArrayList<String> strArrayList = new ArrayList<String>(Arrays.asList(e.getKey().split("\\s+")));
					//System.out.println(strArrayList.toString());
					for (String s : strArrayList)
					{
					    if(myKey1.compareTo(s) == 0) {
					    	dbpediaProperty = e.getValue();
					 }
					}
					 
					 if(dbpediaProperty!=null)
					 break;
					    
					 }
         
		}
		} catch (Exception e) {
			// logger.info("Except: {}", e);
			// TODO Auto-generated catch block
		}
		System.out.println("searchDbLinkInTTL: "+dbpediaProperty);
		return dbpediaProperty;
	}

	public String RemoveSubstring(String str) {
		String listString = null;
		//System.out.println("RemoveSubstring Start: "+ str);
		ArrayList<String> strArrayList = new ArrayList<String>(Arrays.asList(str.split("\\s+")));
		for (String s : removalList.getFilteredWordList()) {
			strArrayList.remove(s);
		}
		
		if(!strArrayList.isEmpty()) {
		for (String s : strArrayList)
		{   
			if(listString!=null) {
		    listString += s + " ";
		    }
		else {
			listString = s+ " ";
		}
		    
		}
		}

		System.out.println("RemoveSubstring End: "+listString.trim());
		return listString.trim();
	}

	public void createDbLinkListSet(ArrayList<String> arrayListWords) {
		
		if (!arrayListWords.isEmpty()) {

			if (!propertyList.isEmpty()) {

				for (int i = 0; i < propertyList.size(); i++) {
					String str = searchDbLinkInTTL(propertyList.get(i));
					if ( str!= null) 
						dbLinkListSet.add(str);

					else {
						String str1 = searchDbLinkInTTL(RemoveSubstring(propertyList.get(i)));
                         	if(str1!= null)
							dbLinkListSet.add(str1);
					 }
				}
			}
			else {
				System.out.print("createDbLinkListSet:Info: No property in sentences");
			}

		} else {
			System.out.print("createDbLinkListSet:Error: No words in sentences");
		}
		
	}
}