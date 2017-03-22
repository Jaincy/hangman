package com.company;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jsoup.helper.StringUtil;
import org.junit.Test;
import com.mysql.jdbc.Driver;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Created by Jain on 2017/3/8.
 */
public class Hangman {
//    String filePath = "C:\\Users\\Jain\\Documents\\4万5英语单词词库\\4万5单词词库A.txt";
    String filePath = "/home/hadoop/test/4wan.txt";

    Set<String> wordSet = new CopyOnWriteArraySet<String>();

    public static void main(String[] args) {

        Hangman hangman = new Hangman();
        hangman.guess();
    }

    @Test
    public void guess() {

        String url = "https://strikingly-hangman.herokuapp.com/game/on";
        wordSet = wordsToSet();
        int n = 0;
        //start 获取sessionId
        String startParams = "{\"playerId\":\"609447971@qq.com\", \"action\":\"startGame\"}";
        String ten = "EIARTONSLCUPMDHGYBFVWKZXQJ";


        String result = sendPost(url, startParams);
        JsonObject returnData = new JsonParser().parse(result).getAsJsonObject();
        String sessionId = returnData.get("sessionId").getAsString();

        //nextword
        String newWordParams;
        Set<String> wordSetUse = new CopyOnWriteArraySet<String>();

        String returnWord;

        String maxLetter;
        String guessParams;
        String wrongCount;
        int score;
        String submintParams;

        for (int i = 0; i < 80; i++) {
            wordSetUse.clear();
            wordSetUse.addAll(wordSet);


            newWordParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"nextWord\"}";
            result = sendPost(url, newWordParams);
            returnData = new JsonParser().parse(result).getAsJsonObject();
            System.out.println(returnData);
            returnWord = returnData.get("data").getAsJsonObject().get("word").getAsString();

            //guess

            Set<String> noLetters = new HashSet<String>();
            String ten2 = ten;

            while (true) {
                //剩余词库中出现几率最大的字母
                maxLetter = getNextLetter(wordSetUse, returnWord, noLetters);//剩余单词出现频率最大字母
                ten2 = ten2.replace(maxLetter, "");

                if (StringUtil.isBlank(getNextLetter(wordSetUse, returnWord, noLetters))) {
                    maxLetter = String.valueOf(ten2.charAt(0));
                    ten2 = ten2.replace(maxLetter, "");

                }
                guessParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"guessWord\",\"guess\":\"" + maxLetter + "\"}";
                //发送请求，guess
                result = sendPost(url, guessParams);
                returnData = new JsonParser().parse(result).getAsJsonObject();

                System.out.println(returnData+"\t"+maxLetter);

                returnWord = returnData.get("data").getAsJsonObject().get("word").getAsString().trim();
                wrongCount = returnData.get("data").getAsJsonObject().get("wrongGuessCountOfCurrentWord").getAsString();
                if (!returnWord.contains(maxLetter)) {
                    noLetters.add(maxLetter);
                }
                if (!returnWord.contains("*")) {
                    n++;
                    System.out.println("正确答案就是" + returnWord +"\t"+ n);
                    if (wordSetUse.size()==0){
                        //将未在词库中出现的词加入词库
                        addToFile(filePath, returnWord);
                    }

                    break;
                }
                if (wrongCount.equals("10")) {
                    break;
                }

            }

        }
        //获取分数
        String resultParams = "{\"sessionId\": \"" + sessionId + "\",\"action\" : \"getResult\"}";
        result = sendPost(url, resultParams);
        returnData = new JsonParser().parse(result).getAsJsonObject();
        score=returnData.get("data").getAsJsonObject().get("score").getAsInt();
        if (score>=900){
            //提交结果
            submintParams="{\"sessionId\": \"" + sessionId + "\",\"action\" : \"submitResult\" }";
            result=sendPost(url, submintParams);
            returnData = new JsonParser().parse(result).getAsJsonObject();

        }
        System.out.println(returnData);


    }
//发送请求
    public static String sendPost(String url, String param) {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            URLConnection conn = realUrl.openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // 获取URLConnection对象对应的输出流
            out = new PrintWriter(conn.getOutputStream());
            // 发送请求参数
            out.print(param);
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            System.out.println("发送 POST 请求出现异常！" + e);
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result;
    }
//获取下一个所猜字母
    public String getNextLetter(Set<String> wordSetUse, String guessWord, Set<String> noLetters) {


        //去除非规定长度单词
        for (String str :
                wordSetUse) {

            if (str.length() != guessWord.length()) {
                wordSetUse.remove(str);
            }
        }

        //去除含非规定字母单词
        for (String str :
                wordSetUse) {
            for (String letter :
                    noLetters) {
                if (str.contains(letter)) {
                    wordSetUse.remove(str);
                }
            }

        }
        //匹配含规定字母及位置单词
        Set<String> letters = new HashSet<String>();
        for (int i = 0; i < guessWord.length(); i++) {
            if (!String.valueOf(guessWord.charAt(i)).equals("*")) {
                letters.add(String.valueOf(guessWord.charAt(i)));
                for (String str :
                        wordSetUse) {
                    if (!String.valueOf(guessWord.charAt(i)).equals(String.valueOf(str.charAt(i)))) {
                        wordSetUse.remove(str);
                    }
                }
            }
        }

        String maxLetter = getMaxLetter(wordSetUse, letters);


        return maxLetter;
    }
//得到词库中出现几率最大字母
    public String getMaxLetter(Set wordSetUse, Set<String> letters) {
        //集合转String
        String str = "";
        for (Object obj : wordSetUse) {
            str = obj.toString() + str;
        }
        for (String s :
                letters) {
            str = str.replace(s, "");
        }
//String转数组，求字母频率，加入map
        char[] chars = str.toCharArray();
        Map<Character, Integer> letterMap = new HashMap<Character, Integer>();
        for (char c : chars
                ) {
            if (!letterMap.containsKey(c)) {
                letterMap.put(c, 1);
            } else {
                letterMap.put(c, letterMap.get(c) + 1);
            }
        }
        //算出频率最大字母
        Iterator keys = letterMap.keySet().iterator();
        int maxV = -1;
        String maxK = "";
        while (keys.hasNext()) {
            Object key = keys.next();
            int value = Integer.parseInt(letterMap.get(key).toString());
            if (value > maxV) {
                maxV = value;
                maxK = key.toString();
            }
        }

        return maxK;

    }

//将词库加入set
    public Set wordsToSet() {
        try {
            String encoding = "utf-8";


            File file = new File(filePath);
            if (file.isFile() && file.exists()) { //判断文件是否存在
                InputStreamReader read = new InputStreamReader(
                        new FileInputStream(file), encoding);//考虑到编码格式
                BufferedReader bufferedReader = new BufferedReader(read);
                String lineTxt = null;
                while ((lineTxt = bufferedReader.readLine()) != null) {
                    //addUser(lineTxt, lineTxt.length());
                    wordSet.add(lineTxt.trim());


                }
                read.close();
            } else {
                System.out.println("找不到指定的文件");
            }
        } catch (Exception e) {
            System.out.println("读取文件内容出错"+filePath);

            e.printStackTrace();
        }

        return wordSet;
    }
//将set加入词库
    public static void addToFile(String file, String conent) {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file, true)));
            out.write(conent + "\r\n");
//            System.out.println("write-----" + conent);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




}

