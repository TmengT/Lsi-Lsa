package com.tmt.nlp.lsa;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tmt.util.MyStaticValue;

import Jama.Matrix;
/*
 * author yuwc 
 * date 2015.7.2
 */
public class CreateMatrix {
	public Map<String, Integer> wordList = new HashMap<String, Integer>();
	public Map<String, Integer> docList  = new HashMap<String, Integer>();
	public Map<Integer,Integer> wordCount = new HashMap<Integer, Integer>();
	
	public Set<String> stopList = new HashSet<String>();
	public String corpusPath = "corpus/";
	
	public int iCol ;
	public int iRow ;
	
	public CreateMatrix(){
		iCol = 0;
		iRow = 0;
		loadStopList();
	}
	
	public Matrix Create(){
		
		CreateKeyWordMap(corpusPath);	
		Matrix mtx = new Matrix(iRow,iCol,0.0);
		
		for (String doc_name : docList.keySet()) {
			String filename = corpusPath+doc_name;
			wordCount.put(docList.get(doc_name), 0);
			
			BufferedReader br;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(filename),"utf-8"));
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] strs = line.split("-|"+MyStaticValue.SEPERATOR_E_BLANK);
					
					for (String word : strs) {
						word = word.toLowerCase();
						
						if(wordList.containsKey(word)){
							mtx.set(wordList.get(word), docList.get(doc_name), 
									mtx.get(wordList.get(word), docList.get(doc_name))+1);
							wordCount.put(docList.get(doc_name), wordCount.get(docList.get(doc_name))+1);
						}
					}
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
		}
		//MyStaticValue.PrintMatrix(mtx);
		return mtx;
	}
	
	//创建单词列表
	public int CreateKeyWordMap(String path){
		List<File> fileList = new ArrayList<File>();
				
		try {
			MyStaticValue.visitDirsAllFiles(new File(path),fileList);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int wordIndex = 0;
		for (File file : fileList) {
			BufferedReader br;
			try {
				br = new BufferedReader(new InputStreamReader(new FileInputStream(file),"utf-8"));
			
				String line = null;
				while ((line = br.readLine()) != null) {
					String[] strs = line.split("-|"+MyStaticValue.SEPERATOR_E_BLANK);
					for (String word : strs) { 						
						//小写
						word = word.toLowerCase();						
						//停用词
						if (!stopList.contains(word)) {
							if (!wordList.containsKey(word)) {
								wordList.put(word, wordIndex);
								wordIndex++;
							} 
						}
					}
				}
				br.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}  
			docList.put(file.getName(), iCol);
			iCol++;
		}
		iRow = wordList.size();
		return 0;
	}
	
	//停用词
	public void loadStopList(){
		BufferedReader br;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream("stopwords.txt"),"utf-8"));
			String temp;
			while((temp = br.readLine()) != null)
			{
				stopList.add(temp);
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public Matrix createTfIdfMatrix(){
		
		Matrix mtx = Create();
		
		for (int i = 0; i < wordList.size() ; i++)
		{
			for (int j = 0; j < docList.size(); j++)
			{
				if (mtx.get(i,j)!=0)
				{
					double termfrequence = mtx.get(i,j)/wordCount.get(j);
					double idf = Math.log((double)docList.size()/(double)getDocumentFrequence(mtx,i));
					mtx.set(i,j,termfrequence*idf);
				}
			   //printf ("m(%d,%d) = %g\t", i, j, mtx.get(i,j));
			}
		} 
		return mtx;
	}
	
	public int getDocumentFrequence(Matrix mtx,int wordId){
		int fre=0;
		for (int i=0;i<docList.size();i++)
		{
			if (mtx.get(wordId,i)!=0)
				fre++;
		}
		return fre;
	}
	
	public String FindDocName(int id){
		for (String doc_name : docList.keySet()) {
			if (docList.get(doc_name) == id) {
				return doc_name;
			}
		}
		return "";		
	}
}
