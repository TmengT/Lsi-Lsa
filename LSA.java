package com.qzsoft.nlputils;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

import java.io.*;
import java.util.*;
import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

/**
 * @@author  yuwc
 * @@date 2021-09-17
 * TODO Java版LSA 潜在语义算法
 */
public class LSA implements Serializable{
    private String stop_path="";
    private List<String> stopwords;
    private List<String> docs;//注意这里输入的文档是分词之后的
    private List<String> doc_keyinfo;//  文档的关键信息  一般输入标题
    private Matrix matrix;
    private Matrix m_svd;
    private Matrix u_p;
    private Map<String, List<Integer>> dictionary = new HashMap<String, List<Integer>>();
    private List<String> keywords = new ArrayList<String>();
    private SingularValueDecomposition svd;
    // 维数
    private static int LSD = 200;
    Map<Integer, Map<String, Double>> doc_word_tf = new HashMap<>();

    public LSA(List<String> docs, List<String>doc_keyinfo,String stop_path) {
        this.docs = docs;
        this.doc_keyinfo = doc_keyinfo;

        if (this.LSD >=docs.size()){
            this.LSD = docs.size();
        }
        this.stop_path = stop_path;
    }

    public void train() {
        // 读取停用词
        readStopwords();
        // 过滤停用词
        removeStopwords();
        // 生成单词字典
        createDictionary();
        // 得到关键词
        addKeywords();
        computeTFIDF();
        // 生成单词-文档矩阵
        createMatrix();
        // SVD分解，降维后的矩阵
        m_svd = SVD();
    }

    /**
     * 读取停用词
     */
    public  List<String> readStopwords() {

        this.stopwords = new ArrayList<String>();
        // 读取停用词表
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(this.stop_path), "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                this.stopwords.add(line);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this.stopwords;
    }

