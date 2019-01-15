package com.ryannm.android.ankimate;

import android.text.TextUtils;

import com.ryannm.android.ankimate.Dao.Configuration;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class ENMLhelper {

    public static HashMap<Integer, ArrayList<String>> getBulletLists(Document doc) {
        HashMap<Integer,ArrayList<String>> lists = new HashMap<>();
        Elements uls = doc.select("ul");
        for (int i = 0 ; i < uls.size(); i++) {
            Element ul = uls.get(i);
            lists.put(i, getBulletArrayList(ul));
        }
        return lists;
    }

    private static ArrayList<String> getBulletArrayList(Element ul) {
        Elements listItems = ul.getElementsByTag("li");
        ArrayList<String> bullets = new ArrayList<>();
        for (Element listItem: listItems) {
            if (listItem.hasText()) {
                bullets.add(listItem.text());
            }
        }
        return bullets;
    }

    // Should always return (callback) in the order Q & A, even if written in evernote in opp. order
    public static void getFieldLists(Document doc, Configuration configuration, String colonEquivalent, Set blackFieldSet, ENMLFieldListCallbacks callbacks ) {
        List<String> fieldKeywords = ConfigurationLab.split(configuration.getFieldKeywords());
      //  Set< List<String>> set = new HashSet<>();
        List<String[]> list = new ArrayList<>();
      //  ArrayList<String> contents = new ArrayList<>();
        String[] contentsArray = new String[fieldKeywords.size()];
        boolean isEmpty = false;
        //int elementBlocksNum = doc.getElementsMatchingText("(?i)^" + fieldKeywords.get(0) + "\\s?"+ colonEquivalent).tagName("div").size();
        Elements blocks = doc.select("div:not(:has(div)):matches((?i)^" + fieldKeywords.get(0) + "\\s?"+ colonEquivalent +")");
        int elementBlocksNum = blocks.size();

        List<Elements> currentFieldElements = new ArrayList<>();
        for (int j = 0; j < fieldKeywords.size(); j++) {
            currentFieldElements.add(doc.select("div:not(:has(div)):matches((?i)^" + fieldKeywords.get(j) + "\\s?"+ colonEquivalent +")"));
        }

        for (int i = 0; i < elementBlocksNum; i++) {

            if (blackFieldSet == null || !blackFieldSet.contains(blocks.get(i).text())) {

                for (int j = 0; j < fieldKeywords.size(); j++) {
                    if (currentFieldElements.get(j).get(i) == null) {
                        isEmpty = true;

                        break;
                    }
                    String fieldContent;
                    try {
                        fieldContent = currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?" + colonEquivalent + "\\s?")[1];
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        fieldContent = "";
                    }
                    if (TextUtils.isEmpty(fieldContent.trim())) { fieldContent = ""; }
                    contentsArray[j] = fieldContent;
                    // contents.add(currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?"+ colonEquivalent + "\\s?")[1]);
                }

                list.add(contentsArray.clone()); // As contentArray itself keeps changing

                if (!isEmpty) {
                    //   AnkiHelper.get(context).save(configuration.getDeckId() , configuration.getModelId(), contents, ConfigurationLab.split(configuration.getTagsToSave() ));
                }
            }
        }

        if (isEmpty) { list.clear(); }


        callbacks.onParseComplete(list);
    }

    // Should always return in the order Q & A, even if written in evernote in opp. order
    public static List<String[]> returnFieldLists(Document doc, Configuration configuration, String colonEquivalent, Set blackFieldSet) {
        List<String> fieldKeywords = ConfigurationLab.split(configuration.getFieldKeywords());
        for (int i=0; i < fieldKeywords.size();i++) fieldKeywords.set(i, App.escapeSpecialChars(fieldKeywords.get(i)));
        //  Set< List<String>> set = new HashSet<>();
        List<String[]> list = new ArrayList<>();
        //  ArrayList<String> contents = new ArrayList<>();
        String[] contentsArray = new String[fieldKeywords.size()];
        boolean isEmpty = false;
        //int elementBlocksNum = doc.getElementsMatchingText("(?i)^" + fieldKeywords.get(0) + "\\s?"+ colonEquivalent).tagName("div").size();
        Elements blocks = doc.select("div:not(:has(div)):matches((?i)^" + fieldKeywords.get(0) + "\\s?"+ colonEquivalent +")");
        int elementBlocksNum = blocks.size();

        List<Elements> currentFieldElements = new ArrayList<>();
        for (int j = 0; j < fieldKeywords.size(); j++) {
            currentFieldElements.add(doc.select("div:not(:has(div)):matches((?i)^" + fieldKeywords.get(j) + "\\s?"+ colonEquivalent +")"));
        }

        for (int i = 0; i < elementBlocksNum; i++) {

            if (blackFieldSet == null || !blackFieldSet.contains(blocks.get(i).text())) {

                for (int j = 0; j < fieldKeywords.size(); j++) {
                    if (currentFieldElements.get(j).get(i) == null) {
                        isEmpty = true;

                        break;
                    }
                    String fieldContent;
                    try {
                        fieldContent = App.returnTrimmedString(currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?" + colonEquivalent + "\\s?")[1]);
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        fieldContent = "";
                    }
                    if (TextUtils.isEmpty(fieldContent.trim())) { fieldContent = ""; }
                    contentsArray[j] = fieldContent;
                    // contents.add(currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?"+ colonEquivalent + "\\s?")[1]);
                }

                list.add(contentsArray.clone()); // As contentArray itself keeps changing

                if (!isEmpty) {
                    //   AnkiHelper.get(context).save(configuration.getDeckId() , configuration.getModelId(), contents, ConfigurationLab.split(configuration.getTagsToSave() ));
                }
            }
        }

        if (isEmpty) { list.clear(); }

        return list;
    }

    public static List<List<String>> returnNotesToAdd(Document doc, Configuration configuration, String commentKey, String colonEquivalent, Set blackFieldSet) { // todo: watch over this method. This can be the only one adding those multi-fields(except if its f*ed up while displaying on NotesToAdd)
        List<String> fieldKeywords = ConfigurationLab.split(configuration.getFieldKeywords());
        //  Set< List<String>> set = new HashSet<>();
        List<List<String>> resultList = new ArrayList<>();
        //  ArrayList<String> contents = new ArrayList<>();
        ArrayList<String> contentsList = new ArrayList<>(fieldKeywords.size());
        boolean isEmpty = false;
        //int elementBlocksNum = doc.getElementsMatchingText("(?i)^" + fieldKeywords.get(0) + "\\s?"+ colonEquivalent).tagName("div").size();
        Elements blocks = doc.select("div:not(:has(div)):matches((?i)^(\\?\\s|\\?)"+fieldKeywords.get(0)+"\\s?:)");// Todo : something wrong here too
        int elementBlocksNum = blocks.size();
       // if ("x".matches("(?i)^(\\?\\s|\\?)(W|Q|C)\\s?:"));

        List<Elements> currentFieldElements = new ArrayList<>();
        for (int j = 0; j < fieldKeywords.size(); j++) {
            currentFieldElements.add(doc.select("div:not(:has(div)):matches((?i)^(\\?\\s|\\?)"+fieldKeywords.get(j)+"\\s?:)"));
        }

        for (int i = 0; i < elementBlocksNum; i++) {

            if (blackFieldSet == null || !blackFieldSet.contains(blocks.get(i).text())) {

                for (int j = 0; j < fieldKeywords.size(); j++) {
                    if (currentFieldElements.get(j).get(i) == null) {
                        isEmpty = true;

                        break;
                    }
                    String fieldContent;
                    try {
                        fieldContent = currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?" + colonEquivalent + "\\s?")[1];
                    } catch (IndexOutOfBoundsException e) {
                        e.printStackTrace();
                        fieldContent = "";
                    }
                    if (TextUtils.isEmpty(fieldContent.trim())) { fieldContent = ""; }
                    contentsList.add(fieldContent);
                    // contents.add(currentFieldElements.get(j).get(i).text().split("(?i)" + fieldKeywords.get(j) + "\\s?"+ colonEquivalent + "\\s?")[1]);
                }

                resultList.add((List<String>) contentsList.clone()); // As contentArray itself keeps changing

                if (!isEmpty) {
                    //   AnkiHelper.get(context).save(configuration.getDeckId() , configuration.getModelId(), contents, ConfigurationLab.split(configuration.getTagsToSave() ));
                }
            }
        }

        if (isEmpty) { resultList.clear(); }

        return resultList;
    }




    public interface ENMLFieldListCallbacks {
        void onParseComplete(List<String[]> lists);
    }

    public interface ENMLBlackListCallbacks {
        void onParseComplete(Set<String> blacklistFirstField, int noteAddedCards);
    }



}
