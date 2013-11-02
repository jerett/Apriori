package apriori;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Apriori {
	private File outputFile;
	private File inputFile;
	//最小支持度，用0-1的比例来表示.
	private double mimumSupportValue;
	//输入文件的记录
	private List<Set<String>> records;
	private List<List<ItemSet>> resultList;
	private int itemCount;

	public Apriori(File inputFile,File outputFile,double mimumSupportVaule){
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.mimumSupportValue = mimumSupportVaule;
		this.itemCount = 0;
		this.records = new ArrayList<Set<String>>();
		this.resultList = new ArrayList<List<ItemSet>>();
	}
	
	public void mine(){
		init();
		if(mimumSupportValue <= (1.0/itemCount)){
			System.err.println("supportValue小于每项记录除以总记录数!");
			System.exit(0);
		}
		//第一次初始化
		List<ItemSet> kRoundResult = new ArrayList<Apriori.ItemSet>();
		for(Set<String> record:records){
			for(String recordItem:record){
				ItemSet itemSet = new ItemSet();
				itemSet.addItem(recordItem);
				if(!kRoundResult.contains(itemSet)){
					kRoundResult.add(itemSet);
				}
			}
		}
		count(kRoundResult);
		prun(kRoundResult);
		
		List<ItemSet> temp = new ArrayList<Apriori.ItemSet>();
		temp.addAll(kRoundResult);
		//迭代
		while(kRoundResult.size() > 0){
			resultList.add(kRoundResult);
			kRoundResult = new ArrayList<Apriori.ItemSet>();
			Collections.sort(temp);
			//找到可以合并，生成Ck+1
			for(int i=0;i<temp.size()-1;++i){
				ItemSet firtSet = temp.get(i);
				for(int j=i+1;j<temp.size();++j){
					ItemSet secondSet = temp.get(j);
					ItemSet mergeItemSet = firtSet.merge(secondSet);
					if(mergeItemSet != null){
						if(!kRoundResult.contains(mergeItemSet)){
							kRoundResult.add(mergeItemSet);
						}
					}
				}
			}
			count(kRoundResult);
			prun(kRoundResult);
			temp.clear();
			temp.addAll(kRoundResult);
		}
		
		output();
	}
	
	//输出结果到指定文件中
	private void output(){
		try {
			PrintWriter writer = new PrintWriter(outputFile);
			for(int i=0;i<resultList.size();++i){
				writer.println("#############L"+(i+1)+"##############");
				for(ItemSet itemSet:resultList.get(i)){
					writer.println(itemSet);
				}
			}
			writer.close();
		} catch (FileNotFoundException e) {
			System.out.println("输出文件发生错误!");
		}
	}
	
	
	//计数
	private void count(List<ItemSet> setList){
		for(Set<String> record:records){
			for(ItemSet itemSet:setList){
				if(record.containsAll(itemSet.itemSet)){
					itemSet.incSupport();
				}
			}
		}
	}
	
	//剪枝
	private void prun(List<ItemSet> setList){
		for(Iterator<ItemSet> iter = setList.iterator(); iter.hasNext();){
			ItemSet set = iter.next();
			if(set.supportValue/(itemCount+0.0) <mimumSupportValue){
				iter.remove();
			}
		}
	}
	
	//从输入文件中读取记录，假设记录之间由空格隔开
	private void init(){
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
			String line = null;
			while((line = reader.readLine()) != null){
				if(!line.isEmpty()){
					String[] data = line.split(" ");
					Set<String> record = new TreeSet<String>();
					for(String itemData:data){
						record.add(itemData);
					}
					itemCount ++;
					records.add(record);
				}
			}
			reader.close();
		} catch (FileNotFoundException e) {
			System.err.println("输入文件没有找到!");
		} catch (IOException e) {
			System.err.println("文件读取错误！");
		}
	}

	public static class ItemSet implements Comparable<ItemSet>{
		Set<String> itemSet;
		int supportValue;
		
		ItemSet(){
			itemSet = new TreeSet<String>();
			this.supportValue = 0;
		}
		
		public void addItem(String item){
			itemSet.add(item);
		}
		
		//检查是否可以合并
		public ItemSet merge(ItemSet other){
			if(other.itemSet.size() != itemSet.size()) return null;
			ItemSet newItem = new ItemSet();
			Iterator<String> selfIter = itemSet.iterator();
			Iterator<String> otherIter = other.itemSet.iterator();
			int i =0;
			while(i < itemSet.size()-1){
				String selfStr = selfIter.next();
				String otherStr = otherIter.next();
				if(!selfStr.equals(otherStr)) return null;
				i++;
				newItem.itemSet.add(selfStr);
			}
			String selfLastStr = selfIter.next();
			String otherLastStr = otherIter.next();
			if(selfLastStr.compareTo(otherLastStr) > 0) return null;
			newItem.addItem(selfLastStr);
			newItem.addItem(otherLastStr);
			return newItem;
		}
		
		public String toString(){
			String toStr = "";
			for(String str:itemSet){
				toStr += str;
				toStr += " ";
			}
			toStr += supportValue;
			return toStr;
		}
		
		public boolean equals(Object o){
			if(o == this) return true;
			if(!(o instanceof ItemSet)) return false;
			ItemSet other = (ItemSet)o;
			return other.itemSet.equals(itemSet);
		}
		
		public void incSupport(){
			supportValue ++;
		}
		
		public int hashCode(){
			return itemSet.hashCode();
		}

		//实际按字典序排序
		@Override
		public int compareTo(ItemSet o) {
			if(this.equals(o)) return 0;
			Iterator<String> firstIter = itemSet.iterator();
			Iterator<String> secondIter = o.itemSet.iterator();
			while(firstIter.hasNext() && secondIter.hasNext()){
				String firstStr = firstIter.next();
				String secondStr = secondIter.next();
				int code = firstStr.compareTo(secondStr);
				if(code < 0) return -1;
				else if (code > 0) return 1;
			}
			if(firstIter.hasNext()){
				return 1;
			}else{
				return -1;
			}
		}
	}
	
	public static void main(String[] args) {
		new Apriori(new File("input"), new File("output"), 0.12).mine();
	}
}