    /**
     * 过滤停用词
     */
    public void removeStopwords() {
        for (int i = 0; i < docs.size(); i++) {
            String[] doc = docs.get(i).split(" ");
            List<String> words = new ArrayList<String>();
            for (String string : doc) {
                words.add(string);
            }
            words.removeAll(stopwords);
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < words.size(); j++) {
                sb.append(words.get(j));
                sb.append(" ");
                sb.toString();
            }
            docs.set(i, sb.toString().trim());
        }
    }

    /**
     * 记录每个单词出现在哪些文档中
     */
    public void createDictionary() {
        for (int i = 0; i < docs.size(); i++) {
            String[] words = docs.get(i).split("\t");

            for (String word : words) {
                if (dictionary.containsKey(word)) {
                    dictionary.get(word).add(i);
                } else {
                    List<Integer> idList = new ArrayList<Integer>();
                    idList.add(i);
                    dictionary.put(word, idList);
                }
            }
        }
    }

    /**
     * 得到关键词列表
     */
    public void addKeywords() {
        for (String word : dictionary.keySet()) {
            if (dictionary.get(word).size() >= 1) {
                keywords.add(word);
            }
        }
    }

    /**
     * 生成单词-文档矩阵
     */
    public void createMatrix() {
        double array[][] = new double[keywords.size()][docs.size()];
        matrix = new Matrix(array);
        for (int i = 0; i < keywords.size(); i++) {
            for (int j = 0; j < docs.size(); j++) {
                if (doc_word_tf.get(j).containsKey( keywords.get(i))){
                    matrix.set(i, j, doc_word_tf.get(j).get(keywords.get(i)));
                }
                else{
                    matrix.set(i, j, 0.0);
                }
            }
        }
    }

    /**
     * 打印矩阵
     *
     * @param matrix
     */
    public void printMatrix(Matrix matrix) {
        for (int i = 0; i < matrix.getRowDimension(); i++) {
            for (int j = 0; j < matrix.getColumnDimension(); j++) {
                System.out.printf("m(%d,%d) = %g\t", i, j, matrix.get(i, j));
            }
            System.out.printf("\n");
        }
    }

    /**
     * SVD分解，降维
     */
    public Matrix SVD() {
        svd = matrix.svd();
        // 注意，这里是v的转置
        u_p = new Matrix(svd.getU().getRowDimension(),LSD);
        for (int i = 0; i < svd.getU().getRowDimension(); i++) {
            for (int j = 0; j < LSD; j++) {
                u_p.set(i, j,(Double)svd.getU().get(i, j));
            }
        }
//        Matrix v_p = new Matrix(LSD,svd.getV().getColumnDimension());
//        for (int i = 0; i < LSD; i++) {
//            for (int j = 0; j < svd.getV().getColumnDimension(); j++) {
//                v_p.set(i, j,(Double)svd.getV().get(i, j));
//            }
//        }
//
//        Matrix s_p = new Matrix(LSD,LSD);
//        for (int i = 0; i < LSD; i++) {
//            for (int j = 0; j < LSD; j++) {
//                s_p.set(i, j,(Double)svd.getS().get(i, j));
//            }
//        }
        return matrix.transpose().times(u_p);
    }

    /**
     * 计算夹角余弦值
     *
     * @param v1
     * @param v2
     */
    public double cos(double[] v1, double[] v2) {
        // Cos(theta) = A(dot)B / |A||B|
        double a_dot_b = 0;
        int  dim = v1.length;
        for (int i = 0; i < dim; i++) {
            a_dot_b += v1[i] * v2[i];
        }
        double A = 0;
        for (int j = 0; j < dim; j++) {
            A += v1[j] * v1[j];
        }
        A = Math.sqrt(A);
        double B = 0;
        for (int k = 0; k < dim; k++) {
            B += v2[k] * v2[k];
        }
        B = Math.sqrt(B);
        return a_dot_b / (A * B);
    }
    /*
     * 文档转换成词袋
     */
    public double[] doc2bow (List<String> terms){
        double array[][] = new double[1][keywords.size()];
        Matrix bow = new Matrix(array);
        Map<String, Double> w_count = new HashMap<>();
        for(String term:terms){
            if(w_count.containsKey(term)){
                w_count.put(term, w_count.get(term)+1.0);
            }
            else{
                w_count.put(term, 1.0);
            }
        }

        for (int  j=0 ; j<keywords.size(); j++) {
            String w = keywords.get(j);
            if(terms.contains(w)){
                Double idf = log((double) docs.size() / (dictionary.get(w).size() + 1));
                bow.set(0, j, (w_count.get(w)/terms.size())*idf);
            }
            else{
                bow.set(0, j, 0.0);
            }
        }
        Matrix res = bow.times(u_p);
        return res.getRowPackedCopy();
    }

    /*
     * 计算 词的TFIDF值
     */
    public void computeTFIDF() {
        // scan files
        for (int i = 0; i < docs.size(); i++) {
            // TF = 候选词出现次数/总词数
            Map<String, Double> w_count = new HashMap<>();

            Map<String, Double> tfMap = new HashMap<>();
            String[] words = docs.get(i).split("\t");
            // 计算单个tf
            for(String w:words){
                if(w_count.containsKey(w)){
                    w_count.put(w, w_count.get(w)+1.0);
                }
                else{
                    w_count.put(w, 1.0);
                }
                Double idf = log((double) docs.size() / (dictionary.get(w).size() + 1));
                tfMap.put(w, (w_count.get(w)/words.length)*idf);
            }
            doc_word_tf.put(i,tfMap);
        }

    }
    /*
     *  log10 函数
     */
    public double log(double value) {
        return (double) (Math.log10(value));
    }

    /*
     * 计算输入文本与其他文本的相似度
     */
    public  List<Map<String,String>>  similarity(String in ,double threshold){
        List<Map<String,String>> sim_docs = new ArrayList<>();
        List<Term> result = HanLP.segment(in);
        List<String> words = new ArrayList<>();
        for(Term t :result) {
            words.add(t.word);
        }
        double[] bow = doc2bow(words);
        double[][] docsArray = m_svd.getArray();
        for(int i=0; i<m_svd.getRowDimension();i++){
            double sim = cos(bow,docsArray[i]);
            if (sim>=threshold){
                Map<String,String> doc_sim = new HashMap<>();
                doc_sim.put("doc_id",String.valueOf(i));
                doc_sim.put("similarity",String.valueOf(sim));
                sim_docs.add(doc_sim);
            }
        }
        if (doc_keyinfo != null && doc_keyinfo.size() >0){
            double a = 0.2;
            for (Map<String,String> sim_doc : sim_docs){
                double edit = similarity(in, doc_keyinfo.get(Integer.valueOf(sim_doc.get("doc_id"))));
                sim_doc.put("similarity", String.valueOf((1-a)*edit+a*Double.valueOf(sim_doc.get("similarity"))));
            }
        }

        // 根据相似度排序
        Collections.sort(sim_docs, new Comparator<Map<String,String>>(){
            @Override
            public int compare (Map<String,String>u1, Map<String,String> u2){
                Double sim1 =  Double.valueOf(u1.get("similarity"));
                Double sim2 =  Double.valueOf(u2.get("similarity"));
                //升序
                //return name1.compareTo(name2);
                //降序
                return sim2.compareTo(sim1);
            }
        });
        return sim_docs;
    }


    /*
     *  两个字符串的编辑距离
     */
    public static int ld(String s, String t) {
        int d[][];
        int sLen = s.length();
        int tLen = t.length();
        int si;
        int ti;
        char ch1;
        char ch2;
        int cost;
        if(sLen == 0) {
            return tLen;
        }
        if(tLen == 0) {
            return sLen;
        }
        d = new int[sLen+1][tLen+1];
        for(si=0; si<=sLen; si++) {
            d[si][0] = si;
        }
        for(ti=0; ti<=tLen; ti++) {
            d[0][ti] = ti;
        }
        for(si=1; si<=sLen; si++) {
            ch1 = s.charAt(si-1);
            for(ti=1; ti<=tLen; ti++) {
                ch2 = t.charAt(ti-1);
                if(ch1 == ch2) {
                    cost = 0;
                } else {
                    cost = 1;
                }
                d[si][ti] = Math.min(Math.min(d[si-1][ti]+1, d[si][ti-1]+1),d[si-1][ti-1]+cost);
            }
        }
        return d[sLen][tLen];
    }

    /*
     * 两个字符串的编辑的相似度
     */
    public static double similarity(String src, String tar) {
        int ld = ld(src, tar);
        return 1 - (double) ld / Math.max(src.length(), tar.length());
    }

    /*
      * 序列化保存LSA model
     */
    public void save(String path){
        if (this != null){
            //创建一个ObjectOutputStream输出流
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
                //将对象序列化到文件s
                oos.writeObject(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * 反序列化LSA model 获取LSA模型
     */
    public static LSA load(String Path){
        //创建一个ObjectInputStream输入流
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(Path))) {
           LSA lsa = (LSA) ois.readObject();
           return lsa;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

