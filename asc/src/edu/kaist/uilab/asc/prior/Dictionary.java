package edu.kaist.uilab.asc.prior;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.kaist.uilab.asc.stemmer.EnglishStemmer;
import edu.kaist.uilab.asc.stemmer.FrenchStemmer;

/**
 * Dictionary.
 * 
 * @author trung nguyen (trung.ngvan@gmail.com)
 */
public class Dictionary {
  static final String DIRECTORY = "C:/datasets/asc/dict";
  static final String EN = "en";
  static final String FR = "fr";
  static final String GOOGLE_TRANSLATE = "https://www.googleapis.com/language/translate/v2?"
      + "key=AIzaSyDK00YAqjrlyeJfQ4DRBD5av-fmX-AjI5M&q=";
  private static final String UTF8 = "utf-8";

  /**
   * Removes duplication in the file retrieved from the source used by James
   * Petterson.
   * 
   * @param file
   * @param newFile
   * @throws IOException
   */
  public static void removeDuplication(String file) throws IOException {
    HashMap<String, String> map = new HashMap<String, String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(file), UTF8));
    String line;
    StringTokenizer tokenizer;
    while ((line = in.readLine()) != null) {
      tokenizer = new StringTokenizer(line, "\t");
      if (tokenizer.countTokens() == 2) {
        map.put(tokenizer.nextToken(), tokenizer.nextToken());
      }
    }
    in.close();
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(file), UTF8));
    for (Map.Entry<String, String> entry : map.entrySet()) {
      out.printf("%s\t%s\n", entry.getKey(), entry.getValue());
    }
    out.close();
  }

  /**
   * Translates words from a source language to a target language.
   * 
   * @param srcLangFile
   *          the file that contains all words to be translated each on one line
   * @param srcLang
   *          the source language
   * @param targetLang
   *          the target language
   * @param outputFile
   *          output file to write the translations
   */
  public static void langToLang(String srcLangFile, String srcLang,
      String targetLang, String outputFile) throws IOException {
    BufferedReader in;
    HashSet<String> existingWords = getExistingWords(outputFile,
        EN.equals(srcLang));
    PrintWriter out = null;
    try {
      in = new BufferedReader(new InputStreamReader(new FileInputStream(
          srcLangFile), UTF8));
      out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(
          outputFile, true), UTF8));
      String word, translatedWord;
      HttpClient httpClient = new DefaultHttpClient();
      while ((word = in.readLine()) != null && !existingWords.contains(word)) {
        translatedWord = translatesWord(word, srcLang, targetLang, httpClient);
        if (translatedWord != null) {
          // always write english word first
          if (EN.equals(srcLang)) {
            out.printf("%s\t%s\n", word, translatedWord);
          } else {
            out.printf("%s\t%s\n", translatedWord, word);
          }
        }
      }
      httpClient.getConnectionManager().shutdown();
      in.close();
    } catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
    } finally {
      if (out != null) {
        out.close();
      }
    }
  }

  /**
   * Makes a dictionary for the pair of words.
   * <p>
   * New pair of words will be appended to the provided dictionary file.
   * 
   * @param enStemFile
   *          the stem file (english language) as produced by the parser. Each
   *          line of this file must contains two words separated by space, an
   *          original word and a stemmed word.
   * @param secondStemFile
   *          the stem file (target language) with same format as the
   *          <code>enStemFile</code>
   * @param secondLanguage
   *          language of the second stem file
   * @param dictFile
   *          the dictionary file.
   */
  public static void make(String enStemFile, String secondStemFile,
      String secondLanguage, String dictFile) throws IOException {
    BufferedReader in;
    PrintWriter out = new PrintWriter(new OutputStreamWriter(
        new FileOutputStream(dictFile, true), UTF8));
    EnglishStemmer enStemmer = new EnglishStemmer();
    FrenchStemmer frStemmer = new FrenchStemmer();
    // english stem file
    String line, word, translatedWord;
    HttpClient httpClient = new DefaultHttpClient();
    int pos;
    in = new BufferedReader(new InputStreamReader(new FileInputStream(
        enStemFile), UTF8));
    while ((line = in.readLine()) != null) {
      line = line.trim();
      pos = line.indexOf(" ");
      if (pos >= 0) {
        word = line.substring(0, pos);
        try {
          translatedWord = translatesWord(word, EN, secondLanguage, httpClient);
          if (translatedWord != null) {
            out.printf("%s\t%s\n", enStemmer.getStem(word),
                frStemmer.getStem(translatedWord));
            System.out.printf("%s\t%s\n", enStemmer.getStem(word),
                frStemmer.getStem(translatedWord));
          }
        } catch (Exception e) {
          // do nothing
        }
      }
    }
    in.close();

    // french stem file
    // TODO(trung): duplicated code
//    in = new BufferedReader(new InputStreamReader(new FileInputStream(
//        secondStemFile), UTF8));
//    while ((line = in.readLine()) != null) {
//      line = line.trim();
//      pos = line.indexOf(" ");
//      if (pos >= 0) {
//        word = line.substring(0, pos);
//        try {
//          translatedWord = translatesWord(word, secondLanguage, EN, httpClient);
//          if (translatedWord != null) {
//            out.printf("%s\t%s\n", enStemmer.getStem(translatedWord),
//                frStemmer.getStem(word));
//            System.out.printf("%s\t%s\n", enStemmer.getStem(translatedWord),
//                frStemmer.getStem(word));
//          }
//        } catch (Exception e) {
//          // do nothing
//        }
//      }
//    }
    httpClient.getConnectionManager().shutdown();
    in.close();
    out.close();
  }

  /**
   * Returns a map between words and their stems from the specified file.
   * 
   * @param stemFile
   * @return
   * @throws IOException
   */
  private static HashMap<String, String> getStemMap(String stemFile)
      throws IOException {
    HashMap<String, String> map = new HashMap<String, String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(stemFile), UTF8));
    String line;
    int pos;
    while ((line = in.readLine()) != null) {
      pos = line.indexOf(" ");
      map.put(line.substring(0, pos), line.substring(pos + 1));
    }
    in.close();
    return map;
  }

  /**
   * Gets the existing English words from the specified dictionary file.
   * <p>
   * The first word of each line in the file is considered the English word with
   * its translation follows.
   * 
   * @param dictFile
   *          the dictionary file
   * @param getEnglish
   *          true to get the English words, false to get the words in the other
   *          language
   * @return
   */
  private static HashSet<String> getExistingWords(String dictFile,
      boolean getEnglish) throws IOException {
    HashSet<String> set = new HashSet<String>();
    BufferedReader in = new BufferedReader(new InputStreamReader(
        new FileInputStream(dictFile), UTF8));
    String line;
    int tabPosition;
    String word;
    while ((line = in.readLine()) != null) {
      tabPosition = line.indexOf("\t");
      if (getEnglish) {
        word = line.substring(0, tabPosition);
      } else {
        word = line.substring(tabPosition + 1);
      }
      set.add(word);
    }
    in.close();
    return set;
  }

  /**
   * Translates the specified word from <code>src</code> language to
   * <code>target</code> language.
   * 
   * @param word
   * @param src
   * @param target
   * @param client
   * @return
   */
  private static String translatesWord(String word, String src, String target,
      HttpClient client) throws URISyntaxException {
    HttpGet httpget = new HttpGet(GOOGLE_TRANSLATE + word + "&source=" + src
        + "&target=" + target);
    ResponseHandler<String> responseHandler = new BasicResponseHandler();
    String translatedWord = null;
    try {
      String response = client.execute(httpget, responseHandler);
      int pos = response.indexOf("translatedText") + "translatedText".length()
          + 2;
      int start = response.indexOf("\"", pos);
      int end = response.indexOf("\"", start + 1);
      translatedWord = response.substring(start + 1, end);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    if (translatedWord != null && !translatedWord.contains(" ")) {
      return translatedWord;
    }
    return null;
  }

  /**
   * Gets words in other languages from the results file produced by document
   * parser.
   */
  private static void getWordsInOtherLanguages() throws IOException {
    BufferedReader en = new BufferedReader(new InputStreamReader(
        new FileInputStream("C:/datasets/asc/reviews/WordList_en.txt"), UTF8));
    String line;
    HashSet<String> enWords = new HashSet<String>();
    while ((line = en.readLine()) != null) {
      enWords.add(line);
    }
    en.close();
    BufferedReader all = new BufferedReader(new InputStreamReader(
        new FileInputStream("C:/datasets/asc/reviews/WordList.txt"), UTF8));
    PrintWriter out = new PrintWriter(
        new OutputStreamWriter(new FileOutputStream(
            "C:/datasets/asc/reviews/WordList_other.txt", true), UTF8));
    while ((line = all.readLine()) != null) {
      if (!enWords.contains(line)) {
        out.println(line);
      }
    }
    all.close();
    out.close();
  }

  public static void main(String args[]) throws Exception {
    // Dictionary.langToLang("C:/datasets/asc/reviews/WordList_en.txt", EN, FR,
    // DIRECTORY + "/en-fr.txt");
    // getWordsInOtherLanguages();
    // Dictionary.langToLang("C:/datasets/asc/reviews/WordList_other.txt", FR,
    // EN,
    // DIRECTORY + "/en-fr.txt");
//    Dictionary.make("C:/datasets/asc/reviews/ElectronicsReviews1/Stem_en.txt",
//        "C:/datasets/asc/reviews/ElectronicsReviews1/Stem_fr.txt", FR,
//        DIRECTORY + "/en-fr2.txt");
    Dictionary.removeDuplication(DIRECTORY + "/en-fr.txt");
  }
}